package com.huafon.common.auto.composite.handler;

import com.huafon.common.auto.composite.metadata.Param;
import com.huafon.common.auto.composite.util.TypeUtil;

import java.util.Set;

public interface ParamComposite<R> extends Composite<Param, R> {

    Set<Class<?>> supportedParamTypes();

    default boolean support(Class<?> clazz) {
        return this.supportedParamTypes() != null &&
                this.supportedParamTypes().stream().anyMatch(c -> Composites.boxIfNeeded(c).isAssignableFrom(Composites.boxIfNeeded(clazz)));
    }

    @Override
    default Class<?> getParamType() {
        return Param.class;
    }

    @Override
    default Class<?> getReturnType() {
        return TypeUtil.getActualClass(this.getClass(), ParamComposite.class, 0);
    }
}
