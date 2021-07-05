package com.huafon.common.auto.composite.handler;

import com.huafon.common.auto.composite.selfreference.SelfReferenceBean;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 旨在提供一种自我调用的解决方案， 例如 query 方法默认用到了 queryList， 但是在默认实现中， 是通过 this 调用的。
 * 无法走到 spring MethodInterceptor中， 通过 self 方式可以实现例如缓存、事务、异步等等等
 * @param <P>
 * @param <R>
 */
public abstract class AbstractComposite<P, R> implements Composite<P, R>, SelfReferenceBean {

    protected Composite<P, R> self;

    @Override
    public R query(P param) {
        List<R> r = self.queryList(param);
        return r != null && r.size() > 0 ? r.get(0) : null;
    }

    @Override
    public Map<P, List<R>> batchQueryList(Collection<P> params) {
        Map<P, List<R>> result = new LinkedHashMap<>();
        for(P p : params) {
            List<R> list = self.queryList(p);
            if(list != null && list.size() > 0) {
                result.put(p, list);
            }
        }
        return result;
    }

    @Override
    public Map<P, R> batchQuery(Collection<P> params) {
        Map<P, R> result = new LinkedHashMap<>();
        Map<P, List<R>> map = self.batchQueryList(params);
        if(map != null && map.size() > 0) {
            map.forEach((k, v) -> {
                if (v != null && v.size() > 0) {
                    result.put(k, v.get(0));
                }
            });
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setSelf(Object bean) {
        this.self = (Composite<P, R>) bean;
    }
}
