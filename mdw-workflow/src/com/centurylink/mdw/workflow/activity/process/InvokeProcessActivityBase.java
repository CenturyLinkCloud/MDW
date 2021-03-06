package com.centurylink.mdw.workflow.activity.process;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.InvokeProcessActivity;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.workflow.activity.AbstractWait;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This abstract activity implementor implements the common funciton
 * for invocation of sub processes, whether it is single, multiple or heterogeneous
 *
 *
 */

public abstract class InvokeProcessActivityBase extends AbstractWait implements InvokeProcessActivity {

    protected static final String SYNCHRONOUS = "synchronous";

    public final boolean resumeWaiting(InternalEvent msg)
            throws ActivityException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            lockActivityInstance();
            if (allSubProcessCompleted()) {
                return true;
            } else {
                EventWaitInstance received = registerWaitEvents(true);
                if (received!=null) {
                    this.setReturnCode(received.getCompletionCode());
                    processOtherMessage(getExternalEventInstanceDetails(received.getMessageDocumentId()));
                    handleEventCompletionCode();
                    return true;
                } else return false;
            }
        } catch (Exception e) {
            throw new ActivityException(-1, e.getMessage(), e);
        } finally {
            stopTransaction(transaction);
        }
    }

    abstract protected boolean allSubProcessCompleted() throws Exception;

    /**
     * This method is invoked to process a received event (other than subprocess termination).
     * You will need to override this method to customize processing of the event.
     *
     * The default method does nothing.
     *
     * The status of the activity after processing the event is configured at design time, which
     * can be either Hold or Waiting.
     *
     * When you override this method, you can optionally set different completion
     * code from those configured at design time by calling setReturnCode().
     *
     * @param msg the entire message content of the external event (from document table)
     * @throws ActivityException
     */
    protected void processOtherMessage(String msg)
        throws ActivityException {
    }

    /**
     * This method is a hook for custom processing, to be called
     * when subprocesses are completed. The default method
     * does nothing.
     * @throws ActivityException
     */
    protected void onFinish() throws ActivityException {
    }

    @Deprecated
    boolean resume_on_other_event(String msg, String compCode) throws ActivityException {
        return resumeOnOtherEvent(msg, compCode);
    }

    protected boolean resumeOnOtherEvent(String msg, String compCode)
            throws ActivityException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            lockActivityInstance();
            this.setReturnCode(compCode);
            processOtherMessage(msg);
            handleEventCompletionCode();
            return true;
        } finally {
            stopTransaction(transaction);
        }
    }

    protected abstract boolean resumeOnProcessFinish(InternalEvent msg, Integer status)
    throws ActivityException;

    public final boolean resume(InternalEvent msg)
        throws ActivityException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            Integer status = lockActivityInstance();
            if (msg.isProcess()) {
                boolean done = resumeOnProcessFinish(msg, status);
                if (done)
                    onFinish();
                return done;
            } else {
                String messageString = this.getMessageFromEventMessage(msg);
                setReturnCode(msg.getCompletionCode());
                processOtherMessage(messageString);
                handleEventCompletionCode();
                return true;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    protected boolean allowInput(Variable childVar) {
        int varCat = childVar.getVariableCategory();
        return varCat == Variable.CAT_INPUT || varCat == Variable.CAT_INOUT || varCat == Variable.CAT_STATIC;
    }

    protected String evaluateBindingValue(Variable childVar, String v) {
        if (v != null && v.length() > 0) {
            int varCat = childVar.getVariableCategory().intValue();
            if (varCat != Variable.CAT_STATIC) {
                if (valueIsVariable(v)) {
                    VariableInstance varinst = getVariableInstance(v.substring(1));
                    v = varinst == null ? null : varinst.getStringValue(getPackage());
                }
                else if (v.startsWith("${") && v.endsWith("}") && v.indexOf('.') == -1 && v.indexOf('[') == -1) {
                    VariableInstance varinst = getVariableInstance(v.substring(2, v.length() - 1));
                    v = varinst == null ? null : varinst.getStringValue(getPackage());
                }
                else {
                    try {
                        if (valueIsJavaExpression(v)) {
                            Object obj = evaluateExpression(getActivityId().toString(), JAVA_EL, v);
                            v = obj == null ? null : obj.toString();
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to evaluate the expression '" + v + "'", e);
                        // treat v as it is
                    }
                }
            } // else v is static value
        }
        return v;
    }

    protected Map<String,String> getOutputParameters(Long subprocInstId, Long subprocId)
        throws IOException, DataAccessException {
        Process subprocDef = ProcessCache.getProcess(subprocId);
        Map<String,String> params = null;
        for (Variable var : subprocDef.getVariables()) {
            if (var.getVariableCategory() == Variable.CAT_OUTPUT || var.getVariableCategory() == Variable.CAT_INOUT) {
                VariableInstance vi = getEngine().getVariableInstance(subprocInstId, var.getName());
                if (vi != null) {
                    if (params == null)
                        params = new HashMap<>();
                    params.put(var.getName(), vi.getStringValue(getPackage()));
                }
            }
        }
        return params;
    }

    /**
     * TODO: Allow expressions that resolve to a version/spec.
     */
    protected Process getSubprocess(String name, String verSpec) throws DataAccessException {
        try {
            Process match = ProcessCache.getProcessSmart(new AssetVersionSpec(name, verSpec));
            if (match == null)
                throw new DataAccessException("Unable to find process definition for " + name + " v" + verSpec);
            return match;
        } catch (IOException ex) {
            throw new DataAccessException("Cannot load " + name + " v" + verSpec, ex);
        }

    }
}
