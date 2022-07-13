package com.epam.learn.microservices.fundamentals.resource.service.controller.error

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.epam.learn.microservices.fundamentals.resource.service.service.exception.EntityDuplicateException
import com.epam.learn.microservices.fundamentals.resource.service.service.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.lang.invoke.MethodHandles
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.validation.ConstraintViolationException

@RestControllerAdvice
class GlobalExceptionHandler(private val messages: MessageSource) {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception::class)
    fun handleDefault(exception: Exception, locale: Locale): ErrorResponse {
        log.error("An error occurred, consider specify a separate exception handler for it", exception)
        return createErrorResponse(ErrorCode.UNCATEGORIZED, locale)
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(EntityDuplicateException::class)
    fun handleEntityDuplicateException(exception: EntityDuplicateException, locale: Locale): ErrorResponse {
        log.error("Resource with specified condition already exists", exception)
        return createErrorResponse(ErrorCode.RESOURCE_DUPLICATE, locale, exception.condition)
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(exception: EntityNotFoundException, locale: Locale): ErrorResponse {
        log.error("Resource with specified condition is not found", exception)
        return createErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, locale, exception.condition)
    }

    @ExceptionHandler(AmazonS3Exception::class)
    fun handleAmazonS3Exception(exception: AmazonS3Exception, locale: Locale): ResponseEntity<ErrorResponse> {
        log.error("Amazon S3 exception occurred", exception)
        return ResponseEntity
            .status(exception.statusCode)
            .body(
                ErrorResponse(
                    message = "Amazon S3 exception occurred",
                    details = listOf(exception.errorMessage),
                    timestamp = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
                )
            )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(exception: ConstraintViolationException, locale: Locale): ErrorResponse {
        log.error("Validation failed", exception)
        val constraintViolationMessages = exception.constraintViolations.map { it.message }
        return createErrorResponse(ErrorCode.VALIDATION_FAILED, locale, details = constraintViolationMessages)
    }

    private fun createErrorResponse(
        code: ErrorCode,
        locale: Locale,
        vararg args: Any,
        details: List<String>? = null
    ): ErrorResponse {
        val message = messages.getMessage(code.label, args, locale)
        val timestamp = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)

        return ErrorResponse(code, message, details, timestamp)
    }

    companion object {
        private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
