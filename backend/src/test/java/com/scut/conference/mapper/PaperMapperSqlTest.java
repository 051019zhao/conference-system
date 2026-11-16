package com.scut.conference.mapper;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperMapperSqlTest {
    private final Configuration configuration = new Configuration();

    PaperMapperSqlTest() {
        configuration.addMapper(PaperMapper.class);
    }

    @Test
    void returnedReviewFilterIsAppliedToBothListAndCount() {
        for (String method : new String[]{"adminList", "countAdminList"}) {
            String sql = sql(method, "REVIEW_RETURNED", "");
            assertTrue(sql.contains("p.review_plan_published = TRUE"));
            assertTrue(sql.contains("NOT EXISTS (SELECT 1 FROM review r"));
            assertFalse(sql.contains("p.status = ?"));
        }
    }

    @Test
    void keywordFilterMatchesBetweenListAndCount() {
        for (String method : new String[]{"adminList", "countAdminList"}) {
            String sql = sql(method, "SUBMITTED", "机器学习");
            assertTrue(sql.contains("p.status = ?"));
            assertTrue(sql.contains("p.search_vector @@ plainto_tsquery('simple', ?)"));
        }
    }

    @Test
    void reviewPlanMutationLocksPaperRow() {
        MappedStatement statement = configuration.getMappedStatement(
                PaperMapper.class.getName() + ".findByIdForUpdate");
        String sql = statement.getBoundSql(Map.of("id", 1L)).getSql();
        assertTrue(sql.contains("FOR UPDATE"));
    }

    private String sql(String method, String status, String keyword) {
        MappedStatement statement = configuration.getMappedStatement(PaperMapper.class.getName() + "." + method);
        BoundSql boundSql = statement.getBoundSql(Map.of(
                "status", status, "keyword", keyword, "offset", 0, "size", 10));
        return boundSql.getSql().replaceAll("\\s+", " ").trim();
    }
}
