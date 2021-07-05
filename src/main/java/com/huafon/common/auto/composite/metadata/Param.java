package com.huafon.common.auto.composite.metadata;

import com.huafon.common.auto.composite.exception.CompositeException;
import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;

import static org.springframework.asm.Opcodes.*;

/**
 * 用于提供一个返回值来源于多种参数类型，但是这些类型又无法抽取为同一个 抽象，并且依据不同类型， 返回值查询逻辑也不同的解决方案
 */
public abstract class Param {

    private static final String INNER_CLASS_NAME = "com/huafon/common/auto/composite/metadata/Param";

    private Object param;

    private Class<?> paramType;

    private String paramName;

    public Object getParam() {
        return param;
    }

    public Class<?> getParamType() {
        return paramType;
    }

    public String getParamName() {
        return paramName;
    }

    public static Param instance(Object param, String paramName) {
        if(param == null) {
            throw new CompositeException(new NullPointerException("param cannot be null"));
        }
        Class<?> type = param.getClass();
        boolean isInterface = type.isInterface();
        if (!isInterface && type.getSuperclass() == null && type != Object.class) {
            throw new IllegalArgumentException("The type must not be an interface, a primitive type, or void.");
        } else {
            String className = type.getName();
            String paramClassName = className + "Param";
            AccessClassLoader loader = AccessClassLoader.get(type);
            Class<?> paramClass;
            synchronized(loader) {
                paramClass = loader.loadAccessClass(paramClassName);
                if (paramClass == null) {
                    String paramClassNameInternal = paramClassName.replace('.', '/');
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    // 定义 Class 的名称和父类
                    cw.visit(V1_1, ACC_PUBLIC + ACC_SUPER, paramClassNameInternal, null, INNER_CLASS_NAME,
                            null);
                    // 添加一个无参构造函数
                    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                    mv.visitCode();
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESPECIAL, INNER_CLASS_NAME, "<init>", "()V", false);
                    mv.visitInsn(RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    cw.visitEnd();

                    byte[] classByte = cw.toByteArray();
                    paramClass = loader.defineAccessClass(paramClassName, classByte);
                }
            }

            try {
                Param result = (Param) paramClass.newInstance();
                result.param = param;
                result.paramType = param.getClass();
                result.paramName = paramName;
                return result;
            } catch (Exception e) {
                throw new CompositeException("Error constructing param class: " + className, e);
            }
        }
    }
}
