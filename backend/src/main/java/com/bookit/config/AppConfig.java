package com.bookit.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** One clock for the whole app so "now" is consistent and can be replaced in tests. */
    @Bean
    public Clock clock(AppProperties props) {
        return Clock.system(ZoneId.of(props.zone()));
    }
}
