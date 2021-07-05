package com.huafon.common.auto.composite.mybatisplus;

import org.apache.ibatis.session.SqlSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnClass(name = {"com.baomidou.mybatisplus.core.mapper.BaseMapper", "org.apache.ibatis.session.SqlSession"})
@ConditionalOnProperty(name = "auto.composite.mybatis-plus-enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class MybatisPlusAutoCompositeConfiguration {

    @Bean
    public MybatisPlusComposites mybatisPlusComposites(SqlSession sqlSession) {
        return new MybatisPlusComposites(sqlSession);
    }
}
