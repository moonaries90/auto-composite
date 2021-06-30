package com.huafon.common.auto.composite.handler;

import com.huafon.common.auto.composite.annotation.AutoComposite;
import com.huafon.common.auto.composite.metadata.CompositeProperties;
import org.aopalliance.aop.Advice;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.annotation.Autowired;

public class CompositePointCutAdvisor extends AbstractPointcutAdvisor {

    private final Pointcut pointcut;

    @Autowired
    private CompositeInterceptor compositeInterceptor;

    public CompositePointCutAdvisor(CompositeProperties properties) {
        Pointcut cpc = new AnnotationMatchingPointcut(AutoComposite.class, true);
        Pointcut mpc = new AnnotationMatchingPointcut(null, AutoComposite.class, true);
        ComposablePointcut pointcut = new ComposablePointcut(cpc);
        pointcut.union(mpc);
        this.pointcut = new CompositePointcut(pointcut, properties);
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this.compositeInterceptor;
    }

    public static class CompositePointcut implements Pointcut {

        private final Pointcut delegate;

        private final CompositeProperties properties;

        public CompositePointcut(Pointcut delegate, CompositeProperties properties) {
            this.delegate = delegate;
            this.properties = properties;
        }

        @Override
        public ClassFilter getClassFilter() {
            return clazz -> !clazz.getName().startsWith("java") && (properties.getBasePackages() == null ||
                    properties.getBasePackages().size() == 0 ||
                    properties.getBasePackages().stream().anyMatch(p -> clazz.getName().startsWith(p))) &&
                    delegate.getClassFilter().matches(clazz);
        }

        @Override
        public MethodMatcher getMethodMatcher() {
            return delegate.getMethodMatcher();
        }
    }
}
