package com.centurylink.mdw.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames="discover", commandDescription="Discover asset packages from Maven or MDW", separators="=")
public class Discover extends Setup {

    @Parameter(names="--group-id", description="Maven group id")
    private String groupId;
    public String getGroupId() {
        return groupId;
    }

    @Parameter(names="--latest", description="Discover only latest asset package versions")
    private boolean latest = false;
    public boolean isLatest() { return latest; }
    public void setLatest(boolean latest) { this.latest = latest; }

    private JSONObject packages;
    public JSONObject getPackages() {
        return packages;
    }

    Discover() {
        // cli use
    }

    public Discover(String groupId, boolean latest) {
        this.groupId = groupId;
        this.latest = latest;
    }

    public Discover run(ProgressMonitor... monitors) throws IOException {
        if (groupId != null) {
            discoverMaven(groupId, monitors);
        }
        else {
            throw new IOException("Only maven discovery supported (groupId required)");
        }
        return this;
    }

    public void discoverMaven(String groupId, ProgressMonitor... monitors) throws IOException {
        JSONObject json = new JSONObject(searchArtifacts(groupId));
        packages = new JSONObject();
        packages.put("assetRoot", getAssetLoc());
        packages.put("packages", new JSONArray());
        if (json.has("response")) {
            JSONObject response = json.getJSONObject("response");
            if (response.has("docs")) {
                JSONArray artifactsArr = response.getJSONArray("docs");
                if (artifactsArr.length() == 0) {
                    if (getMdwVersion() != null && getMdwVersion().startsWith("6.0"))
                        setMdwVersion("6.0.12");
                    else
                        latest = true;
                    discoverMaven(groupId, monitors);
                }
                else {
                    for (int i = 0; i < artifactsArr.length(); i++) {
                        JSONObject artifact = artifactsArr.getJSONObject(i);
                        JSONArray array = packages.getJSONArray("packages");
                        JSONObject jsonObj = new JSONObject();
                        jsonObj.put("name", artifact.getString("g").replace("assets", "")
                                + artifact.getString("a").replace("-", "."));
                        jsonObj.put("artifact", artifact.getString("a"));
                        if (!latest)
                            jsonObj.put("version", artifact.getString("v"));
                        else
                            jsonObj.put("version", artifact.getString("latestVersion"));
                        array.put(jsonObj);
                    }
                }
            }
        }
    }

    protected String searchArtifacts(String groupId) throws IOException {
        StringBuilder query = new StringBuilder(
                "http://search.maven.org/solrsearch/select?wt=json&rows=1000");
        query.append("&q=");
        if (groupId != null)
            query.append("g:").append(groupId);
        if (!latest)
            if (getMdwVersion() != null)
                query.append("+AND+v:").append(getMdwVersion().split("-")[0]);
            else
                query.append("&core=gav");

        String url = query.toString();

        getOut().println("Discovering assets from: " + url);

        URLConnection connection = new URL(url).openConnection();
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder content = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            content.append(line + "\n");
        }
        bufferedReader.close();
        return content.toString();
    }
}
