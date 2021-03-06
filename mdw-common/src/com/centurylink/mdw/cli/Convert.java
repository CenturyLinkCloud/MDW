package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.config.OrderedProperties;
import com.centurylink.mdw.config.YamlBuilder;
import com.centurylink.mdw.config.YamlProperties;
import com.centurylink.mdw.file.VersionProperties;
import com.centurylink.mdw.model.Yamlable;
import com.centurylink.mdw.model.asset.AssetPath;
import com.centurylink.mdw.model.asset.Pagelet;
import com.centurylink.mdw.model.system.MdwVersion;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.file.Packages;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Parameters(commandNames="convert", commandDescription="Convert mdw/app property files, or package.json files to yaml", separators="=")
public class Convert extends Setup  {

    @Parameter(names = "--packages", description = "Update package.json files to package.yaml (ignores other options)")
    private boolean packages;

    public boolean isPackages() {
        return packages;
    }

    public void setPackages(boolean packages) {
        this.packages = packages;
    }

    @Parameter(names = "--processes", description = "Update .proc json files to yaml format (ignores other options)")
    private boolean processes;

    public boolean isProcesses() {
        return processes;
    }

    public void setProcesses(boolean processes) {
        this.processes = processes;
    }

    @Parameter(names = "--input", description = "Input property file or impl file")
    private File input;

    public File getInput() {
        return input;
    }

    @Parameter(names = "--map", description = "Optional compatibility mapping file")
    private File map;
    public File getMap() {
        return map;
    }

    @Parameter(names = "--prefix", description = "Optional common prop prefix")
    private String prefix;

    public String getPrefix() {
        return prefix;
    }

    @Parameter(names = "--language", description = "Output language for impls")
    private String language;
    public String getLanguage() {
        return language;
    }

    Convert() {

    }

    public Convert(File input) {
        this.input = input;
    }

    @Override
    public List<Dependency> getDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(new Dependency("org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar", 41203L));
        return dependencies;
    }

    @Override
    public Convert run(ProgressMonitor... progressMonitors) throws IOException {

        if (isPackages()) {
            convertPackages();
        } else if (isProcesses()) {
            convertProcesses();
        } else {
            if (input != null && input.getName().endsWith(".impl")) {
                convertImplementor();
            }
            if (input != null && input.getName().endsWith(".evth")) {
                convertEventHandler();
            }
            else {
                convertProperties();
            }
        }

        return this;
    }

    protected void convertPackages() throws IOException {

        getOut().println("Converting packages:");
        // use old-school Packages to find JSON packages
        Map<String,File> packageDirs = new Packages(getAssetRoot());
        for (String packageName : packageDirs.keySet()) {
            getOut().println("  " + packageName);
            File packageDir = packageDirs.get(packageName);
            File metaDir = new File(packageDir + "/" + META_DIR);
            File yamlFile = new File(metaDir + "/package.yaml");
            File jsonFile = new File(metaDir + "/package.json");
            if (yamlFile.exists()) {
                if (jsonFile.exists()) {
                    getOut().println("    Removing redundant file: " + jsonFile);
                    new Delete(jsonFile).run();
                } else {
                    getOut().println("    Ignoring existing: " + yamlFile);
                }
            } else {
                getOut().println("    Converting: " + jsonFile);
                JSONObject json = new JSONObject(new String(Files.readAllBytes(Paths.get(jsonFile.getPath()))));
                Map<String, String> vals = new HashMap<>();
                vals.put("name", json.getString("name"));
                MdwVersion mdwVersion = new MdwVersion(json.getString("version"));
                int version = mdwVersion.getIntVersion() + 1;
                vals.put("version", new MdwVersion(version).toString());
                String schemaVer = json.optString("schemaVer");
                if (schemaVer.isEmpty() || schemaVer.startsWith("5") || schemaVer.equals("6.0")) {
                    schemaVer = "6.1";
                }
                vals.put("schemaVersion", schemaVer);
                YamlBuilder yamlBuilder = new YamlBuilder();
                yamlBuilder.append(vals);
                getOut().println("    Writing: " + yamlFile);
                Files.write(Paths.get(yamlFile.getPath()), yamlBuilder.toString().getBytes());
                getOut().println("    Deleting: " + jsonFile);
                new Delete(jsonFile).run();
            }
        }
    }

    protected void convertProcesses() throws IOException {
        getOut().println("Converting processes:");
        Map<String, List<File>> pkgProcFiles = findAllAssets("proc");
        for (String pkg : pkgProcFiles.keySet()) {
            List<File> procFiles = pkgProcFiles.get(pkg);
            for (File procFile : procFiles) {
                if (new String(Files.readAllBytes(procFile.toPath())).startsWith("{")) {
                    Process process = loadProcess(pkg, procFile, true);
                    String yaml = Yamlable.toString(process, 2);
                    getOut().println("  saving " + procFile);
                    Files.write(procFile.toPath(), yaml.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    File versionsFile = new File(procFile.getParent() + "/" + META_DIR + "/versions");
                    VersionProperties versionProperties = new VersionProperties(versionsFile);
                    versionProperties.setProperty(process.getName() + ".proc", String.valueOf(process.getVersion() + 1));
                    versionProperties.save();
                }
            }
        }
    }

    protected void convertProperties() throws IOException {
        InputStream mapIn;
        if (map == null) {
            mapIn = getClass().getClassLoader().getResourceAsStream("META-INF/mdw/configurations.map");
        } else {
            getOut().println("Mapping from " + map.getAbsolutePath());
            mapIn = new FileInputStream(map);
        }
        Properties mapProps = new Properties();
        mapProps.load(mapIn);

        if (input == null) {
            input = new File(getConfigRoot() + "/mdw.properties");
        }
        getOut().println("Loading properties from " + input.getAbsolutePath());
        Properties inputProps = new OrderedProperties();
        inputProps.load(new FileInputStream(input));

        String baseName = input.getName().substring(0, input.getName().lastIndexOf('.'));
        if (prefix == null && baseName.equals("mdw"))
            prefix = "mdw";

        try {
            YamlBuilder yamlBuilder = YamlProperties.translate(prefix, inputProps, mapProps);
            File out = new File(getConfigRoot() + "/" + baseName + ".yaml");
            getOut().println("Writing output config: " + out.getAbsolutePath());
            Files.write(Paths.get(out.getPath()), yamlBuilder.toString().getBytes());
        } catch (ReflectiveOperationException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    private void convertImplementor() throws IOException {
        JSONObject implJson = new JSONObject(new String(Files.readAllBytes(input.toPath())));
        String implClass = implJson.getString("implementorClass");
        String outPath = getAssetRoot() + "/" + implClass.replace(".", "/");
        String suffix = "kotlin".equals(language) || "kt".equals(language) ? "kt" : "java";
        File outFile = new File(outPath + "." + suffix);
        if (outFile.isFile()) {
            throw new IOException("Asset file already exists: " + outFile);
        }
        if (!outFile.getParentFile().isDirectory() && !outFile.getParentFile().mkdirs()) {
            throw new IOException("Asset path cannot be created: " + outFile);
        }

        getOut().println("Creating: " + outFile);

        String label = implJson.getString("label");
        String imports = "import com.centurylink.mdw.annotations.Activity" + (suffix.equals("kt") ? "" : ";") + "\n";
        String annotations = "@Activity(value=\"" + label + "\"";

        String category = "com.centurylink.mdw.activity.types.GeneralActivity";
        if (implJson.has("category")) {
            category = implJson.getString("category");
            imports += "import " + category + (suffix.equals("kt") ? "" : ";") + "\n";
        }
        String categoryClass = category.substring(category.lastIndexOf('.') + 1);
        annotations += ", category=" + categoryClass + (suffix.equals("kt") ? "::class" : ".class");

        String icon = "shape:activity";
        if (implJson.has("icon"))
            icon = implJson.getString("icon");
        annotations += ", icon=\"" + icon + "\"";

        String pageletXml = implJson.has("pagelet") ? implJson.getString("pagelet") : null;
        if (pageletXml != null) {
            try {
                JSONObject pagelet = new Pagelet(pageletXml).getJson();
                getOut().println("pagelet for formatted pasting: " + pagelet.toString(2));
                annotations += ",\n                pagelet=" + JSONObject.quote(pagelet.toString());
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        annotations += ")\n";

        downloadTemplates();
        File templateFile = new File(getTemplateDir() + "/assets/code/activity/general_" + suffix);
        String template = new String(Files.readAllBytes(templateFile.toPath()));
        Map<String,Object> values = new HashMap<>();
        values.put("packageName", implClass.substring(0, implClass.lastIndexOf(".")));
        values.put("className", implClass.substring(implClass.lastIndexOf(".") + 1));
        values.put("annotationsImports", imports);
        values.put("annotations", annotations);
        String source = substitute(template, values);
        Files.write(outFile.toPath(), source.getBytes(), StandardOpenOption.CREATE_NEW);
    }

    private void convertEventHandler() throws IOException {
        JSONObject evthJson = new JSONObject(new String(Files.readAllBytes(input.toPath())));
        Map<String,Object> values = new HashMap<>();
        values.put("path", evthJson.getString("path"));
        values.put("routing", "Content");
        String templateName;
        String outPath = getAssetRoot().toString();
        AssetPath assetPath = getAssetPath(input);
        values.put("className", assetPath.rootName());
        if (!assetPath.toString().startsWith("..")) {
            values.put("packageName", assetPath.pkg);
            outPath += "/" + assetPath.pkgPath() + "/" + assetPath.rootName();
        }
        else {
            outPath = "./" + assetPath.rootName();
        }
        String handlerClass = evthJson.getString("handlerClass");
        if (handlerClass.startsWith("START_PROCESS")) {
            templateName = "process_run";
            values.put("process", handlerClass.substring(26));
        }
        else if (handlerClass.startsWith("NOTIFY_PROCESS")) {
            templateName = "process_notify";
            values.put("event", handlerClass.substring(25));
        }
        else {
            templateName = "general_java";
        }

        downloadTemplates();
        String suffix = "kotlin".equals(language) || "kt".equals(language) ? "kt" : "java";
        File templateFile = new File(getTemplateDir() + "/assets/code/handler/" + templateName + "_" + suffix);
        File outFile = new File(outPath + "." + suffix);
        if (outFile.isFile()) {
            throw new IOException("Asset file already exists: " + outFile);
        }
        if (!outFile.getParentFile().isDirectory() && !outFile.getParentFile().mkdirs()) {
            throw new IOException("Asset path cannot be created: " + outFile);
        }
        String template = new String(Files.readAllBytes(templateFile.toPath()));
        String source = substitute(template, values);
        Files.write(outFile.toPath(), source.getBytes(), StandardOpenOption.CREATE_NEW);
    }
}