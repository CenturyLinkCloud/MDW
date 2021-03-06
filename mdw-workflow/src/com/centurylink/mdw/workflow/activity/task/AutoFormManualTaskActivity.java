package com.centurylink.mdw.workflow.activity.task;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

@Tracked(LogLevel.TRACE)
@Activity(value="Autoform Manual Task", category=TaskActivity.class, icon="com.centurylink.mdw.base/task.png",
        pagelet="com.centurylink.mdw.base/autoformTask.pagelet")
public class AutoFormManualTaskActivity extends ManualTaskActivity {

    /**
     * Creates a task instance unless the INSTANCE_ID_VAR attribute points
     * to a pre-existing instance.  If the attribute is populated but the variable
     * value is null, the variable will be set to the newly-created instanceId.
     */
    @Override
    public void execute() throws ActivityException {
        Long instanceId = null;  // pre-existing instanceId
        String instanceIdSpec = getInstanceIdVariable();
        if (instanceIdSpec != null) {
            Object value = getValue(instanceIdSpec);
            if (value instanceof Long)
                instanceId = (Long) value;
            else if (value != null)
                instanceId = Long.parseLong(value.toString());
        }
        try {
            if (instanceId == null) {
                TaskInstance taskInstance = createTaskInstance();
                instanceId = taskInstance.getTaskInstanceId();
                if (instanceIdSpec != null)
                    setValue(instanceIdSpec, instanceId);
            }
            else {
                if (getTaskInstance(instanceId) == null)
                    throw new ActivityException("Task instance not found: " + instanceId);
                // update secondary owner
                updateOwningTransition(instanceId);
            }

            String taskInstCorrelationId = TaskAttributeConstant.TASK_CORRELATION_ID_PREFIX + instanceId;
            logInfo("Task instance created - ID " + instanceId);
            if (this.needSuspend()) {
                getEngine().createEventWaitInstance(getProcessInstanceId(), getActivityInstanceId(), taskInstCorrelationId,
                        EventType.EVENTNAME_FINISH, true, true);
                EventWaitInstance received = registerWaitEvents(false);
                if (received!=null)
                        resume(getExternalEventInstanceDetails(received.getMessageDocumentId()),
                                         received.getCompletionCode());
            }
        } catch (Exception e) {
            throw new ActivityException(-1, e.getMessage(), e);
        }
    }

    protected String getWaitEvent() throws ActivityException {
        try {
            TaskInstance taskInst = ServiceLocator.getTaskServices().getTaskInstanceForActivity(getActivityInstanceId());
            return TaskAttributeConstant.TASK_CORRELATION_ID_PREFIX + taskInst.getTaskInstanceId();
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    protected boolean messageIsTaskAction(String messageString) throws ActivityException {
        try {
            JSONObject jsonobj = new JsonObject(messageString);
            JSONObject meta = jsonobj.has("META")?jsonobj.getJSONObject("META"):null;
            if (meta==null || !meta.has(TaskAttributeConstant.TASK_ACTION)) return false;
            String action = meta.getString(TaskAttributeConstant.TASK_ACTION);
            return action!=null && action.startsWith("@");
        } catch (JSONException e) {
            throw new ActivityException("Failed to parse JSON message", e);
        }
    }

    protected void processTaskAction(String messageString) throws ActivityException {
        try {
            JSONObject datadoc = new JsonObject(messageString);
            String compCode = extractFormData(datadoc); // this handles both embedded proc and not
            datadoc = datadoc.getJSONObject("META");
            String action = datadoc.getString(TaskAttributeConstant.TASK_ACTION);
            CallURL callurl = new CallURL(action);
            action = callurl.getAction();
            if (compCode==null) compCode = datadoc.has(TaskAttributeConstant.URLARG_COMPLETION_CODE) ? datadoc.getString(TaskAttributeConstant.URLARG_COMPLETION_CODE) : null;
            if (compCode==null) compCode = callurl.getParameter(TaskAttributeConstant.URLARG_COMPLETION_CODE);
            String subaction = datadoc.has(TaskAttributeConstant.URLARG_ACTION) ? datadoc.getString(TaskAttributeConstant.URLARG_ACTION) : null;
            if (subaction==null) subaction = callurl.getParameter(TaskAttributeConstant.URLARG_ACTION);
            if (this.getProcessInstance().isEmbedded() || this.getProcessInstanceOwner().equals("ERROR")) {
                if (subaction==null)
                    subaction = compCode;
                if (action.equals("@CANCEL_TASK")) {
                    if (TaskAction.ABORT.equalsIgnoreCase(subaction))
                        compCode = EventType.EVENTNAME_ABORT + ":process";
                    else compCode = EventType.EVENTNAME_ABORT;
                } else {    // FormConstants.ACTION_COMPLETE_TASK
                    if (TaskAction.RETRY.equalsIgnoreCase(subaction))
                        compCode = TaskAction.RETRY;
                    else if (compCode==null) compCode = EventType.EVENTNAME_FINISH;
                    else compCode = EventType.EVENTNAME_FINISH + ":" + compCode;
                }
                this.setProcessInstanceCompletionCode(compCode);
                setReturnCode(null);
            } else {
                if (action.equals("@CANCEL_TASK")) {
                    if (TaskAction.ABORT.equalsIgnoreCase(subaction))
                        compCode = WorkStatus.STATUSNAME_CANCELLED + "::" + EventType.EVENTNAME_ABORT;
                    else compCode = WorkStatus.STATUSNAME_CANCELLED + "::";
                    setReturnCode(compCode);
                } else {    // FormConstants.ACTION_COMPLETE_TASK
                    setReturnCode(compCode);
                }
            }
        } catch (Exception e) {
            String errmsg = "Failed to parse task completion message";
            logger.error(errmsg, e);
            throw new ActivityException(-1, errmsg, e);
        }
    }

    /**
     * This method is used to extract data from the message received from the task manager.
     * The method updates all variables specified as non-readonly
     *
     * @param datadoc
     * @return completion code
     */
    protected String extractFormData(JSONObject datadoc)
            throws ActivityException, JSONException {
        List<String[]> parsed = getAttributes().getTable(TaskActivity.ATTRIBUTE_TASK_VARIABLES, ',', ';', 5);
        for (String[] one : parsed) {
            String varname = one[0];
            String displayOption = one[2];
            if (displayOption.equals(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED)) continue;
            if (displayOption.equals(TaskActivity.VARIABLE_DISPLAY_READONLY)) continue;
            if (varname.startsWith("#{") || varname.startsWith("${")) continue;
            String data = datadoc.has(varname) ? datadoc.getString(varname) : null;
            setDataToVariable(varname, data);
        }
        return null;
    }

    protected void setDataToVariable(String datapath, String value)
            throws ActivityException {
        if (value == null || value.length() == 0)
            return;
        // w/o above, hit oracle constraints that variable value must not be null
        // shall we consider removing that constraint? and shall we check
        // if the variable should be updated?
        String type = this.getParameterType(datapath);
        if (type == null)
            return; // ignore data that is not a variable
        if (getPackage().getTranslator(type).isDocumentReferenceVariable())
            setParameterValueAsDocument(datapath, type, value);
        else
            setParameterValue(datapath, value);
    }
}
