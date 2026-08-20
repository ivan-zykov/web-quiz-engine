package net.ivanvzykov.webquizengine.application

import net.ivanvzykov.webquizengine.persistence.AppUserEntity
import net.ivanvzykov.webquizengine.persistence.AppUserRepository
import net.ivanvzykov.webquizengine.persistence.CompletionOfQuizEntity
import net.ivanvzykov.webquizengine.persistence.CompletionsOfQuizRepository
import net.ivanvzykov.webquizengine.persistence.JpaQuizzesRepository
import net.ivanvzykov.webquizengine.persistence.QuizEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
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

private const val PAGE_SIZE = 10

@Service
class QuizService @Autowired constructor(
    private val userRepo: AppUserRepository,
    private val jpaQuizRepo: JpaQuizzesRepository,
    private val completionRepo: CompletionsOfQuizRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
) {
    fun addQuiz(newQuiz: NewQuiz, userDetails: UserDetails): PublicQuiz {
        val user = userRepo.findByUsername(userDetails.username)
            ?: throw UsernameNotFoundException(USERNAME_NOT_FOUND_TEMPLATE.format(userDetails.username))

        val entity = newQuiz.toEntity(user)

        return jpaQuizRepo.save(entity).toPublicQuiz()
    }

    @Transactional(readOnly = true)
    fun getQuizBy(id: Long): PublicQuiz =
        jpaQuizRepo.findWithOptionsBy(id)
            .orElseThrow { QuizNotFoundException(QUIZ_NOT_FOUND_TEMPLATE.format(id)) }
            .toPublicQuiz()

    @Transactional(readOnly = true)
    fun getAllQuizzesPaginated(pageNumber: Int): Page<PublicQuiz> {
        val pageWithMaxTenQuizzes: Pageable = PageRequest.of(
            pageNumber,
            PAGE_SIZE,
        )

        val idsPage = jpaQuizRepo.findIds(pageWithMaxTenQuizzes)
        if (idsPage.isEmpty) {
            return Page.empty(pageWithMaxTenQuizzes)
        }
        val quizzes = jpaQuizRepo.findAllWithOptionsByIdIn(idsPage.content)
            .map { it.toPublicQuiz() }

        return PageImpl(
            quizzes,
            pageWithMaxTenQuizzes,
            idsPage.totalElements
        )
    }

    @Transactional
    fun solveQuizBy(
        id: Long,
        answer: Answer,
        userDetails: UserDetails
    ): AnswerResult {
        val quizEntity = jpaQuizRepo.findByIdForUpdate(id)
            .orElseThrow { QuizNotFoundException(QUIZ_NOT_FOUND_TEMPLATE.format(id)) }
        val quiz = quizEntity.toSolvableQuiz()

        val (success, feedback) = quiz.check(answer)

        if (success) {
            saveCompletionFor(userDetails, quizEntity)
        }

        return AnswerResult(
            success = success,
            feedback = feedback,
        )
    }

    fun registerNewUser(credentials: UserCredentials) {
        val passwordEncoded = passwordEncoder.encode(credentials.password)
            ?: throw IllegalStateException("Failed to encode user's password")
        val newUser = AppUserEntity(
            username = credentials.email,
            password = passwordEncoded
        )
        try {
            userRepo.saveAndFlush(newUser)
        } catch (_: DataIntegrityViolationException) {
            throw DuplicatedUserException("User with email ${credentials.email} already exists")
        }
    }

    @Transactional
    fun deleteQuizBy(
        id: Long,
        userDetails: UserDetails
    ) {
        val quizEntity = jpaQuizRepo.findByIdForUpdate(id)
            .orElseThrow { QuizNotFoundException(QUIZ_NOT_FOUND_TEMPLATE.format(id)) }
        val quiz = quizEntity.toDeletableQuiz()

        if (userDetails.username != quiz.authorUsername) {
            throw AccessDeniedException(
                "Error. Username ${userDetails.username} doesn't math the author's username of quiz with ID ${id}."
            )
        }

        completionRepo.deleteInBulkByQuiz(quizEntity)
        jpaQuizRepo.delete(quizEntity)
    }

    @Transactional(readOnly = true)
    fun getTenCompletionsPaginatedSortedDescBy(id: Long, pageNumber: Int): Page<CompletionOfQuiz> {
        val pageWithMaxTenSortedByCompletionDesc: Pageable = PageRequest.of(
            pageNumber,
            PAGE_SIZE,
            Sort.by("completedAt").descending()
        )

        val quizEntity: QuizEntity = jpaQuizRepo.findById(id)
            .orElseThrow { QuizNotFoundException(QUIZ_NOT_FOUND_TEMPLATE.format(id)) }

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

        val pageWithMaxTen: Pageable = PageRequest.of(pageNumber, PAGE_SIZE)

        return completionRepo.findByUserOrderByCompletedAtDescIdAsc(user, pageWithMaxTen)
            .map { it.toDomain() }
    }

    private fun saveCompletionFor(
        userDetails: UserDetails,
        quizEntity: QuizEntity?
    ) {
        val user = userRepo.findByUsername(userDetails.username)
            ?: throw UsernameNotFoundException(USERNAME_NOT_FOUND_TEMPLATE.format(userDetails.username))

        val completionEntity = CompletionOfQuizEntity()
        completionEntity.quiz = quizEntity
        completionEntity.user = user
        completionEntity.completedAt = LocalDateTime.now(clock)
        completionRepo.save(completionEntity)
    }
}

private fun SolvableQuiz.check(answer: Answer) =
    if (this.answers?.toSet() == answer.value.toSet() ||
        (this.answers == null && answer.value.isEmpty())
    ) {
        true to CONGRATULATIONS
    } else {
        false to WRONG_ANSWER
    }

private fun NewQuiz.toEntity(user: AppUserEntity): QuizEntity {
    val entity = QuizEntity()
    entity.title = this.title
    entity.text = this.text
    entity.options = this.options
    entity.answers = this.answer
    entity.author = user

    return entity
}

private fun QuizEntity.toPublicQuiz() = PublicQuiz(
    id = requireNotNull(this.id) { "Error. QuizEntity.id must not be null" },
    title = requireNotNull(this.title) { "Error. QuizEntity.title must not be null" },
    text = requireNotNull(this.text) { "Error. QuizEntity.text must not be null" },
    options = requireNotNull(this.options) { "Error. QuizEntity.options must not be null" },
)

private fun QuizEntity.toSolvableQuiz() = SolvableQuiz(
    answers = this.answers
)

private fun QuizEntity.toDeletableQuiz() = DeletableQuiz(
    authorUsername = requireNotNull(this.author?.username) { "Error. QuizEntity.author must not be null" }
)

private fun CompletionOfQuizEntity.toDomain() = CompletionOfQuiz(
    id = requireNotNull(this.id) { "Error. CompletionOfQuizEntity.id must not be null" },
    quiz = requireNotNull(this.quiz) { "Error. CompletionOfQuizEntity.quiz must not be null" }.toPublicQuiz(),
    userName = requireNotNull(this.user?.username) { "Error. Error. CompletionOfQuizEntity.user.username must not be null" },
    completedAt = requireNotNull(this.completedAt) { "Error. CompletionOfQuizEntity.completedAt must not be null" },
)
