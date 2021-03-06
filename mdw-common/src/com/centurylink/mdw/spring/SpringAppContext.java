package com.centurylink.mdw.spring;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.dataaccess.task.CombinedTaskRefData;
import com.centurylink.mdw.dataaccess.task.MdwTaskRefData;
import com.centurylink.mdw.dataaccess.task.TaskRefData;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.variable.VariableTranslator;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Currently only used in Tomcat.  Allows injection through Spring workflow assets.
 */
public class SpringAppContext implements CacheService {

    public static final String SPRING_CONTEXT_FILE = "spring/application-context.xml";
    public static final String MDW_SPRING_MESSAGE_PRODUCER = "messageProducer";

    private static final Object pkgContextLock = new Object();

    // These are to keep track of variable translators, activity implementors, and event handlers that we
    // already know don't have a bean defined in a package-specific context, so avoid loading the class and
    // trying to get bean from context, since under heavy load, this can cause a bottleneck, especially for
    // variable translators.
    private static volatile Map<String, Map<String, Boolean>> undefinedVariableBeans = new ConcurrentHashMap<String, Map<String, Boolean>>();
    private static volatile Map<String, Map<String, Boolean>> undefinedActivityBeans = new ConcurrentHashMap<String, Map<String, Boolean>>();
    private static volatile Map<String, Map<String, Boolean>> undefinedEventBeans = new ConcurrentHashMap<String, Map<String, Boolean>>();

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * Only to be called from CacheRegistration.
     */
    public SpringAppContext() {
    }

    private static SpringAppContext instance;
    public static SpringAppContext getInstance() {
        if (instance == null)
            instance = new SpringAppContext();
        return instance;
    }

    public void shutDown() {
        if (packageContexts != null) {
            synchronized (pkgContextLock) {
                for (MdwPackageAppContext appContext : packageContexts.values())
                    shutDown(appContext);
            }
        }

        if (springAppContext != null) {
            springAppContext.close();
        }
    }

    private void shutDown(MdwPackageAppContext pkgContext) {
        pkgContext.close();
    }

    private GenericXmlApplicationContext springAppContext;
    public synchronized ApplicationContext getApplicationContext() throws IOException {
        if (springAppContext == null) {
            String springContextFile = SPRING_CONTEXT_FILE;
            Resource resource = new ByteArrayResource(FileHelper.readConfig(springContextFile).getBytes());
            springAppContext = new GenericXmlApplicationContext();
            springAppContext.load(resource);
            springAppContext.refresh();
        }
        return springAppContext;
    }

    private static Map<String, MdwPackageAppContext> packageContexts;

    public ApplicationContext getApplicationContext(Package pkg) throws IOException {
        ApplicationContext appContext = getApplicationContext();
        if (pkg != null) {
            if (packageContexts == null) {
                synchronized (pkgContextLock) {
                    packageContexts = loadPackageContexts(appContext);
                }
            }
            MdwPackageAppContext pkgContext = packageContexts.get(pkg.getName());
            if (pkgContext != null)
                appContext = pkgContext;
        }
        return appContext;
    }

    public void loadPackageContexts() throws IOException {
        synchronized (pkgContextLock) {
            if (packageContexts == null)
                packageContexts = loadPackageContexts(getApplicationContext());
        }
    }

    public Map<String, MdwPackageAppContext> loadPackageContexts(ApplicationContext parent) throws IOException {
        Map<String, MdwPackageAppContext> contexts = new HashMap<>();
        for (Asset springAsset : AssetCache.getAssets("spring")) {
            try {
                Package pkg = PackageCache.getPackage(springAsset.getPackageName());
                if (pkg != null) {
                    String url = MdwPackageAppContext.MDW_SPRING_URL_PREFIX + pkg.getName() + "/" + springAsset.getName();
                    logger.info("Loading Spring asset: " + url + " from " + pkg.getLabel());
                    MdwPackageAppContext pkgContext = new MdwPackageAppContext(url, parent);
                    pkgContext.setClassLoader(pkg.getClassLoader());
                    pkgContext.refresh();
                    contexts.put(pkg.getName(), pkgContext);  // we only support one Spring asset per package
                }
            }
            catch (Exception ex) {
                // do not let this prevent other package contexts from loading
                logger.error(ex.getMessage(), ex);
            }
        }
        return contexts;
    }

    public Object getBean(String name) throws IOException {
        try {
            return getApplicationContext().getBean(name);
        }
        catch (NoSuchBeanDefinitionException e) {  // Search in pkg contexts if not found in ApplicationContext
            if (packageContexts != null) {
                Object bean = null;
                for (ApplicationContext pkgContext : packageContexts.values()) {
                    bean = getBean(name, pkgContext);
                    if (bean != null)
                        return bean;
                }
            }
            throw e;
        }
    }

    private Object getBean(String name, ApplicationContext context) {
        try {
            return context.getBean(name);
        }
        catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    public Object getBean(String name, boolean optional) throws IOException {
        if (optional) {
            try {
                return getBean(name);
            } catch (NoSuchBeanDefinitionException e) {
                return null;
            }
        }
        else {
            return getBean(name);
        }
    }

    public boolean isBeanDefined(String name) {

        try {
            Object bean = getBean(name);
            return (bean !=null);
        } catch (Exception e) {
            //Catch it and return false;
            return false;
        }
    }

    /**
     * Returns an implementation for an MDW injectable type.
     * If no custom implementation is found, use the MDW default.
     * @param type
     * @param mdwImplClass
     * @return the instance
     */
    public Object getInjectable(Class<?> type, Class<?> mdwImplClass) throws IOException {
        Map<String,?> beans = getApplicationContext().getBeansOfType(type);
        Object mdw = null;
        Object injected = null;
        for (Object bean : beans.values()) {
            if (bean.getClass().getName().equals(mdwImplClass.getName()))
                mdw = bean;
            else
                injected = bean;
        }
        return injected == null ? mdw : injected;
    }

    /**
     * Returns null rather than throwing when not found.
     * Insists on finding only one bean of the given type.
     */
    public <T> T getBean(Class<T> type) {
        Map<String,T> beans = new HashMap<>();
        try {
            beans = getApplicationContext().getBeansOfType(type);
            if (packageContexts != null) {
                for (ApplicationContext pkgContext : packageContexts.values()) {
                    beans.putAll(pkgContext.getBeansOfType(type));
                }
            }

            if (beans.isEmpty()) {
                return null;
            }
            else if (beans.values().size() > 1) {
                throw new IOException("Too many bean definitions for type: " + type + " (" + beans.values().size() + ")");
            }
            else {
                return beans.values().iterator().next();
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Prefers any non-MDW TaskRefData implementation.
     */
    public TaskRefData getTaskRefData() {
        try {
            List<TaskRefData> taskRefDatas = new ArrayList<TaskRefData>();
            Map<String,? extends TaskRefData> beans = getApplicationContext().getBeansOfType(TaskRefData.class);
            if (beans != null)
                taskRefDatas.addAll(beans.values());

            synchronized (pkgContextLock) {
                if (packageContexts == null)
                    packageContexts = loadPackageContexts(getApplicationContext());

                for (ApplicationContext pkgContext : packageContexts.values()) {
                    beans = pkgContext.getBeansOfType(TaskRefData.class);
                    if (beans != null)
                        taskRefDatas.addAll(beans.values());
                }
            }

            TaskRefData mdwTaskRefData = null;
            TaskRefData injectedTaskRefData = null;
            for (TaskRefData taskRefData : taskRefDatas) {
                String className = taskRefData.getClass().getName();
                logger.mdwDebug("Found TaskRefData: " + className);
                if (className.equals(MdwTaskRefData.class.getName()))
                    mdwTaskRefData = taskRefData;
                else
                    injectedTaskRefData = taskRefData;
            }
            if (taskRefDatas.size() > 2) {
                List<TaskRefData> injectedTaskRefDatas = new ArrayList<>();
                for (TaskRefData bd : taskRefDatas) {
                    if (bd != mdwTaskRefData)
                        injectedTaskRefDatas.add(bd);
                }
                injectedTaskRefData = new CombinedTaskRefData(injectedTaskRefDatas);
            }
            return injectedTaskRefData == null ? mdwTaskRefData : injectedTaskRefData;
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    public GeneralActivity getActivityImplementor(String type, Package pkg) throws IOException, ClassNotFoundException {
        String key = pkg == null ? "SpringRootContext" : pkg.toString();
        Map<String, Boolean> set = undefinedActivityBeans.get(key);
        if (set != null && set.get(type) != null)
            return null;

        try {
            Class<? extends GeneralActivity> implClass;
            if (pkg == null)
                implClass = Class.forName(type).asSubclass(GeneralActivity.class);
            else
                implClass = pkg.getClassLoader().loadClass(type).asSubclass(GeneralActivity.class);
            for (String beanName : getApplicationContext(pkg).getBeanNamesForType(implClass)) {
                if (getApplicationContext(pkg).isSingleton(beanName))
                    throw new IllegalArgumentException("Bean declaration for injected activity '" + beanName + "' must have scope=\"prototype\"");
            }
            return getApplicationContext(pkg).getBean(implClass);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Add to map of known classes that have no bean declared
            set = undefinedActivityBeans.get(key);
            if (set == null) {
                set = new ConcurrentHashMap<String, Boolean>();
                undefinedActivityBeans.put(key, set);
            }
            set.put(type,true);

            return null; // no bean declared
        }
    }

    public RequestHandler getRequestHandler(String type, Package pkg) throws IOException, ClassNotFoundException {
        String key = pkg == null ? "SpringRootContext" : pkg.toString();
        Map<String, Boolean> set = undefinedEventBeans.get(key);
        if (set != null && set.get(type) != null)
            return null;

        try {
            Class<? extends RequestHandler> implClass;
            if (pkg == null)
                implClass = Class.forName(type).asSubclass(RequestHandler.class);
            else
                implClass = pkg.getClassLoader().loadClass(type).asSubclass(RequestHandler.class);
            for (String beanName : getApplicationContext(pkg).getBeanNamesForType(implClass)) {
                if (getApplicationContext(pkg).isSingleton(beanName))
                    throw new IllegalArgumentException("Bean declaration for injected event handler '" + beanName + "' must have scope=\"prototype\"");
            }
            return getApplicationContext(pkg).getBean(implClass);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Add to map of known classes that have no bean declared
            set = undefinedEventBeans.get(key);
            if (set == null) {
                set = new ConcurrentHashMap<String, Boolean>();
                undefinedEventBeans.put(key, set);
            }
            set.put(type,true);

            return null; // no bean declared
        }
    }

    public VariableTranslator getVariableTranslator(String type, Package pkg) throws IOException, ClassNotFoundException {
        String key = (pkg == null ? "SpringRootContext" : pkg.toString());
        Map<String, Boolean> set = undefinedVariableBeans.get(key);
        if (set != null && set.get(type) != null)
            return null;

        try {
            Class<? extends VariableTranslator> implClass;
              if (pkg == null)
                  implClass = Class.forName(type).asSubclass(VariableTranslator.class);
              else
                  implClass = pkg.getClassLoader().loadClass(type).asSubclass(VariableTranslator.class);
            return getApplicationContext(pkg).getBean(implClass);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Add to map of known classes that have no bean declared
            set = undefinedVariableBeans.get(key);
            if (set == null) {
                set = new ConcurrentHashMap<String, Boolean>();
                undefinedVariableBeans.put(key, set);
            }
            set.put(type, true);

            return null; // no bean declared
        }
    }

    @Override
    public void refreshCache() throws Exception {
        clearCache(); // for lazily reloading
    }

    @Override
    public void clearCache() {
        synchronized (pkgContextLock) {
            if (packageContexts != null) {
                for (MdwPackageAppContext appContext : packageContexts.values())
                    shutDown(appContext);
            }
            packageContexts = null;
        }
    }
}
