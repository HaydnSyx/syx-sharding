package cn.syx.sharding.domain;

public class ShardingContext {

    private static final ThreadLocal<ShardingResult> LOCAL = new ThreadLocal<>();

    public static void setShardingResult(ShardingResult shardingResult) {
        LOCAL.set(shardingResult);
    }

    public static ShardingResult getShardingResult() {
        return LOCAL.get();
    }

    public static void clear() {
        LOCAL.remove();
    }
}
