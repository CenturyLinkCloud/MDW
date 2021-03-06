package com.centurylink.mdw.services.task;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.task.*;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.observer.task.TaskValuesProvider;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.service.data.task.TaskAggregation;
import com.centurylink.mdw.service.data.task.TaskDataAccess;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.task.types.TaskList;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/**
 * Services related to manual tasks.
 */
public class TaskServicesImpl implements TaskServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private TaskDataAccess getTaskDAO() {
        return new TaskDataAccess();
    }

    protected TaskAggregation getAggregateDataAccess() {
        return new TaskAggregation();
    }

    public TaskInstance createTask(Long taskId, String masterRequestId, Long procInstId,
            String secOwner, Long secOwnerId, String title, String comments) throws ServiceException {
        try {
            return TaskWorkflowHelper.createTaskInstance(taskId, masterRequestId, procInstId, secOwner, secOwnerId, title, comments);
        } catch (DataAccessException | IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error creating task " + taskId + " for process instance " + procInstId, ex);
        }
    }

    public TaskInstance createTask(String path, String userCuid, String title, String comments, Instant due) throws ServiceException {
        TaskTemplate template = TaskTemplateCache.getTaskTemplate(path);
        if (template == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Task Template not found: " + path);
        User user = UserGroupCache.getUser(userCuid);
        if (user == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "User '" + userCuid + "' not found");
        try {
            return TaskWorkflowHelper.createTaskInstance(template.getId(), null, title, comments, due, user.getId(), 0L);
        }
        catch (DataAccessException | IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public TaskList getTasks(Query query) throws ServiceException {
        return getTasks(query, null);
    }

    /**
     * Retrieve tasks.
     */
    public TaskList getTasks(Query query, String cuid) throws ServiceException {
        try {
            String workgroups = query.getFilter("workgroups");
            if ("[My Workgroups]".equals(workgroups)) {
                User user = UserGroupCache.getUser(cuid);
                if (user == null)
                    throw new DataAccessException("Unknown user: " + cuid);
                query.setArrayFilter("workgroups", user.getGroupNames());
            }
            if ("[My Tasks]".equals(query.getFilter("assignee")))
                query.getFilters().put("assignee", cuid);
            else if ("[Everyone's Tasks]".equals(query.getFilter("assignee")))
                query.getFilters().remove("assignee");

            // processInstanceId
            long processInstanceId = query.getLongFilter("processInstanceId");
            if (processInstanceId > 0) {
                List<String> processInstanceIds = new ArrayList<>();
                processInstanceIds.add(String.valueOf(processInstanceId));
                // implies embedded subprocess instances also
                ProcessInstance processInstance = ServiceLocator.getWorkflowServices().getProcess(processInstanceId, true);
                if (processInstance.getSubprocesses() != null) {
                    for (ProcessInstance subproc : processInstance.getSubprocesses()) {
                        processInstanceIds.add(String.valueOf(subproc.getId()));
                    }
                }
                query.setArrayFilter("processInstanceIds", processInstanceIds.toArray(new String[]{}));

                // activityInstanceId -- only honored if processInstanceId is also specified
                Long[] activityInstanceIds = query.getLongArrayFilter("activityInstanceIds");
                if (activityInstanceIds != null && activityInstanceIds.length > 0) {
                    // tasks for activity instance -- special logic applied after retrieving
                    TaskList taskList = getTaskDAO().getTaskInstances(query);
                    for (TaskInstance taskInstance : taskList.getTasks()) {
                        TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInstance);
                        taskInstance.setTaskInstanceUrl(helper.getTaskInstanceUrl());
                    }
                    return filterForActivityInstance(taskList, processInstance, activityInstanceIds);
                }
            }

            TaskList taskList = getTaskDAO().getTaskInstances(query);
            for (TaskInstance taskInstance : taskList.getTasks()) {
                TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInstance);
                taskInstance.setTaskInstanceUrl(helper.getTaskInstanceUrl());
            }
            return taskList;
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Ugly logic for determining task instances for activity instances.
     */
    private TaskList filterForActivityInstance(TaskList taskList, ProcessInstance processInstance, Long[] activityInstanceIds)
    throws IOException {
        TaskList filteredList = new TaskList();
        List<TaskInstance> taskInstances = new ArrayList<>();
        Process process = null;
        if (processInstance.getInstanceDefinitionId() > 0L)
            process = ProcessCache.getInstanceDefinition(processInstance.getProcessId(), processInstance.getInstanceDefinitionId());
        if (process == null)
            process = ProcessCache.getProcess(processInstance.getProcessId());
        for (TaskInstance taskInstance : taskList.getItems()) {
            for (Long activityInstanceId : activityInstanceIds) {
                ProcessInstance procInstTemp = processInstance;
                ActivityInstance activityInstance = processInstance.getActivity(activityInstanceId);
                if (activityInstance == null && processInstance.getSubprocesses() != null) {
                    for (ProcessInstance subproc : processInstance.getSubprocesses()) {
                        activityInstance = subproc.getActivity(activityInstanceId);
                        if (activityInstance != null) {
                            procInstTemp = subproc;
                            break;
                        }
                    }
                }
                if (activityInstance != null) {
                    Long activityId = activityInstance.getActivityId();
                    Long workTransInstId = taskInstance.getSecondaryOwnerId();
                    for (TransitionInstance transitionInstance : procInstTemp.getTransitions()) {
                        if (transitionInstance.getTransitionInstanceID().equals(workTransInstId)) {
                            Long transitionId = transitionInstance.getTransitionID();
                            Transition workTrans = process.getTransition(transitionId);
                            if (workTrans == null && process.getSubprocesses() != null) {
                                for (Process subproc : process.getSubprocesses()) {
                                    workTrans = subproc.getTransition(transitionId);
                                    if (workTrans != null)
                                        break;
                                }
                            }
                            if (workTrans != null && workTrans.getToId().equals(activityId))
                                taskInstances.add(taskInstance);
                        }
                    }
                }
            }
        }

        filteredList.setTasks(taskInstances);
        filteredList.setCount(taskInstances.size());
        filteredList.setTotal(taskInstances.size());
        return filteredList;
    }

    public Map<String,String> getIndexes(Long taskInstanceId) throws ServiceException {
        try {
            return getTaskDAO().getIndexes(taskInstanceId);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public void createSubTask(String subTaskPath, Long masterTaskInstanceId)
            throws ServiceException {
        TaskTemplate subTaskTemplate = TaskTemplateCache.getTaskTemplate(subTaskPath);
        if (subTaskTemplate == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Task Template " + subTaskPath + " not found");

        TaskInstance masterTaskInstance = getInstance(masterTaskInstanceId);
        TaskRuntimeContext masterTaskContext = getContext(masterTaskInstance);
        try {
            TaskInstance subTaskInstance = TaskWorkflowHelper.createTaskInstance(subTaskTemplate.getTaskId(), masterTaskContext.getMasterRequestId(),
                    masterTaskContext.getProcessInstanceId(), OwnerType.TASK_INSTANCE, masterTaskContext.getTaskInstanceId(), null, null);
            logger.info("SubTask instance created - ID: " + subTaskInstance.getTaskInstanceId());
        } catch (DataAccessException | IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error creating subtask " + subTaskPath, ex);
        }
    }

    public TaskInstance getInstance(Long id) throws ServiceException {
        try {
            return TaskWorkflowHelper.getTaskInstance(id);
        }
        catch (DataAccessException | IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Cannot retrieve task instance: " + id, ex);
        }
    }

    public TaskRuntimeContext getContext(Long instanceId) throws ServiceException {
        TaskInstance taskInstance = getInstance(instanceId);
        if (taskInstance == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);
        return getContext(taskInstance);
    }

    public TaskRuntimeContext getContext(TaskInstance taskInstance) throws ServiceException {
        try {
            return new TaskWorkflowHelper(taskInstance).getContext();
        } catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Cannot load context for task " + taskInstance.getId(), ex);
        }
    }

    public Map<String,Value> getValues(Long instanceId) throws ServiceException {
        TaskInstance taskInstance = getInstance(instanceId);
        if (taskInstance == null) {
            throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);
        }
        TaskRuntimeContext runtimeContext = getContext(taskInstance);
        if (runtimeContext.getTaskTemplate().isAutoformTask()) {
            return new AutoFormTaskValuesProvider().collect(runtimeContext);
        }
        else {
            // TODO: implement CustomTaskValuesProvider, and also make provider configurable at design time (like TaskIndexProvider)
            return new HashMap<>();
        }
    }

    /**
     * Update task values.
     */
    public void applyValues(Long instanceId, Map<String,String> values) throws ServiceException {
        // TODO: implement CustomTaskValuesProvider, and also make provider configurable at design time (like TaskIndexProvider)
        TaskRuntimeContext runtimeContext = getContext(instanceId);
        TaskValuesProvider valuesProvider;
        if (runtimeContext.getTaskTemplate().isAutoformTask())
            valuesProvider = new AutoFormTaskValuesProvider();
        else
            valuesProvider = new CustomTaskValuesProvider();
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        valuesProvider.apply(runtimeContext, values);
        Map<String,Object> newValues = new HashMap<>();
        for (String name : values.keySet()) {
            if (TaskRuntimeContext.isExpression(name)) {
                String rootVar;
                if (name.indexOf('.') > 0)
                  rootVar = name.substring(2, name.indexOf('.'));
                else
                  rootVar = name.substring(2, name.indexOf('}'));
                newValues.put(rootVar, runtimeContext.evaluate("#{" + rootVar + "}"));
            }
            else {
                newValues.put(name, runtimeContext.getValues().get(name));
            }
        }
        for (String name : newValues.keySet()) {
            Object newValue = newValues.get(name);
            workflowServices.setVariable(runtimeContext, name, newValue);
        }
    }

    public TaskInstance performAction(Long taskInstanceId, String action, String userCuid, String assigneeCuid, String comment,
            String destination, boolean notifyEngine) throws ServiceException {
        try {
            TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInstanceId);
            if (helper.getTaskInstance() == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + taskInstanceId);
            User user = UserGroupCache.getUser(userCuid);
            if (user == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "User not found: " + userCuid);
            Long assigneeId = null;
            if (assigneeCuid != null) {
                User assignee = UserGroupCache.getUser(assigneeCuid);
                if (assignee == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Assignee not found: " + assigneeCuid);
                assigneeId = assignee.getId();
            }
            helper.performAction(action, user.getId(), assigneeId, comment, destination, notifyEngine, true);
            return helper.getTaskInstance();
        }
        catch (IOException | DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error doing " + action + " on task " + taskInstanceId, ex);
        }
    }

    public void performTaskAction(UserTaskAction taskAction) throws ServiceException {
        String action = taskAction.getTaskAction();
        String userCuid = taskAction.getUser();
        try {
            User user = UserGroupCache.getUser(userCuid);
            if (user == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "User not found: " + userCuid);
            Long assigneeId = null;
            if (com.centurylink.mdw.model.task.TaskAction.ASSIGN.equals(action)
                    || com.centurylink.mdw.model.task.TaskAction.CLAIM.equals(action)) {
                String assignee = taskAction.getAssignee();
                if (assignee == null) {
                    assignee = userCuid;
                }
                User assigneeUser = UserGroupCache.getUser(assignee);
                if (assigneeUser == null)
                    throw new ServiceException("Assignee user not found: " + assignee);
                assigneeId = assigneeUser.getId();
            }
            String destination = taskAction.getDestination();
            String comment = taskAction.getComment();

            List<Long> taskInstanceIds;
            Long taskInstanceId = taskAction.getTaskInstanceId();
            if (taskInstanceId != null) {
                taskInstanceIds = new ArrayList<>();
                taskInstanceIds.add(taskInstanceId);
            }
            else {
                taskInstanceIds = taskAction.getTaskInstanceIds();
                if (taskInstanceIds == null || taskInstanceIds.isEmpty())
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Missing TaskAction field: 'taskInstanceId' or 'taskInstanceIds'");
            }

            for (Long instanceId : taskInstanceIds) {
                TaskInstance taskInst = getInstance(instanceId);
                if (taskInst == null)
                    throw new TaskValidationException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);

                TaskRuntimeContext runtimeContext = getContext(taskInst);
                TaskActionValidator validator = new TaskActionValidator(runtimeContext);
                validator.validateAction(taskAction);

                TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInst);
                helper.performAction(action, user.getId(), assigneeId, comment, destination, true,
                        false);

                if (logger.isDebugEnabled())
                    logger.debug("Performed action: " + action + " on task instance: " + instanceId);
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            String msg = "Error performing action: " + action + " on task instance(s)";
            throw new ServiceException(ServiceException.INTERNAL_ERROR, msg, ex);
        }

    }

    public TaskList getSubtasks(Long masterTaskInstanceId) throws ServiceException {
        try {
            TaskWorkflowHelper helper = new TaskWorkflowHelper(masterTaskInstanceId);
            List<TaskInstance> subtasks = helper.getSubtasks(masterTaskInstanceId);
            for (TaskInstance subtask : subtasks) {
                TaskWorkflowHelper subtaskHelper = new TaskWorkflowHelper(subtask);
                subtask.setTaskInstanceUrl(subtaskHelper.getTaskInstanceUrl());
            }
            return new TaskList("subtasks", subtasks);
        }
        catch (IOException | DataAccessException ex) {
            throw new ServiceException("Problem getting subtasks for: " + masterTaskInstanceId, ex);
        }
    }

    public List<TaskAggregate> getTopTasks(Query query) throws ServiceException {
        try {
            CodeTimer timer = new CodeTimer(true);
            List<TaskAggregate> list = getAggregateDataAccess().getTops(query);
            String by = query.getFilter("by");
            if ("throughput".equals(by) || "completionTime".equals(by)) {
                list = populateTasks(list);
            }
            timer.stopAndLogTiming("TaskServicesImpl.getTopTasks()");
            return list;
        }
        catch (DataAccessException ex) {
            throw new ServiceException("Error retrieving top throughput processes: query=" + query, ex);
        }
    }

    public TreeMap<Instant,List<TaskAggregate>> getTaskBreakdown(Query query) throws ServiceException {
        try {
            TreeMap<Instant,List<TaskAggregate>> map = getAggregateDataAccess().getBreakdown(query);
            if (query.getFilters().get("taskIds") != null) {
                for (Instant instant : map.keySet())
                    populateTasks(map.get(instant));
            }
            return map;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving task instance breakdown: query=" + query, ex);
        }
    }

    public List<TaskTemplate> getTaskTemplates(Query query) {
        List<TaskTemplate> templates;
        String find = query.getFind();
        if (find == null) {
            templates = TaskTemplateCache.getTaskTemplates();
        }
        else {
            templates = new ArrayList<>();
            String findLower = find.toLowerCase();
            for (TaskTemplate taskVO : TaskTemplateCache.getTaskTemplates()) {
                if (taskVO.getTaskName() != null && taskVO.getTaskName().toLowerCase().startsWith(findLower))
                    templates.add(taskVO);
                else if (find.indexOf(".") > 0 && taskVO.getPackageName() != null && taskVO.getPackageName().toLowerCase().startsWith(findLower))
                    templates.add(taskVO);
            }
            return templates;
        }
        Collections.sort(templates, (t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()));
        return templates;
    }

    public Map<String, List<TaskTemplate>> getTaskTemplatesByPackage(Query query) {
        List<TaskTemplate> taskVOs = getTaskTemplates(query);
        Map<String, List<TaskTemplate>> templates = new HashMap<>();
        for (TaskTemplate taskVO : taskVOs) {
            List<TaskTemplate> templateList = templates.get(taskVO.getPackageName());
            if (templateList == null) {
                templateList = new ArrayList<>();
                templates.put(taskVO.getPackageName(), templateList);
            }
            templateList.add(taskVO);
        }
        return templates;
    }

    public void updateTask(String userCuid, TaskInstance taskInstance) throws ServiceException {
        try {
            Long instanceId = taskInstance.getTaskInstanceId();
            TaskInstance oldTaskInstance = getInstance(instanceId);
            if (oldTaskInstance == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);
            if (!userCuid.equals(oldTaskInstance.getAssigneeCuid()))
                throw new ServiceException(ServiceException.FORBIDDEN, "Task instance " + instanceId + " not assigned to " + userCuid);
            if (oldTaskInstance.isInFinalStatus())
                throw new ServiceException(ServiceException.FORBIDDEN, "Updates not allowed for task " + instanceId + " with status " + oldTaskInstance.getStatus());

            TaskWorkflowHelper helper = new TaskWorkflowHelper(oldTaskInstance);

            // due date
            if (taskInstance.getDue() == null) {
                if (oldTaskInstance.getDue() != null)
                    helper.updateDue(null, userCuid, null);
            }
            else if (!taskInstance.getDue().equals(oldTaskInstance.getDue())) {
                if (Date.from(taskInstance.getDue()).compareTo(DatabaseAccess.getDbDate()) < 0)
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Cannot set due date in the past for task instance " + instanceId);
                helper.updateDue(taskInstance.getDue(), userCuid, null);
            }

            // priority
            if (taskInstance.getPriority() == null) {
                if (oldTaskInstance.getPriority() != null && oldTaskInstance.getPriority() != 0)
                    helper.updatePriority(0);
            }
            else if (!taskInstance.getPriority().equals(oldTaskInstance.getPriority()))
                helper.updatePriority(taskInstance.getPriority());

            // comments
            if (taskInstance.getComments() == null) {
                if (oldTaskInstance.getComments() != null)
                    helper.updateComments(null);
            }
            else if (!taskInstance.getComments().equals(oldTaskInstance.getComments())) {
                helper.updateComments(taskInstance.getComments());
            }

            // workgroups
            if (taskInstance.getWorkgroups() == null || taskInstance.getWorkgroups().isEmpty()) {
                if (oldTaskInstance.getWorkgroups() != null && !oldTaskInstance.getWorkgroups().isEmpty())
                    helper.updateWorkgroups(new ArrayList<>());
            }
            else if (!taskInstance.getWorkgroupsString().equals(oldTaskInstance.getWorkgroupsString())) {
                helper.updateWorkgroups(taskInstance.getWorkgroups());
            }

            helper.notifyTaskAction(TaskAction.SAVE, null, null);
            // audit log
            UserAction userAction = new UserAction(userCuid, Action.Change, Entity.TaskInstance, instanceId, "summary");
            userAction.setSource("Task Services");
            ServiceLocator.getEventServices().createAuditLog(userAction);
        }
        catch (IOException | DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    /**
     * Fills in task header info from task template definition.
     */
    protected List<TaskAggregate> populateTasks(List<TaskAggregate> taskAggregates) {
        for (TaskAggregate tc : taskAggregates) {
            try {
                TaskTemplate taskTemplate = TaskTemplateCache.getTaskTemplate(tc.getId());
                if (taskTemplate == null)
                    throw new IOException("Missing definition for task id: " + tc.getId());
                tc.setName(taskTemplate.getName());
                tc.setVersion(AssetVersion.formatVersion(taskTemplate.getVersion()));
                tc.setPackageName(taskTemplate.getPackageName());
            } catch (IOException ex) {
                logger.error("Error loading task template for id: " + tc.getId(), ex);
                tc.setName("Unknown");
            }
        }
        return taskAggregates;
    }

    @Override
    public List<EventLog> getHistory(Long taskInstanceId) throws ServiceException {
        EventServices eventManager = ServiceLocator.getEventServices();
        return eventManager.getEventLogs(null, null, "TaskInstance", taskInstanceId);
    }

    @Override
    public void updateIndexes(Long taskInstanceId, Map<String,String> indexes)
            throws ServiceException {
        try {
            new TaskDataAccess().setTaskInstanceIndices(taskInstanceId, indexes);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR,
                    "Index error on task " + taskInstanceId + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public void cancelTaskInstancesForProcess(Long processInstanceId)
            throws ServiceException, DataAccessException {
        CodeTimer timer = new CodeTimer("cancelTaskInstancesForProcess()", true);
        List<TaskInstance> instances = getTaskInstancesForProcess(processInstanceId);
        if (instances == null || instances.size() == 0) {
            timer.stopAndLogTiming("NoTaskInstances");
            return;
        }
        TaskDataAccess dataAccess = new TaskDataAccess();
        for (TaskInstance instance : instances) {
            instance.setComments("Task has been cancelled by ProcessInstance.");
            if (!instance.isInFinalStatus()) {
                Integer prevStatus = instance.getStatusCode();
                Integer prevState = instance.getStateCode();

                instance.setStateCode(TaskState.STATE_CLOSED);
                instance.setStatusCode(TaskStatus.STATUS_CANCELLED);
                Map<String,Object> changes = new HashMap<>();
                changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_CANCELLED);
                changes.put("TASK_INSTANCE_STATE", TaskState.STATE_CLOSED);
                dataAccess.updateTaskInstance(instance.getTaskInstanceId(), changes, true);
                try {
                    Long elapsedMs = dataAccess.getDatabaseTime() - Date.from(instance.getStart()).getTime();
                    dataAccess.setElapsedTime(OwnerType.TASK_INSTANCE, instance.getTaskInstanceId(), elapsedMs);
                    new TaskWorkflowHelper(instance).notifyTaskAction(TaskAction.CANCEL, prevStatus, prevState);
                }
                catch (IOException | SQLException ex) {
                    logger.error("Failed to set timing for task: " + instance.getId(), ex);
                }

            }
        }
        timer.stopAndLogTiming("");
    }

    public List<TaskInstance> getTaskInstancesForProcess(Long processInstanceId)
    throws DataAccessException {
        CodeTimer timer = new CodeTimer("getTaskInstancesForProcess()", true);
        List<TaskInstance> daoResults = getTaskDAO().getTaskInstancesForProcessInstance(processInstanceId);
        timer.stopAndLogTiming("");
        return daoResults;
    }

    public void cancelTaskForActivity(Long activityInstanceId) throws ServiceException, DataAccessException {
        TaskInstance taskInstance = getTaskInstanceForActivity(activityInstanceId);
        if (taskInstance == null)
            throw new ServiceException("Cannot find the task instance for the activity instance: " + activityInstanceId);
        if (taskInstance.getStatusCode().equals(TaskStatus.STATUS_ASSIGNED)
                || taskInstance.getStatusCode().equals(TaskStatus.STATUS_IN_PROGRESS)
                || taskInstance.getStatusCode().equals(TaskStatus.STATUS_OPEN)) {
            new TaskWorkflowHelper(taskInstance).cancel();
        }
    }

    public TaskInstance getTaskInstanceForActivity(Long activityInstanceId)
            throws DataAccessException {
        return getTaskDAO().getTaskInstanceByActivityInstanceId(activityInstanceId);
    }

    public List<String> getGroupsForTaskInstance(TaskInstance taskInstance)
            throws ServiceException {
        try {
            if (taskInstance.isShallow())
                new TaskWorkflowHelper(taskInstance).getTaskInstanceAdditionalInfo();
            return taskInstance.getGroups();
        } catch (IOException | DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error getting groups for task instance " + taskInstance.getId(), ex);
        }
    }

    public List<TaskAction> getActions(Long instanceId, String userCuid, Query query) throws ServiceException {
        TaskInstance taskInstance = getInstance(instanceId);
        if (taskInstance == null) {
            throw new ServiceException(ServiceException.NOT_FOUND,
                    "Unable to load runtime context for task instance: " + instanceId);
        }

        try {
            TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInstance);
            if (query.getBooleanFilter("custom"))
                return helper.getCustomActions();
            else
                return AllowableTaskActions.getTaskDetailActions(userCuid, helper.getContext());
        }
        catch (Exception ex) {
            throw new ServiceException("Failed to get actions for task instance: " + instanceId, ex);
        }
    }

    public void updateTaskInstanceState(Long taskInstId, boolean isAlert)
            throws DataAccessException, ServiceException {
        TaskInstance taskInstance = getInstance(taskInstId);
        TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInstance);
        helper.updateState(isAlert);
    }

    @Override
    public void setElapsedTime(String ownerType, Long instanceId, Long elapsedTime) throws ServiceException {
        try {
            getTaskDAO().setElapsedTime(ownerType, instanceId, elapsedTime);
        }
        catch (SQLException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }
}
