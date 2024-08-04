package cn.syx.sharding.datasource;

import cn.syx.sharding.domain.ShardingContext;
import cn.syx.sharding.domain.ShardingResult;
import cn.syx.sharding.config.ShardingProperties;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class ShardingDataSource extends AbstractRoutingDataSource {

    public ShardingDataSource(ShardingProperties properties) {
        Map<Object, Object> dataSourceMap = new HashMap<>();

        DataSource[] defaultDataSource = {null};
        properties.getDatasources().forEach((k, v) -> {
            try {
                DataSource ds = DruidDataSourceFactory.createDataSource(v);
                if (defaultDataSource[0] == null) {
                    defaultDataSource[0] = ds;
                }
                dataSourceMap.put(k, ds);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        setTargetDataSources(dataSourceMap);
        setDefaultTargetDataSource(defaultDataSource[0]);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        ShardingResult result = ShardingContext.getShardingResult();
        return result == null ? null : result.getTargetDataSourceName();
    }
}
