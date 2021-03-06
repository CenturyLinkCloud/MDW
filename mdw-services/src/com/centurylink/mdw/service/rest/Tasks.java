package com.centurylink.mdw.service.rest;

import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.*;
import com.centurylink.mdw.model.event.Event;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.task.*;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.user.UserList;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.service.data.task.TaskDataAccess;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.services.task.AllowableTaskActions;
import com.centurylink.mdw.task.types.TaskList;
import com.centurylink.mdw.util.JsonUtil;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.xmlbeans.XmlException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;

@Path("/Tasks")
@Api("Task instances")
public class Tasks extends JsonRestService implements JsonExportable {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private TaskServices getTaskServices() {
        return ServiceLocator.getTaskServices();
    }

    /**
     * Retrieve a task or list of tasks, or subData for a task instance.
     */
    @Override
    @Path("/{instanceId}/{subData}")
    @ApiOperation(value = "Retrieve a task instance or a page of task instances", notes = "If taskInstanceId is not present, returns a page of task instances. "
            + "If subData is not present, returns task summary info. "
            + "Options for subData: 'values', 'indexes', 'history', 'actions', 'subtasks'", response = Value.class, responseContainer = "List")
    public JSONObject get(String path, Map<String, String> headers)
            throws ServiceException, JSONException {
        TaskServices taskServices = ServiceLocator.getTaskServices();
        try {
            Query query = getQuery(path, headers);
            String userCuid = headers.get(Listener.AUTHENTICATED_USER_HEADER);
            String segOne = getSegment(path, 1);
            if (segOne == null) {
                // task list query
                TaskList tasks = taskServices.getTasks(query, userCuid);
                return tasks.getJson();
            }
            else {
                if (segOne.equals("templates")) {
                    boolean grouped = query.getBooleanFilter("grouped");
                    if (grouped) {
                        return getPackageTemplatesJson(getTaskServices().getTaskTemplatesByPackage(query));
                    }
                    return getTemplates(query).getJson();
                }
                else if (segOne.equals("categories")) {
                    return getCategories(query).getJson();
                }
                else if (segOne.equals("bulkActions")) {
                    return getBulkActions(query).getJson();
                }
                else if (segOne.equals("assignees")) {
                    return getPotentialAssignees(query, userCuid).getJson();
                }
                else if (segOne.equals("tops")) {
                    return getTops(query).getJson();
                }
                else if (segOne.equals("breakdown")) {
                    return getBreakdown(query).getJson();
                }
                else {
                    // must be instance id
                    try {
                        Long instanceId = Long.parseLong(segOne);
                        TaskInstance taskInstance;
                        String extra = getSegment(path, 2);
                        if (extra == null) {
                            taskInstance = taskServices.getInstance(instanceId);
                            if (taskInstance == null)
                                throw new ServiceException(HTTP_404_NOT_FOUND,
                                        "Task instance not found: " + instanceId);
                            if (taskInstance.isProcessOwned()) {
                                ProcessInstance procInst = ServiceLocator.getWorkflowServices()
                                        .getProcess(taskInstance.getOwnerId());
                                taskInstance.setProcessName(procInst.getProcessName());
                                taskInstance.setPackageName(procInst.getPackageName());
                            }
                            return taskInstance.getJson();
                        }
                        else if (extra.equals("values")) {
                            Map<String, Value> values = taskServices.getValues(instanceId);
                            JSONObject valuesJson = new JsonObject();
                            for (String name : values.keySet()) {
                                valuesJson.put(name, values.get(name).getJson());
                            }
                            return valuesJson;
                        }
                        else if (extra.equals("indexes")) {
                            Map<String, String> indexes = taskServices.getIndexes(instanceId);
                            return JsonUtil.getJson(indexes);
                        }
                        else if (extra.equals("history")) {
                            List<EventLog> eventLogs = taskServices.getHistory(instanceId);
                            JSONObject json = new JsonObject();
                            if (eventLogs != null && eventLogs.size() > 0) {
                                JSONArray historyJson = new JSONArray();

                                for (EventLog log : eventLogs) {
                                    historyJson.put(log.getJson());
                                }
                                json.put("taskHistory", historyJson);
                            }
                            return json;
                        }
                        else if (extra.equals("actions")) {
                            JSONArray jsonTaskActions = new JSONArray();
                            for (TaskAction taskAction : taskServices.getActions(instanceId, userCuid, query)) {
                                jsonTaskActions.put(taskAction.getJson());
                            }
                            return new JsonArray(jsonTaskActions).getJson();
                        }
                        else if (extra.equals("subtasks")) {
                            TaskList subtasks = taskServices.getSubtasks(instanceId);
                            return subtasks.getJson();
                        }
                        else {
                            throw new ServiceException(HTTP_400_BAD_REQUEST,
                                    "Invalid subpath: " + extra);
                        }
                    }
                    catch (NumberFormatException ex) {
                        throw new ServiceException(HTTP_400_BAD_REQUEST,
                                "Invalid task instance id: " + segOne);
                    }
                }
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    @Path("/{instanceId}")
    @ApiOperation(value = "Update a task instance, update an instance's index values, or register to wait for an event", response = StatusMessage.class, notes = "If indexes is present, body is TaskIndexes; if regEvent is present, body is Event; otherwise body is a Task."
            + "If subData is not present, updates task summary info. Options for subData: values, indexes, regEvent")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Task", paramType = "body", dataType = "com.centurylink.mdw.model.task.TaskInstance", value = "When no subData is specified"),
            @ApiImplicitParam(name = "indexes", paramType = "body", dataType = "com.centurylink.mdw.model.task.TaskIndexes", value = "When {subData}=indexes"),
            @ApiImplicitParam(name = "regEvent", paramType = "body", dataType = "com.centurylink.mdw.model.event.Event", value = "When {subData}=regEvent.  Only the id (event name) field is mandatory in Event object.  Optionally, a completionCode can specified - Default is FINISHED"),
            @ApiImplicitParam(name = "values", paramType = "body", dataType = "java.lang.Object", value = "When {subData}=values. JSON object parseable into a key/value Map.") })
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String id = getSegment(path, 1);
        if (id == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST,
                    "Missing path segment: {taskInstanceId}");
        try {
            Long instanceId = Long.parseLong(id);
            String extra = getSegment(path, 2);
            if (extra == null) {
                // update task summary info
                TaskInstance taskInstJson = new TaskInstance(content);
                if (!content.has("id"))
                    throw new ServiceException(HTTP_400_BAD_REQUEST,
                            "Content is missing required field: id");
                long contentInstanceId = content.getLong("id");
                if (instanceId != contentInstanceId)
                    throw new ServiceException(HTTP_400_BAD_REQUEST,
                            "Content/path mismatch (instanceId): " + contentInstanceId + " is not: "
                                    + instanceId);

                ServiceLocator.getTaskServices().updateTask(getAuthUser(headers), taskInstJson);
                return null;
            }
            else if (extra.equals("values")) {
                Map<String, String> values = JsonUtil.getMap(content);
                TaskServices taskServices = ServiceLocator.getTaskServices();
                taskServices.applyValues(instanceId, values);
            }
            else if (extra.equals("indexes")) {
                // update task indexes
                TaskIndexes taskIndexes = new TaskIndexes(content);
                if (instanceId != taskIndexes.getTaskInstanceId())
                    throw new ServiceException(HTTP_400_BAD_REQUEST,
                            "Content/path mismatch (instanceId): " + taskIndexes.getTaskInstanceId()
                                    + " is not: " + instanceId);
                ServiceLocator.getTaskServices().updateIndexes(taskIndexes.getTaskInstanceId(),
                        taskIndexes.getIndexes());

                if (logger.isDebugEnabled())
                    logger.debug("Updated task indexes for instance ID: "
                            + taskIndexes.getTaskInstanceId());

                return null;
            }
            else if (extra.equals("regEvent")) {
                Event event = new Event(content);
                WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
                workflowServices.registerTaskWaitEvent(instanceId, event);
                if (logger.isDebugEnabled())
                    logger.debug("Registered Event : [" + event.getId() + "]Task Instance Id = "
                            + instanceId);
            }
        }
        catch (NumberFormatException ex) {
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Invalid task instance id: " + id);
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * For creating a new task or performing task action(s). When performing
     * actions: old way = path='Tasks/{instanceId}/{action}', new way =
     * path='Tasks/{action}'. Where {action} is an actual valid task action like
     * 'Claim'.
     */
    @Override
    @Path("/action")
    @ApiOperation(value = "Create a task instance or perform an action on existing instance(s)",
            notes = "If {action} is 'Create', then the body contains a task template logical Id; otherwise it contains a TaskAction to be performed.",
            response = StatusMessage.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "TaskAction", paramType = "body", dataType = "com.centurylink.mdw.model.task.UserTaskAction") })
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String segOne = getSegment(path, 1);
        try {
            TaskServices taskServices = ServiceLocator.getTaskServices();
            if (segOne == null || segOne.equalsIgnoreCase("create")) {
                // TODO: create a new task (asset path should drive template)
                // TODO: title, dueDate
                String title = null;
                String comments = null;
                Instant due = null;
                if (content.has("masterTaskInstanceId")) {
                    // TODO: subtask instance
                    return null;
                }
                else {
                    // TODO: top-level task instance
                    return null;
                }
            }
            else {
                try {
                    // handle taskInstanceId as segOne and actual action as segTwo
                    long taskInstanceId = Long.parseLong(segOne);
                    TaskInstance taskInstance = taskServices.getInstance(taskInstanceId);
                    if (taskInstance == null)
                        throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + taskInstanceId);
                    String segTwo = getSegment(path, 2);
                    if (segTwo == null)
                        throw new ServiceException(HTTP_400_BAD_REQUEST,
                                "Missing {action} on request path, should be eg: /Tasks/{instanceId}/Claim");
                    try {
                        if (content.has("taskAction")
                                && !content.getString("taskAction").equals(segTwo))
                            throw new ServiceException(HTTP_400_BAD_REQUEST,
                                    "Content/path mismatch (action): '"
                                            + content.getString("taskAction") + "' is not: '"
                                            + segTwo + "'");
                        UserTaskAction taskAction = new UserTaskAction(content, segTwo);
                        if (taskAction.getTaskInstanceId() == null
                                || taskInstanceId != taskAction.getTaskInstanceId())
                            throw new ServiceException(HTTP_400_BAD_REQUEST,
                                    "Content/path mismatch (instanceId): "
                                            + taskAction.getTaskInstanceId() + " is not: "
                                            + taskInstanceId);
                        taskServices.performTaskAction(taskAction);
                        return null;
                    }
                    catch (IllegalArgumentException ex2) {
                        throw new ServiceException(HTTP_400_BAD_REQUEST,
                                "Invalid task action: '" + segTwo + "'", ex2);
                    }
                }
                catch (NumberFormatException ex) {
                    // segOne must be the action
                    try {
                        UserTaskAction taskAction = new UserTaskAction(content, segOne);
                        if (!segOne.equals(taskAction.getTaskAction()))
                            throw new ServiceException(HTTP_400_BAD_REQUEST,
                                    "Content/path mismatch (action): '" + taskAction.getTaskAction()
                                            + "' is not: '" + segOne + "'");
                        taskServices.performTaskAction(taskAction);
                        return null;
                    }
                    catch (IllegalArgumentException ex2) {
                        throw new ServiceException(HTTP_400_BAD_REQUEST,
                                "Invalid task action: '" + segOne + "'", ex2);
                    }
                }
            }
        }
        catch (JSONException e) {
            logger.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public Jsonable toExportJson(Query query, JSONObject json) throws JSONException {
        try {
            if (json.has(TaskList.TASKS))
                return new TaskList(TaskList.TASKS, json);
            else if ("Tasks/breakdown".equals(query.getPath()))
                return new JsonListMap<>(json, TaskAggregate.class);
            else
                throw new JSONException("Unsupported export type for query: " + query);
        }
        catch (ParseException ex) {
            throw new JSONException(ex);
        }
    }

    @Override
    public String getExportName() { return "Tasks"; }

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.TASK_EXECUTION);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String, String> headers) {
        return Entity.Task;
    }

    private JSONObject getPackageTemplatesJson(Map<String, List<TaskTemplate>> templates) {
        JSONObject templatesJson = new JSONObject();
        JSONArray pkgsArray = new JSONArray();
        for (Map.Entry<String, List<TaskTemplate>> entry : templates.entrySet()) {
            JSONObject json = new JSONObject();
            json.put("name",  entry.getKey());
            List<TaskTemplate> taskVOs = entry.getValue();
            Package taskPkg = PackageCache.getPackage(taskVOs.get(0).getPackageName());
            json.put("version", taskPkg.getVersion().toString());
            JSONArray jsonTasks = new JSONArray();
            for (TaskTemplate taskVO : taskVOs) {
                JSONObject jsonTask = new JsonObject();
                jsonTask.put("taskId", taskVO.getId());
                jsonTask.put("name", taskVO.getName());
                jsonTask.put("version", taskVO.getVersionString());
                jsonTask.put("logicalId", taskVO.getLogicalId());
                jsonTasks.put(jsonTask);
            }
            json.put("templates", jsonTasks);
            pkgsArray.put(json);
        }
        templatesJson.put("packages", pkgsArray);
        return templatesJson;
    }

    @Path("/templates")
    public JsonArray getTemplates(Query query) throws ServiceException {
        List<TaskTemplate> taskVOs = getTaskServices().getTaskTemplates(query);
        JSONArray jsonTasks = new JSONArray();
        for (TaskTemplate taskVO : taskVOs) {
            JSONObject jsonTask = new JsonObject();
            jsonTask.put("packageName", taskVO.getPackageName());
            jsonTask.put("taskId", taskVO.getId());
            jsonTask.put("name", taskVO.getTaskName());
            jsonTask.put("version", taskVO.getVersionString());
            jsonTask.put("logicalId", taskVO.getLogicalId());
            jsonTasks.put(jsonTask);
        }
        return new JsonArray(jsonTasks);
    }

    @Path("/categories")
    public JsonArray getCategories(Query query) {
        Map<Integer,TaskCategory> categories = TaskDataAccess.getTaskRefData().getCategories();
        List<TaskCategory> list = new ArrayList<>();
        list.addAll(categories.values());
        Collections.sort(list);
        return JsonUtil.getJsonArray(list);
    }

    @Path("/bulkActions")
    public JsonArray getBulkActions(Query query) throws IOException, XmlException {
        boolean myTasks = query.getBooleanFilter("myTasks");
        List<TaskAction> taskActions;
        if (myTasks)
            taskActions = AllowableTaskActions.getMyTasksBulkActions();
        else
            taskActions = AllowableTaskActions.getWorkgroupTasksBulkActions();
        JSONArray jsonTaskActions = new JSONArray();
        for (TaskAction taskAction : taskActions) {
            jsonTaskActions.put(taskAction.getJson());
        }
        return new JsonArray(jsonTaskActions);
    }

    @Path("/assignees")
    public UserList getPotentialAssignees(Query query, String userCuid) throws DataAccessException {
        // return potential assignees for all this user's workgroups
        User user = UserGroupCache.getUser(userCuid);
        return ServiceLocator.getUserServices()
                .findWorkgroupUsers(user.getGroupNames(), query.getFind());
    }

    @Path("/tops")
    public JsonArray getTops(Query query) throws ServiceException {
        // dashboard top throughput query
        List<TaskAggregate> taskAggregates = getTaskServices().getTopTasks(query);
        JSONArray taskArr = new JSONArray();
        int ct = 0;
        TaskAggregate other = null;
        long otherTot = 0;
        for (TaskAggregate taskAggregate : taskAggregates) {
            if (ct >= query.getMax()) {
                if (other == null) {
                    other = new TaskAggregate(0);
                    other.setName("Other");
                }
                otherTot += taskAggregate.getCount();
            }
            else {
                taskArr.put(taskAggregate.getJson());
            }
            ct++;
        }
        if (other != null) {
            other.setCount(otherTot);
            taskArr.put(other.getJson());
        }
        return new JsonArray(taskArr);
    }

    @Path("/breakdown")
    public JsonListMap<TaskAggregate> getBreakdown(Query query) throws ServiceException {
        TreeMap<Instant,List<TaskAggregate>> instMap = getTaskServices().getTaskBreakdown(query);
        LinkedHashMap<String,List<TaskAggregate>> listMap = new LinkedHashMap<>();
        for (Instant instant : instMap.keySet()) {
            List<TaskAggregate> taskAggregates = instMap.get(instant);
            listMap.put(Query.getString(instant), taskAggregates);
        }
        return new JsonListMap<>(listMap);
    }
}