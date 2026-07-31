package net.ivanvzykov.webquizengine.presentation

import jakarta.validation.Valid
import net.ivanvzykov.webquizengine.application.Answer
import net.ivanvzykov.webquizengine.application.AnswerResult
import net.ivanvzykov.webquizengine.application.CompletionOfQuiz
import net.ivanvzykov.webquizengine.application.NewQuiz
import net.ivanvzykov.webquizengine.application.PublicQuiz
import net.ivanvzykov.webquizengine.application.QuizService
import net.ivanvzykov.webquizengine.application.UserCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.net.URI

@Suppress("unused")
@RestController
@RequestMapping("/api")
class QuizEngineController @Autowired constructor(private val quizService: QuizService) {

    @PostMapping("/quizzes")
    fun addQuiz(
        @Valid @RequestBody quiz: QuizInDto,
        @AuthenticationPrincipal userDetails: UserDetails,
    ): ResponseEntity<QuizOutDto> {
        val createdQuiz = quizService.addQuiz(quiz.toNewQuiz(), userDetails)

        return ResponseEntity
            .created(URI.create("/api/quizzes/${createdQuiz.id}"))
            .body(createdQuiz.toDto())
    }

    @GetMapping("/quizzes/{id}")
    fun getQuizBy(@PathVariable id: Long): ResponseEntity<QuizOutDto> {
        val quiz = quizService.getQuizBy(id)

        return ResponseEntity
            .ok()
            .body(quiz.toDto())
    }

    @DeleteMapping("/quizzes/{id}")
    fun deleteQuizById(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails,
    ): ResponseEntity<Void> {
        quizService.deleteQuizBy(id, userDetails)

        return ResponseEntity
            .noContent()
            .build()
    }

    @GetMapping("/quizzes")
    fun getAllQuizzes(@RequestParam(defaultValue = "0") page: Int): ResponseEntity<PageResponseDto<QuizOutDto>> {
        val quizzes = quizService.getAllQuizzesPaginated(page)

        return ResponseEntity
            .ok()
            .body(quizzes.toDto { it.toDto() })
    }

    @PostMapping("/quizzes/{id}/solve")
    fun solveQuizBy(
        @PathVariable id: Long,
        @RequestBody answer: AnswerDto,
        @AuthenticationPrincipal userDetails: UserDetails,
    ): ResponseEntity<AnswerResultDto> {
        val result = quizService.solveQuizBy(id, answer.toDomain(), userDetails)

        return ResponseEntity
            .ok()
            .body(result.toDto())
    }

    @GetMapping("/quizzes/completed")
    fun getCompletionsBy(
        @RequestParam(defaultValue = "0") page: Int,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<PageResponseDto<CompletionOfQuizDto>> {
        val completions =
            quizService.getAllCompletionsPaginatedSortedByCompletedAtDescBy(userDetails, page)

        return ResponseEntity
            .ok()
            .body(completions.toDto { it.toDto() })
    }

    @PostMapping("/register")
    fun registerNewUser(@Valid @RequestBody newCredentials: UserCredentialsDTO) {
        quizService.registerNewUser(newCredentials.toDomain())
    }
}

private fun AnswerDto.toDomain(): Answer = Answer(this.answer)

private fun UserCredentialsDTO.toDomain() = UserCredentials(
    email = this.email,
    password = this.password,
)

private fun AnswerResult.toDto() = AnswerResultDto(
    success = success,
    feedback = feedback,
)

private fun QuizInDto.toNewQuiz() = NewQuiz(
    title = title,
    text = text,
    options = options,
    answer = answer,
)

private fun PublicQuiz.toDto() = QuizOutDto(
    id = this.id,
    title = this.title,
    text = this.text,
    options = this.options,
)

private fun CompletionOfQuiz.toDto() = CompletionOfQuizDto(
    id = this.quiz.id,
    completedAt = this.completedAt
)

private fun <T : Any, R> Page<T>.toDto(mapper: (T) -> R) = PageResponseDto(
    content = this.content.map(mapper),
    number = this.number,
    size = this.size,
    totalPages = this.totalPages,
    totalElements = this.totalElements,
    first = this.isFirst,
    last = this.isLast
)