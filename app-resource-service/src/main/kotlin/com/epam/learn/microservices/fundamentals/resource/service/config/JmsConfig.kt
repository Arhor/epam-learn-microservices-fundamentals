package com.epam.learn.microservices.fundamentals.resource.service.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration
import org.springframework.boot.autoconfigure.jms.JmsProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import org.springframework.jms.support.converter.MappingJackson2MessageConverter
import org.springframework.jms.support.converter.MessageConverter
import org.springframework.jms.support.converter.MessageType
import org.springframework.jms.support.destination.DestinationResolver
import org.springframework.jms.support.destination.DynamicDestinationResolver
import javax.jms.ConnectionFactory
import javax.jms.ExceptionListener

/**
 * JmsAutoConfiguration is not imported via [org.springframework.context.annotation.Import]
 * since it causes autoconfiguration to not be applied at all.
 *
 * @see org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JmsProperties::class)
class JmsConfig {

    @Bean
    fun dynamicDestinationResolver(): DestinationResolver {
        return DynamicDestinationResolver()
    }

    @Bean
    fun jacksonJmsMessageConverter(): MessageConverter {
        return MappingJackson2MessageConverter().apply {
            setTargetType(MessageType.TEXT)
            setTypeIdPropertyName("_type")
            setObjectMapper(jacksonObjectMapper())
        }
    }

    /**
     * JmsListenerContainerFactory configured manually since autoconfiguration
     * provided factory set to be transacted which is unsupported by Amazon SQS.
     */
    @Bean
    fun jmsListenerContainerFactory(
        connectionFactory: ConnectionFactory,
        destinationResolver: ObjectProvider<DestinationResolver>,
        messageConverter: ObjectProvider<MessageConverter>,
        exceptionListener: ObjectProvider<ExceptionListener>,
        properties: JmsProperties,
    ): JmsListenerContainerFactory<*> {
        return DefaultJmsListenerContainerFactory().apply {

            setConnectionFactory(connectionFactory)

            destinationResolver.ifUnique {
                setDestinationResolver(it)
            }
            messageConverter.ifUnique {
                setMessageConverter(it)
            }
            exceptionListener.ifUnique {
                setExceptionListener(it)
            }

            properties.listener.let { listener ->
                setAutoStartup(listener.isAutoStartup)

                listener.acknowledgeMode?.let {
                    setSessionAcknowledgeMode(it.mode)
                }
                listener.formatConcurrency()?.let {
                    setConcurrency(it)
                }
                listener.receiveTimeout?.let {
                    setReceiveTimeout(it.toMillis())
                }
            }
        }
    }
}
