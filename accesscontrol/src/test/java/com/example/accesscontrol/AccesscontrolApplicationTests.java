package com.example.accesscontrol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class AccesscontrolApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void mainMethod_runsWithoutErrors_whenWebTypeNone() {
        String key = "spring.main.web-application-type";
        String previous = System.getProperty(key);
        System.setProperty(key, "none");
        try {
            Executable callMain = () -> AccesscontrolApplication.main(new String[]{});
            assertDoesNotThrow(callMain, "AccesscontrolApplication.main should run without throwing when web type is NONE");
        } finally {
            if (previous == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previous);
            }
        }
    }
}
