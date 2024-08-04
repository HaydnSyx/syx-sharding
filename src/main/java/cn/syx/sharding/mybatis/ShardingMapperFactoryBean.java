package cn.syx.sharding.mybatis;

import cn.syx.sharding.domain.ShardingContext;
import cn.syx.sharding.engine.ShardingEngine;
import cn.syx.sharding.domain.ShardingResult;
import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

@Slf4j
@Data
public class ShardingMapperFactoryBean<T> extends MapperFactoryBean<T> {

    private ShardingEngine shardingEngine;

    public ShardingMapperFactoryBean() {
    }

    public ShardingMapperFactoryBean(Class<T> mapperInterface) {
        super(mapperInterface);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getObject() throws Exception {
        Object proxy = super.getObject();

        SqlSession session = getSqlSession();
        Configuration configuration = session.getConfiguration();
        Class<?> clazz = getMapperInterface();
        log.info("准备创建mapper的bean对象...");

        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, (o, method, args) -> {
//            log.info("===> start execute class:{}, method:{}, args:{}",
//                    method.getDeclaringClass(), method.getName(), JSON.toJSONString(args));
            if (method.getDeclaringClass() != Object.class) {
                String mapperId = getMapperId(clazz, method);
                MappedStatement statement = configuration.getMappedStatement(mapperId);
                BoundSql boundSql = statement.getBoundSql(args);
                String sql = boundSql.getSql();

                Object[] params = parseArgs(boundSql, args);

                ShardingResult result = shardingEngine.sharding(sql, params);
                log.info("===> sharding execute end, mapperId:{}, boundSql:{}, params:{} result:{}",
                        mapperId, sql, JSON.toJSONString(params), JSON.toJSONString(result));
                ShardingContext.setShardingResult(result);
            }
            return method.invoke(proxy, args);
        });
    }

    private Object[] parseArgs(BoundSql boundSql, Object[] args) {
        Object[] params = args;
        if (args.length == 1 && !ClassUtils.isPrimitiveOrWrapper(args[0].getClass())) {
            Object obj = args[0];
            List<String> columns = boundSql.getParameterMappings().stream().map(ParameterMapping::getProperty).toList();
            params = columns.stream().map(column -> {
                try {
                    Field field = obj.getClass().getDeclaredField(column);
                    field.setAccessible(true);
                    return field.get(obj);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }).toArray();
        }
        return params;
    }

    private String getMapperId(Class<?> clazz, Method method) {
        return clazz.getName() + "." + method.getName();
    }

}
