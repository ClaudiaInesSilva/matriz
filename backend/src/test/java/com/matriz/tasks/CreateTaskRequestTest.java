package com.matriz.tasks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CreateTaskRequestTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsTitleAtTheValidLengthBoundaries() {
        assertValid("abc");
        assertValid("x".repeat(150));
    }

    @Test
    void rejectsTitleBelowAndAboveTheLengthBoundaries() {
        assertInvalid("ab");
        assertInvalid("x".repeat(151));
    }

    @Test
    void rejectsEmptyAndWhitespaceOnlyTitles() {
        assertInvalid("");
        assertInvalid("   ");
        assertInvalid("\t");
    }

    @Test
    void acceptsTitleWithSurroundingWhitespaceThatStripsToThreeCharacters() {
        CreateTaskRequest request = new CreateTaskRequest("  abc  ", null, null, null);

        assertEquals("abc", request.title());

        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);
        assertFalse(titleViolation(violations).isPresent());
    }

    private static void assertValid(String title) {
        CreateTaskRequest request = new CreateTaskRequest(title, null, null, null);
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertFalse(titleViolation(violations).isPresent());
    }

    private static void assertInvalid(String title) {
        CreateTaskRequest request = new CreateTaskRequest(title, null, null, null);
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertTrue(titleViolation(violations).isPresent());
    }

    private static java.util.Optional<ConstraintViolation<CreateTaskRequest>> titleViolation(
            Set<ConstraintViolation<CreateTaskRequest>> violations) {
        return violations.stream()
                .filter(violation -> violation.getPropertyPath().toString().equals("title"))
                .findFirst();
    }
}
