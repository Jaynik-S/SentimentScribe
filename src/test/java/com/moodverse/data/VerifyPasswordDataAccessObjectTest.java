package com.moodverse.data;

import com.moodverse.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VerifyPasswordDataAccessObjectTest {

    @BeforeEach
    public void setUp() {
        VerifyPasswordDataAccessObject.setSysPasswordForTesting(null);
    }

    @Test
    public void testSavingNewPasswordCreatesEntryInEnv() throws Exception {
        VerifyPasswordDataAccessObject dao = new VerifyPasswordDataAccessObject(new AuthProperties(""));

        String result = dao.verifyPassword("newSecret");

        assertEquals("Created new password.", result);
        assertEquals("Correct Password", dao.verifyPassword("newSecret"));
    }

    @Test
    public void testIncorrectPassword() throws Exception {
        // Simulate an existing password
        VerifyPasswordDataAccessObject.setSysPasswordForTesting("correctPass");
        VerifyPasswordDataAccessObject dao = new VerifyPasswordDataAccessObject(new AuthProperties(""));

        String result = dao.verifyPassword("wrongPass");

        assertEquals("Incorrect Password", result);
    }

    @Test
    public void testCorrectPassword() throws Exception {
        // Simulate an existing password
        VerifyPasswordDataAccessObject.setSysPasswordForTesting("correctPass");
        VerifyPasswordDataAccessObject dao = new VerifyPasswordDataAccessObject(new AuthProperties(""));

        String result = dao.verifyPassword("correctPass");

        assertEquals("Correct Password", result);
    }
}

