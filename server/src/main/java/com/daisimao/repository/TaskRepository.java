package com.daisimao.repository;

import com.daisimao.model.entity.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskRepository extends BaseMapper<Task> {

    @Select("SELECT COUNT(*) FROM task WHERE acceptor_id = #{userId} AND status IN (2, 3) FOR UPDATE")
    long countActiveTasksForUpdate(Long userId);
}
