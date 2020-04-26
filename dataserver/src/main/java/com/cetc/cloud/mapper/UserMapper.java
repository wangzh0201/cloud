package com.cetc.cloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cetc.cloud.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * Created by wangzh on 2020/4/1.
 */
@Repository
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE account=#{account} AND deleted=0 LIMIT 1")
    @Results({
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "modifyTime", column = "modify_time")
    })
    User getUserByAccount(@Param("account") String account);
}
