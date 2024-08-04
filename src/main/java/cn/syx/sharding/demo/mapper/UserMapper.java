package cn.syx.sharding.demo.mapper;

import cn.syx.sharding.demo.model.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("select * from t_user where id = #{id}")
    User findById(int id);

    @Insert("insert into t_user(id, name, age) values (#{id}, #{name}, #{age})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("update t_user set name = #{name} where id = #{id}")
    int update(User user);

    @Delete("delete from t_user where id = #{id}")
    int delete(int id);
}
