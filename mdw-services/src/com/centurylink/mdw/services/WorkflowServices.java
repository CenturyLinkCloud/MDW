package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.event.Event;
import com.centurylink.mdw.model.report.Hotspot;
import com.centurylink.mdw.model.report.Insight;
import com.centurylink.mdw.model.report.Timepoint;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.util.log.ActivityLog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface WorkflowServices {

    Map<String, String> getAttributes(String ownerType, Long ownerId) throws ServiceException;

    /**
     * Replace <b>all</b> attributes for this ownerId
     *
     * @param ownerType
     * @param ownerId    Id of owner
     * @param attributes new attributes to add
     * @throws ServiceException
     */
    void setAttributes(String ownerType, Long ownerId, Map<String, String> attributes) throws ServiceException;

    /**
     * <p>
     * Update specific attributes <b>without</b> clearing all attributes first
     * <p>
     * This method can be used to update a subset of attributes without
     * removing <b>all</b> attributes for this ownerId first
     * </p>
     *
     * @param ownerType
     * @param ownerId
     * @param attributes
     * @throws ServiceException
     */
    void updateAttributes(String ownerType, Long ownerId, Map<String, String> attributes) throws ServiceException;

    Map<String, String> getValues(String ownerType, String ownerId) throws ServiceException;

    VariableInstance getVariableInstance(long processInstanceId, String variableName) throws ServiceException;

    /**
     * Replace <b>all</b> values for this ownerId
     *
     * @param ownerType
     * @param ownerId   Id of owner
     * @param values    new values to add
     * @throws ServiceException
     */
    void setValues(String ownerType, String ownerId, Map<String, String> values) throws ServiceException;

    /**
     * Update specific values for this ownerId
     *
     * @param ownerType
     * @param ownerId   Id of owner
     * @param values    new values to add
     * @throws ServiceException
     */
    void updateValues(String ownerType, String ownerId, Map<String, String> values) throws ServiceException;

    /**
     * Get ValueHolder IDs for the specified name and pattern
     *
     * @param valueName
     * @param valuePattern can be a value or a patter with wildcards (*)
     */
    List<String> getValueHolderIds(String valueName, String valuePattern) throws ServiceException;

    /**
     * Get ValueHolder IDs for the specified name, pattern and ownerType
     *
     * @param valueName
     * @param valuePattern can be a value or a patter with wildcards (*)
     * @param ownerType    the value holder type
     */
    List<String> getValueHolderIds(String valueName, String valuePattern, String ownerType) throws ServiceException;

    void registerTaskWaitEvent(Long taskInstanceId, Event event)
            throws ServiceException;

    void registerTaskWaitEvent(Long taskInstanceId, String eventName)
            throws ServiceException;

    /**
     * @param taskInstanceId (Task Instance Id for Task)
     * @param eventName
     * @param completionCode null for default outcome
     * @return
     * @throws ServiceException
     */
    void registerTaskWaitEvent(Long taskInstanceId, String eventName,
            String completionCode) throws ServiceException;

    /**
     * @param activityInstanceId
     * @param action
     * @param completionCode
     */
    void actionActivity(Long activityInstanceId, String action, String completionCode, String uer)
            throws ServiceException;

    ProcessInstance getProcess(Long instanceId) throws ServiceException;
    ProcessInstance getProcess(Long instanceId, boolean withSubprocs) throws ServiceException;
    ActivityLog getProcessLog(Long instanceId, boolean withActivities) throws ServiceException;
    ActivityLog getProcessLog(Long processInstanceId, Long[] activityInstanceIds) throws ServiceException;

    ProcessInstance getProcessForTrigger(Long triggerId) throws ServiceException;

    /**
     * If multiple matches, returns latest.
     */
    ProcessInstance getMasterProcess(String masterRequestId) throws ServiceException;

    ProcessRuntimeContext getContext(Long instanceId) throws ServiceException;

    ProcessRuntimeContext getContext(Long instanceId, Boolean embeddedVars) throws ServiceException;

    Map<String, Value> getProcessValues(Long instanceId, boolean includeEmpty) throws ServiceException;

    Map<String, Value> getProcessValues(Long instanceId) throws ServiceException;

    /**
     * name can be an expression
     */
    Value getProcessValue(Long instanceId, String name) throws ServiceException;

    ProcessList getProcesses(Query query) throws ServiceException;
    long getProcessCount(Query query) throws ServiceException;

    ActivityList getActivities(Query query) throws ServiceException;

    List<ProcessAggregate> getTopProcesses(Query query) throws ServiceException;

    TreeMap<Instant,List<ProcessAggregate>> getProcessBreakdown(Query query) throws ServiceException;

    List<Insight> getProcessInsights(Query query) throws ServiceException;

    List<Timepoint> getProcessTrend(Query query) throws ServiceException;

    List<Hotspot> getProcessHotspots(Query query) throws ServiceException;

    List<ActivityAggregate> getTopActivities(Query query) throws ServiceException;

    TreeMap<Instant,List<ActivityAggregate>> getActivityBreakdown(Query query) throws ServiceException;

    ActivityInstance getActivity(Long instanceId) throws ServiceException;
    ActivityLog getActivityLog(Long instanceId) throws ServiceException;

    /**
     * Launch a process asynchronously
     */
    Long startProcess(String name, String masterRequestId, String ownerType,
            Long ownerId, Map<String,Object> values) throws ServiceException;
    Long startProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String,Object> values) throws ServiceException;

    /**
     * @deprecated use {@link #startProcess(String, String, String, Long, Map)}
     */
    @Deprecated
    Long launchProcess(String name, String masterRequestId, String ownerType,
            Long ownerId, Map<String,Object> values) throws ServiceException;

    /**
     * @deprecated use {@link #startProcess(String, String, String, Long, Map)}
     */
    @Deprecated
    Long launchProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String,String> values) throws ServiceException;


    /**
     * Invoke a service process synchronously.
     */
    Response invokeProcess(String name, Object masterRequest, String masterRequestId,
            Map<String,Object> values, Map<String,String> headers) throws ServiceException;

    /**
     * Invoke a service process synchronously.
     * responseHeaders will be populated from process value, if any
     */
    Response invokeProcess(String name, Object masterRequest, String masterRequestId,
            Map<String,Object> values, Map<String,String> headers, Map<String,String> responseHeaders) throws ServiceException;

    /**
     * Invoke a service process synchronously.
     */
    Response invokeProcess(String name, String masterRequestId, String ownerType,
            Long ownerId, Map<String,Object> values, Map<String,String> headers) throws ServiceException;
    Response invokeProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String,Object> values, Map<String,String> headers) throws ServiceException;

    /**
     * @deprecated use {@link #invokeProcess(String, Object, String, Map, Map, Map)}
     */
    @Deprecated
    Response invokeServiceProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> values, Map<String,String> headers) throws ServiceException;
    /**
     * @deprecated use {@link #invokeProcess(String, Object, String, Map, Map, Map)}
     */
    @Deprecated
    Response invokeServiceProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> values, Map<String,String> headers, Map<String,String> responseHeaders) throws ServiceException;

    /**
     * @deprecated user {@link #invokeProcess(Process, String, String, Long, Map, Map)}
     */
    @Deprecated
    Response invokeServiceProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String,String> values, Map<String,String> headers) throws ServiceException;

    Integer notify(String event, String message, int delay) throws ServiceException;

    Integer notify(Package runtimePackage, String eventName, Object eventMessage) throws ServiceException;

    Integer notify(Package runtimePackage, String eventName, Object eventMessage, int delay) throws ServiceException;

    void setVariable(Long processInstanceId, String varName, Object value) throws ServiceException;
    void setVariable(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;

    void setVariables(Long processInstanceId, Map<String, Object> values) throws ServiceException;
    void setVariables(ProcessRuntimeContext context, Map<String, Object> values) throws ServiceException;

    void setDocumentValue(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;

    void createDocument(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;

    void updateDocument(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;

    Document getDocument(Long id) throws ServiceException;

    /**
     * Converts a document to a string, applying a consistent format for XML and JSON.
     * Use when comparing document values (such as in Automated Tests).
     */
    String getDocumentStringValue(Long id, String variableType, Package pkg) throws ServiceException;

    ProcessRun runProcess(ProcessRun runRequest) throws ServiceException;

    void createProcess(String assetPath, Query query) throws ServiceException;

    /**
     * Retrieve process definition for a specific instance from the document table.
     * Quickly returns null if no such definition exists.
     */
    Process getInstanceDefinition(String assetPath, Long instanceId) throws ServiceException;

    void saveInstanceDefinition(String assetPath, Long instanceId, Process process) throws ServiceException;

    /**
     * Returns the top-level linked process in the call chain for the specified instance.
     * Downstream calls include all routes, whereas upstream calls include only the specific instance stack.
     */
    Linked<ProcessInstance> getCallHierearchy(Long processInstanceId) throws ServiceException;

    /**
     * Returns milestone instances (not linked) for master processes that have milestones in their hierarchies.
     */
    MilestonesList getMilestones(Query query) throws ServiceException;

    /**
     * Returns deep-linked milestones for a master process.
     *
     * @param future whether potential downstream milestones should be included
     */
    Linked<Milestone> getMilestones(String masterRequestId, boolean future) throws ServiceException;

    /**
     * Returns deep-linked milestones for a master process.
     *
     * @param future whether potential downstream milestones should be included
     */
    Linked<Milestone> getMilestones(Long masterProcessInstanceId, boolean future) throws ServiceException;

    Linked<ActivityInstance> getActivityHierarchy(ProcessInstance processInstance) throws ServiceException;
}