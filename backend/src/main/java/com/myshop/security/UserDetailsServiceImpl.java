package com.myshop.security;

import com.myshop.model.entity.User;
import com.myshop.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UserDetailsServiceImpl — Spring Security's user-loading contract.
 *
 * WHY DOES SPRING SECURITY NEED THIS?
 * Spring Security's authentication mechanism is generic — it doesn't know
 * anything about our User model or UserRepository. This class is the bridge.
 *
 * During login: AuthenticationManager calls loadUserByUsername(email)
 * → fetches User from DB → returns UserDetails
 * → Spring compares the provided password against UserDetails.getPassword()
 * using the configured PasswordEncoder (BCrypt)
 * → If match → authentication success → generate JWT
 *
 * During request: JwtAuthenticationFilter extracts email from token
 * → calls loadUserByUsername(email) → builds
 * UsernamePasswordAuthenticationToken
 * → Spring security context knows who the user is for this request
 *
 * WHY "username" = email here?
 * Spring Security uses "username" as a generic identifier. In our system
 * we use email as the unique login identifier.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is disabled: " + email);
        }

        /**
         * GrantedAuthority: Spring Security's representation of a permission/role.
         * Convention: roles are prefixed with "ROLE_" → "ROLE_USER", "ROLE_ADMIN"
         * This prefix is required for hasRole("ADMIN") to work in SecurityConfig.
         * hasAuthority("ROLE_ADMIN") and hasRole("ADMIN") are equivalent — Spring adds
         * the prefix.
         */
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        /**
         * org.springframework.security.core.userdetails.User (not our User entity!)
         * is Spring Security's UserDetails implementation.
         * We pass: username (email), hashed password, authorities.
         * Spring Security compares against this when authenticating.
         */
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                authorities);
    }
}
