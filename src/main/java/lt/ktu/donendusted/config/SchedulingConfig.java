package lt.ktu.donendusted.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled jobs (the nightly demo reset). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
