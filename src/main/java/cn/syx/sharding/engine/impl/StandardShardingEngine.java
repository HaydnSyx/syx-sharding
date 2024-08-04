package cn.syx.sharding.engine.impl;

import cn.syx.sharding.config.ShardingProperties;
import cn.syx.sharding.domain.ShardingResult;
import cn.syx.sharding.engine.ShardingEngine;
import cn.syx.sharding.strategy.ShardingStrategy;
import cn.syx.sharding.strategy.ShardingStrategyFactory;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

@Slf4j
public class StandardShardingEngine implements ShardingEngine {

    /**
     * <逻辑表名， List<实际数据库名>>
     */
    private final MultiValueMap<String, String> actaulDatabaseNames = new LinkedMultiValueMap<>();
    /**
     * <实际数据库名， List<实际表名>>
     */
    private final MultiValueMap<String, String> actaulTableNames = new LinkedMultiValueMap<>();

    private final Map<String, ShardingStrategy> databaseStrategys = new HashMap<>();
    private final Map<String, ShardingStrategy> tableStrategys = new HashMap<>();

    public StandardShardingEngine(ShardingProperties properties) {
        log.info("StandardShardingEngine parse properties: {}", JSON.toJSONString(properties));
        properties.getTables().forEach((table, tableProperties) -> {
            databaseStrategys.put(table, ShardingStrategyFactory.getShardingStrategy(tableProperties.getDatabaseStrategy()));
            tableStrategys.put(table, ShardingStrategyFactory.getShardingStrategy(tableProperties.getTableStrategy()));

            tableProperties.getActualDataNodes().forEach(actualDataNode -> {
                String[] split = actualDataNode.split("\\.");
                // 数据库名称（实际是该数据库对应的数据源配置名称）
                String databaseName = split[0];
                // 实际表名
                String tableName = split[1];

                actaulDatabaseNames.add(table, databaseName);
                actaulTableNames.add(databaseName, tableName);
            });
        });
        log.info("StandardShardingEngine init finish. actaulDatabaseNames:{}, actaulTableNames:{}",
                JSON.toJSONString(actaulDatabaseNames), JSON.toJSONString(actaulTableNames));
    }

    @Override
    public ShardingResult sharding(String sql, Object[] args) {
        SQLStatement statement = SQLUtils.parseSingleMysqlStatement(sql);

        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
        visitor.setParameters(Arrays.asList(args));
        statement.accept(visitor);

        boolean isInsertSql = statement instanceof SQLInsertStatement;
        List<SQLName> names = new ArrayList<>(new LinkedHashSet<>(visitor.getOriginalTables()));
        if (names.size() != 1) {
            throw new RuntimeException("not support multi table sharding");
        }
        String tableName = names.get(0).getSimpleName();

        // 库处理
        ShardingStrategy databaseStrategy = databaseStrategys.get(tableName);
        Map<String, Object> dbShardingColumsMap = isInsertSql
                ? findShardingColumns((SQLInsertStatement) statement, args)
                : findShardingColumns(visitor, databaseStrategy.getShardingColumns());
        // 根据逻辑表找实际库
        List<String> availableDatabaseNames = actaulDatabaseNames.get(tableName);
        String realDbName = databaseStrategy.doSharding(availableDatabaseNames, tableName, dbShardingColumsMap);
        if (!availableDatabaseNames.contains(realDbName)) {
            throw new RuntimeException("not found available database name");
        }

        // 表处理
        ShardingStrategy tableStrategy = tableStrategys.get(tableName);
        Map<String, Object> tbShardingColumsMap = isInsertSql
                ? findShardingColumns((SQLInsertStatement) statement, args)
                : findShardingColumns(visitor, tableStrategy.getShardingColumns());
        // 根据实际库找实际表
        List<String> availableTableNames = this.actaulTableNames.get(realDbName);
        String realTableName = tableStrategy.doSharding(availableTableNames, tableName, tbShardingColumsMap);
        if (!availableTableNames.contains(realTableName)) {
            throw new RuntimeException("not found available table name");
        }

        log.info("===> sharding calc result. target_db: {}, target_table: {}", realDbName, realTableName);

        ShardingResult result = new ShardingResult();
        result.setTargetDataSourceName(realDbName);
        result.setTargetSqlString(sql.replace(tableName, realTableName));
        result.setParameters(args);
        return result;
    }

    private Map<String, Object> findShardingColumns(SQLInsertStatement statement, Object[] args) {
        Map<String, Object> shardingColumnsMap = new HashMap<>();

        List<SQLExpr> columns = statement.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            SQLIdentifierExpr columnExpr = (SQLIdentifierExpr) columns.get(i);
            shardingColumnsMap.put(columnExpr.getSimpleName(), args[i]);
        }
        return shardingColumnsMap;
    }

    private Map<String, Object> findShardingColumns(MySqlSchemaStatVisitor visitor, List<String> shardingColumns) {
        Map<String, Object> shardingColumnsMap = new HashMap<>();
        for (TableStat.Condition condition : visitor.getConditions()) {
            if (shardingColumns.contains(condition.getColumn().getName())) {
                shardingColumnsMap.put(condition.getColumn().getName(), condition.getValues().get(0));
            }
        }
        return shardingColumnsMap;
    }
}
