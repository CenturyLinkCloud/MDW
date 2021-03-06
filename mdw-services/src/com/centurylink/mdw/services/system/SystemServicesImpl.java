package com.centurylink.mdw.services.system;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.cli.Download;
import com.centurylink.mdw.cli.Unzip;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.PropertyUtil;
import com.centurylink.mdw.config.YamlPropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.container.plugin.CommonThreadPool;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.db.DocumentDb;
import com.centurylink.mdw.model.system.Mbean;
import com.centurylink.mdw.model.system.SysInfo;
import com.centurylink.mdw.model.system.SysInfoCategory;
import com.centurylink.mdw.services.SystemServices;
import com.centurylink.mdw.util.ClasspathUtil;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import javax.management.*;
import java.io.*;
import java.lang.management.*;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.*;

public class SystemServicesImpl implements SystemServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public List<SysInfoCategory> getSysInfoCategories(SysInfoType type, Query query)
            throws ServiceException {
        List<SysInfoCategory> sysInfoCats = new ArrayList<>();
        if (type == SysInfoType.System) {
            // Request and Session info added by REST servlet
            sysInfoCats.add(getSystemInfo());
            sysInfoCats.add(getDbInfo());
            sysInfoCats.add(getSystemProperties());
            sysInfoCats.add(getMdwProperties());
        }
        else if (type == SysInfoType.Thread) {
            sysInfoCats.add(getThreadPoolStatus());
            sysInfoCats.add(getThreadMxBeanInfo());
            sysInfoCats.add(getThreadDump());
        }
        else if (type == SysInfoType.Memory) {
            sysInfoCats.add(getMemoryInfo());
        }
        else if (type == SysInfoType.Class) {
            String className = query.getFilter("className");
            if (className == null)
                throw new ServiceException("Missing parameter: className");
            String classLoader = query.getFilter("classLoader");
            if (classLoader == null) {
                sysInfoCats.add(findClass(className));
            }
            else {
                ClassLoader loader;
                if (classLoader.equals(ClasspathUtil.class.getClassLoader().getClass().getName()))
                    loader = ClasspathUtil.class.getClassLoader();
                else
                    loader = PackageCache.getPackage(classLoader).getClassLoader();
                sysInfoCats.add(findClass(className, loader));
            }
        }
        else if (type == SysInfoType.CLI) {
            String cmd = query.getFilter("command");
            if (cmd == null)
                throw new ServiceException("Missing parameter: command");
            List<SysInfo> cmdInfo = new ArrayList<>();
            try {
                String output = runCliCommand(cmd + " --dependencies");
                output += runCliCommand(cmd);
                cmdInfo.add(new SysInfo(cmd, output));  // TODO actual output
                sysInfoCats.add(new SysInfoCategory("CLI Command Output", cmdInfo));
            }
            catch (Exception ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
            }
        }
        else if (type == SysInfoType.MBeans) {
            Map<String,Mbean> domainMbeans = getDomainMbeans();
            for (String domain : domainMbeans.keySet()) {
                List<SysInfo> sysInfos = new ArrayList<>();
                Mbean mbean = domainMbeans.get(domain);
                for (String name : mbean.getValues().keySet()) {
                    String value = mbean.getValues().get(name);
                    int lf = value.indexOf('\n');
                    if (lf > 0)
                        value = value.substring(0, lf) + "...";
                    sysInfos.add(new SysInfo(name, value));
                }
                sysInfoCats.add(new SysInfoCategory(domain, sysInfos));
            }
        }

        return sysInfoCats;
    }

    public SysInfoCategory getThreadDump() {
        List<SysInfo> threadDumps = new ArrayList<>();
        Map<Thread,StackTraceElement[]> threads = Thread.getAllStackTraces();
        StringBuilder output = new StringBuilder();
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            if (threadBean.isSynchronizerUsageSupported()) {
                long[] deadlocked = threadBean.findDeadlockedThreads();
                if (deadlocked.length > 0) {
                    output.append("DEADLOCKED Threads: ").append(Arrays.toString(deadlocked)).append("\n");
                }
                long[] monitorDeadlocked = threadBean.findMonitorDeadlockedThreads();
                if (monitorDeadlocked.length > 0) {
                    output.append("MONITOR DEADLOCKED Threads").append(Arrays.toString(monitorDeadlocked)).append("\n");
                }
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        output.append(" Total (").append(new Date()).append(") = ").append(threads.size()).append("\n\n");
        for (Thread thread : threads.keySet()) {
            output.append("\"").append(thread.getName()).append("\"");
            output.append(" #").append(thread.getId());
            if (thread.isDaemon())
                output.append(" ").append("daemon");
            output.append(" priority=").append(thread.getPriority());
            if (thread.getThreadGroup() != null)
                output.append(" group=").append(thread.getThreadGroup().getName()).append(" ");
            output.append("\n  java.lang.Thread.State: ").append(thread.getState().toString().toUpperCase());
            output.append("\n");
            StackTraceElement[] elements = threads.get(thread);
            if (elements != null) {
                for (StackTraceElement element : elements) {
                    output.append("      at ").append(element).append("\n");
                }
            }
            output.append("\n");
            try {
                ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
                ThreadInfo threadInfo = threadBean.getThreadInfo(thread.getId());
                if (threadInfo != null) {
                    if (threadBean.isThreadContentionMonitoringSupported()
                            && threadBean.isThreadContentionMonitoringEnabled()) {
                        output.append("    Blocked Count: ").append(threadInfo.getBlockedCount()).append("\n");
                        output.append("    Blocked Time (ms): ").append(threadInfo.getBlockedTime()).append("\n");
                    }
                    if (threadInfo.getLockName() != null) {
                        output.append("    Lock Name: ").append(threadInfo.getLockName()).append("\n");
                        output.append("    Lock Owner: ").append(threadInfo.getLockOwnerName()).append("\n");
                        output.append("    Lock Owner Thread ID: ").append(threadInfo.getLockOwnerId()).append("\n");
                    }
                    output.append("    Waited Count: ").append(threadInfo.getWaitedCount()).append("\n");
                    output.append("    Waited Time (ms): ").append(threadInfo.getWaitedTime()).append("\n");
                    output.append("    Is In Native: ").append(threadInfo.isInNative()).append("\n");
                    output.append("    Is Suspended: ").append(threadInfo.isSuspended());
                }
            }
            catch (Exception ex) {
                // don't let an exception here interfere with display of stack info
            }
            output.append("\n\n");
        }

        System.out.println(output.toString()); // print to stdout
        threadDumps.add(new SysInfo("Threads",  output.toString()));
        return new SysInfoCategory("Thread Dump", threadDumps);
    }

    private SysInfoCategory getThreadMxBeanInfo() {
        List<SysInfo> threadMxBeanInfo = new ArrayList<>();
        try {
            StringBuilder mxBeanOutput = new StringBuilder();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            long[] blockedThreadIds = threadBean.findMonitorDeadlockedThreads();

            if (blockedThreadIds != null) {
                StringBuilder blocked = new StringBuilder("Blocked Thread IDs : ");
                for (long id : blockedThreadIds)
                    blocked.append(id).append(" ");
                mxBeanOutput.append(blocked).append("\n");
            }
            mxBeanOutput.append("\nThread Count: ").append(threadBean.getThreadCount()).append("\n");
            mxBeanOutput.append("Peak Thread Count: ").append(threadBean.getPeakThreadCount());
            threadMxBeanInfo.add(new SysInfo("Thread MXBean",  mxBeanOutput.toString()));
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return new SysInfoCategory("Thread MX Bean", threadMxBeanInfo);
    }

    private SysInfoCategory getThreadPoolStatus() {
        ThreadPoolProvider threadPool = ApplicationContext.getThreadPoolProvider();
        List<SysInfo> poolStatus = new ArrayList<>();
        if (!(threadPool instanceof CommonThreadPool)) {
            poolStatus.add(new SysInfo ("Error getting thread pool status" , "ThreadPoolProvider is not MDW CommonThreadPool"));
        }
        else
            poolStatus.add(new SysInfo ("Current Status" ," Thread Pool\n" + threadPool.currentStatus()));
        return new SysInfoCategory("Pool Status", poolStatus);
    }

    public SysInfoCategory getMemoryInfo() {
        List<SysInfo> memoryInfo = new ArrayList<>();
        memoryInfo.add(new SysInfo(memoryInfo()));
        return new SysInfoCategory("Memory Info", memoryInfo);
    }

    private String memoryInfo() {
        String info;
        StringBuilder output = new StringBuilder();

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        MemoryUsage heapMemUsage = memBean.getHeapMemoryUsage();
        output.append("Heap Memory:\n------------\n");
        output.append(memoryUsage(heapMemUsage, 0));

        output.append("\n");

        MemoryUsage nonHeapMemUsage = memBean.getNonHeapMemoryUsage();
        output.append("Non-Heap Memory:\n----------------\n");
        output.append(memoryUsage(nonHeapMemUsage, 0));

        output.append("\n");

        output.append("Objects Pending Finalization: ").append(memBean.getObjectPendingFinalizationCount()).append("\n\n");

        output.append("Memory Pools:\n-------------\n");
        List<MemoryPoolMXBean> memoryPoolBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean memoryPoolBean : memoryPoolBeans) {
            output.append(memoryPoolBean.getName()).append(" ");
            output.append("(type=").append(memoryPoolBean.getType()).append("):\n");

            if (memoryPoolBean.isUsageThresholdSupported()) {
                output.append("\tUsage Threshold:").append(memoryPoolBean.getUsageThreshold()).append(" (").append(memoryPoolBean.getUsageThreshold() >> 10).append("K)\n");
                output.append("\tUsage Threshold Count:").append(memoryPoolBean.getUsageThresholdCount()).append(" (").append(memoryPoolBean.getUsageThresholdCount() >> 10).append("K)\n");
                output.append("\tUsage Threshold Exceeded: ").append(memoryPoolBean.isUsageThresholdExceeded()).append("\n");
            }

            if (memoryPoolBean.isCollectionUsageThresholdSupported()) {
                output.append("\tCollection Usage Threshold: ").append(memoryPoolBean.getCollectionUsageThreshold()).append(" (").append(memoryPoolBean.getCollectionUsageThreshold() >> 10).append("K)\n");
                output.append("\tCollection Usage Threshold Count: ").append(memoryPoolBean.getCollectionUsageThresholdCount()).append(" (").append(memoryPoolBean.getCollectionUsageThresholdCount() >> 10).append("K)\n");
                output.append("\tCollection Usage Threshold Exceeded: ").append(memoryPoolBean.isCollectionUsageThresholdExceeded()).append("\n");
            }

            if (memoryPoolBean.isUsageThresholdSupported() && memoryPoolBean.getUsage() != null) {
                output.append("\n\tUsage:\n\t------\n").append(memoryUsage(memoryPoolBean.getUsage(), 1));
            }
            if (memoryPoolBean.isCollectionUsageThresholdSupported() && memoryPoolBean.getCollectionUsage() != null) {
                output.append("\n\tCollection Usage:\n\t-----------------\n").append(memoryUsage(memoryPoolBean.getCollectionUsage(), 1));
            }
            if (memoryPoolBean.getPeakUsage() != null) {
                output.append("\n\tPeak Usage:\n\t-----------\n").append(memoryUsage(memoryPoolBean.getPeakUsage(), 1));
            }

            String[] memoryManagerNames = memoryPoolBean.getMemoryManagerNames();
            if (memoryManagerNames != null) {
                output.append("\n\tMemory Manager Names: ");
                for (String memoryManagerName : memoryManagerNames)
                    output.append(memoryManagerName).append(" ");
                output.append("\n");
            }

            output.append("\n");
        }

        info = output.toString() + getTopInfo();
        System.out.println(info);
        return info;
    }

    private String memoryUsage(MemoryUsage memUsage, int indent) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Initial = ").append(memUsage.getInit()).append(" (").append(memUsage.getInit() >> 10).append("K)\n");
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Used = ").append(memUsage.getUsed()).append(" (").append(memUsage.getUsed() >> 10).append("K)\n");
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Committed = ").append(memUsage.getCommitted()).append(" (").append(memUsage.getCommitted() >> 10).append("K)\n");
        for (int i = 0; i < indent; i++)
            output.append("\t");
        output.append("Max = ").append(memUsage.getMax()).append(" (").append(memUsage.getMax() >> 10).append("K)\n");
        return output.toString();
    }

    private Map<String,Mbean> getDomainMbeans() {
        long before = System.currentTimeMillis();
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectInstance> instances = server.queryMBeans(null, null);
        Iterator<ObjectInstance> iterator = instances.iterator();
        Map<String,Mbean> domainMbeans = new TreeMap<>();
        while (iterator.hasNext()) {
            ObjectInstance instance = iterator.next();
            ObjectName objectName = instance.getObjectName();
            try {
                MBeanInfo mbeanInfo = server.getMBeanInfo(objectName);
                String domain = objectName.getDomain();
                String name = objectName.getKeyProperty("name");
                String type = objectName.getKeyProperty("type");
                Mbean mbean = domainMbeans.get(domain);
                if (mbean == null) {
                    mbean = new Mbean(domain, name, type);
                    domainMbeans.put(domain, mbean);
                }
                for (MBeanAttributeInfo attrInfo : mbeanInfo.getAttributes()) {
                    String attrName = attrInfo.getName();
                    try {
                        Object value = server.getAttribute(objectName, attrName);
                        if (name != null)
                            attrName = name + "/" + attrName;
                        if (type != null)
                            attrName = type + "/" + attrName;
                        if (!attrName.toLowerCase().contains("/password")) {
                            String stringValue;
                            if (value != null && value.getClass().getName().equals("[Ljava.lang.String;"))
                                stringValue = Arrays.toString((String[])value);
                            else if (value != null && value.getClass().getName().equals("[Ljava.lang.Object;"))
                                stringValue = Arrays.toString((Object[])value);
                            else
                                stringValue = String.valueOf(value);
                            mbean.getValues().put(attrName, stringValue);
                        }

                    }
                    catch (Exception ex) {
                        logger.trace(ex.getMessage());
                    }
                }

            }
            catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("MBeans queried in " + (System.currentTimeMillis() - before) + "ms");
        return domainMbeans;
    }

    public String getTopInfo() {
        try {
            ProcessBuilder builder = new ProcessBuilder("/usr/bin/top", "-b", "-n", "1");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder output = new StringBuilder("\nTop Output:\n-----------\n");
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        }
        catch (Throwable th) {
            StringWriter writer = new StringWriter();
            th.printStackTrace(new PrintWriter(writer));
            return "\nError running top:\n------------------\n" + writer;
        }
    }

    public SysInfoCategory getSystemInfo() {
        List<SysInfo> systemInfos = new ArrayList<>();
        systemInfos.add(new SysInfo("MDW version", ApplicationContext.getMdwVersion() + " (" + ApplicationContext.getMdwBuildTimestamp() + ")"));
        systemInfos.add(new SysInfo("App ID", ApplicationContext.getAppId()));
        if (!"mdw6".equals(ApplicationContext.getAppId()))
            systemInfos.add(new SysInfo("App version", ApplicationContext.getAppVersion()));
        try {
            systemInfos.add(new SysInfo("Server host", ApplicationContext.getServer().getHost()));
            systemInfos.add(new SysInfo("Server hostname", InetAddress.getLocalHost().getHostName()));
            systemInfos.add(new SysInfo("Server port", String.valueOf(ApplicationContext.getServer().getPort())));
            systemInfos.add(new SysInfo("Server name", ApplicationContext.getServer().toString()));
        }
        catch (Exception ex) {
            systemInfos.add(new SysInfo("Server hostname", String.valueOf(ex)));
        }
        systemInfos.add(new SysInfo("Runtime env", ApplicationContext.getRuntimeEnvironment()));
        systemInfos.add(new SysInfo("Startup dir", System.getProperty("user.dir")));
        File bootJar = ApplicationContext.getBootJar();
        if (bootJar != null)
            systemInfos.add(new SysInfo("Boot jar", bootJar.getAbsolutePath()));

        systemInfos.add(new SysInfo("App user", System.getProperty("user.name")));
        systemInfos.add(new SysInfo("System time", String.valueOf(new Date(System.currentTimeMillis()))));
        systemInfos.add(new SysInfo("Startup time", String.valueOf(ApplicationContext.getStartupTime())));
        systemInfos.add(new SysInfo("Java version", System.getProperty("java.version")));
        systemInfos.add(new SysInfo("Java VM", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version")));
        systemInfos.add(new SysInfo("OS", System.getProperty("os.name")));
        systemInfos.add(new SysInfo("OS version", System.getProperty("os.version")));
        systemInfos.add(new SysInfo("OS arch", System.getProperty("os.arch")));
        Runtime runtime = Runtime.getRuntime();
        systemInfos.add(new SysInfo("Max memory", runtime.maxMemory()/1000000 + " MB"));
        systemInfos.add(new SysInfo("Free memory", runtime.freeMemory()/1000000 + " MB"));
        systemInfos.add(new SysInfo("Heap memory", runtime.totalMemory()/1000000 + " MB"));
        systemInfos.add(new SysInfo("Available processors", String.valueOf(runtime.availableProcessors())));
        systemInfos.add(new SysInfo("Default ClassLoader", ClasspathUtil.class.getClassLoader().getClass().getName()));

        String pathSep = System.getProperty("path.separator");
        String cp = System.getProperty("java.class.path");
        if (cp != null) {
            SysInfo cpInfo = new SysInfo("System classpath");
            String[] cpEntries = cp.split(pathSep);
            cpInfo.setValues(Arrays.asList(cpEntries));
            systemInfos.add(cpInfo);
        }
        String p = System.getenv("PATH");
        if (p != null) {
            SysInfo pInfo = new SysInfo("PATH environment variable");
            String[] pEntries = p.split(pathSep);
            pInfo.setValues(Arrays.asList(pEntries));
            systemInfos.add(pInfo);
        }
        return new SysInfoCategory("System Details", systemInfos);
    }

    public SysInfoCategory getDbInfo() {
        List<SysInfo> dbInfos = new ArrayList<>();
        DatabaseAccess dbAccess = null;
        try {
            dbAccess = new DatabaseAccess(null);
            DatabaseMetaData metadata;
            try (Connection conn = dbAccess.openConnection()) {
                metadata = conn.getMetaData();
            }
            dbInfos.add(new SysInfo("Database", metadata.getDatabaseProductName()));
            dbInfos.add(new SysInfo("DB version", metadata.getDatabaseProductVersion()));
            dbInfos.add(new SysInfo("JDBC Driver", metadata.getDriverName()));
            dbInfos.add(new SysInfo("Driver version", metadata.getDriverVersion()));
            dbInfos.add(new SysInfo("JDBC URL", metadata.getURL()));
            dbInfos.add(new SysInfo("DB user", metadata.getUserName()));
            dbInfos.add(new SysInfo("DB time", String.valueOf(new Date(dbAccess.getDatabaseTime()))));

            if (DatabaseAccess.getDocumentDb() != null) {
                DocumentDb docDb = DatabaseAccess.getDocumentDb();
                dbInfos.add(new SysInfo("Document DB", docDb.getClass().getName() + " " + docDb));
            }
        }
        catch (Exception ex) {
            // don't let runtime exceptions prevent page display
            logger.error(ex.getMessage(), ex);
            dbInfos.add(new SysInfo("Error", String.valueOf(ex)));
        }
        finally {
            if (dbAccess != null) {
                dbAccess.closeConnection();
            }
        }

        try {
            Mbean mbean = getDomainMbeans().get("com.centurylink.mdw");
            if (mbean != null) {
                for (String key : DatasourceAttributes.getAttributes().keySet()) {
                    String value = mbean.getValues().get(key);
                    if (key.equals("MDWDataSource/DefaultTransactionIsolation"))
                        value = getTxIsolationLevel(Integer.parseInt(value));
                    String label = "Datasource " + DatasourceAttributes.getAttributes().get(key);
                    dbInfos.add(new SysInfo(label, value));
                }
                dbInfos.add(new SysInfo("Full Datasource Info", "Available under **MBeans**"));
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        return new SysInfoCategory("Database Details", dbInfos);
    }

    private String getTxIsolationLevel(int txIsolation) {
        if (txIsolation == Connection.TRANSACTION_NONE)
            return "NONE";
        else if (txIsolation == Connection.TRANSACTION_READ_UNCOMMITTED)
            return "READ_UNCOMMITED";
        else if (txIsolation == Connection.TRANSACTION_READ_COMMITTED)
            return "READ_COMMITTED";
        else if (txIsolation == Connection.TRANSACTION_REPEATABLE_READ)
            return "REPEATABLE_READ";
        else if (txIsolation == Connection.TRANSACTION_SERIALIZABLE)
            return "SERIALIZABLE";
        else
            return String.valueOf(txIsolation);
    }

    public SysInfoCategory getSystemProperties() {
        List<SysInfo> sysPropInfos = new ArrayList<>();
        Properties properties = System.getProperties();
        List<String> propNames = new ArrayList<>();
        for (Object o : properties.keySet()) propNames.add(String.valueOf(o));
        Collections.sort(propNames);
        for (String propName : propNames) {
            sysPropInfos.add(new SysInfo(propName, properties.getProperty(propName)));
        }
        return new SysInfoCategory("System Properties", sysPropInfos);
    }

    public SysInfoCategory getMdwProperties() {
        List<SysInfo> mdwPropInfos = new ArrayList<>();
        PropertyManager propMgr = null;
        try {
            propMgr = PropertyUtil.getInstance().getPropertyManager();
            Properties properties = propMgr.getAllProperties();
            if (!properties.containsKey(PropertyNames.MDW_LOGGING_LEVEL)) {
                String logLevel = PropertyManager.getProperty(PropertyNames.MDW_LOGGING_LEVEL);
                if (logLevel != null)
                    properties.put(PropertyNames.MDW_LOGGING_LEVEL, logLevel);
            }
            List<String> propNames = new ArrayList<>();
            for (Object o : properties.keySet()) {
                propNames.add(String.valueOf(o));
            }
            Collections.sort(propNames);
            for (String propName : propNames) {
                try {
                    if (!propName.toLowerCase().contains("password") && !propMgr.isEncrypted(propName))
                        mdwPropInfos.add(new SysInfo(propName, properties.getProperty(propName)));
                    else
                        mdwPropInfos.add(new SysInfo(propName, "********"));
                }
                catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    mdwPropInfos.add(new SysInfo("Error", String.valueOf(ex)));
                }
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            mdwPropInfos.add(new SysInfo("Error", String.valueOf(ex)));
        }

        SysInfoCategory category = new SysInfoCategory("MDW Properties", mdwPropInfos);
        if (propMgr instanceof YamlPropertyManager)
            category.setDescription("Never-accessed properties are not displayed.  Null means the property has been read and not found.");
        return category;
    }

    public SysInfoCategory findClass(String className, ClassLoader classLoader) {
        List<SysInfo> classInfo = new ArrayList<>();
        classInfo.add(new SysInfo(className, ClasspathUtil.locate(className, classLoader)));
        return new SysInfoCategory("Class Info", classInfo);
    }

    public SysInfoCategory findClass(String className) {
        List<SysInfo> classInfo = new ArrayList<>();
        classInfo.add(new SysInfo(className, ClasspathUtil.locate(className)));
        return new SysInfoCategory("Class Info", classInfo);
    }

    public String runCliCommand(String command) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");

        cmd.add("-jar");
        String mdwHome = System.getenv("MDW_HOME");
        if (mdwHome == null) {
            // fall back to system property
            mdwHome = System.getProperty("mdw.home");
            if (mdwHome == null) {
                mdwHome = ApplicationContext.getTempDirectory() + File.separator + "MDW_HOME";
                System.setProperty("mdw.home", mdwHome);
            }
        }

        File cliJar = new File(mdwHome + File.separator + "mdw-cli.jar");
        if (!cliJar.exists()) {
            if (!cliJar.getParentFile().isDirectory() && !cliJar.getParentFile().mkdirs())
                throw new IOException("Cannot create dir: " + cliJar.getParentFile().getAbsolutePath());
            // grab cli zip from github
            String v = ApplicationContext.getMdwVersion();
            URL cliUrl = new URL("https://github.com/CenturyLinkCloud/mdw/releases/download/" + v + "/mdw-cli-" + v + ".zip");
            File tempZip = Files.createTempFile("mdw-cli", ".jar").toFile();
            new Download(cliUrl, tempZip, 210L).run(); // TODO progress via websocket
            // unzip into MDW_HOME
            new Unzip(tempZip, cliJar.getParentFile()).run();
        }
        cmd.add(cliJar.getAbsolutePath());

        // running direct command instead of through bat/sh to avoid permissions issues
        List<String> mdwCmd = new ArrayList<>(Arrays.asList(command.trim().split("\\s+")));
        if (mdwCmd.get(0).equals("mdw"))
            mdwCmd.remove(0);
        if (!mdwCmd.isEmpty()) {
            String first = mdwCmd.get(0);
            if (first.equals("import") || first.equals("export") || first.equals("install")
                    || first.equals("status") || first.equals("test") || first.equals("update")) {
                mdwCmd.add("--config-loc=" + new File(System.getProperty("mdw.config.location")).getAbsolutePath());
            }
        }

        cmd.addAll(mdwCmd);
        logger.debug("Running MDW CLI command: '" + cmd);

        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.environment().put("MDW_HOME", new File(mdwHome).getAbsolutePath());
        builder.directory(new File(System.getProperty("user.dir")));
        builder.redirectErrorStream(true);
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output));
        new Thread(() -> {
            try (BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                out.lines().forEach(writer::println);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();
        process.waitFor();
        writer.flush();
        return new String(output.toByteArray());
    }
}