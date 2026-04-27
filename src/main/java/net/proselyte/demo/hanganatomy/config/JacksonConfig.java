package net.proselyte.demo.hanganatomy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonConfig {

    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }
}
