package com.huafon.common.auto.composite.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TypeUtil {

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

    /**
     * 递归寻找符合 parentClass 的所有父类、接口、接口的父类
     * @param c 待查找的实现类
     * @param parentClass 目标类
     * @param index 泛型在目标类里面的位置
     * @return 泛型类型
     */
    public static Class<?> getActualClass(Class<?> c, Class<?> parentClass, int index) {
        if(!parentClass.isAssignableFrom(c)) {
            return null;
        }
        Class<?> parent = c;
        do {
            Type[] types = parent.getGenericInterfaces();
            for (Type type : types) {
                if (type.getTypeName().startsWith(parentClass.getName())) {
                    Class<?>[] actualTypes = TypeUtil.getActualClass(type);
                    if (actualTypes != null && actualTypes.length > index) {
                        return actualTypes[index];
                    }
                }
            }
            Class<?>[] classes = parent.getInterfaces();
            if(classes.length > 0) {
                for(Class<?> i : classes) {
                    Class<?> ic = getActualClass(i, parentClass, index);
                    if(ic != null) {
                        return ic;
                    }
                }
            }
            parent = c.getSuperclass();
        } while (parent != null && !Object.class.equals(parent));
        return null;
    }
}
