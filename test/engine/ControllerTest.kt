package engine

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

private const val API_PATH = "/api"

private const val TITLE = "test title"
private const val TEXT = "test text"
private const val OPTION = "test option"

private const val CONGRATULATIONS = "Congratulations, you're right!"

private const val USERNAME = "test@user.com"
private const val PASSWORD = "testPass"
private const val OTHER_USERNAME = "other@user.com"
private const val OTHER_PASSWORD = "otherPass"

private val quiz = QuizInDto(
    title = TITLE,
    text = TEXT,
    options = listOf(OPTION, OPTION),
    answer = listOf(0),
)

private val userCredentials = UserCredentialsDTO(
    email = "vanya@mail.com",
    password = "12345"
)

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfig::class)
@ActiveProfiles("test")
class ControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val quizzesRepository: JpaQuizzesRepository,
    private val completionsRepo: CompletionsOfQuizRepository,
    private val mapper: ObjectMapper,
    private val userRepo: AppUserRepository,
    private val passEncoder: PasswordEncoder,
) {
    private val quizSerialized1: String = mapper.writeValueAsString(quiz)

    @BeforeEach
    fun reset() {
        completionsRepo.deleteAll()
        quizzesRepository.deleteAll()

        userRepo.deleteAll()
        val userId = 0
        if (!userRepo.existsById(userId)) {
            val user = AppUser(
                id = userId,
                username = USERNAME,
                password = passEncoder.encode(PASSWORD)
            )
            userRepo.save(user)
        }
    }

    @Test
    fun `Adding quiz returns OK with created quiz`() {
        mockMvc.post("$API_PATH/quizzes") {
            contentType = MediaType.APPLICATION_JSON
            content = quizSerialized1
            with(httpBasic(USERNAME, PASSWORD))
        }
            .andExpectAll {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.id") { isNumber() }
                jsonPath("$.title") { TITLE }
                jsonPath("$.text") { TEXT }
                jsonPath("$.options[0]") { OPTION }
                jsonPath("$.answer") { doesNotExist() }
            }
    }

    @TestFactory
    fun `Adding quiz returns Bad request when validation fails for title field in request's body`() = buildList {
        add(
            Triple(
                "Body missing",
                "",
                "body"
            )
        )

        val bodyTitleMissing = mapper.createObjectNode()
            .toString()
        add(
            Triple(
                "Title field missing",
                bodyTitleMissing,
                "title"
            )
        )

        val bodyTitleBlank = mapper.createObjectNode()
            .put("title", "")
            .put("text", TEXT)
        bodyTitleBlank
            .putArray("options")
            .add(OPTION)
            .add(OPTION)
        add(
            Triple(
                "Title field blank",
                bodyTitleBlank.toString(),
                "title"
            )
        )
    }.map { (displayName, body, errorMessageSubstring) ->
        dynamicTest(displayName) {
            mockMvc.post("$API_PATH/quizzes") {
                contentType = MediaType.APPLICATION_JSON
                content = body
                with(httpBasic(USERNAME, PASSWORD))
            }
                .andExpectAll {
                    status { isBadRequest() }
                    jsonPath("$.error") { value(containsString(errorMessageSubstring)) }
                }
        }
    }

    @Test
    fun `Getting quiz by id returns OK with one quiz`() {
        val addedQuizId = addQuiz(quizSerialized1).id.value

        mockMvc.get("$API_PATH/quizzes/${addedQuizId}") {
            with(httpBasic(USERNAME, PASSWORD))
        }
            .andExpectAll {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.id") { isNumber() }
                jsonPath("$.title") { value(TITLE) }
                jsonPath("$.text") { value(TEXT) }
                jsonPath("$.options[0]") { value(OPTION) }
                jsonPath("$.answer") { doesNotExist() }
            }
    }

    @Test
    fun `Getting quiz by id returns Not found for non-existing id`() {
        val quizId = 0

        mockMvc.get("$API_PATH/quizzes/$quizId") {
            with(httpBasic(USERNAME, PASSWORD))
        }
            .andExpectAll {
                status { isNotFound() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.error") { value(containsString("Error")) }
                jsonPath("$.error") { value(containsString(quizId.toString())) }
            }
    }

    @Test
    fun `Getting all quizzes returns page with empty content`() {
        mockMvc.get("$API_PATH/quizzes") {
            with(httpBasic(USERNAME, PASSWORD))
        }
            .andExpectAll {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.totalPages") { value(0) }
                jsonPath("$.totalElements") { value(0) }
                jsonPath("$.content") { isArray() }
                jsonPath("$.content") { isEmpty() }
            }
    }

    @Test
    fun `Getting all quizzes returns page with two`() {
        addQuiz(quizSerialized1)
        val quizSerialized2 = mapper.writeValueAsString(quiz.copy(title = "$TITLE 2"))
        addQuiz(quizSerialized2)

        mockMvc.get("$API_PATH/quizzes") {
            with(httpBasic(USERNAME, PASSWORD))
        }
            .andExpectAll {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.totalPages") { value(1) }
                jsonPath("$.totalElements") { value(2) }
                jsonPath("$.content[0].id") { isNumber() }
                jsonPath("$.content[0].title") { value(quiz.title) }
                jsonPath("$.content[0].text") { value(TEXT) }
                jsonPath("$.content[0].options[0]") { value(OPTION) }
                jsonPath("$.content[1].id") { isNumber() }
                jsonPath("$.content[1].title") { containsString(TITLE) }
                jsonPath("$.content[1].text") { value(TEXT) }
                jsonPath("$.content[1].options[0]") { value(OPTION) }
            }
    }

    @Test
    fun `Solving quiz by ID returns OK`() {
        val addedQuiz = addQuiz(quizSerialized1)
        val idOfAddedQuiz = addedQuiz.id.value
        val answer = mapper.writeValueAsString(AnswerDto(listOf(0)))
            ?: fail { "Failed to serialize answer" }

        mockMvc.post("$API_PATH/quizzes/{id}/solve", idOfAddedQuiz) {
            contentType = MediaType.APPLICATION_JSON
            content = answer
            with(httpBasic(USERNAME, PASSWORD))
        }
            .andExpectAll {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.success") { value(true) }
                jsonPath("$.feedback") { value(CONGRATULATIONS) }
            }
    }

    @Test
    fun `Solving quiz by ID returns Not found for non-existing quiz`() {
        val idOfNonExistingQuiz = 1
        val answer = mapper.writeValueAsString(AnswerDto(listOf()))

        mockMvc.post("$API_PATH/quizzes/{id}/solve", idOfNonExistingQuiz) {
            contentType = MediaType.APPLICATION_JSON
            content = answer
            with(httpBasic(USERNAME, PASSWORD))
        }.andExpectAll {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.error") { value(containsString("Error")) }
            jsonPath("$.error") { value(containsString(idOfNonExistingQuiz.toString())) }
        }
    }

    @Test
    fun `Deleting quiz by ID returns No content for same user as author`() {
        val addedQuiz = addQuiz(quizSerialized1)

        mockMvc.delete("$API_PATH/quizzes/{id}", addedQuiz.id.value) {
            with(httpBasic(USERNAME, PASSWORD))
        }
            .andExpect {
                status { isNoContent() }
            }
    }

    @Test
    fun `Deleting quiz by ID returns Forbidden for user different than author`() {
        val addedQuiz = addQuiz(quizSerialized1)
        userRepo.save(
            AppUser(
                username = OTHER_USERNAME,
                password = passEncoder.encode(OTHER_PASSWORD)
            )
        )

        mockMvc.delete("$API_PATH/quizzes/{id}", addedQuiz.id.value) {
            with(httpBasic(OTHER_USERNAME, OTHER_PASSWORD))
        }
            .andExpectAll {
                status { isForbidden() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.error") {
                    value("Error. Username $OTHER_USERNAME doesn't math the author's username of quiz with ID ${addedQuiz.id.value}.")
                }
            }
    }

    @Test
    fun `Registering new user returns OK`() {
        mockMvc.post("$API_PATH/register") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(userCredentials)
        }
            .andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `Registering duplicate new user returns Bad request`() {
        mockMvc.post("$API_PATH/register") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(userCredentials)
        }

        mockMvc.post("$API_PATH/register") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(userCredentials)
        }
            .andExpect {
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.error") { value("User with email ${userCredentials.email} already exists") }
            }
    }

    private fun addQuiz(quizSerialized: String): QuizOutDto {
        val result = mockMvc.post("$API_PATH/quizzes") {
            contentType = MediaType.APPLICATION_JSON
            content = quizSerialized
            with(httpBasic(USERNAME, PASSWORD))
        }
            .andReturn()

        val createdQuiz: QuizOutDto? = mapper.readValue(result.response.contentAsByteArray)
        checkNotNull(createdQuiz) { "Error. Failed to add quiz $quizSerialized" }

        return createdQuiz
    }

}
