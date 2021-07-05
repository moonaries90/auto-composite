package com.huafon.common.auto.composite.selfreference;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;

public class SelfReferenceBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor {

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        if(bean instanceof SelfReferenceBean) {
            ((SelfReferenceBean) bean).setSelf(bean);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SelfReferenceBean) {
            ((SelfReferenceBean) bean).setSelf(bean);
        }
        return bean;
    }
}
