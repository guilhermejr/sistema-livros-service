package net.guilhermejr.sistema.livrosservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Libera GET /capas/** sem token: um {@code <img>} não manda o cabeçalho Authorization.
 * Fica entre a cadeia do actuator e a do JWT, que vêm da seguranca-jwt; as duas
 * continuam valendo para todo o resto, porque esta tem outro nome.
 */
@Configuration
public class SegurancaCapasConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain filterChainCapas(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/capas/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

}
