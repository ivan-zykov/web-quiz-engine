package net.ivanvzykov.webquizengine

import jakarta.validation.Validation
import jakarta.validation.Validator
import net.ivanvzykov.webquizengine.presentation.QuizInDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class QuizInDtoTest {
    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `Validation for incoming quiz DTO with blank title and text and just one option fails`() {
        val sut = QuizInDto(
            title = "",
            text = "",
            options = listOf("test option"),
            answer = null,
        )

        val violations = validator.validate(sut)
        val invalidFields = violations.map { it.propertyPath.toString() }.toSet()
        val errorMessages = violations.mapNotNull { it.message }.toSet()

        assertAll(
            {
                assertEquals(
                    setOf("title", "text", "options"),
                    invalidFields
                )
            },
            {
                assertEquals(
                    setOf(
                        "Field title must be not blank",
                        "Field text must be not blank",
                        "Field options should have at least two elements"
                    ),
                    errorMessages
                )
            }
        )
    }
}