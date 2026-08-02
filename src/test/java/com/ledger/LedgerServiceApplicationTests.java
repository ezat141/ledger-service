package com.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/jwks")
class LedgerServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
