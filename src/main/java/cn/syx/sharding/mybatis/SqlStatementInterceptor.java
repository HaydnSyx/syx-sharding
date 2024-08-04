package cn.syx.sharding.mybatis;

import cn.syx.sharding.domain.ShardingContext;
import cn.syx.sharding.domain.ShardingResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.springframework.objenesis.instantiator.util.UnsafeUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

@Slf4j
@Intercepts({
        @org.apache.ibatis.plugin.Signature(
                type = StatementHandler.class,
                method = "prepare",
                args = {java.sql.Connection.class, Integer.class}
        ),
})
public class SqlStatementInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        ShardingResult result = ShardingContext.getShardingResult();
        if (result == null) {
            return invocation.proceed();
        }

        StatementHandler handler = (StatementHandler) invocation.getTarget();

        BoundSql boundSql = handler.getBoundSql();
        if (boundSql.getSql().equals(result.getTargetSqlString())) {
            return invocation.proceed();
        }

        // 更改实际的执行sql
        Field field = boundSql.getClass().getDeclaredField("sql");
        Unsafe unsafe = UnsafeUtils.getUnsafe();
        long fieldOffset = unsafe.objectFieldOffset(field);
        unsafe.putObject(boundSql, fieldOffset, result.getTargetSqlString());

        return invocation.proceed();
    }

}
