package com.epam.learn.microservices.fundamentals.resource.service.service

interface ResourceEventPublisher {

    fun publishEvent(event: ResourceEvent<*>)
}
