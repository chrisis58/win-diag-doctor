package cn.teacy.wdd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static cn.teacy.wdd.common.constants.ServiceConstants.WS_PROBE_ENDPOINT;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WS_PROBE_ENDPOINT).permitAll()
                        .requestMatchers("/api/tasks/**").permitAll() // allow task result reporting API
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/chatui/_next/static/**"
                        ).permitAll()
                        .requestMatchers("/login").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .permitAll()
                        // 登录后强制跳到 dashboard
                        .defaultSuccessUrl("/dashboard.html", true)
                ).httpBasic(withDefaults());

        return http.build();
    }

}
