package com.centurylink.mdw.services.request;

import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.service.*;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.asset.AssetRequest;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.request.RequestHandlerException;
import com.centurylink.mdw.service.data.process.ProcessRequests;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.services.rest.ProcessInvoker;
import com.centurylink.mdw.services.rest.RestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.xmlbeans.XmlObject;
import org.json.JSONObject;

import java.util.Map;

public class ServiceRequestHandler implements RequestHandler {

    protected static final String MDW_REST_SERVICE_PROVIDER_PACKAGE = "com.centurylink.mdw.service.rest";
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public enum Format {
        xml,
        json,
        text
    }

    private Package pkg;
    public Package getPackage() { return pkg; }
    public void setPackage(Package pkg) { this.pkg = pkg; }

    @Override
    public Response handleRequest(Request request, Object message, Map<String, String> headers)
            throws RequestHandlerException {
        TextService service = null;
        String path = headers.get(Listener.METAINFO_REQUEST_PATH);

        Format format = getFormat(headers);

        try {
            if (message instanceof XmlObject) {
                // XML request
                format = Format.xml;
                headers.put(Listener.METAINFO_CONTENT_TYPE, "text/xml");
            }
            else if (message instanceof JSONObject) {
                // JSON request
                format = Format.json;
                headers.put(Listener.METAINFO_CONTENT_TYPE, "application/json");
            }

            service = getServiceInstance(headers);
            if (service == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Unable to handle service request: " + path);

            Response response;

            if (format == Format.json || (service instanceof JsonService && !(service instanceof XmlService))) {
                JsonService jsonService = (JsonService) service;
                String downloadFormat = headers.get(Listener.METAINFO_DOWNLOAD_FORMAT);
                if (downloadFormat != null && !(downloadFormat.equals(Listener.DOWNLOAD_FORMAT_JSON)
                        || downloadFormat.equals(Listener.DOWNLOAD_FORMAT_XML) || downloadFormat.equals(Listener.DOWNLOAD_FORMAT_TEXT))) {
                    // binary download format requires export
                    if (!(jsonService instanceof JsonRestService))
                        throw new ServiceException(ServiceException.BAD_REQUEST, "Export not supported for " + jsonService.getClass());
                    return new Response(((JsonRestService)jsonService).export(downloadFormat, headers));
                }
                else {
                    headers.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_JSON);
                    String json = jsonService.getJson((JSONObject)message, headers);
                    if (json == null)
                        response = createSuccessResponse(Format.json);
                    else
                        response = new Response(json);

                    return response;
                }
            }
            else if (format == Format.xml) {
                headers.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_XML);
                XmlService xmlService = (XmlService) service;
                String xml = xmlService.getXml((XmlObject)message, headers);
                if (xml == null)
                    return createSuccessResponse(Format.xml);
                else
                    response = new Response(xml);

                return response;
            }
            else {
                headers.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_TEXT);
                String text = service.getText(message, headers);
                if (text == null)
                    return createSuccessResponse(Format.text);
                else
                    response = new Response(text);

                return response;
            }
        }
        catch (ServiceException ex) {
            logger.error(ex.getMessage(), ex);
            if (ex.getCode() > 0)
                headers.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(ex.getCode()));
            else
                headers.put(Listener.METAINFO_HTTP_STATUS_CODE, "500");
            return createResponse(ex.getCode(), ex.getMessage(), format);
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            if (service instanceof RestService) {
                headers.put(Listener.METAINFO_HTTP_STATUS_CODE, "500");
                return createResponse(Status.INTERNAL_ERROR.getCode(), ex.getMessage(), format);
            }
            return createErrorResponse(ex, format);
        }
    }

    protected Response createErrorResponse(Throwable t, Format f) {
        if (t instanceof ServiceException && ((ServiceException)t).getCode() >= 400)
            return createResponse(((ServiceException)t).getCode(), t.getMessage(), f);
        Throwable cause = t;
        while (cause.getCause() != null)
          cause = cause.getCause();
        return createResponse(-1, cause.toString(), f);
    }

    protected Response createSuccessResponse(Format format) {
        return createResponse(0, "Success", format);
    }

    protected Response createResponse(int code, String message, Format format) {
        Response response = new Response();
        StatusMessage statusMessage = new StatusMessage();
        statusMessage.setCode(code);
        if (message != null)
            statusMessage.setMessage(message);
        if (code >= ServiceException.BAD_REQUEST) {
            response.setStatusCode(statusMessage.getCode());
            response.setStatusMessage(statusMessage.getMessage());
        }
        if (format == Format.xml)
            response.setContent(statusMessage.getXml());
        else if (format == Format.json)
            response.setContent(statusMessage.getJsonString());
        else
            response.setContent("ERROR " + (code == 0 ? "" : code) + ": " + message);

        return response;
    }

    /**
     * Returns the service instance, consulting the service registry if necessary.
     */
    protected TextService getServiceInstance(Map<String,String> headers) throws ServiceException {
        try {
            String requestPath = headers.get(Listener.METAINFO_REQUEST_PATH);
            String[] pathSegments = requestPath != null ? requestPath.split("/") : null;
            if (pathSegments == null)
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Unable to find a service or handler for request path: " + requestPath);
            String contentType = headers.get(Listener.METAINFO_CONTENT_TYPE);
            String serviceClassName = MDW_REST_SERVICE_PROVIDER_PACKAGE + "." + pathSegments[0];
            try {
                // normal classloader -- built-in service
                Class<? extends TextService> serviceClass = Class.forName(serviceClassName).asSubclass(TextService.class);
                return serviceClass.newInstance();
            }
            catch (ClassNotFoundException ex) {
                // try dynamic based on annotations eg: api/Users/dxoakes
                Class<? extends RegisteredService> serviceType = Listener.CONTENT_TYPE_JSON.equals(contentType) ? JsonService.class : XmlService.class;
                MdwServiceRegistry registry = MdwServiceRegistry.getInstance();
                String pkgName = null;
                TextService matchingService = null;
                // process request (but @Paths take precedence)
                AssetRequest processRequest = ProcessRequests.getRequest(headers.get(Listener.METAINFO_HTTP_METHOD), requestPath);
                if (processRequest != null) {
                    matchingService = new ProcessInvoker(processRequest);
                }
                for (int i = 0; i < pathSegments.length; i++) {
                    String pathSegment = pathSegments[i];
                    if (i == 0)
                        pkgName = pathSegment;
                    else
                        pkgName += "." + pathSegment;
                    Package pkg = PackageCache.getPackage(pkgName);
                    if (pkg != null) {
                        TextService service = null;
                        if (i < pathSegments.length - 1) {
                            service = (TextService)registry.getDynamicServiceForPath(pkg, serviceType, "/" + pathSegments[i + 1]);
                        }
                        if (service == null && processRequest == null) {
                            // fallback -- try without any subpath (@Path="/")
                            service = (TextService) registry.getDynamicServiceForPath(pkg, serviceType, "/");
                        }
                        if (service != null)
                            matchingService = service;
                    }
                }

                if (matchingService != null)
                    return matchingService;


                return null;
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Default format is now JSON.
     */
    protected Format getFormat(Map<String,String> metaInfo) {
        Format format = Format.json;
        metaInfo.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_JSON);
        String formatParam = metaInfo.get("format");
        if (formatParam != null) {
            if (formatParam.equals("xml")) {
                format = Format.xml;
                metaInfo.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_XML);
            }
            else if (formatParam.equals("text")) {
                format = Format.text;
                metaInfo.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_TEXT);
            }
        }
        else {
            if (Listener.CONTENT_TYPE_XML.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE)) || Listener.CONTENT_TYPE_XML.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE.toLowerCase()))) {
                format = Format.xml;
                metaInfo.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_XML);
            }
            else if (Listener.CONTENT_TYPE_TEXT.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE)) || Listener.CONTENT_TYPE_TEXT.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE.toLowerCase()))) {
                format = Format.text;
                metaInfo.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_JSON);
            }
        }
        return format;
    }
}
