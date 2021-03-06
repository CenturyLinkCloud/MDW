package com.centurylink.mdw.pkg;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * ClassLoader for MDW asset packages.  Each package gets its own loader.
 */
public class PackageClassLoader extends ClassLoader {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private List<File> classpath;
    public List<File> getClasspath() { return classpath; }

    private Package mdwPackage;

    private File assetRoot;

    private static List<Asset> cachedJarAssets = null;
    private List<Asset> jarAssets = null;
    public synchronized List<Asset> getJarAssets() {
        List<Asset> newJarAssets = AssetCache.getAssets("jar");

        if (cachedJarAssets != newJarAssets) {
            cachedJarAssets = newJarAssets;
            jarAssets = null;
        }

        if (jarAssets == null) {
            jarAssets = new ArrayList<>();
            jarAssets.addAll(newJarAssets);
            // same-package jars go first
            if (this.mdwPackage.getName() != null) {
                Collections.sort(jarAssets, (rs1, rs2) -> {
                    String pkgName = mdwPackage.getName();
                    if (pkgName.equals(rs1.getPackageName()) && !pkgName.equals(rs2.getPackageName()))
                        return -1;
                    else if (pkgName.equals(rs2.getPackageName()) && !pkgName.equals(rs1.getPackageName()))
                        return 1;
                    else
                        return 0;
                });
            }
        }
        return jarAssets;
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public PackageClassLoader(Package pkg) {
        super(CompiledJavaCache.DynamicJavaClassLoader.getInstance(pkg.getClass().getClassLoader(), pkg, true));
        mdwPackage = pkg;

        classpath = new ArrayList<>();

        String cp = PropertyManager.getProperty(PropertyNames.MDW_JAVA_RUNTIME_CLASSPATH);
        if (cp != null) {
            String[] cps = cp.trim().split(File.pathSeparator);
            for (int i = 0; i < cps.length; i++) {
                classpath.add(new File(cps[i]));
            }
        }

        String libdir = PropertyManager.getProperty(PropertyNames.MDW_JAVA_LIBRARY_PATH);
        if (libdir != null) {
            File dir = new File(libdir);
            if (dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    if (f.getName().endsWith(".jar"))
                      classpath.add(f);
                }
            }
        }

        // add kotlin classes dir in case it exists
        classpath.add(new File(ApplicationContext.getTempDirectory() + "/kotlin/classes"));

        String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
        if (assetLoc != null)
            assetRoot = new File(assetLoc);
    }

    private static Map<String,Class<?>> sharedClassCache = new ConcurrentHashMap<>();

    public Class<?> directFindClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] b = null;
        Class<?> found;
        try {
            // try shared cache
            found = sharedClassCache.get(name);
            if (found != null)
                return found;

            // Check that we didn't already look for it
            b = classesFound.get(name);
            if (b == null) {
                // next try dynamic jar assets
                String path = name.replace('.', '/') + ".class";
                b = findInJarAssets(path);
                // lastly try the file system
                if (b == null)
                    b = findInFileSystem(path);
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(),  ex);
        }

        if (b == null || b.length ==  0) {
            if (b == null)
                classesFound.put(name, new byte[0]);  // byte[0] means we didn't find it
            throw new ClassNotFoundException(name);
        }
        else {
            found = sharedClassCache.get(name);
            if (found != null)
                return found;
        }

        if (logger.isMdwDebugEnabled())
            logger.mdwDebug("Class " + name + " loaded by package classloader for: " + mdwPackage.getLabel());

        String pkgName = mdwPackage.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0)
            pkgName = name.substring(0, lastDot);

        java.lang.Package pkg = getPackage(pkgName);
        if (pkg == null)
            definePackage(pkgName, null, null, null, "MDW",
                    mdwPackage.getVersion().toString(), "CenturyLink", null);
        found = defineClass(name, b, 0, b.length);
        Class<?> temp = sharedClassCache.putIfAbsent(name, found);
        classesFound.put(name, new byte[1]);  // byte[1] means found and loaded

        return temp == null ? found : temp;
    }

    private byte[] findInFileSystem(String path) throws IOException {
        byte[] b = null;
        for (int i = 0; b == null && i < classpath.size(); i++) {
            File file = classpath.get(i);
            if (file.isDirectory()) {
                b = findInDirectory(file, path);
            }
            else if (file.isFile()) {
                String filepath = file.getPath();
                if (filepath.endsWith(".jar")) {
                    b = findInJarFile(file, path);
                }
            }
        }
        return b;
    }

    private byte[] findInDirectory(File dir, String path) throws IOException {
        byte[] b = null;
        File f = new File(dir.getPath() + "/" + path);
        if (f.exists()) {
            FileInputStream fi = null;
            try {
                fi = new FileInputStream(f);
                b = new byte[fi.available()];
                fi.read(b);
                fi.close();
            }
            finally {
                if (fi != null)
                    fi.close();
            }
        }
        return b;
    }

    private byte[] findInJarFile(File jar, String path) throws IOException {
        byte[] b = null;
        InputStream is = null;
        try (JarFile jf = new JarFile(jar)){
            ZipEntry ze = jf.getEntry(path);
            if (ze != null) {
                int k, n, m;
                is = jf.getInputStream(ze);
                n = is.available();
                m = 0;
                k = 1;
                b = new byte[n];
                while (m < n && k > 0) {
                    k = is.read(b, m, n-m);
                    if (k > 0)
                        m += k;
                }
                if (m < n) {
                    String msg = "Package class loader: expect " + n + ", read " + m;
                    throw new IOException(msg);
                }
            }
        }
        finally {
            if (is != null)
                is.close();

        }
        return b;
    }

    private byte[] findInJarAssets(String path) throws IOException {
        // prefer assets in the same package
        byte[] b = null;
        if (assetRoot != null) {
            for (Asset jarAsset : getJarAssets()) {
                File jarFile = jarAsset.getFile();
                if (jarFile == null)
                    jarFile = new File(assetRoot + "/" + jarAsset.getPackageName().replace('.', '/') + "/" + jarAsset.getName());
                b = findInJarFile(jarFile, path);
                if (b != null)
                    return b;
            }
        }
        return b;
    }

    private static Map<String,byte[]> classesFound = new ConcurrentHashMap<>();

    /**
     * Looks for classes in Jar assets or on the prop-specified classpath.
     * Results are cached, so Jar/CP changes require app redeployment.
     */
    public boolean hasClass(String name) {
        byte[] b = classesFound.get(name);
        Boolean found = b == null ? null : b.length > 0;
        if (found != null)
            return found;

        try {
            // try dynamic jar assets
            String path = name.replace('.', '/') + ".class";
            b = findInJarAssets(path);
            // try the file system
            if (b == null)
                b = findInFileSystem(path);
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(),  ex);
        }
        found = b != null;
        classesFound.put(name, found ? b : new byte[0]);
        return found;
    }

    /**
     * This is used by XMLBeans for loading the type system.
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] b = null;
        try {
            Asset resource = AssetCache.getAsset(mdwPackage.getName() + "/" + name);
            if (resource != null)
                b = resource.getContent();
            if (b == null)
                b = findInJarAssets(name);
            if (b == null)
                b = findInFileSystem(name);
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(),  ex);
        }

        if (b == null)
            return super.getResourceAsStream(name);
        else
            return new ByteArrayInputStream(b);
    }

    @Override
    public URL getResource(String name) {
        URL result = null;
        for (int i = 0; result == null && i < classpath.size(); i++) {
            File one = classpath.get(i);
            if (one.isDirectory()) {
                File searchResource = new File(one.getPath() + "/" + name);
                if ( searchResource.exists() ) {
                    try {
                        result = searchResource.toURI().toURL();
                    } catch (MalformedURLException mfe) {
                        result = null;
                    }
                }
            }
        }
        if (result == null) {
            if (assetRoot != null) {
                String sep = File.separator.equals("/") ? "" : "/";
                for (Asset jarAsset : getJarAssets()) {
                    File jarFile = jarAsset.getFile();
                    if (jarFile == null)
                        jarFile = new File(assetRoot + "/" + jarAsset.getPackageName().replace('.', '/') + "/" + jarAsset.getName());
                    try (JarFile jf = new JarFile(jarFile)) {
                        ZipEntry entry = jf.getEntry(name);
                        if (entry == null && name.startsWith("/"))
                            entry = jf.getEntry(name.substring(1));
                        if (entry != null) {
                            return new URL("jar:file:" + sep + jarFile.getAbsolutePath().replace('\\', '/') + "!/" + name);
                        }
                    }
                    catch (Exception ex) {
                        logger.error("Error loading resource: " + name, ex);
                    }
                }
            }

            result = super.findResource(name);
        }
        return result;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Vector<URL> resUrls = null;

        if (assetRoot != null) {
            String sep = File.separator.equals("/") ? "" : "/";
            for (Asset jarAsset : getJarAssets()) {
                File jarFile = jarAsset.getFile();
                if (jarFile == null)
                    jarFile = new File(assetRoot + "/" + jarAsset.getPackageName().replace('.', '/') + "/" + jarAsset.getName());
                try (JarFile jf = new JarFile(jarFile)) {
                    if (jf.getEntry(name) != null) {
                        if (resUrls == null)
                            resUrls = new Vector<>();
                        resUrls.addElement(new URL("jar:file:" + sep + jarFile.getAbsolutePath().replace('\\', '/') + "!/" + name));
                    }
                }
                catch (Exception ex) {
                    logger.error("Error loading resource: " + name, ex);
                }
            }
        }
        if (resUrls != null) {
            Enumeration<URL> superEnum = super.getResources(name);
            while (superEnum.hasMoreElements()) {
                resUrls.addElement(superEnum.nextElement());
            }
            return resUrls.elements();
        }

        return super.getResources(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> loaded = null;
        if (CompiledJavaCache.classicLoading)  //Classic loading, delegate to parent loaders, assets are searched last
            loaded = super.loadClass(name);
        else
            loaded = getParent().loadClass(name); //Search assets first

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("Loaded class: '" + name + "' from package classloader with parent: " + getParent());
            if (logger.isTraceEnabled())
                logger.trace("Stack trace: ", new Exception("ClassLoader stack trace"));
        }
        return loaded;
    }

    public String getPackageName() {
        return mdwPackage.getName();
    }

    public static void clearCaches() {
        sharedClassCache.clear();
        sharedClassCache = new ConcurrentHashMap<>();
        classesFound.clear();
        classesFound = new ConcurrentHashMap<>();
        cachedJarAssets = null;
    }

    public String toString() {
        return this.getClass() + (mdwPackage == null ? "null" : (":" + mdwPackage.getName()));
    }

}
