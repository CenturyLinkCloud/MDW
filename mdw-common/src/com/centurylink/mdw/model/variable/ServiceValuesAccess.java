package com.centurylink.mdw.model.variable;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.variable.VariableTranslator;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared attributes with standardized default naming.
 */
public class ServiceValuesAccess {

    public static final String REQUEST_VARIABLE = "Request Variable";
    public static final String REQUEST_HEADERS_VARIABLE = "Request Headers Variable";
    public static final String RESPONSE_VARIABLE = "Response Variable";
    public static final String RESPONSE_HEADERS_VARIABLE = "Response Headers Variable";

    private ProcessRuntimeContext context;

    public ServiceValuesAccess(ProcessRuntimeContext context) {
        this.context = context;
    }

    public Object getRequest() {
        return context.getValues().get(getRequestVariableName());
    }

    /**
     * Most usages do not provide a way to set the REQUEST_VARIABLE attribute.
     * For these cases, a custom .impl asset would be needed to configure through
     * Studio or MDWHub.
     */
    public String getRequestVariableName() {
        return context.getAttribute(REQUEST_VARIABLE, "request");
    }

    @SuppressWarnings("unchecked")
    public Map<String,String> getRequestHeaders() {
        return (Map<String,String>)context.getValues().get(getRequestHeadersVariableName());
    }

    /**
     * Most usages do not provide a way to set the REQUEST_HEADERS_VARIABLE attribute.
     * For these cases, a custom .impl asset would be needed to configure through
     * Studio or MDWHub.
     */
    public String getRequestHeadersVariableName() {
        return context.getAttribute(REQUEST_HEADERS_VARIABLE, "requestHeaders");
    }

    public Object getResponse() {
        return context.getValues().get(getResponseVariableName());
    }

    /**
     * Most usages do not provide a way to set the RESPONSE_VARIABLE attribute.
     * For these cases, a custom .impl asset would be needed to configure through
     * Studio or MDWHub.
     */
    public String getResponseVariableName() {
        return context.getAttribute(RESPONSE_VARIABLE, "response");
    }

    @SuppressWarnings("unchecked")
    public Map<String,String> getResponseHeaders() {
        return (Map<String,String>)context.getValues().get(getResponseHeadersVariableName());
    }

    /**
     * Most usages do not provide a way to set the RESPONSE_HEADERS_VARIABLE attribute.
     * For these cases, a custom .impl asset would be needed to configure through
     * Studio or MDWHub.
     */
    public String getResponseHeadersVariableName() {
        return context.getAttribute(RESPONSE_HEADERS_VARIABLE, "responseHeaders");
    }

    public String getHttpMethod() {
        Map<String,String> requestHeaders = getRequestHeaders();
        if (requestHeaders == null)
            return null;
        return requestHeaders.get(Listener.METAINFO_HTTP_METHOD);
    }

    /**
     * Always begins with "/".
     */
    public String getRequestPath() {
        Map<String,String> requestHeaders = getRequestHeaders();
        if (requestHeaders == null)
            return null;
        String path = requestHeaders.get(Listener.METAINFO_REQUEST_PATH);
        return path.startsWith("/") ? path : "/" + path;
    }

    public Query getQuery() {
        return new Query(getRequestPath(), getParameters(getRequestHeaders()));
    }

    protected Map<String,String> getParameters(Map<String,String> headers) {
        Map<String,String> params = new HashMap<String,String>();
        String query = headers.get(Listener.METAINFO_REQUEST_QUERY_STRING);
        if (query == null)
            query = headers.get("request-query-string");
        if (query != null && !query.isEmpty()) {
            for (String pair : query.split("&")) {
                int idx = pair.indexOf("=");
                try {
                    params.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
                catch (UnsupportedEncodingException ex) {
                    // as if UTF-8 is going to be unsupported
                }
            }
        }
        return params;
    }

    public JSONObject toJson(String variableName, Object objectValue) throws TranslationException {
        if (objectValue == null)
            return null;
        Variable variable = context.getProcess().getVariable(variableName);
        VariableTranslator translator = context.getPackage().getTranslator(variable.getType());
        if (translator instanceof JsonTranslator) {
            return ((JsonTranslator)translator).toJson(objectValue);
        }
        else {
            throw new TranslationException("Cannot convert to JSON using " + translator.getClass());
        }
    }

    public Object fromJson(String variableName, JSONObject json, String type) throws TranslationException {
        if (json == null)
            return null;
        Variable variable = context.getProcess().getVariable(variableName);
        VariableTranslator translator = context.getPackage().getTranslator(variable.getType());
        if (translator instanceof JsonTranslator) {
            return ((JsonTranslator)translator).fromJson(json, type);
        }
        else {
            throw new TranslationException("Cannot convert from JSON using " + translator.getClass());
        }
    }

}
