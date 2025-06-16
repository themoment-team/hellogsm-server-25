package team.themoment.hellogsmv3.global.security.data;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDateTime;

@ConfigurationProperties(prefix = "schedule")
public record ScheduleEnvironment(
    LocalDateTime oneseoSubmissionStart,
    LocalDateTime oneseoSubmissionEnd,
    LocalDateTime firstResultsAnnouncement,
    LocalDateTime competencyEvaluation,
    LocalDateTime interview,
    LocalDateTime finalResultsAnnouncement
) {
}
