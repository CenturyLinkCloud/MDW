package com.centurylink.mdw.tests.code;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

// asset-defined jaxb asset java class

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "requiredElement",
    "optionalElement"
})
@XmlRootElement(name = "JaxbAsset")
public class JaxbAsset implements java.io.Serializable {

    @XmlElement(name = "RequiredElement", required = true)
    protected String requiredElement;
    public String getRequiredElement() {
        return requiredElement;
    }
    public void setRequiredElement(String value) {
        this.requiredElement = value;
    }

    @XmlElement(name = "OptionalElement")
    protected String optionalElement;
    public String getOptionalElement() {
        return optionalElement;
    }
    public void setOptionalElement(String value) {
        this.optionalElement = value;
    }

    @XmlAttribute(required = true)
    protected String requiredAttribute;
    public String getRequiredAttribute() {
        return requiredAttribute;
    }
    public void setRequiredAttribute(String value) {
        this.requiredAttribute = value;
    }

    @XmlAttribute
    protected String optionalAttribute;
    public String getOptionalAttribute() {
        return optionalAttribute;
    }
    public void setOptionalAttribute(String value) {
        this.optionalAttribute = value;
    }
}
