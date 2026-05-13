package com.lostfound.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.service.AuditLogService;
import com.lostfound.vo.AuditLogVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final RowMapper<AuditLogVO> ROW_MAPPER = new RowMapper<>() {
        @Override
        public AuditLogVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp created = rs.getTimestamp("created_at");
            return AuditLogVO.builder()
                    .id(rs.getLong("id"))
                    .adminUserId(rs.getLong("admin_user_id"))
                    .adminUsername(rs.getString("admin_username"))
                    .adminNickname(rs.getString("admin_nickname"))
                    .action(rs.getString("action"))
                    .targetType(rs.getString("target_type"))
                    .targetId(rs.getObject("target_id", Long.class))
                    .detail(rs.getString("detail"))
                    .createdAt(created != null ? created.toLocalDateTime() : null)
                    .build();
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public AuditLogServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public IPage<AuditLogVO> list(String action, String targetType, String keyword, long page, long size) {
        String baseFrom =
                " FROM audit_log al LEFT JOIN `user` u ON u.id = al.admin_user_id WHERE 1=1";
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        if (StringUtils.hasText(action)) {
            where.append(" AND al.action = ?");
            params.add(action.trim());
        }
        if (StringUtils.hasText(targetType)) {
            where.append(" AND al.target_type = ?");
            params.add(targetType.trim());
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (al.detail LIKE ? OR al.action LIKE ? OR al.target_type LIKE ? OR u.username LIKE ? OR u.nickname LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        String countSql = "SELECT COUNT(*)" + baseFrom + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        if (total == null) {
            total = 0L;
        }

        long current = Math.max(page, 1);
        long pageSize = size < 1 ? 10 : Math.min(size, 100);
        long offset = (current - 1) * pageSize;

        String dataSql =
                "SELECT al.id, al.admin_user_id, al.action, al.target_type, al.target_id, al.detail, al.created_at,"
                        + " u.username AS admin_username, u.nickname AS admin_nickname"
                        + baseFrom
                        + where
                        + " ORDER BY al.created_at DESC LIMIT ? OFFSET ?";

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<AuditLogVO> records = jdbcTemplate.query(dataSql, ROW_MAPPER, dataParams.toArray());

        Page<AuditLogVO> result = new Page<>(current, pageSize, total);
        result.setRecords(records);
        return result;
    }
}
