package com.subastasapp.backend.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.http.HttpMethod
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val authTokenFilter: AuthTokenFilter
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/ws/**").permitAll() // WebSockets
                    // Catálogos públicos: cualquiera puede LEER subastas e ítems (el precio
                    // base se oculta a invitados desde el front). Solo se permite GET — POST
                    // sobre estos paths (incluido /pujar) requiere autenticación.
                    .requestMatchers(HttpMethod.GET, "/api/subastas").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/subastas/activas").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/subastas/*/detalle").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/subastas/*/pujas").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/subastas/*/items").permitAll()
                    .anyRequest().authenticated()
            }
        
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
        
        return http.build()
    }
}
