package com.centurylink.mdw.services.process;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.activity.types.SuspendableActivity;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Attributes;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.model.workflow.WorkStatus.InternalLogMessage;
import com.centurylink.mdw.monitor.ActivityMonitor;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.monitor.OfflineMonitor;
import com.centurylink.mdw.pkg.PackageClassLoader;
import com.centurylink.mdw.script.*;
import com.centurylink.mdw.service.data.activity.ImplementorCache;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.service.data.process.EngineDataAccessCache;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.OfflineMonitorTrigger;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.TrackingTimer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class that implements the Controlled Activity.
 * All the controlled activities should extend this class.
 */
public abstract class BaseActivity implements GeneralActivity {

    /**
     * Replaced during execution by ActivityLogger.
     */
    protected StandardLogger logger = LoggerUtil.getStandardLogger();
    /**
     * Use this logger for activity output to make it appear in the activity_log.
     */
    protected StandardLogger getLogger() { return logger; }

    public static final String JAVASCRIPT = "JavaScript";
    public static final String GROOVY = "Groovy";
    public static final String JAVA_EL = "javax.el";
    public static final String OUTPUTDOCS = "Output Documents";
    public static final String DISABLED = "disabled";

    private Long workTransitionInstanceId;
    private String returnCode;
    private String returnMessage;
    private List<VariableInstance> parameters;
    private Attributes attributes;
    private ProcessExecutor engine;
    private TrackingTimer timer;
    private String[] outputDocuments;
    private Activity activityDef;
    private ProcessInstance processInstance;
    private ActivityInstance activityInstance;
    private ActivityRuntimeContext _runtimeContext;
    private Package pkg;

    /**
     * Repopulates variable values in case they've changed during execution.
     */
    public ActivityRuntimeContext getRuntimeContext() {
        for (VariableInstance var : getParameters()) {
            try {
                _runtimeContext.getValues().put(var.getName(), getVariableValue(var.getName()));
            }
            catch (ActivityException ex) {
                logger.error("Error populating " + var.getName(), ex);
            }
        }
        return _runtimeContext;
    }

    /**
     * This version is used by the new engine (ProcessExecuter).
     * @param parameters variable instances of the process instance,
     *    or of the parent process instance when this is in an embedded process
     */
    void prepare(Activity actVO, ProcessInstance pi, ActivityInstance ai, List<VariableInstance> parameters,
            Long transInstId, TrackingTimer timer, ProcessExecutor engine) {
        try {
            if (timer != null)
                timer.start("Prepare Activity");
            this.engine = engine;
            this.processInstance = pi;
            this.activityDef = actVO;
            this.activityInstance = ai;
            this.workTransitionInstanceId = transInstId;
            this.parameters = parameters;
            this.attributes = actVO.getAttributes();
            this.timer = timer;
            try {
                pkg = PackageCache.getPackage(getMainProcessDefinition().getPackageName());
                ActivityImplementor implementor = ImplementorCache.get(activityDef.getImplementor());
                String category = implementor == null ? GeneralActivity.class.getName() : implementor.getCategory();
                StandardLogger activityLogger = LoggerUtil.getStandardLogger(getClass().getName());
                _runtimeContext = new ActivityRuntimeContext(activityLogger, pkg, getProcessDefinition(), processInstance,
                        getPerformanceLevel(), getEngine().isInService(), activityDef, category, activityInstance,
                        this instanceof SuspendableActivity);
                if (!(logger instanceof ActivityLogger))
                    logger = new ActivityLogger(_runtimeContext);
                for (VariableInstance var : getParameters()) {
                    try {
                        _runtimeContext.getValues().put(var.getName(), getVariableValue(var.getName()));
                    }
                    catch (ActivityException ex) {
                        logger.error("Error populating " + var.getName(), ex);
                    }
                }
            }
            catch (NullPointerException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        finally {
            if (timer != null)
                timer.stopAndLogTiming();
        }
    }

    /**
     * This version is used to initialize adapter activities to be called
     * from API (not from runtime engine) -- mainly for unit testing
     */
    public void prepare(ActivityRuntimeContext runtimeContext) {
        if (!(logger instanceof ActivityLogger))
            logger = new ActivityLogger(runtimeContext);

        EngineDataAccess edao = EngineDataAccessCache.getInstance(true, 9);
        // InternalMessenger msgBroker = MessengerFactory.newInternalMessenger();
        engine = new ProcessExecutor(edao, null, false);

        this.processDefinition = this.mainProcessDefinition = runtimeContext.getProcess();
        this.processInstance = runtimeContext.getProcessInstance();
        this.pkg = runtimeContext.getPackage();
        this._runtimeContext = runtimeContext;
        this.activityDef = runtimeContext.getActivity();
        this.activityInstance = runtimeContext.getActivityInstance();

        if (runtimeContext.getAttributes() != null) {
            attributes = new Attributes();
            for (String attrName : runtimeContext.getAttributes().keySet())
                attributes.put(attrName, runtimeContext.getAttribute(attrName));
        }

        if (runtimeContext.getValues() != null) {
            parameters = new ArrayList<>();
            for (String varName : runtimeContext.getValues().keySet())
                parameters.add(new VariableInstance(varName, runtimeContext.getValues().get(varName)));
        }
    }

    /**
     * Provides a hook to allow activity implementors to initialize themselves.
     * @param runtimeContext the activity runtime context
     */
    protected void initialize(ActivityRuntimeContext runtimeContext) throws ActivityException {
        // TODO consider funneling all client runtime info access
    }

    protected VariableInstance getVariableInstance(String pName) {
        for (int i = 0; i < parameters.size(); i++) {
            if (pName.equalsIgnoreCase(parameters.get(i).getName())) {
                return parameters.get(i);
            }
        }
        return null;
    }

    void execute(ProcessExecutor engine) throws ActivityException {
        this.engine = engine;
        if (isDisabled()) {
            loginfo("Skipping disabled activity: " + getActivityName());
        }
        else {
            try {
                initialize(_runtimeContext);
                Object ret = execute(_runtimeContext);
                if (ret != null)
                    setReturnCode(String.valueOf(ret));
            }
            finally
            {
                if (Thread.currentThread().getContextClassLoader() instanceof PackageClassLoader)
                    ApplicationContext.resetContextClassLoader();
            }
        }
    }

    void executeTimed(ProcessExecutor engine) throws ActivityException {
        try {
            if (timer != null)
                timer.start("Execute Activity");
            execute(engine);
        }
        finally {
            if (timer != null)
                timer.stopAndLogTiming();
        }
    }

    /**
     * Executes the workflow activity.
     * This method is the main method that subclasses need to override.
     * The implementation in the default implementation does nothing.
     */
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        // compatibility dictates that the default implementation delegate to execute()
        execute();
        return runtimeContext.getCompletionCode();
    }

    /**
     * Return the activity instance ID
     * @return activity instance ID
     */
    protected Long getActivityInstanceId() {
        return activityInstance.getId();
    }

    protected ActivityInstance getActivityInstance() {
        return activityInstance;
    }

    /**
     * Return the owner type of the process instance.
     * The owner is typically an external event or another process
     * instance, but can be other things as well.
     * @return the owner type
     */
    protected String getProcessInstanceOwner() {
        return this.processInstance.getOwner();
    }

    /**
     * Return the ID of the owner of the process instance.
     * The owner is typically an external event or another process
     * instance, but can be other things as well.
     * @return the process instance owner ID.
     */
    protected Long getProcessInstanceOwnerId() {
        return this.processInstance.getOwnerId();
    }

    /**
     * Return the process instance ID
     * @return process instance ID
     */
    protected Long getProcessInstanceId() {
        return processInstance.getId();
    }

    /**
     * This method is used internally by MDW.
     * @return the ID of the transition instance leading to the activity
     *      instance.
     */
    protected Long getWorkTransitionInstanceId() {
        return workTransitionInstanceId;
    }

    /**
     * Return the activity (definition) ID
     * @return activity ID
     */
    protected Long getActivityId() {
        return this.activityDef.getId();
    }

    /**
     * Return the name of the activity (definition).
     * @return name of the activity
     */
    protected String getActivityName() {
        return this.activityDef.getName();
    }

    /**
     * Return the process (definition) ID
     * @return process ID
     */
    protected Long getProcessId() {
        return this.processInstance.getProcessId();
    }


    /**
     * Return the master request ID.
     * @return master request ID
     */
    protected String getMasterRequestId() {
        return this.processInstance.getMasterRequestId();
    }

    /**
     * Return the return code, a.k.a completion code
     * @return the return code.
     */
    protected String getReturnCode() {
        return this.returnCode;
    }
    protected void setReturnMessage(String pMessage) {
        this.returnMessage = pMessage;
    }
    protected String getReturnMessage() {
        return this.returnMessage;
    }
    protected void setReturnCode(String code) {
        this.returnCode = code;
    }

    protected void setProcessInstanceCompletionCode(String code)
            throws ActivityException {
        try {
            if (code!=null) {
                if (code.equals(TaskAction.CANCEL)) code = EventType.EVENTNAME_ABORT;
                else if (code.equals(TaskAction.ABORT))
                    code = EventType.EVENTNAME_ABORT + ":process";
                else if (code.equals(TaskAction.COMPLETE)) code = EventType.EVENTNAME_FINISH;
                else if (code.equals(TaskAction.RETRY )) code = EventType.EVENTNAME_START;
                else code = EventType.EVENTNAME_FINISH + ":" + code;
            }
            engine.setProcessInstanceCompletionCode(getProcessInstanceId(), code);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ActivityException(0, ex.getMessage(),ex);
        }
    }

    protected ProcessInstance getProcessInstance() {
        return this.processInstance;
    }

    private Process processDefinition;
    protected Process getProcessDefinition() {
        if (processDefinition == null) {
            if (processInstance.getInstanceDefinitionId() > 0L)
                processDefinition = ProcessCache.getInstanceDefinition(processInstance.getProcessId(), processInstance.getInstanceDefinitionId());
            if (processDefinition == null) {
                try {
                    processDefinition = ProcessCache.getProcess(processInstance.getProcessId());
                } catch (IOException ex) {
                    logger.error("Error loading process definition for instance " + processInstance.getId(), ex);
                }
            }
            if (processInstance.isEmbedded() && processDefinition != null)
                processDefinition = processDefinition.getSubProcess(new Long(processInstance.getComment()));
        }
        return processDefinition;
    }

    private Process mainProcessDefinition;
    protected Process getMainProcessDefinition() {
        if (mainProcessDefinition == null) {
            if (processInstance.getInstanceDefinitionId() > 0L)
                mainProcessDefinition = ProcessCache.getInstanceDefinition(processInstance.getProcessId(), processInstance.getInstanceDefinitionId());
            if (mainProcessDefinition == null) {
                try {
                    mainProcessDefinition = ProcessCache.getProcess(processInstance.getProcessId());
                } catch (IOException ex) {
                    logger.error("Error loading main definition for process instance " + processInstance.getId(), ex);
                }
            }
        }
        return mainProcessDefinition;
    }

    /**
     * @deprecated use {@link #getValue(String)}
     */
    @Deprecated
    protected Object getParameterValue(String name) {
        VariableInstance var = getVariableInstance(name);
        return var == null ? null : var.getData(getPackage());
    }

    /**
     * @deprecated {@link #getValue(String)}
     */
    @Deprecated
    protected String getParameterStringValue(String name) {
        VariableInstance var = getVariableInstance(name);
        return var == null ? null : var.getStringValue(getPackage());
    }

    /**
     * Get the variable instance ID from its name.
     * Despite the name of the method, the method works for
     * all variables, not just parameters of the process instances.
     * @param name variable name
     * @return the variable instance ID
     */
    protected Long getParameterId(String name) {
        for (int i = 0; i < parameters.size(); i++) {
            if (name.equalsIgnoreCase(parameters.get(i).getName())) {
                return parameters.get(i).getId();
            }
        }
        return null;
    }

    /**
     * Get the variable instance ID from its name.
     * Despite the name of the method, the method works for
     * all variables, not just parameters of the process instances.
     * @param name variable name
     * @return the variable instance ID
     */
    protected String getParameterType(String name) {
        for (VariableInstance varinst : parameters) {
            if (varinst.getName().equals(name)) return varinst.getType();
        }
        Process procdef = getMainProcessDefinition();
        List<Variable> vs = procdef.getVariables();
        String varName;
        for (int i=0; i<vs.size(); i++) {
            varName = vs.get(i).getName();
            if (varName.equals(name))
                return vs.get(i).getType();
        }
        return null;
    }

    /**
     * @deprecated {@link #getValues()}
     */
    @Deprecated
    protected List<VariableInstance> getParameters() {
        return parameters;
    }

    @Deprecated
    protected void setParameterValues(Map<String,Object> map)
            throws ActivityException {
        for (String name : map.keySet()) {
            setParameterValue(name, map.get(name));
        }
    }

    /**
     * Set the value of the variable instance.
     * @param name variable name
     * @param value variable value; the value must not be null or
     *    empty string, which will cause database not-null constraint
     *    violation
     * @return variable instance ID
     */
    protected Long setParameterValue(String name, Object value)
            throws ActivityException {
        Long varInstId;
        try {
            VariableInstance varInst = this.getVariableInstance(name);
            if (varInst != null) {
                varInstId = varInst.getId();
                varInst.setData(value);
                engine.updateVariableInstance(varInst, getPackage());
            } else {
                varInst = engine.createVariableInstance(processInstance, name, value);  // This also adds it to ProcessInstance
                varInstId = varInst.getId();
                if (!parameters.contains(varInst))  // Should already be there when added to ProcessInstance
                    parameters.add(varInst);    // This adds to ProcessInstanceVO as well - do not think this ever executes
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ActivityException(0, ex.getMessage(),ex);
        }
        return varInstId;
    }

    /**
     * Set the value of the variable as a document reference to the given
     * document. If the variable is already bound to a document reference,
     * the method updates the content of the referred document.
     * The method will throw an exception if the document reference points to
     * a remote document.
     * @param name name
     * @param variableType variable type
     * @param value this is the document
     */
    protected DocumentReference setParameterValueAsDocument(String name, String variableType, Object value)
            throws ActivityException {
        DocumentReference docref = (DocumentReference)getParameterValue(name);
        if (docref == null) {
            docref = createDocument(variableType, value, OwnerType.VARIABLE_INSTANCE, 0L);
            Long varInstId = setParameterValue(name, docref);
            updateDocumentInfo(docref, value.getClass().getName(), OwnerType.VARIABLE_INSTANCE, varInstId);
        } else {
            updateDocumentContent(docref, value, variableType);
            if (!value.getClass().getName().equals(variableType))
                updateDocumentInfo(docref, value.getClass().getName(), null, null);
        }
        return docref;
    }

    /**
     * Method that returns the external event instance data
     *
     * @param externalEventInstId instance id
     * @return EventDetails as String
     */
    protected String getExternalEventInstanceDetails(Long externalEventInstId)
            throws ActivityException {
        DocumentReference docref = new DocumentReference(externalEventInstId);
        Document docvo;
        try {
            docvo = engine.getDocument(docref, false);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
        return docvo==null?null:docvo.getContent(getPackage());
    }

    protected Attributes getAttributes() {
        return attributes;
    }

    /**
     * Get the value of the attribute with the given name
     * @param name attribute name
     * @return the attribute value
     */
    protected String getAttributeValue(String name) {
        return attributes.get(name);
    }

    @Deprecated
    protected String getVariableValueSmart(String name) throws PropertyException {
        return getValueSmart(name,name);
    }

    /**
     * Same as getAttributeValue() except performing translations in
     * the following cases:
     * <ul>
     *  <li>If the value starts with &ldquo;<code>prop:</code>&rdquo;, treat it as a property
     *   specification and return the property value.
     *   The property specification has the syntax
     *  &ldquo;<code>prop:<i>property-group</i>/<i>property-name</i></code>&rdquo;.</li>
     *  <li>If the value starts with a dollar sign followed by an identifier,
     *    it is treated as a variable name and the method returns the variable instance
     *    value as the result.</li>
     *  <li>If the value starts with <code>string:</code>,
     *    the rest of the value is returned without interpretation.
     *    This may be needed when the literal string contains special
     *    characters that may be misinterpreted as expression.</li>
     *  <li>If the value starts with <code>magic:</code>,
     *      the rest of the value is considered an expression
     *      in Magic Box language. It evaluates the expression and return the result.
     *      If it fails to evaluate, the method throws a PropertyException.</li>
     *  <li>If the value contains { and }, and the text in between is a variable
     *    (a dollar sign followed by variable name), then the value is
     *    obtained by replacing the place holders by corresponding variable values</li>
     *  <li>If the value looks like a Magic expression (if it contains dollar sign
     *      or colon), evaluate the expression and return the result.
     *      If it fails to evaluate, return the original value (instead
     *      of throwing an exception in
     *      the case when the value starts with <code>magic:</code>).</li>
     *  <li>If none of above applies, return the value as it is.</li>
     * </ul>
     *
     * @param name name
     * @return attribute value (literal or translated).
     * @throws PropertyException if a translation rule is applied
     *      and translation fails.
     */

    protected String getAttributeValueSmart(String name) {
        return getValueSmart(attributes.get(name), "A:" + name);
    }

    /**
     * @deprecated {@link #getValue(String)}
     */
    @Deprecated
    protected String getValueSmart(String value, String tag) {

        if (value==null) return null;
        if (value.startsWith("prop:")) {
            value = this.getProperty(value.substring(5));
        } else if (valueIsVariable(value)) {
            Object valueObj = this.getParameterValue(value.substring(1).trim());
            value = valueObj == null ? null : valueObj.toString();
        } else if (value.startsWith("string:")) {
            value = value.substring(7);
        } else if (value.startsWith("groovy:") || value.startsWith("g:") || value.startsWith("javascript:") || value.startsWith("js:")) {
            String name = ScriptNaming.getValidName(getActivityName() + "_" + getActivityId() + "_" + tag);
            Object obj = evaluateExpression(name, value.startsWith("j") ? JAVASCRIPT : GROOVY, value.substring(value.indexOf(':') + 1));
            value = obj == null ? null : obj.toString();
        } else if (valueIsPlaceHolder(value)) {
            value = this.translatePlaceHolder(value);
        } else if (valueIsJavaExpression(value)) {
            Object obj = evaluateExpression(tag, JAVA_EL, value);
            value = obj == null ? null : obj.toString();
        }
        return value == null ? null : value.trim();
    }

    /**
     * General-purpose expression evaluation.
     * Language is one of Groovy, JavaScript or (optionally) Kotlin.
     * Variables for this activity instance are bound.
     */
    protected Object evaluateExpression(String name, String language, String expression)
            throws ExecutionException {
        if (getPerformanceLevel() < 9)
            _runtimeContext.setLogPersister(ActivityLogger::persist);
        try {
            if (JAVA_EL.equals(language)) {
                return _runtimeContext.evaluate(expression);
            }
            ScriptEvaluator evaluator = getScriptEvaluator(name, language);
            Process processVO = getMainProcessDefinition();
            List<Variable> varVOs = processVO.getVariables();
            Map<String,Object> bindings = new HashMap<>();
            bindings.put("runtimeContext", _runtimeContext);
            for (Variable varVO: varVOs) {
                Object value = getParameterValue(varVO.getName());
                if (value instanceof DocumentReference) {
                    DocumentReference docref = (DocumentReference) value;
                    value = getDocument(docref, varVO.getType());
                }
                bindings.put(varVO.getName(), value);
            }
            return evaluator.evaluate(expression, bindings);
        }
        catch (ActivityException ex) {
            throw new ExecutionException(ex.getMessage(), ex);
        }
    }

    protected ScriptEvaluator getScriptEvaluator(String name, String language) throws ExecutionException {
        if (language == null)
            throw new NullPointerException("Missing script evaluator language");
        ScriptEvaluator evaluator = ExecutorRegistry.getInstance().getEvaluator(language);
        evaluator.setName(name);
        return evaluator;
    }

    /**
     * The method checks if a string is of the form of a variable reference,
     * i.e. a dollar sign followed by an identifier.
     * This is typically used to check if an attribute specifies a variable.
     * @param v value
     * @return true if the argument is a dollar sign followed by an identifier
     */
    protected boolean valueIsVariable(String v) {
        if (v==null || v.length()<2) return false;
        if (v.charAt(0)!='$') return false;
        for (int i=1; i<v.length(); i++) {
            char ch = v.charAt(i);
            if (!Character.isLetterOrDigit(ch) && ch!='_') return false;
        }
        return true;
    }

    /**
     * Checks if the string contains #{ or ${ with a corresponding closing }
     * to determine if it is a Java expression
     */
    protected boolean valueIsJavaExpression(String v) {
        if (v == null)
            return false;
        int start = v.indexOf("#{") >= 0 ? v.indexOf("#{") : v.indexOf("${");
        return ((start != -1) && (start < v.indexOf('}')));
    }

    private boolean valueIsPlaceHolder(String v) {
        int n = v.length();
        for (int i=0; i<n; i++) {
            char ch = v.charAt(i);
            if (ch=='{') {
                int k = i+1;
                while (k<n) {
                    ch = v.charAt(k);
                    if (ch=='}') break;
                    k++;
                }
                if (k<n) {
                    if (v.charAt(i+1)=='$') {
                        String varname = v.substring(i+2,k).trim();
                        boolean isIdentifier = true;
                        for (int j=0; isIdentifier && j<varname.length(); j++) {
                            ch = varname.charAt(j);
                            if (!Character.isLetterOrDigit(ch) && ch!='_') isIdentifier = false;
                        }
                        if (isIdentifier) return true;
                    }
                } // else  '{' without '}' - ignore string after '{'
                i = k;
            }
        }
        return false;
    }

    /**
     * The method translates place holders for attributes such as
     * event names.
     * TODO: combine this with getAttributeValueSmart?
     * @param eventName event name
     */
    protected String translatePlaceHolder(String eventName) {
        // honor java EL expressions
        if (this.valueIsJavaExpression(eventName)) {
            try {
                Object o = evaluateExpression(getActivityId().toString(), JAVA_EL, eventName);
                return o == null ? "" : o.toString();
            }
            catch (ExecutionException ex) {
                logger.error(ex.getMessage(), ex);
                return "";
            }
        }
        int k, i, n;
        StringBuffer sb = new StringBuffer();
        n = eventName.length();
        for (i=0; i<n; i++) {
            char ch = eventName.charAt(i);
            if (ch=='{') {
                k = i+1;
                while (k<n) {
                    ch = eventName.charAt(k);
                    if (ch=='}') break;
                    k++;
                }
                if (k<n) {
                    String expression = eventName.substring(i+1,k);
                    String value;
                    if (this.valueIsVariable(expression)) {
                        String varname = expression.substring(1);
                        if (varname.equalsIgnoreCase(Variable.PROCESS_INSTANCE_ID)) value = this.getProcessInstanceId().toString();
                        else if (varname.equalsIgnoreCase(Variable.MASTER_REQUEST_ID)) value = this.getMasterRequestId();
                        else if (varname.equalsIgnoreCase(Variable.ACTIVITY_INSTANCE_ID)) value = this.getActivityInstanceId().toString();
                        else {
                            Object binding = this.getParameterValue(varname);
                            if (binding!=null) value = binding.toString();
                            else value = "";
                        }
                    } else {
                        try {
                            value = (String)evaluateExpression(getActivityId().toString()+":"+expression, GROOVY, expression);
                        } catch (ExecutionException ex) {
                            logwarn("Exception in evaluating expression " + expression + ": " + ex.getMessage());
                            value = "";
                        }
                    }
                    sb.append(value);
                } // else  '{' without '}' - ignore string after '{'
                i = k;
            } else if (ch == '\\' ) {
                ++i;
                sb.append(eventName.charAt(i));

            } else sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Given a document reference object (typically bound to a
     * document variable), returns the actual document object.
     */
    protected Object getDocument(DocumentReference docRef, String variableType) throws ActivityException {
        Document doc;
        try {
            doc = engine.getDocument(docRef, false);
            // deserialize if needed
            return doc.getObject(variableType, getPackage());
        } catch (Exception ex) {
            logexception("Error retrieving " + docRef, ex);
            throw new ActivityException("Error retrieving " + docRef, ex);
        }
    }

    /**
     * Returns variable type if document does not exist.
     */
    protected String getDocumentType(String variableName) throws ActivityException {
        Variable variable = getMainProcessDefinition().getVariable(variableName);
        if (variable == null)
            throw new ActivityException("Variable not defined: " + variableName);
        VariableInstance variableInstance = getVariableInstance(variableName);
        if (variableInstance == null)
            return variable.getType();
        if (!variableInstance.isDocument(getPackage()))
            throw new ActivityException("Variable is not a document: " + variableName);
        try {
            DocumentReference docRef = new DocumentReference(variableInstance.getStringValue(getPackage()));
            return engine.getDocument(docRef, false).getType();
        } catch (DataAccessException ex) {
            throw new ActivityException("Error retrieving document for: " + variableName, ex);
        }
    }

    protected String getDocumentContent(DocumentReference docRef)
            throws ActivityException {
        Document doc;
        try {
            doc = engine.getDocument(docRef, false);
            return doc == null ? null : doc.getContent(getPackage());
        } catch (Exception ex) {
            logexception("Error retrieving " + docRef, ex);
            throw new ActivityException("Error retrieving " + docRef, ex);
        }
    }

    /**
     * Same as getDocument() but also puts a write lock on it.
     * The lock will be released at the end of the transaction.
     *
     * @param docRef document reference
     */
    protected Object getDocumentForUpdate(DocumentReference docRef, String variableType) throws ActivityException {
        Document doc;
        try {
            doc = engine.getDocument(docRef, true);
            // deserialize here to restore runtime type
            Object obj = getPackage().getObjectValue(variableType, doc.getContent(getPackage()), true, doc.getType());
            doc.setObject(obj);
            doc.setVariableType(variableType);
        } catch (Exception ex) {
            logexception("Error retrieving " + docRef, ex);
            throw new ActivityException("Error retrieving " + docRef, ex);
        }
        return doc.getObject(variableType, getPackage());
    }

    protected DocumentReference createDocument(String variableType, Object docObj, String ownerType,
            Long ownerId) throws ActivityException {
        return createDocument(variableType, docObj, ownerType, ownerId, null, null, null);
    }

    protected DocumentReference createDocument(String docType, Object docObj, String ownerType,
            Long ownerId, String path) throws ActivityException {
        return createDocument(docType, docObj, ownerType, ownerId, null, null, path);
    }

    protected DocumentReference createDocument(String variableType, Object document, String ownerType,
            Long ownerId, Integer statusCode, String statusMessage) throws ActivityException {
        return createDocument(variableType, document, ownerType, ownerId, statusCode, statusMessage, null);
    }

    /**
     * Create a new document, persisted in database, and return a document
     * reference object, to be bound to a variable.
     *
     * @param variableType type of the document (i.e. variable type)
     * @param docObj document object itself
     * @param ownerType owner type. More than likely this will be OwnerType.VARIABLE_INSTANCE
     *      other possible types are LISTENER_REQUEST, LISTENER_RESPONSE,
     *      ADAPTOR_REQUEST, ADAPTOR_RESPONSE
     * @param ownerId ID of the owner, dependent on owner type.
     * @param statusCode status code
     * @param statusMessage message
     * @return document reference
     */
    protected DocumentReference createDocument(String variableType, Object docObj, String ownerType,
            Long ownerId, Integer statusCode, String statusMessage, String path) throws ActivityException {
        try {
            return engine.createDocument(variableType, ownerType, ownerId, statusCode, statusMessage, path, docObj, getPackage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Update the content (actual document object) bound to the given
     * document reference object.
     * @param docRef document reference
     * @param docObj document
     */
    protected void updateDocumentContent(DocumentReference docRef, Object docObj, String variableType)
            throws ActivityException {
        try {
            if (!(docObj instanceof String)) {
                // serialize here TODO: leftover support for package aware translator providers
                docObj = getPackage().getStringValue(variableType, docObj, true);
            }
            engine.updateDocumentContent(docRef, docObj, getPackage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    protected void updateDocumentInfo(DocumentReference docref, String documentType,
            String ownerType, Long ownerId) throws ActivityException {
        updateDocumentInfo(docref, documentType, ownerType, ownerId, null, null);
    }

    /**
     * Update document information (everything but document content itself).
     * The method will update only the arguments that have non-null values.
     */
    protected void updateDocumentInfo(DocumentReference docref, String documentType,
            String ownerType, Long ownerId, Integer statusCode, String statusMessage) throws ActivityException {
        try {
            engine.updateDocumentInfo(docref, documentType, ownerType, ownerId, statusCode, statusMessage);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }


    protected Package getPackage() {
        return pkg;
    }

    /**
     * Get MDW system property value (not java system property value,
     * which can be retrieved using System.getProperty), using configured
     * property manager.
     *
     * If package-specific properties are defined, then those will be used.
     *
     * MDW allows to supply a custom property manager in place of the out-of-box
     * property manager, using java system property "property_manager", which
     * takes the class name of the custom property manager as its value.
     *
     * @param propertyName property name
     * @return value of the property, or null if the property does not exist.
     */
    protected String getProperty(String propertyName) {
        return PropertyManager.getProperty(propertyName);
    }

    /**
     * @return completion code if onExecute() returns non-null
     */
    String notifyMonitors(InternalLogMessage logMessage) {
        try {
            for (ActivityMonitor monitor : MonitorRegistry.getInstance().getActivityMonitors(_runtimeContext)) {
                Map<String,Object> updates = null;

                // Document variables are up-to-date as of activity start.
                // Since there is no guaranteed order to the multiple monitors at this point, there cannot be an expectation to keep
                // the runtimeContext process variables map up-to-date from one monitor to the next.
                // TODO Implement a way to determine priority/order when having multiple monitors

                if (monitor.isOffline()) {
                    @SuppressWarnings("unchecked")
                    OfflineMonitor<ActivityRuntimeContext> activityOfflineMonitor = (OfflineMonitor<ActivityRuntimeContext>) monitor;
                    new OfflineMonitorTrigger<>(activityOfflineMonitor, _runtimeContext).fire(logMessage);
                }
                else {
                    if (logMessage == InternalLogMessage.ACTIVITY_START) {
                        updates = monitor.onStart(_runtimeContext);
                    }
                    else if (logMessage == InternalLogMessage.ACTIVITY_EXECUTE) {
                        String compCode = monitor.onExecute(_runtimeContext);
                        if (compCode != null) {
                            loginfo("Activity short-circuited by monitor: " + monitor.getClass().getName() + " with code: " + compCode);
                            return compCode;
                        }
                    }
                    else if (logMessage == InternalLogMessage.ACTIVITY_COMPLETE) {
                        updates = monitor.onFinish(_runtimeContext);
                    }
                    else if (logMessage == InternalLogMessage.ACTIVITY_FAIL) {
                        monitor.onError(_runtimeContext);
                    }
                    else if (logMessage == InternalLogMessage.ACTIVITY_SUSPEND) {
                        monitor.onSuspend(_runtimeContext);
                    }
                }

                if (updates != null) {
                    for (String varName : updates.keySet()) {
                        loginfo("Variable: " + varName + " updated by ActivityMonitor: " + monitor.getClass().getName());
                        setVariableValue(varName, updates.get(varName));
                    }
                }
            }
        }
        catch (Exception ex) {
            logexception(ex.getMessage(), ex);
        }
        return null;
    }

    public void logInfo(String message) {
        _runtimeContext.logInfo(message);
    }
    /**
     * @deprecated  use {@link #logInfo(String)}
     */
    @Deprecated
    public void loginfo(String message) {
        logInfo(message);
    }

    public void logDebug(String message) {
        _runtimeContext.logDebug(message);
    }
    /**
     * @deprecated  use {@link #logDebug(String)}
     */
    @Deprecated
    public void logdebug(String message) {
        logDebug(message);
    }

    public void logWarn(String message) {
        _runtimeContext.logWarn(message);
    }
    /**
     * @deprecated  use {@link #logWarn(String)}
     */
    @Deprecated
    public void logwarn(String message) {
        logWarn(message);
    }

    public void logError(String message) {
        _runtimeContext.logError(message);
    }
    /**
     * @deprecated  use {@link #logError(String)}
     */
    @Deprecated
    public void logsevere(String message) {
        logError(message);
    }

    public void logError(String msg, Throwable t) {
        _runtimeContext.logError(msg, t);
    }
    /**
     * @deprecated  use {@link #logError(String, Throwable)}
     */
    @Deprecated
    public void logexception(String msg, Exception e) {
        logError(msg, e);
    }

    public boolean isLogInfoEnabled() {
        return _runtimeContext.isLogInfoEnabled();
    }

    public boolean isLogDebugEnabled() {
        return _runtimeContext.isLogDebugEnabled();
    }

    protected Integer lockActivityInstance() throws ActivityException {
        try {
            return engine.lockActivityInstance(this.getActivityInstanceId());
        } catch (Exception e) {
            throw new ActivityException(-1, "Failed to lock activity instance", e);
        }
    }

    protected Integer lockProcessInstance() throws ActivityException {
        try {
            return engine.lockProcessInstance(this.getProcessInstanceId());
        } catch (Exception e) {
            throw new ActivityException(-1, "Failed to lock process instance", e);
        }
    }

    TrackingTimer getTimer() {
        return timer;
    }

    protected ProcessExecutor getEngine() {
        return engine;
    }

    /**
     * Executes a script, passing additional bindings to be made available to the script.
     * Script should return a value for the result code if the default (null is not desired).
     *
     * @param script - the script content
     * @param language - built-in support for Groovy, and JavaScript (default is Groovy)
     */
    protected Object executeScript(String script, String language, Map<String,Object> addlBindings, String qualifier)
            throws ActivityException {

        if (getPerformanceLevel() < 9)
            _runtimeContext.setLogPersister(ActivityLogger::persist);

        outputDocuments = attributes.containsKey(OUTPUTDOCS) ? attributes.getList(OUTPUTDOCS).toArray(new String[0]) : new String[0];
        Object retObj;
        try {
            Process process = getMainProcessDefinition();
            List<Variable> vars = process.getVariables();
            Map<String,Object> bindings = new HashMap<>();
            for (Variable var: vars) {
                try {
                    bindings.put(var.getName(), getVariableValue(var.getName()));
                }
                catch (ActivityException ex) {
                    logger.error("Error populating " + var.getName(), ex);
                }
            }
            bindings.put("runtimeContext", _runtimeContext);

            if (addlBindings != null) {
                bindings.putAll(addlBindings);
            }

            ScriptExecutor executor = getScriptExecutor(language, qualifier);
            retObj = executor.execute(script, bindings);

            for (Variable var : vars) {
                String variableName = var.getName();
                Object bindValue = bindings.get(variableName);
                String varType = var.getType();
                Object value = bindValue;
                if (varType.equals("java.lang.String") && value != null)
                    value = value.toString();  // convert to string
                setVariableValue(variableName, varType, value);
            }
        }
        catch (Exception ex) {
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
        return retObj;
    }

    protected ScriptExecutor getScriptExecutor(String language, String qualifier) throws PropertyException {
        if (language == null)
            throw new NullPointerException("Missing script executor language");
        ScriptExecutor executor = ExecutorRegistry.getInstance().getExecutor(language);
        executor.setName(getScriptExecClassName(qualifier));
        return executor;
    }

    protected String getScriptExecClassName(String qualifier) {
        String name = getProcessDefinition().getLabel() + "_" + getActivityName() + "_" + getActivityId();
        if (qualifier != null)
            name += "_" + qualifier;
        return ScriptNaming.getValidName(name);
    }

    /**
     * @deprecated {@link #getValue(String)}
     */
    @Deprecated
    protected Object getVariableValue(String varName) throws ActivityException {
        VariableInstance var = getVariableInstance(varName);
        if (var == null)
            return null;
        Object value = var.getData(getPackage());
        if (var.isDocument(pkg)) {
            DocumentReference docref = (DocumentReference)value;
            if (isOutputDocument(varName))
                value = getDocumentForUpdate(docref, var.getType());
            else
                value = getDocument(docref, var.getType());
        }
        return value;
    }

    /**
     * Convenience method to set variable value regardless of type.
     */
    protected void setVariableValue(String varName, Object value) throws ActivityException {
        Variable variable = getProcessDefinition().getVariable(varName);
        if (variable == null)
            throw new ActivityException("No such variable defined for process: " + varName);
        String varType = variable.getType();
        if (getPackage().getTranslator(varType).isDocumentReferenceVariable())
            setParameterValueAsDocument(varName, varType, value);
        else
            setParameterValue(varName, value);
    }

    /**
     * Convenience method that sets a variable value, including document content.
     * Meant to be used by Script, Dynamic Java and other activities where it is
     * not known whether the value of a document has changed after execution.
     * Note: to update a document value through this method it must be included in getOutputDocuments().
     * @param varName variable name
     * @param varType variable type
     * @param value new value to set
     */
    protected void setVariableValue(String varName, String varType, Object value) throws ActivityException {
        if (value == null)
            return;
        if (pkg.getTranslator(varType).isDocumentReferenceVariable()) {
            try {
                boolean isOutputDoc = isOutputDocument(varName);
                boolean doUpdate = isOutputDoc;

                // don't check in production (or for cache-only perf level since old values are not retained)
                if (!ApplicationContext.isProduction() && (getPerformanceLevel() < 5)) {
                    boolean changed = hasDocumentValueChanged(varName, varType, value);

                    if (changed){
                        if (!isOutputDoc) {
                            String msg = "*** WARNING: Attempt to change value of non-output document '" + varName + "'";
                            if (Object.class.getName().equals(varType))
                                msg += ".  Make sure to implement equals().";
                            logwarn(msg);
                        }
                    }
                    else {
                        doUpdate = false;
                    }
                }

                if (doUpdate) {
                    setParameterValueAsDocument(varName, varType, value);
                }
            }
            catch (DataAccessException ex) {
                throw new ActivityException(ex.getMessage(), ex);
            }
        }
        else {
            this.setParameterValue(varName, value);
        }
    }

    public int getPerformanceLevel() {
        return getEngine().getPerformanceLevel();
    }

    public String[] getOutputDocuments() { return outputDocuments; }
    public void setOutputDocuments(String[] outputDocs) { this.outputDocuments = outputDocs; }

    protected boolean isOutputDocument(String variableName) {
        if (outputDocuments == null)
            return false;
        for (String outputDoc : outputDocuments) {
            if (outputDoc.equals(variableName))
                return true;
        }
        return false;
    }

    private boolean hasDocumentValueChanged(String varName, String varType, Object newValue) throws DataAccessException {
        DocumentReference docRef = (DocumentReference) getParameterValue(varName);

        if (docRef == null)
            return newValue != null;

        Document doc = getEngine().loadDocument(docRef, false);

        if (doc == null || doc.getContent(getPackage()) == null)
            return newValue != null;

        if (newValue == null)
            return true;  // we already know old value is not null


        String docType = newValue.getClass().getName();
        String oldString = doc.getContent(getPackage());
        Object oldObject = getPackage().getObjectValue(varType, oldString, true, docType);

        if (doc.getType().equals(Object.class.getName()))
            return !newValue.equals(oldObject);

        // general comparison involves reserializing since round-trip results are not guaranteed
        oldString = getPackage().getStringValue(varType, oldObject, true);
        String newString = getPackage().getStringValue(varType, newValue, true);
        return !newString.equals(oldString);
    }

    public TransactionWrapper startTransaction() throws ActivityException {
        try {
            return engine.startTransaction();
        } catch (DataAccessException e) {
            throw new ActivityException(0, e.getMessage(), e);
        }
    }

    public void stopTransaction(TransactionWrapper transaction) throws ActivityException {
        try {
            engine.stopTransaction(transaction);
        } catch (DataAccessException e) {
            throw new ActivityException(0, e.getMessage(), e);
        }
    }

    protected boolean isDisabled() throws ActivityException {
        try {
            return "true".equalsIgnoreCase(getAttributeValueSmart(DISABLED));
        }
        catch (PropertyException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Get a runtime value.
     * @param name can be a variable name or expression
     */
    protected Object getValue(String name) throws ActivityException {
        if (ProcessRuntimeContext.isExpression(name)) {
            return getRuntimeContext().evaluate(name);
        }
        else {
            return getVariableValue(name);
        }
    }

    protected Map<String,Object> getValues() {
        return _runtimeContext.getValues();
    }

    /**
     * Set a runtime value.
     * @param name can be a variable name or expression
     * @param value the value to set
     */
    protected void setValue(String name, Object value) throws ActivityException {
        if (ProcessRuntimeContext.isExpression(name)) {
            // create or update document variable referenced by expression
            ActivityRuntimeContext runtimeContext = getRuntimeContext();
            runtimeContext.set(name, value);
            String rootVar = name.substring(2, name.indexOf('.'));
            Variable doc = runtimeContext.getProcess().getVariable(rootVar);
            String stringValue = runtimeContext.getPackage().getStringValue(doc.getType(), runtimeContext.evaluate("${" + rootVar + "}"));
            setParameterValueAsDocument(rootVar, doc.getType(), stringValue);
        }
        else {
            setVariableValue(name, value);
        }
    }

    public Object getRequiredVariableValue(String name) throws ActivityException {
        if (getMainProcessDefinition().getVariable(name) == null)
            throw new ActivityException("Missing process variable: " + name);
        return getVariableValue(name);
    }

    protected String getAttribute(String name) {
        String v = getAttributeValueSmart(name);
        return v == null || v.isEmpty() ? null : v;
    }

    protected boolean getAttribute(String name, Boolean defaultValue) {
        String v = getAttribute(name);
        return v == null ? defaultValue : v.equalsIgnoreCase("true");
    }

    protected int getAttribute(String name, Integer defaultValue) {
        String v = getAttribute(name);
        return v == null ? defaultValue : Integer.parseInt(v);
    }

    protected String getAttribute(String name, String defaultValue) {
        String v = getAttribute(name);
        return v == null ? defaultValue : v;
    }

    protected String getRequiredAttribute(String name) throws ActivityException {
        String v = getAttribute(name);
        if (v == null)
            throw new ActivityException("Missing required attribute: " + name);
        return v;
    }
}
