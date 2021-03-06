package com.centurylink.mdw.common.translator.impl;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.lang.reflect.Method;

public class XmlBeanTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static XmlOptions xmlOptions = getXmlOptions();

    /**
     * TODO honor passed type to create appropriate instance
     */
    @Override
    public Object toObject(String str, String type) throws TranslationException {
        try {
            XmlObject xmlBean = XmlObject.Factory.parse(str, xmlOptions);
            return xmlBean;
        }
        catch (XmlException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    @Override
    public String toString(Object obj, String variableType) throws TranslationException {
        XmlObject xmlBean = (XmlObject) obj;
        XmlOptions tempOptions = new XmlOptions(xmlOptions);
        tempOptions.setSavePrettyPrint();
        return xmlBean.xmlText(tempOptions);
    }

    public Document toDomDocument(Object obj) throws TranslationException {
        return (Document) ((XmlObject) obj).getDomNode();
    }

    public Object fromDomNode(Node domNode) throws TranslationException {
        try {
            return XmlObject.Factory.parse(domNode);
        }
        catch (XmlException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    /**
     * Moved this to a static block/method so it's done only once
     * @return XmlOptions
     */
    private static XmlOptions getXmlOptions() {
        String[] xmlOptionsProperties = new String[] { PropertyNames.MDW_TRANSLATOR_XMLBEANS_LOAD_OPTIONS,
                PropertyNames.MDW_TRANSLATOR_XMLBEANS_SAVE_OPTIONS };
        /**
         * First set it up with default compatibility options
         */
        xmlOptions = new XmlOptions();

        /**
         * Next get the options that the user needs from the properties file.
         */
        Class<?> xmlOptionsClass = xmlOptions.getClass();

        for (int i = 0; i < xmlOptionsProperties.length; i++) {
            String property = xmlOptionsProperties[i];
            String opt = PropertyManager.getProperty(property);
            /**
             * Only do reflection if we need to
             */
            if (opt != null && !"".equals(opt)) {
                try {
                    String[] propTable = opt.split(",");
                    for (int j = 0; j < propTable.length; j++) {
                        String prop = propTable[j];
                        String[] optTable = prop.split("=");
                        String option = optTable[0];
                        String value = null;

                        Class<?>[] classArray = new Class<?>[] { Object.class };
                        if (optTable.length > 1) {
                            // Check for int
                            value = optTable[1];
                            classArray = new Class<?>[] { Object.class, Object.class };
                            boolean gotInteger = true;
                            try {
                                Integer.parseInt(value);
                            }
                            catch (NumberFormatException e) {
                                // It's not an integer
                                gotInteger = false;
                            }

                            Method method = xmlOptionsClass.getMethod("put", classArray);
                            method.invoke(xmlOptions, new Object[] { option,
                                    gotInteger ? Integer.valueOf(value) : value });

                        }
                        else {

                            Method method = xmlOptionsClass.getMethod("put", classArray);
                            method.invoke(xmlOptions, new Object[] { option });

                        }
                    }
                }
                catch (Exception ex) {
                    // Just log it
                    logger.error("Unable to set XMLOption " + opt + " due to " + ex.getMessage(), ex);
                }
            }
        }
        return xmlOptions;

    }
}
