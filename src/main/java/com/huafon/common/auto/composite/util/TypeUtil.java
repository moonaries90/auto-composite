package com.huafon.common.auto.composite.util;

import com.huafon.common.auto.composite.handler.Composite;
import org.springframework.cglib.proxy.Enhancer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TypeUtil {

    private Enhancer enhancer;

    public <T> T transform(T t) {
        return null;
    }

    public static Class<?>[] getActualClass(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] actualTypes = ((ParameterizedType) type).getActualTypeArguments();
            Class<?>[] actualClasses = new Class[actualTypes.length];
            for (int i = 0; i < actualTypes.length; i++) {
                Type actualType = actualTypes[i];
                if (actualType instanceof Class) {
                    actualClasses[i] = (Class<?>) actualType;
                }
                if (actualType instanceof ParameterizedType) {
                    return getActualClass(actualType);
                }
            }
            return actualClasses;
        }
        return null;
    }

    public static Class<?> getActualClass(Class<?> c, int index) {
        Type[] types = c.getGenericInterfaces();
        for (Type type : types) {
            if(type.getTypeName().startsWith(Composite.class.getName())) {
                Class<?>[] actualTypes = TypeUtil.getActualClass(type);
                if(actualTypes != null && actualTypes.length > index) {
                    return actualTypes[index];
                }
            }
        }
        return null;
    }
}
