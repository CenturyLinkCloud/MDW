package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * List of root milestones (without children).
 */
public class MilestonesList implements Jsonable {

    public MilestonesList(List<Milestone> milestones, long total) {
        this.milestones = milestones;
        this.total = total;
    }

    private List<Milestone> milestones;
    public List<Milestone> getMilestones() { return milestones; }
    public void setMilestones(List<Milestone> milestones) { this.milestones = milestones; }

    private long total;
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("total", total);
        JSONArray array = new JSONArray();
        if (milestones != null) {
            for (Milestone milestone : milestones)
                array.put(milestone.getJson());
        }
        json.put(getJsonName(), array);
        return json;
    }

    @Override
    public String getJsonName() { return "milestones"; }
}
