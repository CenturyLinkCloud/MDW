package com.centurylink.mdw.container.plugin.activemq;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.naming.NamingException;

import com.centurylink.mdw.container.ContextProvider;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.connection.DelegatingConnectionFactory;

import com.centurylink.mdw.container.JmsProvider;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class ActiveMqJms implements JmsProvider {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Autowired
    @Qualifier("pooledConnectionFactory")
    private ConnectionFactory defaultConnectionFactory;

    public ConnectionFactory getConnectionFactory() throws JMSException, NamingException {
        return getQueueConnectionFactory(null, null);
    }

    public QueueConnectionFactory getQueueConnectionFactory(ContextProvider contextProvider, String name) throws JMSException, NamingException {
        ConnectionFactory connectionFactory = retrieveConnectionFactory(name);
        if (connectionFactory instanceof QueueConnectionFactory) {
            return (QueueConnectionFactory) connectionFactory;
        }
        else {
            DelegatingConnectionFactory delegatingFactory = new DelegatingConnectionFactory();
            delegatingFactory.setTargetConnectionFactory(connectionFactory);
            return delegatingFactory;
        }
    }

    public TopicConnectionFactory getTopicConnectionFactory(ContextProvider contextProvider, String name) throws JMSException, NamingException {
        ConnectionFactory connectionFactory = retrieveConnectionFactory(name);
        if (connectionFactory instanceof TopicConnectionFactory) {
            return (TopicConnectionFactory) connectionFactory;
        }
        else {
            DelegatingConnectionFactory delegatingFactory = new DelegatingConnectionFactory();
            delegatingFactory.setTargetConnectionFactory(connectionFactory);
            return delegatingFactory;
        }
    }

    public Topic getTopic(String name) {
        return new ActiveMQTopic(name);
    }

    public Queue getQueue(Session session, ContextProvider contextProvider, String name) throws JMSException {
        return session.createQueue(name);
    }

    public void setMessageDelay(QueueSender sender, Message message, long delaySeconds) throws JMSException {
        message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delaySeconds * 1000);
    }

    /**
     * Pooling and remote queues are configured via Spring in broker config XML.
     */
    protected ConnectionFactory retrieveConnectionFactory(String name) throws JMSException {
        if (name == null && defaultConnectionFactory != null) {
            return defaultConnectionFactory; // injected
        }
        else {
            try {
                // autowiring did not occur
                return (ConnectionFactory)SpringAppContext.getInstance().getBean(name);
            }
            catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new JMSException("JMS ConnectionFactory not found: " + name);
            }
        }
    }
}
