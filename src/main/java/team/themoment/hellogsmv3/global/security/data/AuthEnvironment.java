package team.themoment.hellogsmv3.global.security.data;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "auth")
public record AuthEnvironment(
        List<String> allowedOrigins
) {
}