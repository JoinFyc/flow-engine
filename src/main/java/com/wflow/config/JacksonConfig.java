package com.wflow.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.Setter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author : JoinFyc
 * @since : 2024/5/13
 */
@Configuration
public class JacksonConfig extends StdSerializer<Long> {

    @Setter
    private Integer threshold;

    public JacksonConfig() {
        super(Long.class);
    }

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (String.valueOf(value).length() > threshold) {
            gen.writeString(value.toString());
        } else {
            gen.writeNumber(value);
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        //把long类型值转字符串给前端，防止丢失精度 1789834471275814913
        return builder -> {
            JacksonConfig config = new JacksonConfig();
            config.setThreshold(15);
            builder.serializerByType(Long.class, config);
        };
    }
}
