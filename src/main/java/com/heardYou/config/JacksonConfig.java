package com.heardYou.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 3 (Spring Boot 4) changed the default field visibility from ANY to PUBLIC_ONLY.
 * The existing POJO request DTOs (CheckinRequest, BriefRequest) declare private fields
 * with public getter/setter pairs. Jackson 3 discovers these via the setter (ANY) and
 * getter (PUBLIC_ONLY) mechanisms, which should be sufficient. However, when the private
 * fields are not also exposed as field-level properties, some Jackson 3 configurations
 * can fail deserialization of inner static classes.
 *
 * This configuration sets field visibility to ANY so that Jackson 3 finds private fields
 * directly, matching the Jackson 2 default and ensuring all existing DTOs deserialize
 * without @JsonProperty annotations.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jacksonFieldVisibilityCustomizer() {
        return builder -> builder.changeDefaultVisibility(
                vc -> vc.withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        );
    }
}