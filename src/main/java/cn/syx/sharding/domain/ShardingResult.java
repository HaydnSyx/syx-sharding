package cn.syx.sharding.domain;

import lombok.Data;

@Data
public class ShardingResult {

    private String targetDataSourceName;
    private String targetSqlString;
    private Object[] parameters;
}
