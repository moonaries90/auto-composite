package com.huafon.common.auto.composite.handler;

import com.huafon.common.auto.composite.metadata.CompositeField;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Composites implements InitializingBean {

    /*
     * <K, V> = <ReturnType, AssembleHandler>
     */
    private static final Map<Class<?>[], Composite<?, ?>> handlerMap = new HashMap<>();

    public static final ConcurrentHashMap<Method, List<CompositeField>> COMPOSITE_MAP = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private List<Composite> composites;

    /**
     * 这里仅支持有具体实现的 Class， 不支持匿名类
     */
    public static void registerHandler(Composite<?, ?> handler) {
        Class<?> returnType = handler.getReturnType();
        if(returnType != null) {
            handlerMap.put(keyPairs(handler), handler);
        }
    }

    public static Composite<?, ?> getHandler(Class<?> paramType, Class<?> returnType) {
        for(Map.Entry<Class<?>[], Composite<?, ?>> entry : handlerMap.entrySet()) {
            if(entry.getKey()[0].isAssignableFrom(boxIfNeeded(paramType)) && entry.getKey()[1].isAssignableFrom(returnType)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Class<?> boxIfNeeded(Class<?> paramType) {
        if(paramType == int.class) {
            return Integer.class;
        } else if (paramType == long.class) {
            return Long.class;
        } else if(paramType == short.class) {
            return Short.class;
        } else if(paramType == double.class) {
            return Double.class;
        } else if (paramType == boolean.class) {
            return Boolean.class;
        } else if(paramType == float.class) {
            return Float.class;
        } else if(paramType == void.class) {
            return Void.class;
        } else if(paramType == byte.class) {
            return Byte.class;
        }
        return paramType;
    }

    @Override
    public void afterPropertiesSet() {
        if(this.composites != null) {
            this.composites.forEach(Composites::registerHandler);
        }
    }

    private static Class<?>[] keyPairs(Composite<?, ?> handler) {
        return new Class<?>[]{handler.getParamType(), handler.getReturnType()};
    }
}
