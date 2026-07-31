package net.ivanvzykov.webquizengine.application

import java.time.LocalDateTime

data class NewQuiz(
    val title: String,
    val text: String,
    val options: List<String>,
    val answer: List<Int>?,
)

data class Quiz(
    val title: String,
    val text: String,
    val options: List<String>,
    val answer: List<Int>?,
    val id: Long,
    val authorUsername: String,
)

data class PublicQuiz(
    val id: Long,
    val title: String,
    val text: String,
    val options: List<String>,
)

data class SolvableQuiz(
    val answers: List<Int>?,
)

data class DeletableQuiz(
    val authorUsername: String,
)

data class AnswerResult(
    val success: Boolean,
    val feedback: String
)

@JvmInline
value class Answer(val value: List<Int>)

data class UserCredentials(
    val email: String,
    val password: String
)

data class CompletionOfQuiz(
    val id: Long,
    val quiz: Quiz,
    val userName: String,
    val completedAt: LocalDateTime,
)
