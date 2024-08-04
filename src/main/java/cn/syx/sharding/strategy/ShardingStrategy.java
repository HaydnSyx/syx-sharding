package cn.syx.sharding.strategy;

import java.util.List;
import java.util.Map;

public interface ShardingStrategy {

    List<String> getShardingColumns();

    String doSharding(List<String> availableTargetNames, String logicTableName, Map<String, Object> shardingParams);
}
