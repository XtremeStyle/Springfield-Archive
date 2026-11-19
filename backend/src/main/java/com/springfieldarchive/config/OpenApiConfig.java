package com.springfieldarchive.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI springfieldArchiveOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Springfield Archive API")
                .description("API pública de lectura del catálogo de Springfield Archive.")
                .version("1.1.1")
                .license(new License().name("MIT")));
    }
}
