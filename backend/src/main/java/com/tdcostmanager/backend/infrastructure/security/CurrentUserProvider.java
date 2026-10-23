package com.tdcostmanager.backend.infrastructure.security;

import com.tdcostmanager.backend.domain.model.User;
import com.tdcostmanager.backend.domain.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Devuelve el identificador del usuario autenticado en la petición actual.
 * Los servicios lo usan para asignar y filtrar el propietario de los datos.
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
            throw new AuthenticationCredentialsNotFoundException("No hay ningún usuario autenticado");
        }

        if (authentication.getPrincipal() instanceof User user && user.getId() != null) {
            return user.getId();
        }

        return userRepository.findByEmail(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "El usuario autenticado no existe: " + authentication.getName()));
    }
}
