package com.tdcostmanager.backend;

import com.tdcostmanager.backend.domain.model.Role;
import com.tdcostmanager.backend.domain.model.User;
import com.tdcostmanager.backend.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import java.time.Instant;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    protected UserRepository userRepository;

    /**
     * Crea el usuario si no existe y lo deja autenticado para las siguientes
     * llamadas a servicios y peticiones MockMvc del test.
     */
    protected User authenticateAs(String email) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setPasswordHash("not-used-in-tests");
            u.setRole(Role.USER);
            u.setEnabled(true);
            u.setCreatedAt(Instant.now());
            u.setUpdatedAt(Instant.now());
            return userRepository.saveAndFlush(u);
        });
        TestSecurityContextHolder.setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return user;
    }
}
