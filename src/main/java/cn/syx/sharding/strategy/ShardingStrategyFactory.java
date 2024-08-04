package cn.syx.sharding.strategy;

import cn.syx.sharding.strategy.impl.HashShardingStrategy;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

public class ShardingStrategyFactory {

    public static ShardingStrategy getShardingStrategy(Properties properties) {
        String type = properties.getProperty("type");
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type can not be null or blank text");
        }
        ShardingStrategy strategy;
        try {
            strategy = createShardingStrategy(type);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        Binder binder = new Binder(new MapConfigurationPropertySource(properties));
        binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(strategy));
        return strategy;
    }

    private static ShardingStrategy createShardingStrategy(String type) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        switch (type) {
            case "hash":
                return new HashShardingStrategy();
            default:
                Class<?> clazz = ClassUtils.forName(type, ClassUtils.getDefaultClassLoader());
                if (!clazz.isAssignableFrom(ShardingStrategy.class)) {
                    throw new IllegalArgumentException("type must be subclass of ShardingStrategy");
                }
                return (ShardingStrategy) clazz.getConstructor().newInstance();
        }
    }
}
