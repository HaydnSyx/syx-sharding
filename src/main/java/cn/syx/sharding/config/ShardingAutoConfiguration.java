package cn.syx.sharding.config;

import cn.syx.sharding.datasource.ShardingDataSource;
import cn.syx.sharding.engine.ShardingEngine;
import cn.syx.sharding.engine.impl.StandardShardingEngine;
import cn.syx.sharding.mybatis.SqlStatementInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ShardingProperties.class})
public class ShardingAutoConfiguration {

    @Bean
    public ShardingDataSource shardingDataSource(ShardingProperties properties) {
        return new ShardingDataSource(properties);
    }

    @Bean
    public ShardingEngine shardingEngine(ShardingProperties shardingProperties) {
        return new StandardShardingEngine(shardingProperties);
    }

    @Bean
    public SqlStatementInterceptor sqlStatementInterceptor() {
        return new SqlStatementInterceptor();
    }
}
