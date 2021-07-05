package com.huafon.common.auto.composite.mybatisplus;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huafon.common.auto.composite.handler.Composite;
import com.huafon.common.auto.composite.handler.Composites;
import com.huafon.common.auto.composite.util.TypeUtil;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MybatisPlusComposites implements InitializingBean {

    private static SqlSession sqlSession;

    private static final Map<Class<?>, Class<?>> mapperMap = new HashMap<>();

    public MybatisPlusComposites(SqlSession sqlSession) {
        MybatisPlusComposites.sqlSession = sqlSession;
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public boolean support(Class<?> returnType) {
        return mapperMap.get(returnType) != null;
    }

    public MybatisPlusComposite getComposite(String paramName, Composites.Pair pair) {
        if(support(pair.getReturnType())) {
            return new MybatisPlusComposite(pair.getParamType(), pair.getReturnType(), paramName);
        }
        return null;
    }

    @Override
    public void afterPropertiesSet() {
        Collection<Class<?>> mappers = sqlSession.getConfiguration().getMapperRegistry().getMappers();
        if(mappers != null && mappers.size() > 0) {
            mappers.forEach(mapper -> {
                try {
                    Class<?> entityClass = TypeUtil.getActualClass(mapper, BaseMapper.class, 0);
                    if (entityClass != null) {
                        mapperMap.put(entityClass, mapper);
                    }
                } catch (Exception e) {
                    logger.error("mybatis-plus auto composite failed, exception is {}", e.getMessage(), e);
                }
            });
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static class MybatisPlusComposite implements Composite {

        private final Class<?> paramType, returnType;

        private final String paramName;

        /**
         *
         * @param paramType
         * @param returnType
         * @param paramName
         */
        public MybatisPlusComposite(Class<?> paramType, Class<?> returnType, String paramName) {
            this.paramType = paramType;
            this.returnType = returnType;
            this.paramName = paramName;
        }

        @Override
        public List queryList(Object param) {
            BaseMapper baseMapper = (BaseMapper) sqlSession.getMapper(mapperMap.get(this.returnType));
            return baseMapper.selectList(Wrappers.query().eq(paramName, param));
        }

        @Override
        public Class<?> getReturnType() {
            return this.returnType;
        }

        @Override
        public Class<?> getParamType() {
            return this.paramType;
        }
    }
}
