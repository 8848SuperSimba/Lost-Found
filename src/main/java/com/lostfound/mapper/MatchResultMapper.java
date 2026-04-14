package com.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lostfound.entity.MatchResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MatchResultMapper extends BaseMapper<MatchResult> {

    boolean insertOrUpdate(@Param("matchResult") MatchResult matchResult);
}
