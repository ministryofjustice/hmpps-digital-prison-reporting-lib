package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.UncategorizedSQLException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import software.amazon.awssdk.services.redshiftdata.model.ActiveStatementsExceededException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.MissingTableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UserAuthorisationException

@RestControllerAdvice
class DigitalPrisonReportingExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  @ResponseStatus(BAD_REQUEST)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> = respondWithBadRequest(e)

  @ExceptionHandler(software.amazon.awssdk.services.redshiftdata.model.ValidationException::class)
  @ResponseStatus(BAD_REQUEST)
  fun handleRedshiftDataValidationException(e: Exception): ResponseEntity<ErrorResponse> = respondWithBadRequest(e)

  @ExceptionHandler(ActiveStatementsExceededException::class)
  @ResponseStatus(TOO_MANY_REQUESTS)
  fun handleRedshiftActiveStatementsExceededException(e: Exception): ResponseEntity<ErrorResponse> = respondWithTooManyRequests(e)

  @ExceptionHandler(NoResourceFoundException::class)
  @ResponseStatus(NOT_FOUND)
  fun handleNotImplemented(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "Endpoint does not exist: ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleTypeMismatch(e: Exception): ResponseEntity<ErrorResponse> = respondWithBadRequest(e)

  @ExceptionHandler(UncategorizedSQLException::class)
  fun handleEntityNotFound(e: Exception): ResponseEntity<ErrorResponse> {
    val entityNotFoundMessage = "EntityNotFoundException from glue - Entity Not Found"
    return if (e.message?.contains(entityNotFoundMessage) == true) {
      log.warn("Table not found exception: {}", e.message)
      return ResponseEntity
        .status(NOT_FOUND)
        .body(
          ErrorResponse(
            status = NOT_FOUND,
            userMessage = "The stored report or dashboard was not found.",
            developerMessage = e.message,
          ),
        )
    } else {
      handleInternalServerError(e)
    }
  }

  @ExceptionHandler(MissingTableException::class)
  fun handleMissingTableException(e: Exception): ResponseEntity<ErrorResponse> {
    log.warn("Table not found exception: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "The stored report or dashboard was not found.",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  @ResponseStatus(INTERNAL_SERVER_ERROR)
  fun handleInternalServerError(e: java.lang.Exception): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error.",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(UserAuthorisationException::class)
  @ResponseStatus(FORBIDDEN)
  fun handleUserAuthorisationException(e: Exception): ResponseEntity<ErrorResponse> = respondWithForbiddenRequest(e)

  private fun respondWithBadRequest(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  private fun respondWithTooManyRequests(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Number of active statements exceeded the limit: {}", e.message)
    return ResponseEntity
      .status(TOO_MANY_REQUESTS)
      .body(
        ErrorResponse(
          status = TOO_MANY_REQUESTS,
          userMessage = "Number of active statements exceeded the limit: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  private fun respondWithForbiddenRequest(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("User Authorisation exception: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "User Authorisation exception: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
