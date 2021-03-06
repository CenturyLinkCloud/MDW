package com.centurylink.mdw.script;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public class GroovyExecutor implements ScriptExecutor, ScriptEvaluator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final Object scriptCacheLock = new Object();

    private static String getRootDir() {
        String rootDirStr = ApplicationContext.getTempDirectory();
        File rootDir = new File(rootDirStr);
        if (!rootDir.exists())
            rootDir.mkdirs();
        return rootDirStr;
    }

    private static Map<String,String> scriptCache = new ConcurrentHashMap<>();
    public static void clearCache() {
        scriptCache.clear();
    }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private Binding binding;
    protected Binding getBinding() { return binding; }

    public Object execute(String script, Map<String,Object> bindings) throws ExecutionException {
            script += "\nreturn;";  // default to returning null

            binding = new Binding();

            for (String bindName : bindings.keySet()) {
                Object value = bindings.get(bindName);
                binding.setVariable(bindName, value);
            }

            Object retObj = runScript(script);

            for (String bindName : bindings.keySet()) {
                Object value = binding.getVariable(bindName);
                bindings.put(bindName, value);
            }

            return retObj;
    }

    public Object evaluate(String expression, Map<String, Object> bindings)
    throws ExecutionException {
        binding = new Binding();

        for (String bindName : bindings.keySet()) {
            binding.setVariable(bindName, bindings.get(bindName));
        }

        return runScript(expression);
    }

    protected Object runScript(String script) throws ExecutionException {
        try {
            CodeTimer timer = new CodeTimer("Create and cache groovy script", true);
            File groovyFile = new File(getRootDir() + "/" + name + ".groovy");
            String cached = scriptCache.get(name);
            if (!groovyFile.exists() || !script.equals(cached)) {
                boolean needsUpdate = true;
                if (groovyFile.exists()) {
                    FileInputStream fis = null;
                    String existingScript = null;
                    String existingScript2 = null;
                    try {
                        fis = new FileInputStream(groovyFile);
                        byte[] bytes = new byte[(int) groovyFile.length()];
                        fis.read(bytes);
                        existingScript = new String(bytes);
                        existingScript2 = existingScript + "\nreturn;";
                    }
                    finally {
                        if (fis != null)
                            fis.close();
                    }
                    if (script.equals(existingScript) || script.equals(existingScript2)) {
                        scriptCache.put(name, script);
                        needsUpdate = false;
                    }
                }
                if (needsUpdate) {
                    synchronized (scriptCacheLock) {
                        groovyFile = new File(getRootDir() + "/" + name + ".groovy");
                        cached = scriptCache.get(name);
                        if (!groovyFile.exists() || !script.equals(cached)) {
                            File rootDir = new File(getRootDir());
                            if (!rootDir.exists()) {
                                if (!rootDir.mkdirs())
                                    throw new ExecutionException("Failed to create script root dir: " + rootDir);
                            }
                            FileWriter writer = null;
                            try {
                                writer = new FileWriter(groovyFile);
                                writer.write(script);
                            } finally {
                                if (writer != null)
                                    writer.close();
                                timer.stopAndLogTiming("");
                            }
                            scriptCache.put(name, script);
                        }
                    }
                }
            }
            return getScriptEngine().run(name + ".groovy", binding);
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ExecutionException("Error executing Groovy script: '" + name + "'\n" + ex.toString(), ex);
        }
    }

    private static GroovyScriptEngine scriptEngine;
    private static synchronized GroovyScriptEngine getScriptEngine() throws DataAccessException, IOException, CachingException {

        if (scriptEngine == null || !AssetCache.isLoaded()) {
            CodeTimer timer = new CodeTimer("Initialize script libraries", true);
            initializeScriptLibraries();
            initializeDynamicJavaAssets();
            String[] rootDirs = new String[]{getRootDir()};
            if (PackageCache.getPackage(Package.MDW + ".base").getClassLoader() != null)
                scriptEngine = new GroovyScriptEngine(rootDirs, PackageCache.getPackage(Package.MDW + ".base").getClassLoader());
            else
                scriptEngine = new GroovyScriptEngine(rootDirs);
            // clear the cached library versions
            scriptEngine.getGroovyClassLoader().clearCache();
            timer.stopAndLogTiming("");
        }

        return scriptEngine;
    }

    public static void initialize() throws DataAccessException, IOException, CachingException {
        clearCache();
        scriptEngine = null;
        getScriptEngine();
    }

    private static void initializeScriptLibraries() throws DataAccessException, IOException, CachingException {
        // write groovy-language assets into the root directory
        logger.info("Initializing Groovy script assets...");

        for (Asset groovy : AssetCache.getAssets("groovy")) {
            Package pkg = PackageCache.getPackage(groovy.getPackageName());
            String packageName = pkg == null ? null : JavaNaming.getValidPackageName(pkg.getName());
            File dir = createNeededDirs(packageName);
            String filename = dir + "/" + groovy.getName();
            if (!filename.endsWith(".groovy"))
                filename += ".groovy";
            File file = new File(filename);
            logger.debug("  - writing " + file.getAbsoluteFile());
            if (file.exists())
                file.delete();

            String content = groovy.getText();
            if (content != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(content);
                }
            }
        }
        logger.info("Groovy script assets initialized.");
    }

    /**
     * so that Groovy scripts can reference these assets during compilation
     */
    private static void initializeDynamicJavaAssets() throws DataAccessException, IOException, CachingException {
        logger.info("Initializing Dynamic Java assets for Groovy...");

        for (Asset java : AssetCache.getAssets("java")) {
            Package pkg = PackageCache.getPackage(java.getPackageName());
            String packageName = pkg == null ? null : JavaNaming.getValidPackageName(pkg.getName());
            File dir = createNeededDirs(packageName);
            String filename = dir + "/" + java.getName();
            if (filename.endsWith(".java"))
                filename = filename.substring(0, filename.length() - 5);
            filename += ".groovy";
            File file = new File(filename);
            logger.mdwDebug("  - writing " + file.getAbsoluteFile());
            if (file.exists())
                file.delete();

            String content = java.getText();
            if (content != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(content);
                }
            }
        }
        logger.info("Dynamic Java assets initialized for groovy.");
    }

    private static File createNeededDirs(String packageName) {

        String path = getRootDir();
        File dir = new File(path);
        if (packageName != null) {
            StringTokenizer st = new StringTokenizer(packageName, ".");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                path += "/" + token;
                dir = new File(path);
                if (!dir.exists())
                    dir.mkdir();
            }
        }

        return dir;
    }

    public static ClassLoader getClassLoader() {
        try {
            return getScriptEngine().getGroovyClassLoader();
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }
}
