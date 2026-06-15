package net.ivanvzykov.webquizengine

import com.ninjasquad.springmockk.MockkSpyBean
import io.mockk.every
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

private const val USERNAME = "test@user.com"
private const val PASSWORD = "testPass"

@DataJpaTest
@Import(QuizService::class, PasswordEncoderConfig::class, ClockConfig::class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class QuizServiceTransactionTest @Autowired constructor(
    private val sut: QuizService,
    private val userRepo: AppUserRepository,
    private val quizRepo: JpaQuizzesRepository,
    private val completionRepo: CompletionsOfQuizRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @MockkSpyBean
    private lateinit var spyQuizRepo: JpaQuizzesRepository

    private val newQuiz = NewQuiz(
        title = "The Java Logo",
        text = "What is depicted on the Java logo?",
        options = listOf("Robot", "Tea leaf", "Cup of coffee", "Bug"),
        answer = listOf(2),
    )

    private val userDetails: UserDetails = AppUserAdapter(
        AppUser(
            username = USERNAME,
            password = PASSWORD,
        )
    )

    @BeforeEach
    fun setUp() {
        if (userRepo.findByUsername(USERNAME) == null) {
            userRepo.save(
                AppUser(
                    username = USERNAME,
                    password = passwordEncoder.encode(PASSWORD),
                )
            )
        }
    }

    @AfterEach
    fun cleanUp() {
        completionRepo.deleteAll()
        quizRepo.deleteAll()
        userRepo.deleteAll()
    }

    @Test
    fun `Deleting quiz rolls back when final delete fails`() {
        val quiz = sut.addQuiz(newQuiz = newQuiz, userDetails = userDetails)
        val result = sut.solveQuizBy(id = quiz.id, answer = Answer(listOf(2)), userDetails = userDetails)
        check(result.success) { "Failed to solve quiz in test" }

        every { spyQuizRepo.deleteById(any()) } throws IllegalStateException("Trigger transaction rollback")

        assertThrows<IllegalStateException> {
            sut.deleteQuizBy(id = quiz.id, userDetails = userDetails)
        }

        val persistedQuiz = quizRepo.findById(quiz.id.value.toLong())

        assertTrue(persistedQuiz.isPresent)
        assertEquals(1, completionRepo.findAll().size)
    }
}
