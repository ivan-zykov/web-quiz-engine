package net.ivanvzykov.webquizengine.application

import net.ivanvzykov.webquizengine.persistence.AppUser
import net.ivanvzykov.webquizengine.persistence.AppUserRepository
import net.ivanvzykov.webquizengine.persistence.CompletionOfQuizEntity
import net.ivanvzykov.webquizengine.persistence.CompletionsOfQuizRepository
import net.ivanvzykov.webquizengine.persistence.JpaQuizzesRepository
import net.ivanvzykov.webquizengine.persistence.QuizEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

private const val CONGRATULATIONS = "Congratulations, you're right!"
private const val WRONG_ANSWER = "Wrong answer! Please, try again."
private const val USERNAME_NOT_FOUND_TEMPLATE = "Error. Username %s not found."
private const val QUIZ_NOT_FOUND_TEMPLATE = "Error. Quiz with ID %s not found."

@Service
class QuizService @Autowired constructor(
    private val userRepo: AppUserRepository,
    private val jpaQuizRepo: JpaQuizzesRepository,
    private val completionRepo: CompletionsOfQuizRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
) {
    fun addQuiz(newQuiz: NewQuiz, userDetails: UserDetails): Quiz {
        val user = userRepo.findByUsername(userDetails.username)
            ?: throw UsernameNotFoundException(USERNAME_NOT_FOUND_TEMPLATE.format(userDetails.username))

        val entity = newQuiz.toEntity(user)

        return jpaQuizRepo.save(entity).toDomain()
    }

    @Transactional(readOnly = true)
    fun getQuizBy(id: QuizId): Quiz =
        jpaQuizRepo.findById(id.value)
            .orElseThrow { QuizNotFoundException(QUIZ_NOT_FOUND_TEMPLATE.format(id.value)) }
            .toDomain()

    @Transactional(readOnly = true)
    fun getAllQuizzesPaginated(pageNumber: Int): Page<Quiz> {
        val pageWithMaxTenQuizzes: Pageable = PageRequest.of(
            pageNumber,
            10,
            Sort.by("id").ascending()
        )

        return jpaQuizRepo.findAll(pageWithMaxTenQuizzes)
            .map { it.toDomain() }
    }

    fun solveQuizBy(
        id: QuizId,
        answer: Answer,
        userDetails: UserDetails
    ): AnswerResult {
        val quizEntity = jpaQuizRepo.findById(id.value)
            .orElseThrow { QuizNotFoundException(QUIZ_NOT_FOUND_TEMPLATE.format(id.value)) }
        val quiz = quizEntity.toDomain()

        val (success, feedback) = quiz.check(answer)

        if (success) {
            val user = userRepo.findByUsername(userDetails.username)
                ?: throw UsernameNotFoundException(USERNAME_NOT_FOUND_TEMPLATE.format(userDetails.username))

            val completionEntity = CompletionOfQuizEntity()
            completionEntity.quiz = quizEntity
            completionEntity.user = user
            completionEntity.completedAt = LocalDateTime.now(clock)
            completionRepo.save(completionEntity)
        }

        return AnswerResult(
            success = success,
            feedback = feedback,
        )
    }

    fun registerNewUser(credentials: UserCredentials) {
        val existingUser = userRepo.findByUsername(credentials.email)
        if (existingUser != null) {
            throw DuplicatedUserException("User with email ${credentials.email} already exists")
        }
        val passwordEncoded = passwordEncoder.encode(credentials.password)
            ?: throw IllegalStateException("Failed to encode user's password")
        val newUser = AppUser(
            username = credentials.email,
            password = passwordEncoded
        )
        userRepo.save(newUser)
    }

    @Transactional
    fun deleteQuizBy(
        id: QuizId,
        userDetails: UserDetails
    ) {
        val quizEntity = jpaQuizRepo.findById(id.value)
            .orElseThrow { QuizNotFoundException(QUIZ_NOT_FOUND_TEMPLATE.format(id.value)) }
        val quiz = quizEntity.toDomain()

        if (userDetails.username != quiz.authorUsername) {
            throw AccessDeniedException(
                "Error. Username ${userDetails.username} doesn't math the author's username of quiz with ID ${id.value}."
            )
        }

        val completions = completionRepo.findByQuiz(quizEntity)
            .map { it.toDomain() }
        completions.forEach { completionRepo.deleteById(it.id) }

        jpaQuizRepo.deleteById(id.value)
    }

    @Transactional(readOnly = true)
    fun getTenCompletionsPaginatedSortedDescBy(id: QuizId, pageNumber: Int): Page<CompletionOfQuiz> {
        val pageWithMaxTenSortedByCompletionDesc: Pageable = PageRequest.of(
            pageNumber,
            10,
            Sort.by("completedAt").descending()
        )

        val quizEntity: QuizEntity = jpaQuizRepo.findById(id.value)
            .orElseThrow { QuizNotFoundException(QUIZ_NOT_FOUND_TEMPLATE.format(id.value)) }

        return completionRepo.findByQuiz(quizEntity, pageWithMaxTenSortedByCompletionDesc)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    fun getAllCompletionsPaginatedSortedByCompletedAtDescBy(
        userDetails: UserDetails,
        pageNumber: Int
    ): Page<CompletionOfQuiz> {
        val user = userRepo.findByUsername(userDetails.username)
            ?: throw UsernameNotFoundException(USERNAME_NOT_FOUND_TEMPLATE.format(userDetails.username))

        val pageWithMaxTen: Pageable = PageRequest.of(pageNumber, 10)

        return completionRepo.findByUserOrderByCompletedAtDescIdAsc(user, pageWithMaxTen)
            .map { it.toDomain() }
    }
}

private fun Quiz.check(answer: Answer) =
    if (this.answer?.toSet() == answer.value.toSet() ||
        (this.answer == null && answer.value.isEmpty())
    ) {
        true to CONGRATULATIONS
    } else {
        false to WRONG_ANSWER
    }

private fun NewQuiz.toEntity(user: AppUser): QuizEntity {
    val entity = QuizEntity()
    entity.title = this.title
    entity.text = this.text
    entity.options = this.options
    entity.answers = this.answer
    entity.author = user

    return entity
}

private fun QuizEntity.toDomain() = Quiz(
    title = requireNotNull(this.title) { "Error. QuizEntity.title must not be null" },
    text = requireNotNull(this.text) { "Error. QuizEntity.text must not be null" },
    options = requireNotNull(this.options) { "Error. QuizEntity.options must not be null" },
    answer = this.answers,
    id = QuizId(requireNotNull(this.id) { "Error. QuizEntity.id must not be null" }),
    authorUsername = requireNotNull(this.author?.username) { "Error. QuizEntity.author must not be null" },
)

private fun CompletionOfQuizEntity.toDomain() = CompletionOfQuiz(
    id = requireNotNull(this.id) { "Error. CompletionOfQuizEntity.id must not be null" },
    quiz = requireNotNull(this.quiz) { "Error. CompletionOfQuizEntity.quiz must not be null" }.toDomain(),
    userName = requireNotNull(this.user?.username) { "Error. Error. CompletionOfQuizEntity.user.username must not be null" },
    completedAt = requireNotNull(this.completedAt) { "Error. CompletionOfQuizEntity.completedAt must not be null" },
)
