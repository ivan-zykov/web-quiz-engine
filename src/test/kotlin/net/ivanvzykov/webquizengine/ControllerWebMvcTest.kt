package net.ivanvzykov.webquizengine

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import net.ivanvzykov.webquizengine.application.CompletionOfQuiz
import net.ivanvzykov.webquizengine.application.DuplicatedUserException
import net.ivanvzykov.webquizengine.application.Quiz
import net.ivanvzykov.webquizengine.application.QuizId
import net.ivanvzykov.webquizengine.application.QuizNotFoundException
import net.ivanvzykov.webquizengine.application.QuizService
import net.ivanvzykov.webquizengine.config.SecurityConfig
import net.ivanvzykov.webquizengine.presentation.AnswerDto
import net.ivanvzykov.webquizengine.presentation.QuizEngineController
import net.ivanvzykov.webquizengine.presentation.UserCredentialsDTO
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

private const val API_PATH = "/api"

@WebMvcTest(QuizEngineController::class)
@Import(SecurityConfig::class)
@WithMockUser
@ActiveProfiles("test")
class ControllerWebMvcTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) {
    @MockkBean
    private lateinit var quizService: QuizService

    @Test
    fun `Getting quiz by id returns Not found for non-existing id`() {
        val quizId = 0L
        every { quizService.getQuizBy(QuizId(quizId)) }
            .throws(QuizNotFoundException("Error. Quiz with ID $quizId not found."))

        mockMvc.get("$API_PATH/quizzes/$quizId")
            .andExpectAll {
                status { isNotFound() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.error") {
                    value("Error. Quiz with ID $quizId not found.")
                }
            }
    }

    @Test
    fun `Getting all quizzes returns page with empty content`() {
        val mockedEmptyPage: Page<Quiz> = Page.empty(PageRequest.of(0, 10))
        every { quizService.getAllQuizzesPaginated(0) }
            .returns(mockedEmptyPage)

        mockMvc.get("$API_PATH/quizzes")
            .andExpectAll {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.number") { value(0) }
                jsonPath("$.size") { value(10) }
                jsonPath("$.totalPages") { value(0) }
                jsonPath("$.totalElements") { value(0) }
                jsonPath("$.first") { value(true) }
                jsonPath("$.last") { value(true) }
                jsonPath("$.content") { isArray() }
                jsonPath("$.content") { isEmpty() }
            }
    }

    @Test
    fun `Getting completed quizzes returns page with empty content`() {
        val mockedEmptyPage: Page<CompletionOfQuiz> = Page.empty(PageRequest.of(0, 10))
        every {
            quizService.getAllCompletionsPaginatedSortedByCompletedAtDescBy(any(), 0)
        }.returns(mockedEmptyPage)

        mockMvc.get("$API_PATH/quizzes/completed")
            .andExpectAll {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.number") { value(0) }
                jsonPath("$.size") { value(10) }
                jsonPath("$.totalPages") { value(0) }
                jsonPath("$.totalElements") { value(0) }
                jsonPath("$.first") { value(true) }
                jsonPath("$.last") { value(true) }
                jsonPath("$.content") { isArray() }
                jsonPath("$.content") { isEmpty() }
            }
    }

    @Test
    @WithAnonymousUser
    fun `Getting completed quizzes returns Unauthorized for anonymous user`() {
        mockMvc.get("$API_PATH/quizzes/completed")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `Solving quiz by ID returns Not found for non-existing quiz`() {
        val idOfNonExistingQuiz = 1
        val answer = mapper.writeValueAsString(AnswerDto(listOf()))
        every { quizService.solveQuizBy(any(), any(), any()) }
            .throws(QuizNotFoundException("Error. Quiz with ID $idOfNonExistingQuiz not found."))

        mockMvc.post("$API_PATH/quizzes/{id}/solve", idOfNonExistingQuiz) {
            contentType = MediaType.APPLICATION_JSON
            content = answer
        }.andExpectAll {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.error") {
                value("Error. Quiz with ID $idOfNonExistingQuiz not found.")
            }
        }
    }

    @Test
    fun `Deleting quiz by ID returns Forbidden for user different than author`() {
        val quizId = 1
        every { quizService.deleteQuizBy(any(), any()) }
            .throws(AccessDeniedException("Error. Username doesn't math the author's username of quiz with ID $quizId."))

        mockMvc.delete("$API_PATH/quizzes/{id}", quizId)
            .andExpectAll {
                status { isForbidden() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.error") {
                    value("Error. Username doesn't math the author's username of quiz with ID $quizId.")
                }
            }
    }

    @Test
    @WithAnonymousUser
    fun `Registering duplicate new user returns Bad request`() {
        every { quizService.registerNewUser(any()) }
            .throws(DuplicatedUserException("User with this email already exists"))
        val userCredentials = UserCredentialsDTO(
            email = "vanya@mail.com",
            password = "12345"
        )

        mockMvc.post("$API_PATH/register") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(userCredentials)
        }
            .andExpect {
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.error") {
                    value("User with this email already exists")
                }
            }
    }
}
