package com.algaworks.brewer.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Configuração de Segurança do Spring Security.
 *
 * FASE 4: Migrado para Spring Boot 3.x (Spring Security 6)
 * - WebSecurityConfigurerAdapter removido (deprecated desde Spring Security 5.7)
 * - Usando SecurityFilterChain (component-based security configuration)
 * - authorizeRequests() → authorizeHttpRequests()
 * - antMatchers() → requestMatchers()
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				// Public endpoints - no authentication required
				.requestMatchers("/", "/login", "/error", "/404", "/500", "/403").permitAll()
				// Actuator endpoints - public health check, restricted management endpoints
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				.requestMatchers("/actuator/info").permitAll()
				.requestMatchers("/actuator/**").hasRole("ADMIN")
				// Application endpoints
				.requestMatchers("/cidades/nova").hasRole("CADASTRAR_CIDADE")
				.requestMatchers("/usuarios/**").hasRole("CADASTRAR_USUARIO")
				.anyRequest().authenticated()
			)
			.formLogin(form -> form
				.loginPage("/login")
				.defaultSuccessUrl("/cervejas", false)
				.permitAll()
			)
			.logout(logout -> logout
				.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
				.logoutSuccessUrl("/login")
			)
			.exceptionHandling(exception -> exception
				.accessDeniedPage("/403")
			)
			.sessionManagement(session -> session
				.invalidSessionUrl("/login")
				.sessionFixation().newSession()
				// Limit to 1 concurrent session per user to enhance security
				// Users will be logged out from other devices when logging in from a new device
				.maximumSessions(1)
			);

		return http.build();
	}

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.ignoring()
			.requestMatchers("/layout/**")
			.requestMatchers("/images/**");
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}