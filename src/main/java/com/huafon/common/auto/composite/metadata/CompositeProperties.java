package com.huafon.common.auto.composite.metadata;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "auto.composite")
public class CompositeProperties {

    private boolean mybatisPlusEnabled = true;

    private List<String> basePackages;

    public List<String> getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(List<String> basePackages) {
        this.basePackages = basePackages;
    }

    public boolean isMybatisPlusEnabled() {
        return mybatisPlusEnabled;
    }

    public void setMybatisPlusEnabled(boolean mybatisPlusEnabled) {
        this.mybatisPlusEnabled = mybatisPlusEnabled;
    }
}
