package com.centurylink.mdw.workflow.adapter.jms;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.adapter.AdapterException;
import com.centurylink.mdw.adapter.ConnectionException;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.util.JMSServices;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.ObjectAdapterActivity;
import org.apache.xmlbeans.XmlObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.w3c.dom.Document;

import javax.jms.*;


/**
 * New implementation of JMS Adapter which can be
 * configured at design time and does not implement
 * AdapterActivity interface.
 */
@Tracked(LogLevel.TRACE)
@Activity(value="JMS Adapter", category=AdapterActivity.class, icon="com.centurylink.mdw.base/adapter.png",
        pagelet="com.centurylink.mdw.base/jmsAdapter.pagelet")
public class JmsAdapter extends ObjectAdapterActivity {
    public static final String SERVER_URL = "JNDI Server URL";
    public static final String REQUEST_QUEUE_NAME = "Queue Name";
    public static final String RESPONSE_QUEUE_NAME = "Reply Queue Name";
    public static final String CORRELATION_ID = "Correlation ID";
    public static final String SYNCHRONOUS_RESPONSE = "Wait for response?";
    public static final String TIMEOUT = "Time out (sec)";

    private QueueConnection qConnection;
    private QueueSession qSession;
    private Queue queue;

    @Autowired
    @Qualifier("jmsTemplate")
    private JmsTemplate jmsSenderReceiver;

    /**
     * Default constructor with params
     */
    public JmsAdapter(){
       super();
    }

    /**
     * The synchronous JMS adapter is not yet implemented, so the method always
     * returns false.
     */
    public boolean isSynchronous() {
        String attr = this.getAttributeValue(SYNCHRONOUS_RESPONSE);
        return (attr!=null && attr.equalsIgnoreCase("true"));
    }

    /**
     * The method is used by {@link #openConnection()} to obtain the queue name.
     * The default implementation returns the value of the attribute "Queue Name",
     * using getAttributeValueSmart, so that a variable or property specification
     * is allowed in addition to plain queue name.
     * You can override the method to obtain a queue name in a different way.
     * @return the actual name of the remote queue to receive the message.
     * @throws PropertyException
     */
    protected String getQueueName() throws PropertyException {
        return this.getAttributeValueSmart(REQUEST_QUEUE_NAME);
    }

    /**
     * The method overrides the one in the super class to perform
     * JMS specific functions.
     */
    @Override
    public Object invoke(Object conn, Object requestData)
        throws AdapterException, ConnectionException
    {
        try {
            Object result;

            if (jmsSenderReceiver != null) {

                final String responseQueueName = this.getAttributeValueSmart(RESPONSE_QUEUE_NAME);
                final String requestDataForMessage = (String) requestData;
                final Queue replyQueue;
                final String correlationId = getCorrelationId();

                boolean replyIsTemporaryQueue = false;
                if (responseQueueName != null && responseQueueName.length() > 0) {
                    replyQueue = (Queue) ApplicationContext.getContextProvider().lookup(null,
                            responseQueueName, Queue.class);
                    // msg.setJMSReplyTo(replyQueue);
                }
                else if (this.isSynchronous()) {
                    replyQueue = qSession.createTemporaryQueue();
                    // msg.setJMSReplyTo(replyQueue) ;
                    replyIsTemporaryQueue = true;
                }
                else
                    replyQueue = null;

                jmsSenderReceiver.send(getQueueName(), new MessageCreator() {

                    @Override
                    public Message createMessage(Session session) throws JMSException {
                        Message message = session.createTextMessage(requestDataForMessage);
                        if (correlationId != null)
                            message.setJMSCorrelationID(correlationId);
                        if (replyQueue != null) {
                            message.setJMSReplyTo(replyQueue);
                        }
                        return message;
                    }

                });
                if (this.isSynchronous()) {
                    Message message = null;
                    jmsSenderReceiver.setReceiveTimeout(getTimeoutForResponse() * 1000L);
                    if (replyIsTemporaryQueue) {
                        message = jmsSenderReceiver.receive(replyQueue);
                    }
                    else {
                        String messageSelector = "JMSCorrelationID = '" + correlationId + "'";
                        message = jmsSenderReceiver.receiveSelected(replyQueue, messageSelector);
                    }
                    if (message == null) {
                        throw new ActivityException(
                                "Synchronous JMS call times out while waiting for response");
                    }
                    else {
                        result = ((TextMessage)message).getText();
                    }
                }
                else
                    result = null;
            }
            else {
                QueueSession qSession = (QueueSession)conn;
                Queue replyQueue;
                MessageConsumer consumer = null;
                boolean replyIsTemporaryQueue = false;
                TextMessage msg;
                String correlationId;
                try (QueueSender qSender = qSession.createSender(queue)) {
                    msg = qSession.createTextMessage((String) requestData);
                    String responseQueueName = this.getAttributeValueSmart(RESPONSE_QUEUE_NAME);
                    if (responseQueueName != null && responseQueueName.length() > 0) {
                        replyQueue = (Queue) ApplicationContext.getContextProvider().lookup(null, responseQueueName, Queue.class);
                        msg.setJMSReplyTo(replyQueue);
                    } else if (this.isSynchronous()) {
                        replyQueue = qSession.createTemporaryQueue();
                        msg.setJMSReplyTo(replyQueue);
                        replyIsTemporaryQueue = true;
                    } else replyQueue = null;
                    correlationId = getCorrelationId();
                    if (correlationId != null) msg.setJMSCorrelationID(correlationId);
                    qSender.send(msg);
                    if (this.isSynchronous()) {
                        if (replyIsTemporaryQueue) {
                            consumer = qSession.createConsumer(replyQueue);
                        } else {
                            String messageSelector = "JMSCorrelationID = '" + correlationId + "'";
                            consumer = qSession.createConsumer(replyQueue, messageSelector);
                        }
                        msg = (TextMessage)consumer.receive(getTimeoutForResponse()*1000L);
                        if (msg==null) {
                            throw new ActivityException("Synchronous JMS call times out while waiting for response");
                        } else {
                            result = msg.getText();
                        }
                    } else result = null;
                }
                finally {
                    if (consumer != null)
                        consumer.close();
                }
            }
            return result;
        } catch (Exception e) {
            throw new AdapterException(-1, "Exception in invoking JmsAdapter" , e);
        }
    }

    protected String getCorrelationId() throws PropertyException {
         String correlationId = this.getAttributeValueSmart(CORRELATION_ID);
         if (correlationId!=null && correlationId.length()>0) {
             correlationId = translatePlaceHolder(correlationId);
         }
         return correlationId;
    }

    /**
     * Timeout value for waiting for responses. Used for synchronous mode only.
     * The default method reads from the attribute "Time out (sec)", and
     * return 30 seconds if the attribute is not defined.
     *
     * @return timeout value in seconds
     */
    protected int getTimeoutForResponse() {
        String timeout_s = null;
        int timeout;
        try {
            timeout_s = this.getAttributeValueSmart(TIMEOUT);
            timeout = timeout_s==null?30:Integer.parseInt(timeout_s);
            if (timeout<0) timeout = 30;
        } catch (NumberFormatException e) {
            getLogger().error("Cannot parse timeout value " + timeout_s, e);
            timeout = 30;
        } catch (PropertyException e) {
            getLogger().error("Cannot read timeout attribute " + TIMEOUT, e);
            timeout = 30;
        }
        return timeout;
    }

    /**
     * The method overrides the one in the super class to perform
     * JMS specific functions.
     */
    @Override
    protected void closeConnection(Object connection) {
        try {
            if (qSession!=null) qSession.close();
            if (qConnection!=null) qConnection.close();
        } catch (Exception ignored) {
        }
        qSession = null;
        qConnection = null;
    }

    /**
     * The method overrides the one in the super class to perform
     * JMS specific functions.
     */
    @Override
    protected Object openConnection() throws ConnectionException {
        qConnection = null;
        qSession = null;
        queue = null;
        try {
            String server_url = this.getAttributeValueSmart(SERVER_URL);
            if ("THIS_SERVER".equals(server_url)) server_url = null;
            String queue_name = this.getQueueName();
            JMSServices jmsServices = JMSServices.getInstance();
            QueueConnectionFactory qFactory = jmsServices.getQueueConnectionFactory(server_url);
            qConnection = qFactory.createQueueConnection();
            qSession = qConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            qConnection.start();
            queue = jmsServices.getQueue(qSession, queue_name);
        } catch (Exception e) {
            getLogger().error("Exception in JmsAdapter.openConnection()" , e);
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, "Exception in invoking JmsAdapter" , e);

        }
        return qSession;
    }

    /**
     * The method overrides the one from the super class and perform the followings:
     * <ul>
     *   <li>It gets the value of the variable with the name specified in the attribute
     *      REQUEST_VARIABLE. The value is typically an XML document or a string</li>
     *   <li>It invokes the variable translator to convert the value into a string
     *      and then return the string value.</li>
     * </ul>
     * @throws ActivityException if the variable is not bound, or the value is not a DocumentReference or String
     */
    @Override
    protected Object getRequestData() throws ActivityException {
        Object request = super.getRequestData();
        if (request == null)
            throw new ActivityException("Request data is null");
        if (request instanceof DocumentReference)
            request = getDocumentContent((DocumentReference)request);
        if (request instanceof String)
            return request;
        else if (request instanceof Document)
            return getPackage().getStringValue(Document.class.getName(), request, true);
        else if (request instanceof XmlObject)
            return ((XmlObject)request).xmlText();
        else
            throw new ActivityException("Cannot handle request of type " + request.getClass().getName());
    }
}
