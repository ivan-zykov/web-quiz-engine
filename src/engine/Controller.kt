package engine

import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

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
            .ok()
            .body(createdQuiz.toDto())
    }

    @GetMapping("/quizzes/{id}")
    fun getQuizBy(@PathVariable id: QuizId): ResponseEntity<QuizOutDto> {
        val quiz = quizService.getQuizBy(id)

        return ResponseEntity
            .ok()
            .body(quiz.toDto())
    }

    @DeleteMapping("/quizzes/{id}")
    fun deleteQuizById(
        @PathVariable id: QuizId,
        @AuthenticationPrincipal userDetails: UserDetails,
    ): ResponseEntity<Void> {
        quizService.deleteQuizBy(id, userDetails)

        return ResponseEntity
            .noContent()
            .build()
    }

    @GetMapping("/quizzes")
    fun getAllQuizzes(@RequestParam(defaultValue = "0") page: Int): ResponseEntity<Page<QuizOutDto>> {
        val quizzes = quizService.getAllQuizzesPaginated(page)

        return ResponseEntity
            .ok()
            .body(quizzes.map { it.toDto() })
    }

    @PostMapping("/quizzes/{id}/solve")
    fun solveQuizBy(
        @PathVariable id: QuizId,
        @RequestBody answer: AnswerDto,
        @AuthenticationPrincipal userDetails: UserDetails,
    ): ResponseEntity<ResultDto> {
        val result = quizService.solveQuizBy(id, answer.toDomain(), userDetails)

        return ResponseEntity
            .ok()
            .body(result.toDto())
    }

    @GetMapping("/quizzes/completed")
    fun getCompletionsBy(
        @RequestParam(defaultValue = "0") page: Int,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Page<CompletionOfQuizDto>> {
        val completions =
            quizService.getAllCompletionsPaginatedSortedByCompletedAtDescBy(userDetails, page)

        return ResponseEntity
            .ok()
            .body(completions.map { it.toDto() })
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

private fun AnswerResult.toDto() = ResultDto(
    success = success,
    feedback = feedback,
)

private fun QuizInDto.toNewQuiz() = NewQuiz(
    title = title,
    text = text,
    options = options,
    answer = answer,
)

private fun Quiz.toDto() = QuizOutDto(
    id = id,
    title = title,
    text = text,
    options = options,
)

private fun CompletionOfQuiz.toDto() = CompletionOfQuizDto(
    id = this.quiz.id.value.toLong(),
    completedAt = this.completedAt
)
