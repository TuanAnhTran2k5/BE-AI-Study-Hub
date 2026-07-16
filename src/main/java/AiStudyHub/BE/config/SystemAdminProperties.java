package AiStudyHub.BE.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "app.system-admin")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemAdminProperties {
    String email;
    String password;
    String fullName;
}
