package com.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lostfound.vo.AuditLogVO;

public interface AuditLogService {

    IPage<AuditLogVO> list(String action, String targetType, String keyword, long page, long size);
}
