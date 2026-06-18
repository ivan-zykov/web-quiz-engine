package net.ivanvzykov.webquizengine.presentation

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import net.ivanvzykov.webquizengine.application.QuizId
import java.time.LocalDateTime

data class QuizOutDto(
    val id: QuizId,
    val title: String,
    val text: String,
    val options: List<String>,
)

data class ResultDto(
    val success: Boolean,
    val feedback: String
)

data class QuizInDto(
    @field:NotBlank(message = "Field title must be not blank")
    val title: String,
    @field:NotBlank(message = "Field text must be not blank")
    val text: String,
    @field:Size(min = 2, message = "Field options should have at least two elements")
    val options: List<String>,
    val answer: List<Int>?,
)

data class AnswerDto(val answer: List<Int>)

data class UserCredentialsDTO(
    @field:NotNull(message = "Field email must be not null")
    @field:Email(regexp = ".+@.+\\..+", message = "Field email must be a valid email")
    val email: String,

    @field:Size(min = 5, message = "Field password must have at least five characters")
    val password: String,
)

data class CompletionOfQuizDto(
    val id: Long,
    val completedAt: LocalDateTime,
)

data class PageResponseDto<T>(
    val content: List<T>,
    val number: Int,
    val size: Int,
    val totalPages: Int,
    val totalElements: Long,
    val first: Boolean,
    val last: Boolean,
)
