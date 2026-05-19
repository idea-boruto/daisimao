package com.daisimao.repository;

import com.daisimao.model.entity.CreditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CreditLogRepository extends BaseMapper<CreditLog> {
}
