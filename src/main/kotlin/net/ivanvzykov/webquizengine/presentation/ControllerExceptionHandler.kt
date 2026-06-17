package net.ivanvzykov.webquizengine.presentation

import net.ivanvzykov.webquizengine.application.DuplicatedUserException
import net.ivanvzykov.webquizengine.application.QuizNotFoundException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

typealias ErrorBody = Map<String, String>

@ControllerAdvice
@Suppress("unused")
class ControllerExceptionHandler : ResponseEntityExceptionHandler() {
    override fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<in Any>? {
        val body = makeErrorBodyFor(ex)

        return ResponseEntity(body, headers, HttpStatus.BAD_REQUEST)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<in Any>? {
        val body = makeErrorBodyFor(ex)

        return ResponseEntity(body, headers, HttpStatus.BAD_REQUEST)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<in Any>? {
        val errorMessages: List<String> = buildList {
            ex.bindingResult.allErrors.forEach { error ->
                error.defaultMessage?.let { add(it) }
            }
        }
        val body: ErrorBody = mapOf(
            "error" to errorMessages.joinToString("; ")
        )

        return ResponseEntity(body, headers, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorBody> {
        val body = makeErrorBodyFor(ex)

        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(QuizNotFoundException::class)
    fun handleQuizNotFound(exception: QuizNotFoundException): ResponseEntity<ErrorBody> {
        val body = makeErrorBodyFor(exception)

        return ResponseEntity(body, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(DuplicatedUserException::class)
    fun handleDuplicatedUser(exception: DuplicatedUserException): ResponseEntity<ErrorBody> {
        val body = makeErrorBodyFor(exception)

        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(exception: AccessDeniedException): ResponseEntity<ErrorBody> {
        val body = makeErrorBodyFor(exception)

        return ResponseEntity(body, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleAccessDenied(exception: IllegalStateException): ResponseEntity<ErrorBody> {
        val body = makeErrorBodyFor(exception)

        return ResponseEntity(body, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun makeErrorBodyFor(ex: Exception): ErrorBody =
        mapOf("error" to (ex.message ?: ""))
}
