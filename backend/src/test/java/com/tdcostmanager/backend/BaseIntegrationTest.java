package com.tdcostmanager.backend;

import com.tdcostmanager.backend.domain.model.Role;
import com.tdcostmanager.backend.domain.model.User;
import com.tdcostmanager.backend.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
     * Crea un usuario que puede actuar como propietario de los datos del test.
     * Combinar con @WithMockUser(username = email) para ejecutar como ese usuario.
     */
    protected User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("not-used-in-tests");
        user.setRole(Role.USER);
        user.setEnabled(true);
        return userRepository.save(user);
    }
}
