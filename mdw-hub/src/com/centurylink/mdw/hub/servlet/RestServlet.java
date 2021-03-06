package com.centurylink.mdw.hub.servlet;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@WebServlet(urlPatterns={"/api/*", "/services/*", "/Services/*", "/REST/*"}, loadOnStartup=1)
public class RestServlet extends ServiceServlet {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestServlet.doGet()", true);

        if (request.getPathInfo() == null) {
            // redirect to html documentation
            response.sendRedirect(ApplicationContext.getMdwHubUrl() + "/doc/webServices.html");
            return;
        }
        else if (request.getPathInfo().startsWith("/SOAP")) {
            // forward to SOAP servlet
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(request.getPathInfo());
            requestDispatcher.forward(request, response);
            return;
        }
        else if (ApplicationContext.isDevelopment() && isFromLocalhost(request)) {
            // this is only allowed from localhost and in dev
            if ("/System/exit".equals(request.getPathInfo())) {
                response.setStatus(200);
                new Thread(new Runnable() {
                    public void run() {
                        System.exit(0);
                    }
                }).start();
                return;
            }
        }

        Map<String,String> metaInfo = buildMetaInfo(request);
        try {
            String responseString = handleRequest(request, response, metaInfo);

            String downloadFormat = metaInfo.get(Listener.METAINFO_DOWNLOAD_FORMAT);
            if (downloadFormat != null) {
                response.setContentType(Listener.CONTENT_TYPE_DOWNLOAD);
                String fileName = request.getPathInfo().substring(1).replace('/', '-') + "." + downloadFormat;
                response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
                if (downloadFormat.equals(Listener.DOWNLOAD_FORMAT_JSON)
                        || downloadFormat.equals(Listener.DOWNLOAD_FORMAT_XML)
                        || downloadFormat.equals(Listener.DOWNLOAD_FORMAT_TEXT)) {
                    response.setContentLength(responseString.getBytes().length);
                    response.getOutputStream().write(responseString.getBytes());
                }
                else if (downloadFormat.equals(Listener.DOWNLOAD_FORMAT_FILE)) {
                    String f = metaInfo.get(Listener.METAINFO_DOWNLOAD_FILE);
                    if (f == null)
                        throw new ServiceException(ServiceException.INTERNAL_ERROR, "Missing meta");
                    File file = new File(f);
                    if (!file.isFile())
                        throw new ServiceException(ServiceException.NOT_FOUND, "File not found: " + file.getAbsolutePath());
                    int max = PropertyManager.getIntegerProperty(PropertyNames.MAX_DOWNLOAD_BYTES, 104857600);
                    if (file.length() > max)
                        throw new ServiceException(ServiceException.NOT_ALLOWED, file.getAbsolutePath() + " exceeds max download size (" + max + "b )");
                    response.setHeader("Content-Disposition", "attachment;filename=\"" + file.getName() + "\"");
                    response.setContentLengthLong(file.length());
                    try (InputStream in = new FileInputStream(file)) {
                        int read = 0;
                        byte[] bytes = new byte[8192];
                        while ((read = in.read(bytes)) != -1)
                            response.getOutputStream().write(bytes, 0, read);
                    }
                }
                else {
                    // for binary content string response will have been Base64 encoded
                    byte[] decoded = Base64.decodeBase64(responseString.getBytes());
                    response.setContentLength(decoded.length);
                    response.getOutputStream().write(decoded);
                }
            }
            else {
                if (("/System/sysInfo".equals(request.getPathInfo()) || "/System/System".equals(request.getPathInfo()))
                        && "application/json".equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE))) {
                    responseString = WebAppContext.addContextInfo(responseString, request);
                }
                response.setContentLength(responseString.getBytes().length);
                response.getOutputStream().write(responseString.getBytes());
            }
        }
        catch (ServiceException ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(ex.getCode());
            response.getWriter().println(createErrorResponseMessage(request, metaInfo, ex));
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestServlet.doPost()", true);

        if (request.getPathInfo() != null && request.getPathInfo().startsWith("/SOAP")) {
            // forward to SOAP servlet
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(request.getPathInfo());
            requestDispatcher.forward(request, response);
            return;
        }

        Map<String,String> metaInfo = buildMetaInfo(request);
        try {
            String responseString = handleRequest(request, response, metaInfo);
            response.getOutputStream().print(responseString);
        }
        catch (ServiceException ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(ex.getCode());
            response.getWriter().println(createErrorResponseMessage(request, metaInfo, ex));
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestServlet.doPut()", true);

        Map<String,String> metaInfo = buildMetaInfo(request);
        try {
            String responseString = handleRequest(request, response, metaInfo);
            response.getOutputStream().print(responseString);
        }
        catch (ServiceException ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(ex.getCode());
            response.getWriter().println(createErrorResponseMessage(request, metaInfo, ex));
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }


    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestServlet.doDelete()", true);

        Map<String,String> metaInfo = buildMetaInfo(request);
        try {
            String responseString = handleRequest(request, response, metaInfo);
            response.getOutputStream().print(responseString);
        }
        catch (ServiceException ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(ex.getCode());
            response.getWriter().println(createErrorResponseMessage(request, metaInfo, ex));
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestServlet.doPatch()", true);

        Map<String,String> metaInfo = buildMetaInfo(request);
        try {
            String responseString = handleRequest(request, response, metaInfo);
            response.getOutputStream().print(responseString);
        }
        catch (ServiceException ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(ex.getCode());
            response.getWriter().println(createErrorResponseMessage(request, metaInfo, ex));
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod()) || "PATCH".equalsIgnoreCase(request.getHeader("X-HTTP-Method-Override"))) {
            doPatch(request, response);
        }
        else {
            super.service(request, response);
        }
    }

    protected String handleRequest(HttpServletRequest request, HttpServletResponse response, Map<String,String> metaInfo)
            throws ServiceException, IOException {

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug(getClass().getSimpleName() + " " + request.getMethod() + ":\n   "
              + request.getRequestURI() + (request.getQueryString() == null ? "" : ("?" + request.getQueryString())));
        }

        String requestString = null;
        // DELETE can have a body in some containers
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            BufferedReader reader = request.getReader();
            StringBuffer requestBuffer = new StringBuffer(request.getContentLength() < 0 ? 0 : request.getContentLength());
            String line;
            while ((line = reader.readLine()) != null)
                requestBuffer.append(line).append('\n');

            // log request
            requestString = requestBuffer.toString();
            if (logger.isMdwDebugEnabled()) {
                logger.mdwDebug(getClass().getSimpleName() + " " + request.getMethod() + " Request:\n" + requestString);
            }
        }

        authenticate(request, metaInfo, requestString);
        if (metaInfo.containsKey(Listener.METAINFO_REQUEST_PAYLOAD)) {
            requestString = metaInfo.get(Listener.METAINFO_REQUEST_PAYLOAD);
            metaInfo.remove(Listener.METAINFO_REQUEST_PAYLOAD);
        }

        Set<String> reqHeaderKeys = new HashSet<>(metaInfo.keySet());
        String responseString = new ListenerHelper().processRequest(requestString, metaInfo);
        populateResponseHeaders(reqHeaderKeys, metaInfo, response);
        if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) == null)
            response.setContentType("application/json");
        else
            response.setContentType(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));

        if (metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE) != null && !metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE).equals("0"))
            response.setStatus(Integer.parseInt(metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE)));

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug(getClass().getSimpleName() + " " + request.getMethod() + " Response:\n" + responseString);
        }

        return responseString;
    }

}
