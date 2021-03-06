package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.git.VersionControlGit;
import com.centurylink.mdw.discovery.GitDiscoverer;
import com.centurylink.mdw.model.asset.api.*;
import com.centurylink.mdw.services.asset.Renderer;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Services for interacting with design-time workflow assets.
 */
public interface AssetServices {

    File getAssetRoot();

    VersionControlGit getVersionControl() throws IOException;

    /**
     * Returns the list of workflow packages.  Does not include archived packages.
     * Includes Git information if available.
     */
    PackageList getPackages(boolean withVcsInfo) throws ServiceException;

    /**
     * Returns packages, with their assets filtered by query criteria.
     */
    AssetPackageList getAssetPackageList(Query query) throws ServiceException;

    /**
     * Return the PackageInfo for a name.  Does not contain assets or VCS info.
     */
    PackageInfo getPackage(String name) throws ServiceException;

    /**
     * Returns the assets of a workflow package latest version.
     */
    PackageAssets getAssets(String packageName) throws ServiceException;
    PackageAssets getAssets(String packageName, boolean withVcsInfo) throws ServiceException;

    /**
     * Returns all assets for a given file extension (mapped to their package name).
     * Assets do not contain VCS info.
     */
    Map<String,List<AssetInfo>> getAssetsWithExtension(String extension) throws ServiceException;
    /**
     * Returns all assets for a list of file extensions (mapped to their package name).
     * Assets do not contain VCS info.
     */
    Map<String,List<AssetInfo>> getAssetsWithExtensions(String[] extensions) throws ServiceException;

    Map<String,List<AssetInfo>> findAssets(Predicate<AssetInfo> predicate) throws ServiceException;

    /**
     * @param assetPath - myPackage/myAsset.ext
     * @param withVcsInfo - include Git info if available
     * @throws ServiceException if asset not found
     */
    AssetInfo getAsset(String assetPath, boolean withVcsInfo) throws ServiceException;

    /**
     * Without VCS info.
     * @param assetPath - myPackage/myAsset.ext
     */
    AssetInfo getAsset(String assetPath) throws ServiceException;

    void createPackage(String packageName) throws ServiceException;
    void deletePackage(String packageName) throws ServiceException;

    void createAsset(String assetPath) throws ServiceException;
    void createAsset(String assetPath, String template) throws ServiceException;
    void createAsset(String assetPath, byte[] content) throws ServiceException;
    void deleteAsset(String assetPath) throws ServiceException;

    String getDefaultTemplate(String assetExt);

    AssetInfo getImplAsset(String className) throws ServiceException;

    Renderer getRenderer(String assetPath, String renderTo) throws ServiceException;

    JSONObject getGitBranches(String[] repoUrls) throws ServiceException;

    void updateAssetVersion(String assetPath, String version) throws ServiceException;
    void removeAssetVersion(String assetPath) throws ServiceException;

    JSONObject discoverGitAssets(String repoUrl, String branch) throws ServiceException;

    GitDiscoverer getDiscoverer(String repoUrl) throws IOException;

    static String packageName(String assetPath) throws ServiceException {
        int slash = assetPath.lastIndexOf('/');
        if (slash < 0 || slash >= assetPath.length() - 1)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Bad path: " + assetPath);
       return assetPath.substring(0, slash);
    }
    static String assetName(String assetPath) throws ServiceException {
        int slash = assetPath.lastIndexOf('/');
        if (slash < 0 || slash >= assetPath.length() - 1)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Bad path: " + assetPath);
        return assetPath.substring(slash + 1);
    }
}
