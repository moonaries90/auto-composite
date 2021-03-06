package com.huafon.common.auto.composite.handler;

import com.huafon.common.auto.composite.util.TypeUtil;
import org.springframework.core.Ordered;

import java.util.*;

/**
 * 组装处理
 *
 * @author lji
 * @date 2021/06/22
 */
public interface Composite<P, R> extends Ordered {

    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 获取返回值的类型
     * 不支持匿名类实现， 匿名类需要手动指定
     * @return 返回值类型
     */
    default Class<?> getReturnType() {
        return TypeUtil.getActualClass(this.getClass(), Composite.class, 1);
    }

    /**
     * 获取参数的类型
     * 不支持匿名类实现， 匿名类需要手动指定
     * @return 参数类型
     */
    default Class<?> getParamType() {
        return TypeUtil.getActualClass(this.getClass(), Composite.class, 0);
    }

    /**
     * 查询单个列表， 多对一
     *
     * @param param 参数
     * @return {@link R}
     */
    List<R> queryList(P param);

    /**
     * 查询一个， 一对一
     *
     * @param param 参数
     * @return {@link R}
     */
    default R query(P param) {
        List<R> r = queryList(param);
        return r != null && r.size() > 0 ? r.get(0) : null;
    }

    /**
     * 查询批量， 多对一
     *
     * @param params 参数
     */
    default Map<P, List<R>> batchQueryList(Collection<P> params) {
        Map<P, List<R>> result = new LinkedHashMap<>();
        for(P p : params) {
            List<R> list = queryList(p);
            if(list != null && list.size() > 0) {
                result.put(p, list);
            }
        }
        return result;
    }

    /**
     * 查询批量， 一对一
     * @param params
     * @return
     */
    default Map<P, R> batchQuery(Collection<P> params) {
        Map<P, R> result = new LinkedHashMap<>();
        Map<P, List<R>> map = batchQueryList(params);
        if(map != null && map.size() > 0) {
            map.forEach((k, v) -> {
                if (v != null && v.size() > 0) {
                    result.put(k, v.get(0));
                }
            });
        }
        return result;
    }
}
