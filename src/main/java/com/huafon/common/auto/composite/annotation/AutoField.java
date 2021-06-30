package com.huafon.common.auto.composite.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoField {

    /**
     * 当前对象的哪一个属性作为参数
     * 默认将当前对象作为参数传入
     */
    String property();

    /**
     * 当前对象作为参数时的参数名称，后续可以依据该参数名以决定执行哪种方法
     * @return 参数名称
     */
    String paramName();


    /**
     * 获取类型
     *
     * @return {@link FetchType}
     */
    FetchType fetchType() default FetchType.LAZY;

    /**
     * 订单
     *
     * @return int
     */
    int order() default 0;
}
