package dev.felipe.canchas.adapter.web;

import dev.felipe.canchas.core.port.in.StaffDirectoryUseCase;
import dev.felipe.canchas.core.model.StaffRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Quién entra a qué:
 * - Público: /horarios (canchas libres por hora) y la página de ingreso.
 * - Recepción y administración: agenda, reservas y equipos.
 * - Solo administración: canchas, tarifas, personal y balance.
 */
@Configuration
public class WebSecurity {
    private static final String ADMIN = StaffRole.ADMIN.name();

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers("/", "/horarios", "/css/**", "/error", "/error/**").permitAll()
                        .requestMatchers("/canchas/**", "/tarifas/**", "/personal/**", "/reportes/**").hasRole(ADMIN)
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/ingreso").loginProcessingUrl("/ingreso")
                        .defaultSuccessUrl("/agenda", true).failureUrl("/ingreso?error").permitAll())
                .logout(logout -> logout.logoutUrl("/salir").logoutSuccessUrl("/ingreso?salio").permitAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService staffAccounts(StaffDirectoryUseCase staff) {
        return username -> staff.byUsername(username)
                .map(account -> User.withUsername(account.username())
                        .password(account.passwordHash())
                        .roles(account.role().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No existe la cuenta " + username));
    }
}
