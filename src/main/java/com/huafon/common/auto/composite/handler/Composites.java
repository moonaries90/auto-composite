package com.huafon.common.auto.composite.handler;

import com.huafon.common.auto.composite.metadata.CompositeField;
import com.huafon.common.auto.composite.mybatisplus.MybatisPlusComposites;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Composites implements ApplicationListener<ApplicationReadyEvent> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /*
     * <K, V> = <ReturnType, Composite>
     */
    private static final Map<Pair, Composite<?, ?>> compositeMap = new HashMap<>();

    /*
     * <K, V> = <ReturnType, ParamComposite>
     */
    private static final Map<Class<?>, ParamComposite<?>> paramCompositeMap = new HashMap<>();

    public static final ConcurrentHashMap<Method, List<CompositeField>> COMPOSITE_MAP = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private MybatisPlusComposites mybatisPlusComposites;

    /**
     * 这里仅支持有具体实现的 Class， 不支持匿名类
     */
    public static void registerHandler(Composite<?, ?> handler) {
        Class<?> returnType = handler.getReturnType();
        if(returnType != null) {
            if (handler instanceof ParamComposite) {
                paramCompositeMap.put(returnType, (ParamComposite<?>) handler);
            } else {
                compositeMap.put(keyPairs(handler), handler);
            }
        }
    }

    public static Composite<?, ?> getHandler(Pair pair) {
        for(Map.Entry<Pair, Composite<?, ?>> entry : compositeMap.entrySet()) {
            if(entry.getKey().canApply(pair)) {
                return entry.getValue();
            }
        }
        for(Map.Entry<Class<?>, ParamComposite<?>> entry: paramCompositeMap.entrySet()) {
            if(boxIfNeeded(pair.getReturnType()).isAssignableFrom(boxIfNeeded(entry.getKey()))) {
                if(entry.getValue().support(pair.getParamType())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public Composite<?, ?> getCommonHandler(String paramName, Class<?> returnType) {
        if(mybatisPlusComposites != null) {
            return mybatisPlusComposites.getComposite(paramName, new Pair(returnType));
        }
        return null;
    }

    public static Class<?> boxIfNeeded(Class<?> paramType) {
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

    private static Pair keyPairs(Composite<?, ?> handler) {
        return new Pair(handler.getParamType(), handler.getReturnType());
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(event.getApplicationContext(), Composite.class);
        if(beanNames.length > 0) {
            for(String beanName : beanNames) {
                try {
                    Composites.registerHandler((Composite) event.getApplicationContext().getBean(beanName));
                } catch (Exception e) {
                    logger.error("failed to find composite, name is {}", beanName, e);
                }
            }
        }
    }

    public static class Pair {

        private final Class<?> paramType, returnType;

        public Pair(Class<?> paramType, Class<?> returnType) {
            this.paramType = paramType;
            this.returnType = returnType;
        }

        public Pair(Class<?> returnType) {
            this.paramType = Object.class;
            this.returnType = returnType;
        }

        /**
         * composite 所约定的参数类型， 应该是实际参数类型的父类
         * composite 所约定的返回值类型， 应该是是实际类型的子类
         * @param pair 实际入参、出参类型
         * @return 是否匹配
         */
        public boolean canApply(Pair pair) {
            return
                    boxIfNeeded(this.paramType).isAssignableFrom(boxIfNeeded(pair.paramType)) &&
                    boxIfNeeded(pair.returnType).isAssignableFrom(boxIfNeeded(this.returnType));
        }

        public Class<?> getParamType() {
            return paramType;
        }

        public Class<?> getReturnType() {
            return returnType;
        }
    }
}
