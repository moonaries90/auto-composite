package com.huafon.common.auto.composite;

import com.huafon.common.auto.composite.handler.*;
import com.huafon.common.auto.composite.handler.Composites;
import com.huafon.common.auto.composite.metadata.CompositeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CompositeProperties.class)
public class AutoCompositeConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CompositePointCutAdvisor compositePointCutAdvisor(CompositeProperties properties) {
        return new CompositePointCutAdvisor(properties);
    }

    @Bean
    public Composites composites() {
        return new Composites();
    }

    @Bean
    @ConditionalOnMissingBean
    public CompositeInterceptor compositeInterceptor() {
        return new CompositeInterceptor(composites());
    }
}
