package com.centurylink.mdw.workflow.task.strategy;

import java.util.List;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class ShortestQueueAutoAssignStrategy implements AutoAssignStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public User selectAssignee(TaskInstance taskInstance) throws ObserverException {
        try {
            List<String> groups = ServiceLocator.getTaskServices().getGroupsForTaskInstance(taskInstance);
            List<User> taskUsers = ServiceLocator.getUserServices().getWorkgroupUsers(groups);
            if (taskUsers.isEmpty()) {
                return null;
            }

            User assignee = taskUsers.get(0);
            int shortest = Integer.MAX_VALUE;
            for (User user : taskUsers) {
                int depth = ServiceLocator.getTaskServices().getTasks(new Query().setFilter("assigneee", user.getCuid())).getCount();
                if (depth < shortest) {
                    assignee = user;
                    shortest = depth;
                }
            }
            return assignee;
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ObserverException(-1, ex.getMessage(), ex);
        }
    }
}
