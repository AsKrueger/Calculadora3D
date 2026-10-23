package com.tdcostmanager.backend.infrastructure.security;

import com.tdcostmanager.backend.domain.model.User;
import com.tdcostmanager.backend.domain.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Obtiene el usuario autenticado de la petición actual a partir del SecurityContext.
 * Es la única fuente del propietario de los datos: nunca se acepta uno enviado por el cliente.
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado");
        }

        // JwtAuthenticationFilter deja como principal la entidad User cargada desde la base de datos
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }

        return userRepository.findByEmail(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "El usuario autenticado no existe: " + authentication.getName()));
    }

    /**
     * Referencia gestionada al usuario actual, para asignarlo como propietario sin una consulta extra.
     */
    public User getCurrentUser() {
        return userRepository.getReferenceById(getCurrentUserId());
    }
}
