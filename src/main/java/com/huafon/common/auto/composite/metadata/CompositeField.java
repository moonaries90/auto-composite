package com.huafon.common.auto.composite.metadata;

import com.huafon.common.auto.composite.annotation.AutoField;
import com.huafon.common.auto.composite.annotation.FetchType;
import com.huafon.common.auto.composite.exception.CompositeException;
import com.huafon.common.auto.composite.handler.Composite;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.LazyLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * class Target {
 * <p>
 * String a;
 * <p>
 * ToBeComposite toBeComposite;
 * }
 * <p>
 * class ToBeComposite {
 * <p>
 * String b;
 * }
 * <p>
 * class SomeService {
 * <p>
 * Target someMethod(Object... args) {
 * return xxx;
 * }
 * }
 *
 * @author lji
 * @date 2021/06/24
 */
@SuppressWarnings({"unchecked", "rawtypes", "static-access"})
public class CompositeField implements Comparable<CompositeField> {

    private final Class<?> actualReturnType;

    private final Class<?> returnType;

    private final Field field;

    private Field propertyField;

    private Method getPropertyMethod;

    private final Enhancer enhancer;

    private FetchType fetchType;

    private boolean selfProperty;

    private Composite composite;

    private String paramName;

    private int order;

    /**
     * 组装类
     *
     * @param actualReturnType 实际返回类型
     * @param returnType       返回类型
     * @param field            属性域
     * @param fetchType        获取类型
     */
    public CompositeField(Class<?> actualReturnType, Class<?> returnType, Field field, FetchType fetchType) {
        this.field = field;
        this.field.setAccessible(true);
        this.enhancer = new Enhancer();
        this.returnType = returnType;
        this.actualReturnType = actualReturnType;
        if (field.getType().equals(List.class) || field.getType().equals(ArrayList.class)) {
            this.enhancer.setSuperclass(ArrayList.class);
        }
        if (field.getType().equals(Set.class) || field.getType().equals(HashSet.class)) {
            this.enhancer.setSuperclass(HashSet.class);
        }
        this.fetchType = fetchType;
        AutoField autoField = field.getAnnotation(AutoField.class);
        if (autoField != null) {
            this.order = autoField.order();
            this.fetchType = autoField.fetchType();
            this.paramName = autoField.paramName();
            tryProperty(actualReturnType, autoField.property());
        }
    }

    private void tryProperty(Class<?> actualReturnType, String property) {
        try {
            propertyField = actualReturnType.getDeclaredField(property);
            propertyField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                getPropertyMethod = actualReturnType.getDeclaredMethod("get" + property.substring(0, 1).toUpperCase() + property.substring(1));
                getPropertyMethod.setAccessible(true);
            } catch (NoSuchMethodException ex) {
                throw new CompositeException(ex);
            }
        }
    }

    public void processComposite(Object target) {
        if(Map.class.isAssignableFrom(returnType)) {
            process(((Map<?, ?>) target).values());
        } else if(Collection.class.isAssignableFrom(returnType)) {
            process((Collection<?>) target);
        } else {
            process(Collections.singletonList(target));
        }
    }

    private void process(Collection<?> target) {
        Map paramKeyResultValue = new HashMap<>();
        Collection params;
        if(selfProperty) {
            params = target;
            target.forEach(o -> paramKeyResultValue.put(o, o));
        } else {
            params = target.stream().map(o -> {
                try {
                    Object p = null;
                    if (propertyField != null) {
                        p = propertyField.get(o);
                    } else if (getPropertyMethod != null) {
                        p = getPropertyMethod.invoke(o);
                    }
                    if(p != null) {
                        paramKeyResultValue.put(p, o);
                        return p;
                    }
                    throw new CompositeException("cannot find property for " + composite.getClass().getName());
                } catch (Exception e) {
                    throw new CompositeException(e);
                }
            }).collect(Collectors.toList());
        }
        if(fetchType == FetchType.EAGER) {
            params.forEach(p -> {
                if(!this.composite.getParamType().isAssignableFrom(p.getClass())) {
                    throw new CompositeException("assembleHandler param type and actual param type mismatch");
                }
                Object o = paramKeyResultValue.get(p);
                List f = this.composite.queryList(p, paramName);
                if (o != null && f != null) {
                    try {
                        if (List.class.equals(field.getType()) || ArrayList.class.equals(field.getType())) {
                            field.set(o, new ArrayList<>(f));
                        } else if (Set.class.equals(field.getType()) || HashSet.class.equals(field.getType())) {
                            field.set(o, new HashSet<>(f));
                        } else if (f.size() > 0) {
                            field.set(o, f.get(0));
                        }
                    } catch (IllegalAccessException ignore) {
                    }
                }
            });
        } else if(fetchType == FetchType.LAZY){
            try {
                for(Object p : params) {
                    if(!this.composite.getParamType().isAssignableFrom(p.getClass())) {
                        throw new CompositeException("assembleHandler param type and actual param type mismatch");
                    }
                    Object o = paramKeyResultValue.get(p);
                    Function f = (r) -> composite.queryList(p, paramName);
                    Callback listCallback = (LazyLoader) () -> f.apply(null);
                    Callback setCallback = (LazyLoader) () -> new HashSet((List) f.apply(null));
                    if (List.class.equals(field.getType()) || ArrayList.class.equals(field.getType())) {
                        List list = (List) enhancer.create(List.class, listCallback);
                        field.set(o, list);
                    } else if (Set.class.equals(field.getType()) || HashSet.class.equals(field.getType())) {
                        Set set = (Set) enhancer.create(Set.class, setCallback);
                        field.set(o, set);
                    } else {
                        List list = (List) enhancer.create(List.class, listCallback);
                        if(list != null && list.size() > 0) {
                            field.set(o, list.get(0));
                        }
                    }
                }
            } catch (IllegalAccessException ignore) {
            }
        }
    }

    @Override
    public int compareTo(CompositeField o) {
        if(o == null) {
            return -1;
        }
        return Integer.compare(this.order, o.order);
    }

    public Class<?> getParamType() {
        if(this.propertyField != null) {
            return this.propertyField.getType();
        } else if(this.getPropertyMethod != null) {
            return this.getPropertyMethod.getReturnType();
        } else {
            return this.actualReturnType;
        }
    }

    public void setAssembler(Composite<?, ?> composite) {
        this.composite = composite;
        this.selfProperty = this.composite.getParamType().isAssignableFrom(this.actualReturnType);
    }
}
