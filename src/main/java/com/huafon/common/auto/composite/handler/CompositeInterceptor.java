package com.huafon.common.auto.composite.handler;

import com.huafon.common.auto.composite.annotation.AutoComposite;
import com.huafon.common.auto.composite.metadata.FetchType;
import com.huafon.common.auto.composite.metadata.CompositeField;
import com.huafon.common.auto.composite.util.TypeUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class CompositeInterceptor implements MethodInterceptor {

    private final Composites composites;

    public CompositeInterceptor(Composites composites) {
        this.composites = composites;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result = invocation.proceed();
        try {
            Method method = invocation.getMethod();
            // 获取返回值的类型
            Class<?> returnType = method.getReturnType();
            Type resultType = method.getGenericReturnType();
            Class<?> actualReturnType = null;
            if (Collection.class.isAssignableFrom(returnType)) {
                Class<?>[] _realResultType = TypeUtil.getActualClass(resultType);
                actualReturnType = _realResultType != null && _realResultType.length > 0 ? _realResultType[0] : null;
            } else if (Map.class.isAssignableFrom(returnType)) {
                Class<?>[] _realResultType = TypeUtil.getActualClass(resultType);
                actualReturnType = _realResultType != null && _realResultType.length > 1 ? _realResultType[1] : null;
            } else if (resultType.getTypeName().equals(returnType.getName())) {
                actualReturnType = returnType;
            }
            if (actualReturnType == null) {
                return result;
            }

            List<String> excludes = new ArrayList<>();
            FetchType defaultFetchType = FetchType.LAZY;
            AutoComposite autoComposite = AnnotationUtils.findAnnotation(method, AutoComposite.class);
            if (autoComposite == null) {
                autoComposite = AnnotationUtils.findAnnotation(method.getDeclaringClass(), AutoComposite.class);
            }
            if (autoComposite != null) {
                excludes.addAll(Arrays.asList(autoComposite.excludes()));
                defaultFetchType = autoComposite.fetchType();
            }

            final Class<?> _actualReturnType = actualReturnType;
            final FetchType _defaultFetchType = defaultFetchType;

            // 获取需要 组装(composite) 的属性
            List<CompositeField> compositeFields = Composites.COMPOSITE_MAP.computeIfAbsent(method, r -> {
                // 获取实际的属性类型， 这里仅处理 List 和 Set 的特殊情况
                return Arrays.stream(_actualReturnType.getDeclaredFields()).map(toBeComposite -> {
                    if (excludes.isEmpty() || excludes.stream().noneMatch(e -> e.equalsIgnoreCase(toBeComposite.getName()))) {
                        Class<?> actualCompositeType = this.getFieldActualType(toBeComposite);
                        if (!ignore(actualCompositeType)) {
                            CompositeField compositeField = new CompositeField(_actualReturnType, returnType, toBeComposite, _defaultFetchType);
                            Composite<?, ?> composite = Composites.getHandler(new Composites.Pair(compositeField.getParamType(), actualCompositeType));
                            if(composite == null && compositeField.getParamName() != null && !compositeField.getParamName().equals("")) {
                                composite = composites.getCommonHandler(compositeField.getParamName(), actualCompositeType);
                            }
                            if (composite != null) {
                                compositeField.setAssembler(composite);
                                return compositeField;
                            }
                        }
                    }
                    return null;
                }).filter(Objects::nonNull).sorted(CompositeField::compareTo).collect(Collectors.toList());
            });
            if(!compositeFields.isEmpty()) {
                compositeFields.forEach(c -> c.processComposite(result));
            }
        } catch (Throwable ignore) {
        }
        return result;
    }

    private boolean ignore(Class<?> fieldType) {
        return fieldType == null ||
                int.class.equals(fieldType) ||
                short.class.equals(fieldType) ||
                long.class.equals(fieldType) ||
                double.class.equals(fieldType) ||
                void.class.equals(fieldType) ||
                byte.class.equals(fieldType) ||
                float.class.equals(fieldType) ||
                boolean.class.equals(fieldType) ||
                fieldType.getName().startsWith("java");
    }

    private Class<?> getFieldActualType(Field field) {
        Class<?> clazz = field.getType();
        if(List.class.equals(clazz) || ArrayList.class.equals(clazz) || Set.class.equals(clazz) || HashSet.class.equals(clazz)) {
            Type type = field.getGenericType();
            Class<?>[] cc = TypeUtil.getActualClass(type);
            return cc != null && cc.length > 0 ? cc[0] : null;
        }
        return clazz;
    }
}
