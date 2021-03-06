package com.centurylink.mdw.monitor;

import java.util.Map;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.ServiceException;

/**
 * ServiceMonitors can be registered through @Monitor annotations to intercept
 * service calls handled by the MDW framework.  Must be threadsafe.
 * Normally all registered monitors are called in sequence.  However the first monitor whose onHandle()
 * returns non-null breaks the chain, and this non-null response is passed back to the consumer.
 *
 * Same instance is guaranteed for all lifecycle methods.
 */
public interface ServiceMonitor extends RegisteredService, Monitor {

    /**
     * Called when the request comes in.  Can change the incoming request contents.
     * Currently must return null or a String.
     *
     * @param request Incoming request message content (String, XMLBean, JAXB Element, JSONObject, etc).
     * @param headers Incoming protocol header values.  May be modified by the monitor.
     * @return null, or an alternative request that should be passed on instead of the incoming request
     */
    default Object onRequest(Object request, Map<String,String> headers) throws ServiceException {
        return null;
    }

    /**
     * Called after onRequest(), but before the appropriate handler is determined.
     * Can be used to short-circuit the request.  Currently must return null or a String.
     * Returning non-null from here bypasses onResponse() and other monitors' onHandle().
     *
     * @param request Incoming request message content (String, XMLBean, JAXB Element, JSONObject, etc).
     * @param headers protocol headers
     * @return null, or an alternative response to send back to the service consumer
     */
    default Object onHandle(Object request, Map<String,String> headers) throws ServiceException {
        return null;
    }

    /**
     * Called before the response is sent out from the MDW event handler or camel route.
     * Currently must return null or a String.
     *
     * @param response Outgoing response message content (probably a String, XMLBean or JAXB Element)
     * @param headers Outbound protocol header values.  May be modified by the monitor.
     * @return null, or an alternative response to send back to the service consumer.
     */
    default Object onResponse(Object response, Map<String,String> headers) throws ServiceException {
        return null;
    }

    /**
     * @param t Throwable error that was encountered
     * @param headers service meta information
     * @return alternate response or null
     */
    default Object onError(Throwable t, Map<String,String> headers) {
        return null;
    }

}
