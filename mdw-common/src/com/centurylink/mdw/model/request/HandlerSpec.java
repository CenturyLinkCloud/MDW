package com.centurylink.mdw.model.request;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.xml.XmlPath;
import org.apache.xmlbeans.XmlException;

/**
 * Request handler spec.
 * TODO: JSONPath
 */
public class HandlerSpec implements Comparable<HandlerSpec>, Jsonable {

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private final String handlerClass;
    public String getHandlerClass() { return handlerClass; }

    private String path;
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    private boolean contentRouting = true;
    public boolean isContentRouting() { return contentRouting; }
    public void setContentRouting(boolean contentRouting) { this.contentRouting = contentRouting; }

    public HandlerSpec(String name, String handlerClass) {
        this.name = name;
        this.handlerClass = handlerClass;
    }

    private XmlPath xpath;
    public XmlPath getXpath() throws XmlException {
        if (xpath == null)
            xpath = new XmlPath(path);
        return xpath;
    }

    private String assetPath;
    public String getAssetPath() { return assetPath; }
    public void setAssetPath(String assetPath) { this.assetPath = assetPath; }

    @Override
    public int compareTo(HandlerSpec other) {
        if (handlerClass.equals(other.handlerClass))
            return path.compareToIgnoreCase(other.handlerClass);
        else
            return handlerClass.compareToIgnoreCase(other.handlerClass);
    }

    @Override
    public String toString() {
        return path + " -> " + handlerClass;
    }

    @Override
    public boolean equals(Object other) {
        return this.toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
