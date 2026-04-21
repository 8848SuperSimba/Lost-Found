package com.lostfound.service;

import com.lostfound.vo.AreaStatVO;
import com.lostfound.vo.CategoryStatVO;
import com.lostfound.vo.OverviewVO;
import com.lostfound.vo.TrendStatVO;
import com.lostfound.vo.UserStatVO;
import java.util.List;

public interface StatsService {

    OverviewVO getOverview();

    List<CategoryStatVO> getCategoryStats(String postType, Integer days);

    List<AreaStatVO> getAreaStats(String postType, Integer days);

    List<TrendStatVO> getTrend(String postType, Integer days);

    UserStatVO getUserStats();
}
