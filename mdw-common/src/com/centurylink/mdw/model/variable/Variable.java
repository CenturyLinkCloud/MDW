package com.centurylink.mdw.model.variable;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.Value.Display;
import com.centurylink.mdw.model.Yamlable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class Variable implements Comparable<Variable>, Jsonable, Yamlable {

    public static final int CAT_LOCAL = 0;
    public static final int CAT_INPUT = 1;
    public static final int CAT_OUTPUT = 2;
    public static final int CAT_INOUT = 3;
    public static final int CAT_STATIC = 4;

    // built-in variable names
    public static final String MASTER_REQUEST_ID = "MasterRequestID";
    public static final String PROCESS_INSTANCE_ID = "ProcessInstanceID";
    public static final String ACTIVITY_INSTANCE_ID = "ActivityInstanceID";

    public static final Integer DATA_REQUIRED = 0;
    public static final Integer DATA_OPTIONAL = 1;
    public static final Integer DATA_READONLY = 2;
    public static final Integer DATA_HIDDEN = 3;
    public static final Integer DATA_EXCLUDED = 4;

    private Long id;
    private String type;
    private String name;
    private String description;
    private String label;
    private Integer displayMode;
    private Integer displaySequence;

    public Variable() {
    }

    public Variable(String name, String type) {
        this.id = 0L;
        this.name = name;
        this.type = type;
    }

    public Variable(Long id, String name, String type,
            String label, Integer displayMode, Integer displaySequence) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.label = label;
        this.displayMode = displayMode;
        this.displaySequence = displaySequence;
    }

    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return this.label;
    }
    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getDisplaySequence() {
        return displaySequence;
    }
    public void setDisplaySequence(Integer displaySequence) {
        this.displaySequence = displaySequence;
    }

    public Integer getDisplayMode() {
        return this.displayMode;
    }
    public void setDisplayMode(Integer displayMode) {
        this.displayMode = displayMode;
    }
    public boolean isRequired() {
        return getDisplayMode().equals(Variable.DATA_REQUIRED);
    }

    public Integer getVariableCategory() {
        return displayMode;
    }

    public void setVariableCategory(Integer variableCategory) {
        this.displayMode = variableCategory;
    }

    public int compareTo(Variable other) {
        if (other == null)
          return 1;

        return this.getName().compareTo(other.getName());
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isInput() {
        return getVariableCategory() == CAT_INPUT || getVariableCategory() == CAT_INOUT;
    }

    public boolean isOutput() {
        return getVariableCategory() == CAT_OUTPUT || getVariableCategory() == CAT_INOUT;
    }

    public boolean isString() {
        return String.class.getName().equals(getType()) || StringDocument.class.getName().equals(getType());
    }

    public boolean isJavaObject() {
        return Object.class.getName().equals(getType());
    }

    public String getCategory() {
        int cat = getVariableCategory() == null ? 0 : getVariableCategory();
        if (cat == CAT_INPUT)
            return "INPUT";
        else if (cat == CAT_OUTPUT)
            return "OUTPUT";
        else if (cat == CAT_INOUT)
            return "INOUT";
        else if (cat == CAT_STATIC)
            return "STATIC";
        else
            return "LOCAL";
    }

    private Display display;
    public Display getDisplay() { return display; }
    public void setDisplay(Display display) { this.display = display; }

    public int getCategoryCode(String category) {
        if ("INPUT".equals(category))
            return CAT_INPUT;
        else if ("OUTPUT".equals(category))
            return CAT_OUTPUT;
        else if ("INOUT".equals(category))
            return CAT_INOUT;
        else if ("STATIC".equals(category))
            return CAT_STATIC;
        else
            return CAT_LOCAL;
    }

    public Value toValue() {
        Value value = new Value(getName());
        value.setType(getType());
        if (getDisplay() != null)
            value.setDisplay(getDisplay());
        if (getDisplaySequence() != null)
            value.setSequence(getDisplaySequence());
        if (getLabel() != null)
            value.setLabel(getLabel());
        return value;
    }

    public Variable(JSONObject json) throws JSONException {
        if (json.has("name"))
            this.name = json.getString("name");
        this.type = json.getString("type");
        if (json.has("category"))
            this.setVariableCategory(getCategoryCode(json.getString("category")));
        if (json.has("label"))
            this.label = json.getString("label");
        if (json.has("sequence"))
            this.displaySequence = json.getInt("sequence");
        if (json.has("display"))
            this.display = Display.valueOf(json.getString("display"));
        this.setId(0L);
    }

    public Variable(Map<String,Object> yaml) {
        if (yaml.containsKey("name"))
            this.name = (String)yaml.get("name");
        this.type = (String)yaml.get("type");
        if (yaml.containsKey("category"))
            this.setVariableCategory(getCategoryCode((String)yaml.get("category")));
        if (yaml.containsKey("label"))
            this.label = (String)yaml.get("label");
        if (yaml.containsKey("sequence"))
            this.displaySequence = (int)yaml.get("sequence");
        if (yaml.containsKey("display"))
            this.display = Display.valueOf((String)yaml.get("display"));
        this.setId(0L);
    }

    /**
     * Serialized as an object, so name is not included.
     */
    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("type", type);
        json.put("category", getCategory());
        if (label != null && !label.isEmpty())
            json.put("label", label);
        if (displaySequence != null && displaySequence > 0)
            json.put("sequence", displaySequence);
        if (display != null)
            json.put("display", display.toString());
        return json;
    }

    public String getJsonName() {
        return getName();
    }

    @Override
    public Map<String,Object> getYaml() {
        Map<String,Object> yaml = Yamlable.create();
        yaml.put("type", type);
        yaml.put("category", getCategory());
        if (label != null && !label.isEmpty())
            yaml.put("label", label);
        if (displaySequence != null && displaySequence > 0)
            yaml.put("sequence", displaySequence);
        if (display != null)
            yaml.put("display", display.toString());
        return yaml;
    }

    public String toString() {
        return getJson().toString(2);
    }
}
