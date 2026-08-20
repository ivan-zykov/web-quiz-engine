package net.ivanvzykov.webquizengine

import net.ivanvzykov.webquizengine.application.Answer
import net.ivanvzykov.webquizengine.application.AnswerResult
import net.ivanvzykov.webquizengine.application.NewQuiz
import net.ivanvzykov.webquizengine.config.PasswordEncoderConfig
import net.ivanvzykov.webquizengine.application.QuizNotFoundException
import net.ivanvzykov.webquizengine.application.QuizService
import net.ivanvzykov.webquizengine.application.UserCredentials
import net.ivanvzykov.webquizengine.persistence.AppUserEntity
import net.ivanvzykov.webquizengine.application.AppUserAdapter
import net.ivanvzykov.webquizengine.application.DuplicatedUserException
import net.ivanvzykov.webquizengine.persistence.AppUserRepository
import net.ivanvzykov.webquizengine.persistence.CompletionOfQuizEntity
import net.ivanvzykov.webquizengine.persistence.CompletionsOfQuizRepository
import net.ivanvzykov.webquizengine.persistence.JpaQuizzesRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.check

private const val CONGRATULATIONS = "Congratulations, you're right!"
private const val WRONG_ANSWER = "Wrong answer! Please, try again."

private const val USERNAME = "test@user.com"
private const val PASSWORD = "testPass"

private const val dateTimeString = "2026-01-01T10:00:00Z"

@DataJpaTest
@Import(PasswordEncoderConfig::class)
@ActiveProfiles("test")
class QuizServiceIntegrationTest @Autowired constructor(
    private val userRepo: AppUserRepository,
    jpaQuizRepo: JpaQuizzesRepository,
    private val completionRepo: CompletionsOfQuizRepository,
    passEncoder: PasswordEncoder,
) {
    private val clockFixed: Clock = Clock.fixed(
        Instant.parse(dateTimeString),
        ZoneOffset.UTC
    ).apply {
        if (this == null) {
            throw IllegalStateException("Failed to instantiate clock in ${QuizServiceIntegrationTest::class}")
        }
    }

    private val sut = QuizService(
        userRepo,
        jpaQuizRepo,
        completionRepo,
        passEncoder,
        clockFixed
    )

    private val user = AppUserEntity(
        id = 1,
        username = USERNAME,
        password = PASSWORD
    )
    val otherUser = AppUserEntity(
        username = "other@user.com",
        password = PASSWORD
    )
    val passwordEncoded = passEncoder.encode(PASSWORD)
        ?: throw IllegalStateException("Failed to encode user's password")
    private val encodedUser = AppUserEntity(
        username = USERNAME,
        password = passwordEncoded
    )
    private val userDetails: UserDetails = AppUserAdapter(user)

    @BeforeEach
    fun setUp() {
        userRepo.save(encodedUser)
    }

    private val newQuiz1 = NewQuiz(
        title = "The Java Logo",
        text = "What is depicted on the Java logo?",
        options = listOf("Robot", "Tea leaf", "Cup of coffee", "Bug"),
        answers = listOf(2),
    )
    private val userCredentials = UserCredentials(
        email = "vanya@mail.com",
        password = "12345"
    )

    @Test
    fun `Adds a quiz`() {
        val actualQuiz = sut.addQuiz(newQuiz = newQuiz1, userDetails = userDetails)

        assertAll(
            { assertEquals(newQuiz1.title, actualQuiz.title) },
            { assertEquals(newQuiz1.text, actualQuiz.text) },
            { assertEquals(newQuiz1.options, actualQuiz.options) },
            { assertNotNull(actualQuiz.id) }
        )
    }

    @Test
    fun `Adding quiz throws when user not found`() {
        val otherUserDetails = AppUserAdapter(otherUser)

        val exception = assertThrows<RuntimeException> {
            sut.addQuiz(newQuiz = newQuiz1, userDetails = otherUserDetails)
        }
        assertEquals(
            "Error. Username ${otherUserDetails.username} not found.",
            exception.message
        )
    }

    @Test
    fun `Gets quiz by ID`() {
        val savedQuiz = sut.addQuiz(newQuiz1, userDetails)

        val fetchedQuiz = sut.getQuizBy(id = savedQuiz.id)

        assertAll(
            { assertEquals(savedQuiz.id, fetchedQuiz.id) },
            { assertEquals(savedQuiz.title, fetchedQuiz.title) },
            { assertEquals(savedQuiz.text, fetchedQuiz.text) },
            { assertEquals(savedQuiz.options, fetchedQuiz.options) },
        )
    }

    @Test
    fun `Gets page with zero quizzes`() {
        val quizzesPaginated = sut.getAllQuizzesPaginated(0)

        assertTrue(quizzesPaginated.isEmpty)
    }

    @Test
    fun `Gets page with two quizzes`() {
        val savedQuiz1 = sut.addQuiz(newQuiz = newQuiz1, userDetails = userDetails)
        val newQuiz2 = newQuiz1.copy(title = "The Java Logo 2")
        val savedQuiz2 = sut.addQuiz(newQuiz = newQuiz2, userDetails = userDetails)

        val fetchedQuizzes = sut.getAllQuizzesPaginated(0)

        assertAll(
            { assertEquals(1, fetchedQuizzes.totalPages) },
            { assertEquals(2, fetchedQuizzes.totalElements) },
            { assertEquals(savedQuiz1.id, fetchedQuizzes.content[0].id) },
            { assertEquals(savedQuiz1.title, fetchedQuizzes.content[0].title) },
            { assertEquals(savedQuiz1.text, fetchedQuizzes.content[0].text) },
            { assertEquals(savedQuiz1.options, fetchedQuizzes.content[0].options) },
            { assertEquals(savedQuiz2.id, fetchedQuizzes.content[1].id) },
            { assertEquals(savedQuiz2.title, fetchedQuizzes.content[1].title) },
            { assertEquals(savedQuiz2.text, fetchedQuizzes.content[1].text) },
            { assertEquals(savedQuiz2.options, fetchedQuizzes.content[1].options) },
        )
    }

    @Test
    fun `Solves quiz by ID with correct answer`() {
        val addedQuiz = sut.addQuiz(newQuiz = newQuiz1, userDetails = userDetails)

        val answerResult = sut.solveQuizBy(
            id = addedQuiz.id,
            answer = Answer(listOf(2)),
            userDetails = userDetails
        )
        val completion: CompletionOfQuizEntity = completionRepo.findAll().first()

        assertAll(
            { assertEquals(AnswerResult(success = true, feedback = CONGRATULATIONS), answerResult) },
            { assertNotNull(completion.id) },
            { assertEquals(addedQuiz.title, completion.quiz?.title) },
            {
                assertEquals(
                    LocalDateTime.ofInstant(Instant.parse(dateTimeString), ZoneOffset.UTC),
                    completion.completedAt
                )
            }
        )
    }

    @Test
    fun `Solves quiz by ID with wrong answer`() {
        val addedQuizId = sut.addQuiz(newQuiz = newQuiz1, userDetails = userDetails).id

        val actual = sut.solveQuizBy(
            id = addedQuizId,
            answer = Answer(listOf(0, 1)),
            userDetails = userDetails
        )
        val completions = completionRepo.findAll()

        assertAll(
            { assertEquals(AnswerResult(success = false, feedback = WRONG_ANSWER), actual) },
            { assertTrue(completions.isEmpty()) }
        )
    }

    @Test
    fun `Solves quiz with empty answers and empty provided answer`() {
        val quizWithNullAnswer = newQuiz1.copy(answers = emptyList())
        val addedQuizId = sut.addQuiz(newQuiz = quizWithNullAnswer, userDetails = userDetails).id

        val actual = sut.solveQuizBy(
            id = addedQuizId,
            answer = Answer(listOf()),
            userDetails = userDetails
        )

        assertAll(
            { assertEquals(true, actual.success) },
            { assertEquals(CONGRATULATIONS, actual.feedback) }
        )
    }

    @Test
    fun `Solving non-existing quiz throws`() {
        val quizId = 1L

        val exception = assertThrows<QuizNotFoundException> {
            sut.solveQuizBy(id = quizId, answer = Answer(listOf()), userDetails = userDetails)
        }
        assertEquals("Error. Quiz with ID $quizId not found.", exception.message)
    }

    @Test
    fun `Solving quiz with user not found throws`() {
        val addedQuizId = sut.addQuiz(newQuiz = newQuiz1, userDetails = userDetails).id
        val otherUserDetails = AppUserAdapter(otherUser)

        val exception = assertThrows<UsernameNotFoundException> {
            sut.solveQuizBy(
                id = addedQuizId,
                answer = Answer(listOf(2)),
                userDetails = otherUserDetails
            )
        }
        assertEquals(
            "Error. Username ${otherUserDetails.username} not found.",
            exception.message
        )
    }

    @Test
    fun `Registers new user`() {
        assertDoesNotThrow {
            sut.registerNewUser(userCredentials)
        }
    }

    @Test
    fun `Registering duplicate new user throws`() {
        sut.registerNewUser(userCredentials)

        val exception = assertThrows<DuplicatedUserException> {
            sut.registerNewUser(userCredentials)
        }
        assertEquals(
            "User with email ${userCredentials.email} already exists",
            exception.message
        )
    }

    @Test
    fun `Deletes quiz of the same author`() {
        val quiz = sut.addQuiz(newQuiz1, userDetails)
        val answer = sut.solveQuizBy(id = quiz.id, answer = Answer(listOf(2)), userDetails = userDetails)
        assertTrue(answer.success)

        sut.deleteQuizBy(id = quiz.id, userDetails = userDetails)

        assertAll(
            { assertTrue(sut.getAllQuizzesPaginated(0).isEmpty) },
            { assertTrue(completionRepo.findAll().isEmpty()) }
        )
    }

    @Test
    fun `Deleting quiz of different author throws`() {
        sut.addQuiz(newQuiz1, userDetails)
        val quiz = sut.getAllQuizzesPaginated(0).first()
        val otherUserDetails = AppUserAdapter(otherUser)

        val exception = assertThrows<AccessDeniedException> {
            sut.deleteQuizBy(quiz.id, otherUserDetails)
        }
        assertEquals(
            "Error. Username ${otherUser.username} doesn't math the author's username of quiz with ID ${quiz.id}.",
            exception.message
        )
    }

    @Test
    fun `Getting completions by non-existing quiz throws`() {
        assertThrows<QuizNotFoundException> {
            sut.getTenCompletionsPaginatedSortedDescBy(99L, 1)
        }
    }

    @Test
    fun `Gets no completions by existing quiz without completions`() {
        val quiz = sut.addQuiz(newQuiz1, userDetails)

        val completions = sut.getTenCompletionsPaginatedSortedDescBy(quiz.id, 1)

        assertTrue(completions.isEmpty)
    }

    @Test
    fun `Gets two completions by quiz`() {
        val quiz = sut.addQuiz(newQuiz1, userDetails)
        val correctAnswer = Answer(listOf(2))
        val result1 = sut.solveQuizBy(quiz.id, answer = correctAnswer, userDetails)
        check(result1.success)
        val result2 = sut.solveQuizBy(quiz.id, answer = correctAnswer, userDetails)
        check(result2.success)

        val completions = sut.getTenCompletionsPaginatedSortedDescBy(quiz.id, 0)

        assertAll(
            { assertEquals(10, completions.size) },
            { assertEquals(1, completions.totalPages) },
            { assertEquals(2, completions.totalElements) },
            { assertEquals(quiz.id, completions.content[0].quizId) },
            { assertEquals(quiz.id, completions.content[1].quizId) }
        )
    }

    @Test
    fun `Getting completions by user not found throws`() {
        val otherUserDetails = AppUserAdapter(otherUser)

        val exception = assertThrows<UsernameNotFoundException> {
            sut.getAllCompletionsPaginatedSortedByCompletedAtDescBy(otherUserDetails, 0)
        }

        assertEquals(
            "Error. Username ${otherUserDetails.username} not found.",
            exception.message
        )
    }

    @Test
    fun `Gets zero completions for existing user without completions`() {
        val completions = sut.getAllCompletionsPaginatedSortedByCompletedAtDescBy(userDetails, 0)

        assertTrue(completions.isEmpty)
    }

    @Test
    fun `Gets paginated completions for user`() {
        val newQuiz2 = newQuiz1.copy(title = "The Java Logo 2")
        val quiz1 = sut.addQuiz(newQuiz1, userDetails)
        val quiz2 = sut.addQuiz(newQuiz2, userDetails)
        val otherEncodedUser = AppUserEntity(
            username = otherUser.username,
            password = "encoded-${otherUser.password}"
        )
        userRepo.save(otherEncodedUser)
        val otherUserDetails = AppUserAdapter(otherUser)
        val correctAnswer = Answer(listOf(2))

        repeat(6) {
            val result = sut.solveQuizBy(quiz1.id, answer = correctAnswer, userDetails)
            check(result.success)
        }
        repeat(5) {
            val result = sut.solveQuizBy(quiz2.id, answer = correctAnswer, userDetails)
            check(result.success)
        }
        repeat(2) {
            val result = sut.solveQuizBy(quiz1.id, answer = correctAnswer, otherUserDetails)
            check(result.success)
        }

        val completionsPage0 = sut.getAllCompletionsPaginatedSortedByCompletedAtDescBy(userDetails, 0)
        val completionsPage1 = sut.getAllCompletionsPaginatedSortedByCompletedAtDescBy(userDetails, 1)
        val expectedCompletedAt = LocalDateTime.ofInstant(Instant.parse(dateTimeString), ZoneOffset.UTC)
        val expectedPage0QuizIds = listOf(
            quiz1.id,
            quiz1.id,
            quiz1.id,
            quiz1.id,
            quiz1.id,
            quiz1.id,
            quiz2.id,
            quiz2.id,
            quiz2.id,
            quiz2.id,
        )
        val expectedPage1QuizIds = listOf(quiz2.id)

        assertAll(
            { assertEquals(2, completionsPage0.totalPages) },
            { assertEquals(11, completionsPage0.totalElements) },
            { assertEquals(10, completionsPage0.size) },
            { assertEquals(10, completionsPage0.content.size) },
            { assertEquals(2, completionsPage1.totalPages) },
            { assertEquals(11, completionsPage1.totalElements) },
            { assertEquals(10, completionsPage1.size) },
            { assertEquals(1, completionsPage1.content.size) },
            { assertEquals(expectedPage0QuizIds, completionsPage0.content.map { it.quizId }) },
            { assertEquals(expectedPage1QuizIds, completionsPage1.content.map { it.quizId }) },
            {
                assertTrue(
                    completionsPage0.content.all {
                        it.completedAt == expectedCompletedAt
                    }
                )
            },
            {
                assertTrue(
                    completionsPage1.content.all {
                        it.completedAt == expectedCompletedAt
                    }
                )
            }
        )
    }
}
