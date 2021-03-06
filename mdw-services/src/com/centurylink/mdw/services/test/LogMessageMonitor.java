package com.centurylink.mdw.services.test;

import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.model.workflow.WorkStatus.InternalLogMessage;
import com.centurylink.mdw.soccom.SoccomServer;
import com.centurylink.mdw.util.log.LoggerUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogMessageMonitor extends SoccomServer {

    private Pattern activityPattern;
    private Pattern procPattern;
    private Map<String,Object> waitingObjects;
    private Map<String,String> procInstMasterRequestMap;

    public LogMessageMonitor() throws IOException {
        this(LoggerUtil.getStandardLogger().getDefaultPort());
    }

    public LogMessageMonitor(int port) throws IOException {
        super(String.valueOf(port), (PrintStream)null);
        activityPattern = Pattern.compile("\\[\\(.\\)([0-9.:]+) p([0-9]+)\\.([0-9]+) a([0-9]+)\\.([0-9]+)\\] (.*)", 0);
        procPattern = Pattern.compile("\\[\\(.\\)([0-9.:]+) p([0-9]+)\\.([0-9]+) m.([^\\]]+)\\] (.*)", 0);
        waitingObjects = new HashMap<>();
        procInstMasterRequestMap = new HashMap<>();
    }

    public String createKey(String masterRequestId, Long processId, Long activityId, String status) {
        return masterRequestId + ":" + processId.toString() + ":" + activityId.toString() + ":" + status;
    }

    public void register(Object obj, String key) {
        synchronized (waitingObjects) {
            waitingObjects.put(key, obj);
        }
    }

    public Object remove(String key) {
        synchronized (waitingObjects) {
            return waitingObjects.remove(key);
        }
    }

    private String parseActivityStatus(String msg) {
        InternalLogMessage internalMessage = InternalLogMessage.match(msg);
        if (internalMessage != null) {
            return internalMessage.statusName;
        }
        else {
            return null;
        }
    }

    @SuppressWarnings("squid:S2446")
    private void checkWaitCondition(String procInstId, String procId, String actId, String status) {
        String masterRequestId = procInstMasterRequestMap.get(procInstId);
        if (masterRequestId == null)
            return;
        String key = masterRequestId + ":" + procId + ":" + actId + ":" + status;
        Object obj = remove(key);
        if (obj != null) {
            synchronized (obj) {
                obj.notify();
            }
        }
    }

    protected void handleActivityMatch(Matcher activityMatcher) {
        String msg = activityMatcher.group(6);
        String status = parseActivityStatus(msg);
        if (status != null) {
            checkWaitCondition(activityMatcher.group(3),
                activityMatcher.group(2), activityMatcher.group(4), status);
        }
    }

    protected void handleProcessMatch(Matcher procMatcher) {
        String processInstId = procMatcher.group(3);
        String msg = procMatcher.group(5);

        InternalLogMessage internalMessage = InternalLogMessage.match(msg);
        if (internalMessage != null) {
            if (internalMessage == InternalLogMessage.PROCESS_START) {
                procInstMasterRequestMap.put(processInstId, procMatcher.group(4));
            }
            else if (internalMessage == InternalLogMessage.PROCESS_COMPLETE) {
                checkWaitCondition(processInstId, procMatcher.group(2), "0", WorkStatus.STATUSNAME_COMPLETED);
            }
        }
    }

    @Override
    protected void requestProc(String threadId, String msgId, byte[] msg, int msgSize, OutputStream out) {
        String msgstr = new String(msg, 0, msgSize);
        handleMessage(msgstr);
    }

    protected synchronized void handleMessage(String message) {
        Matcher procMatcher = procPattern.matcher(message);
        Matcher activityMatcher = activityPattern.matcher(message);
        if (activityMatcher.matches()) {
            handleActivityMatch(activityMatcher);
        }
        else if (procMatcher.matches()) {
            handleProcessMatch(procMatcher);
        }
    }

}
