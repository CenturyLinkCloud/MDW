package com.centurylink.mdw.drools;

import org.kie.api.KieBase;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.observer.task.ParameterizedStrategy;

public abstract class RulesBasedStrategy extends ParameterizedStrategy {

    public static final String DECISION_TABLE_SHEET = "Decision Table Sheet";

    protected KieBase getKnowledgeBase() throws StrategyException {
        Object kbName = getParameter(getKnowledgeBaseAttributeName());
        if (kbName == null)
            throw new StrategyException("Missing strategy parameter: " + getKnowledgeBaseAttributeName());
        KieBase knowledgeBase = getKnowledgeBase(kbName.toString(), getDecisionTableSheetName());
        if (knowledgeBase == null)
            throw new StrategyException("Cannot load knowledge base: " + kbName);

        return knowledgeBase;
    }

    protected String getDecisionTableSheetName() {
        Object sheetName = getParameter(DECISION_TABLE_SHEET);
        return sheetName == null ? null : sheetName.toString();
    }

    /**
     * Returns the latest version.
     *
     * Modifier is sheet name.
     *
     * Override to apply additional or non-standard attribute conditions.
     * @throws StrategyException
     */
    protected KieBase getKnowledgeBase(String name, String modifier) throws StrategyException {
        KnowledgeBaseAsset kbrs = DroolsKnowledgeBaseCache.getKnowledgeBaseAsset(name, modifier, getClassLoader());

        if (kbrs == null) {
            return null;
        }
        else {
            return kbrs.getKnowledgeBase();
        }
    }
    /**
     * <p>
     * By default returns the package Classloader
     * The knowledge base name is of the format "packageName/assetName", e.g "com.centurylink.mdw.test/TestDroolsRules.drl"
     * If an application needs a different class loader for Strategies then this method should be overridden
     * </p>
     * @return Class Loader used for Knowledge based strategies
     * @throws StrategyException
     */
    public ClassLoader getClassLoader() throws StrategyException {

        // Determine the package by parsing the attribute name (different name for different types of Rules based strategies)
        String kbAttributeName = getKnowledgeBaseAttributeName();
        Object kbName = getParameter(kbAttributeName);
        if (kbName == null)
            throw new StrategyException("Missing strategy parameter: " + kbAttributeName);

        // Get the package Name
        String kbNameStr = kbName.toString();
        int lastSlash = kbNameStr.lastIndexOf('/');
        String pkgName = lastSlash == -1 ? null : kbNameStr.substring(0, lastSlash) ;

        try {
            Package pkg = PackageCache.getPackage(pkgName);
            if (pkg == null)
                throw new StrategyException("Unable to get package name from strategy: " + kbAttributeName +" value="+kbNameStr);
            // return the cloud class loader by default, unless the bundle spec is set
            return pkg.getClass().getClassLoader();
        }
        catch (CachingException ex) {
            throw new StrategyException("Error getting strategy package: " + pkgName, ex);
        }
    }

    protected abstract String getKnowledgeBaseAttributeName();

}
