package com.centurylink.mdw.workflow.activity.event;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.process.CompletionCode;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;

@Tracked(LogLevel.TRACE)
@Activity(value="Event Check Activity", category=EventWaitActivity.class, icon="com.centurylink.mdw.base/check.png",
        pagelet="com.centurylink.mdw.base/eventCheck.pagelet")
public class EventCheckActivity extends EventWaitActivity {

    private Integer exitStatus;

    /**
     * Method that executes the logic based on the work
     */
    public void execute() throws ActivityException {
        EventWaitInstance received = registerWaitEvents(false);
        if (received!=null) {
            setCompCodeAndExitStatus(received.getCompletionCode());
            processMessage(getExternalEventInstanceDetails(received.getMessageDocumentId()));
            boolean toFinish = handleCompletionCode();
            if (toFinish && exitStatus==null)
                exitStatus = WorkStatus.STATUS_COMPLETED;
        } else {
            exitStatus = WorkStatus.STATUS_COMPLETED;
            setReturnCode(null);
        }
    }


    public boolean needSuspend() {
        return !WorkStatus.STATUS_COMPLETED.equals(exitStatus);
    }

    protected void setCompCodeAndExitStatus(String code) {
        CompletionCode compcode = new CompletionCode();
        compcode.parse(code);
        exitStatus = compcode.getActivityInstanceStatus();
        if (compcode.getEventType().equals(EventType.FINISH)) {
            setReturnCode(compcode.getCompletionCode());
        } else {
            setReturnCode(compcode.getEventTypeName() + ":" +
                    (compcode.getCompletionCode()==null?"":compcode.getCompletionCode()));
        }
    }

    protected boolean handleCompletionCode() throws ActivityException {
        String compCode = this.getReturnCode();
        if (compCode!=null && (compCode.length()==0||compCode.equals(EventType.EVENTNAME_FINISH)))
            compCode = null;
        String actInstStatusName;
        if (exitStatus==null) actInstStatusName = null;
        else if (exitStatus.equals(WorkStatus.STATUS_CANCELLED)) actInstStatusName = WorkStatus.STATUSNAME_CANCELLED;
        else if (exitStatus.equals(WorkStatus.STATUS_WAITING)) actInstStatusName = WorkStatus.STATUSNAME_WAITING;
        else if (exitStatus.equals(WorkStatus.STATUS_HOLD)) actInstStatusName = WorkStatus.STATUSNAME_HOLD;
        else actInstStatusName = null;
        if (actInstStatusName!=null) {
            if (compCode==null) compCode = actInstStatusName + "::";
            else compCode = actInstStatusName + "::" + compCode;
        }
        setReturnCode(compCode);
        if (WorkStatus.STATUS_WAITING.equals(exitStatus)) {
            this.registerWaitEvents(true);
            return compCode != null && (compCode.startsWith(WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_CORRECT)
                    || compCode.startsWith(WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_ABORT)
                    || compCode.startsWith(WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_ERROR));
        } else return true;
    }

    /**
     * Typically you will not override this method
     */
    public boolean resumeWaiting(InternalEvent eventMessageDoc) throws ActivityException {
        if (WorkStatus.STATUSNAME_COMPLETED.equalsIgnoreCase(eventMessageDoc.getCompletionCode())){
            setReturnCode(null);
            return true;
        }
        EventWaitInstance received = registerWaitEvents(true);
        if (received!=null) {
            setCompCodeAndExitStatus(received.getCompletionCode());
            processMessage(getExternalEventInstanceDetails(received.getMessageDocumentId()));
            return handleCompletionCode();
        } else {
            setReturnCode(null);
            return true;
        }
    }


}
