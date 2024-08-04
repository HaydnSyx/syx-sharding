package cn.syx.sharding.demo;


import cn.syx.sharding.config.ShardingAutoConfiguration;
import cn.syx.sharding.demo.mapper.UserMapper;
import cn.syx.sharding.demo.model.User;
import cn.syx.sharding.mybatis.ShardingMapperFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ShardingAutoConfiguration.class})
@MapperScan(value = "cn.syx.sharding.demo.mapper", factoryBean = ShardingMapperFactoryBean.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Autowired
    private UserMapper mapper;

    @Bean
    ApplicationRunner runner() {
        return x -> {
            System.out.println("Run syx sharding test for mybatis CRUD...");

            /*int delete = mapper.delete(1);
            System.out.println("1. delete id = 1, result: " + delete);

            User u = new User();
            u.setId(1);
            u.setName("th");
            u.setAge(18);
            int th = mapper.insert(u);
            System.out.println("2. insert id = 1, name=th, result: " + th);

            u = mapper.findById(1);
            System.out.println("3. select/findById id=1, and name: " + u.getName());

            u = new User();
            u.setId(1);
            u.setName("syx");
            int syx = mapper.update(u);
            System.out.println("4. update id=1, name=syx, result: " + syx);

            u = mapper.findById(1);
            System.out.println("5. select/findById id=1, and name: " + u.getName());*/

            int num = 10;
            for (int i = 1; i <= num; i++) {
                System.out.println("============= id: " + i + "  start =============");

                User u = new User();
                u.setId(i);
                u.setName("th");
                u.setAge(18);
                int th = mapper.insert(u);
                System.out.println("2. insert result: " + th);

                u = mapper.findById(i);
                System.out.println("3. select/findById name: " + u.getName());

                u = new User();
                u.setId(i);
                u.setName("syx_" + String.format("%03d", i));
                int syx = mapper.update(u);
                System.out.println("4. update, result: " + syx);

                System.out.println("============= id: " + i + "  end =============");
            }

            /*User u = mapper.findById(9);
            System.out.println("3. select/findById name: " + u.getName());*/

            System.out.println("Run all test completely.");

        };
    }


//    // this is testing engine
//    @Autowired
//    ShardingProperties properties;
//
//    @Bean
//    ApplicationRunner runner() {
//        return x -> {
//            System.out.println("run...");
//            System.out.println(properties.toString());
//
//            String sql = "select * from t_user where id = ?";
//
//            ShardingEngine engine = new StandardShardingEngine(properties);
//            ShardingResult result = engine.sharding(sql, new Object[]{1});
//            System.out.println(result.toString());
//
//            result = engine.sharding(sql, new Object[]{2});
//            System.out.println(result.toString());
//
//        };
//    }
}
