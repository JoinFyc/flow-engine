package com.wflow.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.wflow.workflow.config.WflowGlobalVarDef;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : willian fu
 * @date : 2022/9/20
 */
@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataSourceProperties dataSourceProperties){
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        //添加分页插件
        if (dataSourceProperties.getDriverClassName().contains(DbType.MYSQL.getDb())) {
            WflowGlobalVarDef.DB_TYPE = DbType.MYSQL;
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        } else if (dataSourceProperties.getDriverClassName().contains(DbType.ORACLE.getDb())) {
            WflowGlobalVarDef.DB_TYPE = DbType.ORACLE;
//            ORACLE("oracle", "Oracle11g及以下数据库(高版本推荐使用ORACLE_NEW)"),
//            ORACLE_12C("oracle12c", "Oracle12c+数据库"),
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.ORACLE));
        }
        return interceptor;
    }
}
