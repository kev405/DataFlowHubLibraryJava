package com.practice.apiservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none"
        })
class ProfileSmokeIT {

    @Autowired Environment env;
    @Autowired DataSource ds;

    @Test
    void usesTestProfileAndH2() {
        assertThat(env.getActiveProfiles()).contains("test");
        assertThat(ds).isInstanceOf(HikariDataSource.class);
        var url = ((HikariDataSource) ds).getJdbcUrl();
        assertThat(url).startsWith("jdbc:h2:mem:");
        assertThat(env.getProperty("logging.level.root")).isEqualTo("WARN");
    }
}
