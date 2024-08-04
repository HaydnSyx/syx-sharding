package cn.syx.sharding.engine;

import cn.syx.sharding.domain.ShardingResult;

public interface ShardingEngine {

    ShardingResult sharding(String sql, Object[] args);
}
