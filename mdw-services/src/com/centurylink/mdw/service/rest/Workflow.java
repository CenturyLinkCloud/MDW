package com.centurylink.mdw.service.rest;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.api.AssetPackageList;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.Linked;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.process.HierarchyCache;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.asset.CustomPageLookup;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.List;
import java.util.Map;

@Path("/Workflow")
@Api("MDW process definitions")
public class Workflow extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Process;
    }

    @Override
    @Path("/{packageName}/{processName}/{processInstanceId}")
    @ApiOperation(value="Retrieve a process definition JSON.",
        notes="Path segments {packageName} and {processName} are required, while {processVersion} and {processInstanceId} are optional.",
        response=Process.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="version", paramType="query", dataType="string"),
        @ApiImplicitParam(name="summary", paramType="query", dataType="boolean")})
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        Query query = getQuery(path, headers);
        try {
            // (for previous versions) process by id
            long processId = query.getLongFilter("id");
            if (processId > 0) {
                Process p = ProcessCache.getProcess(processId);
                if (p == null) {
                    throw new ServiceException(ServiceException.NOT_FOUND, "Process ID not found: " + processId);
                }
                else {
                    JSONObject json = query.getBooleanFilter("summary") ? new JsonObject() : p.getJson();
                    json.put("id", p.getId());
                    json.put("name", p.getName());
                    json.put("package", p.getPackageName());
                    json.put("version", p.getVersionString());
                    return json;
                }
            }

            String[] segments = getSegments(path);

            if (segments.length == 1) {
                long callHierarchyFor = query.getLongFilter("callHierarchyFor");
                if (callHierarchyFor != -1) {
                    Process process = ProcessCache.getProcess(callHierarchyFor);
                    if (process == null)
                        throw new ServiceException(ServiceException.NOT_FOUND, "Definition not found: " + callHierarchyFor);
                    JSONObject hierarchyJson = new JSONObject();
                    JSONArray callersJson = new JSONArray();
                    hierarchyJson.put("hierarchy", callersJson);
                    for (Linked<Process> caller : HierarchyCache.getHierarchy(callHierarchyFor)) {
                        callersJson.put(caller.getJson());
                    }
                    return hierarchyJson;
                }
                else {
                    // definitions -- with start pages
                    Query procQuery = new Query();
                    procQuery.setFilter("extension", "proc");
                    AssetPackageList procPkgList = ServiceLocator.getAssetServices().getAssetPackageList(procQuery);
                    return procPkgList.getJson();
                }
            }

            if (segments.length < 3)
                throw new ServiceException(ServiceException.BAD_REQUEST, "Path segments {packageName}/{processName} are required");
            if (query.getBooleanFilter("summary")) {
                JSONObject json = new JsonObject();
                Process process;
                if (segments.length == 4) {
                    process = ProcessCache.getProcess(segments[1] + "/" + segments[2], AssetVersion.parseVersion(segments[3]));
                    Process latest = ProcessCache.getProcess(process.getPackageName() + "/" + process.getName());
                    // If null it means it is archived but was renamed or removed from current assets
                    if (latest == null || !latest.getId().equals(process.getId()))
                        json.put("archived", true);
                }
                else {
                    process = ProcessCache.getProcess(segments[1] + "/" + segments[2]);
                }
                json.put("id", process.getId());
                json.put("name", process.getName());
                json.put("package", process.getPackageName());
                json.put("version", process.getVersionString());
                Process latest = ProcessCache.getProcess(process.getPath());
                if (latest != null) {  // not deleted
                    if (HierarchyCache.hasMilestones(latest.getId())) {
                        json.put("hasMilestones", true);
                    }
                }
                return json;
            }
            else {
                String assetPath = segments[1] + "/" + segments[2];
                Process process = null;
                Long instanceId = null;
                if (segments.length >= 4) {
                    if (segments[3].startsWith("v")) {
                        query.setFilter("version", AssetVersion.parseVersion(segments[3]));
                    }
                    if (segments.length == 5 || !segments[3].startsWith("v")) {
                        instanceId = Long.parseLong(segments.length == 5 ? segments[4] : segments[3]);
                        if (!assetPath.endsWith(".proc"))
                            assetPath += ".proc";
                        process = workflowServices.getInstanceDefinition(assetPath, instanceId);
                        if (process == null)
                            instanceId = null;  // to indicate instance override below
                    }
                }
                if (process == null) {
                    process = getProcessDefinition(assetPath, query);
                    if (process == null)
                        throw new ServiceException(ServiceException.NOT_FOUND, "Not found: " + path);
                }
                JSONObject json = process.getJson(); // does not include name, package or id
                json.put("name", process.getName());
                json.put("id", process.getId());
                json.put("packageName", process.getPackageName());
                json.put("version", process.getVersionString());
                if (instanceId != null) {
                    json.put("instanceId", instanceId);
                }
                String startPage = process.getAttribute(WorkAttributeConstant.PROCESS_START_PAGE);
                if (startPage != null) {
                    String assetSpec = startPage;
                    String ver = process.getAttribute(WorkAttributeConstant.PROCESS_START_PAGE_ASSET_VERSION);
                    if (ver != null)
                        assetSpec += " v" + ver;
                    AssetVersionSpec startPageSpec = AssetVersionSpec.parse(assetSpec);
                    json.put("startPageUrl", new CustomPageLookup(startPageSpec, null).getUrl());
                }
                Process latest = ProcessCache.getProcess(process.getPath());
                if (latest != null) {  // not deleted
                    if (HierarchyCache.hasMilestones(latest.getId())) {
                        json.put("hasMilestones", true);
                    }
                }

                return json;
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    @Path("//{packageName}/{processName}")
    @ApiOperation(value="Update a process definition", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Process", paramType="body", dataType="com.centurylink.mdw.model.workflow.Process")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        // TODO implement update
        return super.put(path, content, headers);
    }

    @Path("/{packageName}/{processName}/{processVersion}/{processInstanceId}")
    public Process getProcessDefinition(String assetPath, Query query) throws ServiceException {
        return ServiceLocator.getDesignServices().getProcessDefinition(assetPath, query);
    }
}
