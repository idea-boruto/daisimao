package com.daisimao.repository;

import com.daisimao.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRepository extends BaseMapper<User> {

    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User selectByUsername(String username);
}
