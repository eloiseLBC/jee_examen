package com.example.jee.examen.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI yamOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Yam API")
                        .description("API REST Spring Boot pour jouer au Yam (Yahtzee)")
                        .version("1.0.0"));
    }
}
