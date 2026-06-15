package net.ivanvzykov.webquizengine

import java.time.LocalDateTime

data class NewQuiz(
    val title: String,
    val text: String,
    val options: List<String>,
    val answer: List<Int>?,
)

@JvmInline
value class QuizId(val value: Long)

data class Quiz(
    val title: String,
    val text: String,
    val options: List<String>,
    val answer: List<Int>?,
    val id: QuizId,
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserCredentials

        return email == other.email
    }

    override fun hashCode(): Int {
        return email.hashCode()
    }
}

class CompletionOfQuiz(
    val id: Long,
    val quiz: Quiz,
    val userName: String,
    val completedAt: LocalDateTime,
)
