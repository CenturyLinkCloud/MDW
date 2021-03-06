package com.centurylink.mdw.service.data.process;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HierarchyCache implements CacheService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final String MILESTONES_PACKAGE = "com.centurylink.mdw.milestones";

    private static volatile Map<Long,List<Linked<Process>>> hierarchies = new HashMap<>();
    // latest processIds to milestone hierarchies (TODO: map by assetPath for clarity)
    private static volatile Map<Long,Linked<Milestone>> milestones = new HashMap<>();
    private static volatile Map<Long,Linked<Activity>> activityHierarchies = new HashMap<>();
    private static volatile List<Long> milestoned = null;

    public static List<Linked<Process>> getHierarchy(Long processId) {
        List<Linked<Process>> hierarchy;
        Map<Long,List<Linked<Process>>> hierarchyMap = hierarchies;
        if (hierarchyMap.containsKey(processId)){
            hierarchy = hierarchyMap.get(processId);
        } else {
            synchronized(HierarchyCache.class) {
                hierarchyMap = hierarchies;
                if (hierarchyMap.containsKey(processId)) {
                    hierarchy = hierarchyMap.get(processId);
                } else {
                    hierarchy = loadHierarchy(processId, true);
                    // null values are stored to avoid repeated processing
                    hierarchies.put(processId, hierarchy);
                }
            }
        }
        return hierarchy;
    }

    private static List<Linked<Process>> loadHierarchy(Long processId, boolean downward) {
        try {
            return ServiceLocator.getDesignServices().getProcessHierarchy(processId, downward);
        } catch (ServiceException ex) {
            throw new CachingException("Failed to load hierarchy for: " + processId, ex);
        }
    }

    /**
     * Descending milestones starting with the specified process.
     */
    public static Linked<Milestone> getMilestones(Long processId) throws IOException {
        Linked<Milestone> processMilestones;
        Map<Long,Linked<Milestone>> milestoneMap = milestones;
        if (milestoneMap.containsKey(processId)) {
            processMilestones = milestoneMap.get(processId);
        } else {
            synchronized (HierarchyCache.class) {
                milestoneMap = milestones;
                if (milestoneMap.containsKey(processId)) {
                    processMilestones = milestoneMap.get(processId);
                } else {
                    processMilestones = loadMilestones(processId);
                    // null values are stored to avoid repeated processing
                    milestones.put(processId, processMilestones);
                }
            }
        }
        return processMilestones;
    }

    private static Linked<Milestone> loadMilestones(Long processId) throws IOException {
        if (PackageCache.getPackage(MILESTONES_PACKAGE) == null)
            return null; // don't bother and save some time
        Process process = ProcessCache.getProcess(processId);
        if (process != null) {
            MilestoneFactory milestoneFactory = new MilestoneFactory(process);
            Linked<Milestone> start = milestoneFactory.start();
            Linked<Activity> activityHierarchy = getActivityHierarchy(processId);
            if (activityHierarchy != null)
                addMilestones(start, activityHierarchy);
            if (!start.getChildren().isEmpty())
                return start;
        }
        return null;
    }

    private static void addMilestones(Linked<Milestone> head, Linked<Activity> start) throws IOException {
        Activity activity = start.get();
        Process process = ProcessCache.getProcess(activity.getProcessId());
        Milestone milestone = new MilestoneFactory(process).getMilestone(activity);
        if (milestone != null) {
            Linked<Milestone> linkedMilestone = new Linked<>(milestone);
            if (!head.getChildren().contains(linkedMilestone)) {
                // otherwise already reached here from another path
                linkedMilestone.setParent(head);
                head.getChildren().add(linkedMilestone);
                for (Linked<Activity> child : start.getChildren()) {
                    addMilestones(linkedMilestone, child);
                }
            }
        }
        else {
            for (Linked<Activity> child : start.getChildren()) {
                addMilestones(head, child);
            }
        }
    }

    public static Linked<Activity> getActivityHierarchy(Long processId) throws IOException {
        Linked<Activity> hierarchy;
        Map<Long,Linked<Activity>> endToEndMap = activityHierarchies;
        if (endToEndMap.containsKey(processId)) {
            hierarchy = endToEndMap.get(processId);
        }
        else {
            synchronized (HierarchyCache.class) {
                endToEndMap = activityHierarchies;
                if (endToEndMap.containsKey(processId)) {
                    hierarchy = endToEndMap.get(processId);
                } else {
                    hierarchy = loadActivityHierarchy(processId);
                    activityHierarchies.put(processId, hierarchy);
                }
            }
        }
        return hierarchy;
    }

    private static int MAX_DEPTH;

    private static Linked<Activity> loadActivityHierarchy(Long processId) throws IOException {
        if (PackageCache.getPackage(MILESTONES_PACKAGE) == null)
            return null; // don't bother and save some time
        MAX_DEPTH = PropertyManager.getIntegerProperty(PropertyNames.MDW_MILESTONE_MAX_DEPTH, 7500);
        Process process = ProcessCache.getProcess(processId);
        if (process != null) {
            Linked<Process> hierarchy = getHierarchy(processId).get(0);
            Linked<Activity> endToEndActivities = process.getLinkedActivities();
            ScopedActivity scoped = new ScopedActivity(hierarchy, endToEndActivities);
            addSubprocessActivities(scoped, null);
            return endToEndActivities;
        }
        return null;
    }

    /**
     * Activities are scoped to avoid process invocation circularity.
     */
    private static void addSubprocessActivities(ScopedActivity start, List<ScopedActivity> downstreams)
            throws IOException {

        if (start.depth() > MAX_DEPTH)
            return;

        List<ScopedActivity> furtherDowns = downstreams;

        for (ScopedActivity scopedChild : start.getScopedChildren()) {
            Activity activity = scopedChild.get();
            List<Linked<Process>> subhierarchies = getInvoked(scopedChild);
            if (!subhierarchies.isEmpty()) {
                // link downstream children
                if (furtherDowns != null) {
                    for (Linked<Activity> downstreamChild : scopedChild.getChildren()) {
                        for (Linked<Activity> end : downstreamChild.getEnds()) {
                            if (end.get().isStop()) {
                                end.setChildren(new ArrayList<>(furtherDowns));
                            }
                        }
                    }
                }
                if (activity.isSynchronous()) {
                    furtherDowns = scopedChild.getScopedChildren();
                    scopedChild.setChildren(new ArrayList<>());
                }
                for (Linked<Process> subhierarchy : subhierarchies) {
                    Process loadedSub = ProcessCache.getProcess(subhierarchy.get().getId());
                    Linked<Activity> subprocActivities = loadedSub.getLinkedActivities();
                    scopedChild.getChildren().add(subprocActivities);
                    subprocActivities.setParent(scopedChild);
                    ScopedActivity subprocScoped = new ScopedActivity(subhierarchy, subprocActivities);
                    addSubprocessActivities(subprocScoped, furtherDowns);
                }
                furtherDowns = downstreams;
            }
            else {
                // non-invoker
                if (scopedChild.get().isStop() && scopedChild.getChildren().isEmpty() ) {
                    if (furtherDowns != null) {
                        scopedChild.setChildren(new ArrayList<>(furtherDowns));
                        furtherDowns = null;
                    }
                }
                addSubprocessActivities(scopedChild, furtherDowns);
            }
        }
    }


    /**
     * Omits invoked subflows that would cause circularity by consulting relevant process hierarchy.
     */
    private static List<Linked<Process>> getInvoked(ScopedActivity scopedActivity) {
        List<Linked<Process>> invoked = new ArrayList<>();
        for (Linked<Process> subprocess : scopedActivity.findInvoked(ProcessCache.getProcesses(false))) {
            if (!isIgnored(subprocess.get()))
                invoked.add(subprocess);
        }
        return invoked;
    }

    /**
     * Processes with milestones in their hierarchies.
     */
    public static List<Long> getMilestoned() {
        List<Long> milestonedTemp = milestoned;
        if (milestonedTemp == null)
            synchronized(HierarchyCache.class) {
                milestonedTemp = milestoned;
                if (milestonedTemp == null)
                    milestoned = milestonedTemp = loadMilestoned();
            }
        return milestonedTemp;
    }

    private static List<Long> loadMilestoned() {
        List<Long> milestoned = new ArrayList<>();
        if (PackageCache.getPackage(MILESTONES_PACKAGE) != null) {
            for (Process process : ProcessCache.getProcesses(false)) {
                try {
                    List<Linked<Process>> hierarchyList = getHierarchy(process.getId());
                    if (!hierarchyList.isEmpty()) {
                        if (hasMilestones(hierarchyList.get(0))) {
                            milestoned.add(process.getId());
                        }
                    }
                }
                catch (CachingException ex) {
                    logger.error("Cannot check milestones for process " + process.getLabel(), ex);
                }
            }
        }
        return milestoned;
    }

    public static boolean hasMilestones(Long processId) {
        return getMilestoned().contains(processId);
    }

    private static boolean hasMilestones(Linked<Process> hierarchy) {
        Process process = hierarchy.get();
        Linked<Activity> start = process.getLinkedActivities();

        if (hasMilestones(new MilestoneFactory(process), start))
            return true;

        for (Linked<Process> child : hierarchy.getChildren()) {
            if (hasMilestones(child))
                return true;
        }

        return false;
    }

    private static boolean hasMilestones(MilestoneFactory factory, Linked<Activity> start) {
        if (factory.getMilestone(start.get()) != null)
            return true;
        for (Linked<Activity> child : start.getChildren()) {
            if (hasMilestones(factory, child))
                return true;
        }
        return false;
    }

    @Override
    public void clearCache() {
        synchronized(HierarchyCache.class) {
            hierarchies = new HashMap<>();
            milestones = new HashMap<>();
            activityHierarchies = new HashMap<>();
            milestoned = null;
        }
    }

    @Override
    public void refreshCache() {
        // hierarchies are lazily loaded
        clearCache();
    }

    private static List<String> IGNORES;
    private static boolean isIgnored(Process process) {
        if (IGNORES == null) {
            List<String> ignores = PropertyManager.getListProperty(PropertyNames.MDW_MILESTONE_IGNORES);
            IGNORES = ignores == null ? new ArrayList<>() : ignores;
        }
        return IGNORES.contains(process.getPackageName() + "/" + process.getName() + ".proc");
    }
}
