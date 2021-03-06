package com.centurylink.mdw.test;

import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Status;

public class ApiResponse implements Jsonable, Verifiable {

    public ApiResponse(JSONObject json) {
        bind(json);
    }

    private Status status;
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    private Map<String,String> headers;
    public Map<String,String> getHeaders() { return headers; }
    public void setHeaders(Map<String,String> headers) { this.headers = headers; }

    private int time;
    public int getTime() { return time; }
    public void setTime(int time) { this.time = time; }

    private String body;
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    /**
     * Case-insensitive lookup.  Returns the value with it's original capitalization.
     */
    public String getHeader(String name) {
        for (String key : headers.keySet()) {
            if (key.equalsIgnoreCase(name))
                return headers.get(key);
        }
        return null;
    }

    /**
     * Verifies that a header matches the expected value.
     * Both name and value are case-insensitive.
     */
    public boolean checkHeader(String name, String value) {
        String val = getHeader(name);
        return val == null ? value == null : val.equalsIgnoreCase(value);
    }

    /**
     * Verifier options.
     */
    private Map<String,Object> options;
    public Map<String,Object> getOptions() { return options; }
    public void setOptions(Map<String,Object> options) {
        this.options = options;
    }

    /**
     * Verifier values.
     */
    private Map<String,Object> values;
    public Map<String,Object> getValues() { return values; }
    public void setValues(Map<String,Object> values) {
        this.values = values;
    }

    private boolean success;
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
