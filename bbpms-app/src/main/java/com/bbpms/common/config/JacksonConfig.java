package com.bbpms.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Long -> String to avoid precision loss on JS clients
        SimpleModule longModule = new SimpleModule();
        longModule.addSerializer(Long.class, new StdSerializer<>(Long.class) {
            @Override
            public void serialize(Long v, JsonGenerator g, SerializerProvider p) throws IOException {
                if (v == null) g.writeNull(); else g.writeString(v.toString());
            }
        });
        m.registerModule(longModule);
        return m;
    }
}
