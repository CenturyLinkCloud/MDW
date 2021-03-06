package com.centurylink.mdw.service.rest;

import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.Pagelet;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/Pagelets")
@Api("Pagelet assets")
public class Pagelets extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    protected List<String> getRoles(String path, String method) {
        if (method.equals("GET")) {
            List<String> roles = new ArrayList<>();
            if (UserGroupCache.getRole(Role.ASSET_VIEW) != null) {
                roles.add(Role.ASSET_VIEW);
                roles.add(Role.ASSET_DESIGN);
                roles.add(Workgroup.SITE_ADMIN_GROUP);
            }
            return roles;
        }
        else {
            return super.getRoles(path, method);
        }
    }

    @Override
    @Path("/{assetPath}")
    @ApiOperation(value="Retrieve a pagelet asset as JSON")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length != 3)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        String assetPath = segments[1] + '/' + segments[2];
        try {
            Asset asset = AssetCache.getAsset(assetPath);
            if (asset == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Pagelet not found: " + assetPath);
            return new Pagelet(new String(asset.getContent())).getJson();
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

}
