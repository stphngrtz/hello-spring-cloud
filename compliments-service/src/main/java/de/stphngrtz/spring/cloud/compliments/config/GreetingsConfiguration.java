package de.stphngrtz.spring.cloud.compliments.config;

import com.netflix.loadbalancer.AvailabilityFilteringRule;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.PingUrl;
import org.springframework.context.annotation.Bean;

public class GreetingsConfiguration {

    @Bean
    IPing ribbonPing() {
        return new PingUrl();
    }

    @Bean
    IRule ribbonRule() {
        return new AvailabilityFilteringRule();
    }
}
