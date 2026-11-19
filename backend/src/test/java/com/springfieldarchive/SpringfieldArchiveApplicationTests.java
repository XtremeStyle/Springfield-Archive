package com.springfieldarchive;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
@Import(TestDatabaseContainer.class)
class SpringfieldArchiveApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoadsWithExplicitTestProfile() {
        assertThat(environment.getActiveProfiles()).containsExactly("test");
    }
}
