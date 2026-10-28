package in.carenest.clinic.security;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
  private final JdbcTemplate db;
  private final String username;
  private final String password;

  public AdminBootstrap(
      JdbcTemplate db,
      @Value("${app.bootstrap-admin-username:}") String username,
      @Value("${app.bootstrap-admin-password:}") String password) {
    this.db = db;
    this.username = username;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (username.isBlank() || password.isBlank()) {
      throw new IllegalStateException(
          "BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD are required");
    }
    if (password.length() < 14) {
      throw new IllegalStateException("Bootstrap administrator password must be at least 14 characters");
    }
    db.update("""
        insert into users(id,username,password_hash,display_name,role,joined_on,active)
        values (?, ?, crypt(?,gen_salt('bf')), 'Platform Administrator', 'PLATFORM_ADMIN', current_date, true)
        on conflict (username) do update set
          password_hash=excluded.password_hash,
          active=true,
          role='PLATFORM_ADMIN'
        """, UUID.randomUUID(), username, password);
  }
}
