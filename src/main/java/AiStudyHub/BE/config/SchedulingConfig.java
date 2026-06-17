package AiStudyHub.BE.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Enables Spring's scheduled task execution capability so that methods
// annotated with @Scheduled (e.g. the daily reputation job) are
// picked up and run on schedule.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
