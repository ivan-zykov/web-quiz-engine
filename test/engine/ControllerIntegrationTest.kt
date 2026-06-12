package engine

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import kotlin.apply

private const val API_PATH = "/api"

private const val TITLE = "test title"
private const val TEXT = "test text"
private const val OPTION = "test option"

private const val CONGRATULATIONS = "Congratulations, you're right!"

private const val USERNAME = "test@user.com"
private const val PASSWORD = "testPass"

var options = listOf(OPTION, OPTION)
private val quiz = QuizInDto(
    title = TITLE,
    text = TEXT,
    options = options,
    answer = listOf(0),
)

private val userCredentials = UserCredentialsDTO(
    email = "vanya@mail.com",
    password = "12345"
)

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(SecurityConfig::class)
@ActiveProfiles("test")
class ControllerIntegrationTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val quizzesRepository: JpaQuizzesRepository,
    private val completionsRepo: CompletionsOfQuizRepository,
    private val mapper: ObjectMapper,
    private val userRepo: AppUserRepository,
    private val passEncoder: PasswordEncoder,
) {
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
        val headers = HttpHeaders().apply {
            this.contentType = MediaType.APPLICATION_JSON
            this.setBasicAuth(USERNAME, PASSWORD)
        }
        val request = HttpEntity(quiz, headers)

        val response = restTemplate.postForEntity<QuizOutDto>("$API_PATH/quizzes", request)

        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON) },
            { assertThat(response.body?.id).isNotNull },
            { assertThat(response.body?.title).isEqualTo(quiz.title) },
            { assertThat(response.body?.text).isEqualTo(quiz.text) },
            { assertThat(response.body?.options).isEqualTo(quiz.options) },
        )
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
            val headers = HttpHeaders().apply {
                this.contentType = MediaType.APPLICATION_JSON
                this.setBasicAuth(USERNAME, PASSWORD)
            }
            val request = HttpEntity(body, headers)
            val response = restTemplate.postForEntity<Map<String, String>>(
                "$API_PATH/quizzes",
                request
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.get("error")).contains(errorMessageSubstring) }
            )
        }
    }

    @Test
    fun `Getting quiz by id returns OK with one quiz`() {
        val addedQuizId = addQuizNew(quiz).id.value
        val headers = HttpHeaders().apply {
            this.setBasicAuth(USERNAME, PASSWORD)
        }
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "$API_PATH/quizzes/${addedQuizId}",
            HttpMethod.GET,
            request,
            object : ParameterizedTypeReference<Map<String, Any>>() {}
        )

        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON) },
            { assertThat(response.body).containsEntry("id", addedQuizId.toInt()) },
            { assertThat(response.body).containsEntry("title", TITLE) },
            { assertThat(response.body).containsEntry("text", TEXT) },
            { assertThat(response.body).containsEntry("options", options) },
            { assertThat(response.body).doesNotContainKey("answer") }
        )
    }

    @Test
    fun `Getting all quizzes returns page with two`() {
        addQuizNew(quiz)
        val title2 = "$TITLE 2"
        addQuizNew(quiz.copy(title = title2))
        val headers = HttpHeaders().apply {
            this.setBasicAuth(USERNAME, PASSWORD)
        }
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "$API_PATH/quizzes",
            HttpMethod.GET,
            request,
            object : ParameterizedTypeReference<PageResponse<QuizOutDto>>() {}
        )

        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON) },
            { assertThat(response.body?.totalPages).isEqualTo(1) },
            { assertThat(response.body?.totalElements).isEqualTo(2) },
            { assertThat(response.body?.content[0]?.id).isNotNull },
            { assertThat(response.body?.content[1]?.id).isNotNull },
            { assertThat(response.body?.content[0]?.title).isEqualTo(TITLE) },
            { assertThat(response.body?.content[1]?.title).isEqualTo(title2) },
            { assertThat(response.body?.content[0]?.text).isEqualTo(TEXT) },
            { assertThat(response.body?.content[1]?.text).isEqualTo(TEXT) },
            { assertThat(response.body?.content[0]?.options).isEqualTo(options) },
            { assertThat(response.body?.content[1]?.options).isEqualTo(options) }
        )
    }

    @Test
    fun `Solving quiz by ID returns OK`() {
        val idOfAddedQuiz = addQuizNew(quiz).id.value
        val headers = HttpHeaders().apply {
            this.contentType = MediaType.APPLICATION_JSON
            this.setBasicAuth(USERNAME, PASSWORD)
        }
        val answer = AnswerDto(listOf(0))
        val request = HttpEntity(answer, headers)

        val response = restTemplate.postForEntity<ResultDto>(
            "$API_PATH/quizzes/$idOfAddedQuiz/solve",
            request
        )

        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON) },
            { assertThat(response.body?.success).isTrue },
            { assertThat(response.body?.feedback).isEqualTo(CONGRATULATIONS) }
        )
    }

    @Test
    fun `Deleting quiz by ID returns No content for same user as author`() {
        val idOfAddedQuiz = addQuizNew(quiz).id.value
        val headers = HttpHeaders().apply {
            this.setBasicAuth(USERNAME, PASSWORD)
        }
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "$API_PATH/quizzes/$idOfAddedQuiz",
            HttpMethod.DELETE,
            request,
            object : ParameterizedTypeReference<Void>() {}
        )

        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT) },
            { assertThat(quizzesRepository.findById(idOfAddedQuiz).isEmpty) }
        )
    }

    @Test
    fun `Registering new user returns OK`() {
        val headers = HttpHeaders().apply {
            this.contentType = MediaType.APPLICATION_JSON
        }
        val request = HttpEntity(userCredentials, headers)

        val response = restTemplate.postForEntity<Void>(
            "$API_PATH/register",
            request
        )

        val registeredUser = userRepo.findByUsername(userCredentials.email)
        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(registeredUser?.username).isEqualTo(userCredentials.email) },
            {
                assertThat(
                    passEncoder.matches(
                        userCredentials.password,
                        registeredUser?.password
                    )
                ).isTrue
            },
        )
    }

    private fun addQuizNew(quizInDto: QuizInDto): QuizOutDto {
        val headers = HttpHeaders().apply {
            this.contentType = MediaType.APPLICATION_JSON
            this.setBasicAuth(USERNAME, PASSWORD)
        }
        val request = HttpEntity(quizInDto, headers)
        val response = restTemplate.postForEntity<QuizOutDto>(
            "$API_PATH/quizzes",
            request
        )
        val addedQuiz = response.body ?: fail { "Error. Failed to add quiz $quizInDto" }

        return addedQuiz
    }

    private class PageResponse<T>(
        val content: List<T>,
        val totalPages: Int,
        val totalElements: Long,
    )
}
