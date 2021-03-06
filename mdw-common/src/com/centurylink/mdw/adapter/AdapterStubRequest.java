package com.centurylink.mdw.adapter;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.JsonUtil;

public class AdapterStubRequest implements Jsonable {

    public static final String JSON_NAME = "AdapterStubRequest";

    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String masterRequestId) { this.masterRequestId = masterRequestId; }

    private String url;
    public String getUrl() { return url; }
    public void setUrl(String endpoint) { this.url = endpoint; }

    private String method;
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    private Map<String,String> headers;
    public Map<String,String> getHeaders() { return headers; }
    public void setHeaders(Map<String,String> headers) { this.headers = headers; }

    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public AdapterStubRequest(String masterRequestId, String content) {
        this.masterRequestId = masterRequestId;
        this.content = content;
    }

    public AdapterStubRequest(JSONObject json) throws JSONException {
        JSONObject requestJson = json.getJSONObject(getJsonName());
        this.masterRequestId = requestJson.getString("masterRequestId");
        this.content = requestJson.getString("content");
        if (requestJson.has("url"))
            this.url = requestJson.getString("url");
        if (requestJson.has("method"))
            this.method = requestJson.getString("method");
        if (requestJson.has("headers"))
            this.headers = JsonUtil.getMap(requestJson.getJSONObject("headers"));
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        JSONObject requestJson = create();
        requestJson.put("masterRequestId", masterRequestId);
        requestJson.put("content", content);
        if (url != null)
            requestJson.put("url", url);
        if (method != null)
            requestJson.put("method", method);
        if (headers != null)
            requestJson.put("headers", JsonUtil.getJson(headers));
        json.put(getJsonName(), requestJson);
        return json;
    }

    public String getJsonName() {
        return JSON_NAME;
    }

}
