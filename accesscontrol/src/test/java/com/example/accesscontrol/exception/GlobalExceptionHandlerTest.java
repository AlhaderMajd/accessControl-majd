package com.example.accesscontrol.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    private static void assertCommonBody(Map<String, Object> body, HttpStatus expectedStatus) {
        assertNotNull(body, "Response body should not be null");
        assertTrue(body.containsKey("timestamp"), "Body should contain 'timestamp'");
        assertTrue(body.containsKey("status"), "Body should contain 'status'");
        assertTrue(body.containsKey("error"), "Body should contain 'error'");
        assertEquals(expectedStatus.value(), body.get("status"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    @DisplayName("handleUserNotFound returns 404 with message")
    void handleUserNotFound() {
        UserNotFoundException ex = new UserNotFoundException("someone@example.com");
        ResponseEntity<Map<String, Object>> response = handler.handleUserNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertCommonBody(response.getBody(), HttpStatus.NOT_FOUND);
        assertEquals(ex.getMessage(), response.getBody().get("error"));
    }

    @Test
    @DisplayName("handleInvalidCredentials returns 401 with message")
    void handleInvalidCredentials() {
        InvalidCredentialsException ex = new InvalidCredentialsException();
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertCommonBody(response.getBody(), HttpStatus.UNAUTHORIZED);
        assertEquals(ex.getMessage(), response.getBody().get("error"));
    }

    @Test
    @DisplayName("handleUserDisabled returns 403 with message")
    void handleUserDisabled() {
        UserDisabledException ex = new UserDisabledException();
        ResponseEntity<Map<String, Object>> response = handler.handleUserDisabled(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertCommonBody(response.getBody(), HttpStatus.FORBIDDEN);
        assertEquals(ex.getMessage(), response.getBody().get("error"));
    }

    @Nested
    class DatabaseIssues {
        @Test
        @DisplayName("handleDatabaseConnectionIssue returns 503 for CannotCreateTransactionException")
        void handleDatabaseIssue_cannotCreateTx() {
            Exception ex = new CannotCreateTransactionException("db down", null);
            ResponseEntity<Map<String, Object>> response = handler.handleDatabaseConnectionIssue(ex);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertCommonBody(response.getBody(), HttpStatus.SERVICE_UNAVAILABLE);
            assertEquals("Database connection failed. Please try again later.", response.getBody().get("error"));
        }

        @Test
        @DisplayName("handleDatabaseConnectionIssue returns 503 for DataAccessResourceFailureException")
        void handleDatabaseIssue_dataAccessFailure() {
            Exception ex = new DataAccessResourceFailureException("db pool exhausted");
            ResponseEntity<Map<String, Object>> response = handler.handleDatabaseConnectionIssue(ex);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertCommonBody(response.getBody(), HttpStatus.SERVICE_UNAVAILABLE);
            assertEquals("Database connection failed. Please try again later.", response.getBody().get("error"));
        }
    }

    @Test
    @DisplayName("handleEmailAlreadyUsed returns 409 with message")
    void handleEmailAlreadyUsed() {
        EmailAlreadyUsedException ex = new EmailAlreadyUsedException("Email already in use");
        ResponseEntity<Map<String, Object>> response = handler.handleEmailAlreadyUsed(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertCommonBody(response.getBody(), HttpStatus.CONFLICT);
        assertEquals(ex.getMessage(), response.getBody().get("error"));
    }

    @Test
    @DisplayName("handleIllegalArgument returns 400 with message")
    void handleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad input");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertCommonBody(response.getBody(), HttpStatus.BAD_REQUEST);
        assertEquals(ex.getMessage(), response.getBody().get("error"));
    }

    @Test
    @DisplayName("handleOtherExceptions returns 500 with prefixed message")
    void handleOtherExceptions() {
        Exception ex = new Exception("Boom");
        ResponseEntity<Map<String, Object>> response = handler.handleOtherExceptions(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertCommonBody(response.getBody(), HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals("Something went wrong: " + ex.getMessage(), response.getBody().get("error"));
    }
}
