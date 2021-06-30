package com.huafon.common.auto.composite.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

public abstract class CacheableComposite<P, R> implements Composite<P, R> {

    @Autowired
    private CacheManager cacheManager;


}
