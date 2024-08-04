package cn.syx.sharding.strategy.impl;

import cn.syx.sharding.strategy.utils.InlineExpressionParser;
import cn.syx.sharding.strategy.ShardingStrategy;
import groovy.lang.Closure;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class HashShardingStrategy implements ShardingStrategy {

    private String shardingColumn;
    private String algorithmExpression;

    @Override
    public List<String> getShardingColumns() {
        return List.of(shardingColumn);
    }

    @Override
    public String doSharding(List<String> availableTargetNames, String logicTableName, Map<String, Object> shardingParams) {
        // 转义表达式
        String expression = InlineExpressionParser.handlePlaceHolder(algorithmExpression);
        InlineExpressionParser parser = new InlineExpressionParser(expression);
        // 执行解析
        Closure<?> closure = parser.evaluateClosure();
        closure.setProperty(shardingColumn, shardingParams.get(shardingColumn));
        return closure.call().toString();
    }
}
