package team.themoment.hellogsmv3.global.security;

import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.global.common.logging.LoggingFilter;
import team.themoment.hellogsmv3.global.security.auth.AuthEnvironment;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;
import team.themoment.hellogsmv3.global.security.filter.TimeBasedFilter;
import team.themoment.hellogsmv3.global.security.handler.CustomAccessDeniedHandler;
import team.themoment.hellogsmv3.global.security.handler.CustomAuthenticationEntryPoint;
import team.themoment.hellogsmv3.global.security.handler.CustomUrlAuthenticationSuccessHandler;

import java.time.LocalDateTime;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final ScheduleEnvironment scheduleEnv;
    private final AuthEnvironment authEnv;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final LoggingFilter loggingFilter;

    @Bean
    public Filter timeBasedFilter() {
        LocalDateTime oneseoSubmissionStart = scheduleEnv.oneseoSubmissionStart();
        LocalDateTime oneseoSubmissionEnd = scheduleEnv.oneseoSubmissionEnd();

        return new TimeBasedFilter()
                .addFilter(HttpMethod.POST, "/oneseo/v3/temp-storage", oneseoSubmissionStart, oneseoSubmissionEnd)
                .addFilter(HttpMethod.POST, "/oneseo/v3/oneseo/me", oneseoSubmissionStart, oneseoSubmissionEnd)
                .addFilter(HttpMethod.PUT, "/oneseo/v3/oneseo/{memberId}", oneseoSubmissionStart, oneseoSubmissionEnd)
                .addFilter(HttpMethod.POST, "/oneseo/v3/image", oneseoSubmissionStart, oneseoSubmissionEnd);
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        return new DefaultAuthorizationCodeTokenResponseClient();
    }

    @Configuration
    @EnableWebSecurity
    public class LocalSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

            basicSetting(http);
            cors(http);
            oauth2Login(http);
            exceptionHandling(http);
            authorizeHttpRequests(http);
            addLoggingFilter(http);

            return http.build();
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(authEnv.allowedOrigins());

        configuration.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()));

        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void basicSetting(HttpSecurity http) throws Exception {
        http.formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);
    }

    private void cors(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));
    }

    private void oauth2Login(HttpSecurity http) throws Exception {
        http.oauth2Login(oauth2Login ->
                oauth2Login
                        .authorizationEndpoint(authorizationEndpointConfig ->
                                authorizationEndpointConfig.baseUri(authEnv.loginEndPointBaseUri()))
                        .loginProcessingUrl(authEnv.loginProcessingUri())
                        .successHandler(new CustomUrlAuthenticationSuccessHandler(authEnv.dryRun(), authEnv.hgStudent(), authEnv.hgAdmin()))
                        .failureHandler(new SimpleUrlAuthenticationFailureHandler(authEnv.hgStudent()))

        );
    }

    private void exceptionHandling(HttpSecurity http) throws Exception {
        http.exceptionHandling(handling -> handling
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint));
    }

    private void authorizeHttpRequests(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(httpRequests -> httpRequests

                // for CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**/*").permitAll()

                // auth
                .requestMatchers("/auth/v3/**").permitAll()

                // member
                .requestMatchers(HttpMethod.GET, "/member/v3/member/me").hasAnyAuthority(
                        Role.UNAUTHENTICATED.name(),
                        Role.APPLICANT.name(),
                        Role.ADMIN.name(),
                        Role.ROOT.name()
                )
                .requestMatchers(HttpMethod.GET, "/member/v3/member/{memberId}").hasAnyAuthority(
                        Role.ADMIN.name()
                )
                .requestMatchers(HttpMethod.POST,
                        "/member/v3/member/me/send-code",
                        "/member/v3/member/me/auth-code"
                ).hasAnyAuthority(
                        Role.UNAUTHENTICATED.name(),
                        Role.APPLICANT.name(),
                        Role.ADMIN.name(),
                        Role.ROOT.name()
                )
                .requestMatchers(HttpMethod.POST, "/member/v3/member/me/send-code-test").hasAnyAuthority(
                        Role.ROOT.name()
                )
                .requestMatchers(HttpMethod.POST, "/member/v3/member/me").hasAnyAuthority(
                        Role.UNAUTHENTICATED.name(),
                        Role.APPLICANT.name(),
                        Role.ADMIN.name(),
                        Role.ROOT.name()
                )
                .requestMatchers(HttpMethod.GET, "/member/v3/auth-info/me").hasAnyAuthority(
                        Role.UNAUTHENTICATED.name(),
                        Role.APPLICANT.name(),
                        Role.ADMIN.name(),
                        Role.ROOT.name()
                )
                .requestMatchers(HttpMethod.GET, "/member/v3/auth-info/{memberId}").hasAnyAuthority(
                        Role.ADMIN.name()
                )
                .requestMatchers(HttpMethod.GET, "/member/v3/first-test-result/me").hasAnyAuthority(
                        Role.APPLICANT.name(),
                        Role.ROOT.name()
                )
                .requestMatchers(HttpMethod.GET, "/member/v3/second-test-result/me").hasAnyAuthority(
                        Role.APPLICANT.name(),
                        Role.ROOT.name()
                )
                .requestMatchers(HttpMethod.GET, "/member/v3/check-duplicate").hasAnyAuthority(
                        Role.UNAUTHENTICATED.name(),
                        Role.ROOT.name()
                )

                // oneseo
                .requestMatchers("/oneseo/v3/oneseo/me").hasAnyAuthority(
                        Role.APPLICANT.name(),
                        Role.ROOT.name()
                )
                .requestMatchers("/oneseo/v3/oneseo/{memberId}").hasAnyAuthority(
                        Role.ADMIN.name()
                )
                .requestMatchers(HttpMethod.PATCH, "/oneseo/v3/arrived-status/{memberId}").hasAnyAuthority(
                        Role.ADMIN.name()
                )
                .requestMatchers(HttpMethod.PATCH, "/oneseo/v3/competency-score/{memberId}").hasAnyAuthority(
                        Role.ADMIN.name()
                )
                .requestMatchers(HttpMethod.PATCH, "/oneseo/v3/interview-score/{memberId}").hasAnyAuthority(
                        Role.ADMIN.name()
                )
                .requestMatchers(HttpMethod.POST, "/oneseo/v3/image").hasAnyAuthority(
                        Role.APPLICANT.name(),
                        Role.ADMIN.name(),
                        Role.ROOT.name()
                )
                .requestMatchers(HttpMethod.DELETE, "/oneseo/v3/oneseo/me").hasAnyAuthority(
                        Role.APPLICANT.name(),
                        Role.ROOT.name()
                )
                .requestMatchers(HttpMethod.GET, "/oneseo/v3/oneseo/search").hasAnyAuthority(
                        Role.ADMIN.name(),
                        Role.ROOT.name()
                )
                .requestMatchers(HttpMethod.PUT, "/oneseo/v3/final-submit").hasAnyAuthority(
                        Role.APPLICANT.name()
                )
                .requestMatchers(HttpMethod.GET, "/oneseo/v3/excel").hasAnyAuthority(
                        Role.ADMIN.name()
                )
                .requestMatchers(HttpMethod.GET, "/oneseo/v3/admission-tickets").hasAnyAuthority(
                        Role.ADMIN.name()
                )
                .requestMatchers(HttpMethod.GET, "/oneseo/v3/editability").hasAnyAuthority(
                        Role.ADMIN.name()
                )

                // operation test result api
                .requestMatchers("/operation/**").hasAnyAuthority(
                        Role.ADMIN.name()
                )

                // test result api
                .requestMatchers("/test-result/v3/auth-code").hasAnyAuthority(
                        Role.UNAUTHENTICATED.name(),
                        Role.APPLICANT.name()
                )
                .requestMatchers("/test-result/v3/send-code").hasAnyAuthority(
                        Role.UNAUTHENTICATED.name(),
                        Role.APPLICANT.name()
                )
                .requestMatchers("/test-result/v3/my/**").hasAnyAuthority(
                        Role.UNAUTHENTICATED.name(),
                        Role.APPLICANT.name()
                )

                // mock score calculate api
                .requestMatchers(HttpMethod.POST, "/oneseo/v3/calculate-mock-score").permitAll()

                // common / get date api
                .requestMatchers(HttpMethod.GET, "/date").permitAll()

                .anyRequest().permitAll()
        );
    }

    private void addLoggingFilter(HttpSecurity http) {
        http.addFilterBefore(loggingFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
