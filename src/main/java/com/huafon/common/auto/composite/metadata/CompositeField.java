package com.huafon.common.auto.composite.metadata;

import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.huafon.common.auto.composite.annotation.AutoField;
import com.huafon.common.auto.composite.exception.CompositeException;
import com.huafon.common.auto.composite.handler.Composite;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.LazyLoader;

import java.lang.reflect.Field;
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

    private final Class<?> returnType;

    private final POM toBeCompositePom;

    private POM propertyPom;

    private Class<?> paramType;

    private final Enhancer enhancer;

    private FetchType fetchType;

    private Composite composite;

    private String paramName;

    private int order;

    /**
     * 组装类
     *
     * @param actualReturnType 实际返回类型
     * @param returnType       返回类型
     * @param toBeComposite            属性域
     * @param fetchType        获取类型
     */
    public CompositeField(Class<?> actualReturnType, Class<?> returnType, Field toBeComposite, FetchType fetchType) {
        this.enhancer = new Enhancer();
        FieldAccess actualFieldAccess = FieldAccess.get(actualReturnType);
        MethodAccess actualMethodAccess = MethodAccess.get(actualReturnType);
        this.toBeCompositePom = new POM(actualFieldAccess, actualMethodAccess, toBeComposite.getName());
        this.returnType = returnType;
        this.paramType = actualReturnType;
        if (toBeCompositePom.propertyType.equals(List.class) || toBeCompositePom.propertyType.equals(ArrayList.class)) {
            this.enhancer.setSuperclass(ArrayList.class);
        }
        if (toBeCompositePom.propertyType.equals(Set.class) || toBeCompositePom.propertyType.equals(HashSet.class)) {
            this.enhancer.setSuperclass(HashSet.class);
        }
        this.fetchType = fetchType;
        AutoField autoField = toBeComposite.getAnnotation(AutoField.class);
        if (autoField != null) {
            this.order = autoField.order();
            this.fetchType = autoField.fetchType();
            this.paramName = autoField.paramName();
            this.propertyPom = new POM(actualFieldAccess, actualMethodAccess, autoField.property());
            this.paramType = this.propertyPom.propertyType;
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
        if(this.composite == null) {
            return;
        }
        Map paramKeyResultValue = new HashMap<>();
        Collection params;
        final boolean parameterization = Param.class.isAssignableFrom(this.composite.getParamType());
        if(parameterization) {
            params = target.stream().map(o -> {
                if(propertyPom != null) {
                    Object p = propertyPom.getProperty(o);
                    p = Param.instance(p, paramName);
                    paramKeyResultValue.put(p, o);
                    return p;
                } else {
                    Object p = Param.instance(o, paramName);
                    paramKeyResultValue.put(p, o);
                    return p;
                }
            }).collect(Collectors.toList());
        } else {
            if(propertyPom != null) {
                params = target.stream().map(o -> {
                    Object p = propertyPom.getProperty(o);
                    paramKeyResultValue.put(p, o);
                    return p;
                }).collect(Collectors.toList());
            } else {
                params = target;
                target.forEach(o -> paramKeyResultValue.put(o, o));
            }
        }
        if(fetchType == FetchType.EAGER) {
            params.forEach(p -> {
                Object o = paramKeyResultValue.get(p);
                List f = this.composite.queryList(p);
                if (o != null && f != null) {
                    if (List.class.equals(toBeCompositePom.propertyType) || ArrayList.class.equals(toBeCompositePom.propertyType)) {
                        this.toBeCompositePom.setProperty(o, new ArrayList<>(f));
                    } else if (Set.class.equals(toBeCompositePom.propertyType) || HashSet.class.equals(toBeCompositePom.propertyType)) {
                        this.toBeCompositePom.setProperty(o, new HashSet<>(f));
                    } else if (f.size() > 0) {
                        this.toBeCompositePom.setProperty(o, f.get(0));
                    }
                }
            });
        } else if(fetchType == FetchType.LAZY) {
            for(Object p : params) {
                Object o = paramKeyResultValue.get(p);
                Function f = (r) -> composite.queryList(p);
                Callback listCallback = (LazyLoader) () -> f.apply(null);
                Callback setCallback = (LazyLoader) () -> new HashSet((List) f.apply(null));
                if (List.class.equals(toBeCompositePom.propertyType) || ArrayList.class.equals(toBeCompositePom.propertyType)) {
                    List list = (List) enhancer.create(List.class, listCallback);
                    this.toBeCompositePom.setProperty(o, list);
                } else if (Set.class.equals(toBeCompositePom.propertyType) || HashSet.class.equals(toBeCompositePom.propertyType)) {
                    Set set = (Set) enhancer.create(Set.class, setCallback);
                    this.toBeCompositePom.setProperty(o, set);
                } else {
                    List list = (List) enhancer.create(List.class, listCallback);
                    if(list != null && list.size() > 0) {
                        this.toBeCompositePom.setProperty(o, list.get(0));
                    }
                }
            }
        }
    }

    public String getParamName() {
        return paramName;
    }

    @Override
    public int compareTo(CompositeField o) {
        if(o == null) {
            return -1;
        }
        return Integer.compare(this.order, o.order);
    }

    public Class<?> getParamType() {
        return this.paramType;
    }

    public void setAssembler(Composite<?, ?> composite) {
        this.composite = composite;
    }

    /**
     * property or method
     */
    static class POM {

        private int fieldIndex = -1, getIndex = -1, setIndex = -1;

        public Class<?> propertyType;

        private final FieldAccess fieldAccess;

        private final MethodAccess methodAccess;

        public POM(FieldAccess fieldAccess, MethodAccess methodAccess, String property) {
            this.fieldAccess = fieldAccess;
            this.methodAccess = methodAccess;
            try {
                this.fieldIndex = this.fieldAccess.getIndex(property);
                this.propertyType = this.fieldAccess.getFieldTypes()[fieldIndex];
            } catch (Exception ignore) {
                String propertyMethod = property.substring(0, 1).toUpperCase() + property.substring(1);
                try {
                    getIndex = this.methodAccess.getIndex("get" + propertyMethod);
                    this.propertyType = this.methodAccess.getReturnTypes()[getIndex];
                } catch (Exception ignored) {
                }
                try {
                    setIndex = this.methodAccess.getIndex("set" + propertyMethod);
                    this.propertyType = this.methodAccess.getParameterTypes()[setIndex][0];
                } catch (Exception ignored) {
                }
                if(getIndex < 0 && setIndex < 0) {
                    throw new CompositeException("cannot access property " + property);
                }
            }
        }

        public Object getProperty(Object target) {
            if(fieldIndex >= 0) {
                return fieldAccess.get(target, fieldIndex);
            } else {
                return methodAccess.invoke(target, getIndex);
            }
        }

        public void setProperty(Object target, Object value) {
            if(fieldIndex >= 0) {
                fieldAccess.set(target, fieldIndex, value);
            } else {
                methodAccess.invoke(target, setIndex, value);
            }
        }
    }
}
