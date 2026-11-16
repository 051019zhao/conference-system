package com.scut.conference.mapper;

import com.scut.conference.entity.Review;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/** 评审表 Mapper */
public interface ReviewMapper {

    /** 评审表五维评分列的查询别名(带双引号保住驼峰命名;尾部带逗号供拼接) */
    String SCORE_COLS = "r.score_fit AS \"scoreFit\", r.score_innovation AS \"scoreInnovation\", " +
            "r.score_complete AS \"scoreComplete\", r.score_depth AS \"scoreDepth\", " +
            "r.score_clarity AS \"scoreClarity\", ";

    @Insert("<script>" +
            "INSERT INTO review(paper_id, expert_id) VALUES " +
            "<foreach collection='expertIds' item='eid' separator=','>(#{paperId}, #{eid})</foreach>" +
            "</script>")
    int insertBatch(@Param("paperId") Long paperId, @Param("expertIds") List<Long> expertIds);

    /** 重新分配前,先删除尚未提交的评审(PENDING),已提交的保留 */
    @Delete("DELETE FROM review WHERE paper_id = #{paperId} AND status = 'PENDING'")
    int deletePendingByPaperId(Long paperId);

    @Select("SELECT * FROM review WHERE id = #{id}")
    Review findById(Long id);

    @Select("SELECT * FROM review WHERE paper_id = #{paperId} AND expert_id = #{expertId}")
    Review findByPaperAndExpert(@Param("paperId") Long paperId, @Param("expertId") Long expertId);

    @Select("SELECT COUNT(*) FROM review WHERE paper_id = #{paperId}")
    int countByPaperId(Long paperId);

    @Select("SELECT COUNT(*) FROM review WHERE paper_id = #{paperId} AND status = 'SUBMITTED'")
    int countSubmittedByPaperId(Long paperId);

    /**
     * 调用数据库函数 fn_match_experts:关键词匹配/回避/去重规则在 openGauss 库内原子完成,
     * 这里只取匹配度前 N 位。展示 plpgsql 函数 + CTE + 返回表的数据库编程能力。
     */
    @Select("SELECT o_user_id AS \"userId\", o_real_name AS \"realName\", " +
            "o_affiliation AS \"affiliation\", o_keywords AS \"researchKeywords\", o_score " +
            "FROM fn_match_experts(#{paperId}) m JOIN expert_profile e ON e.user_id = m.o_user_id " +
            "JOIN paper p ON p.id = #{paperId} LEFT JOIN topic t ON t.id = p.topic_id " +
            "WHERE o_score > 0 AND e.review_enabled = true " +
            "AND (e.review_topics IS NULL OR btrim(e.review_topics) = '' OR EXISTS (" +
            "SELECT 1 FROM unnest(string_to_array(replace(e.review_topics, '，', ','), ',')) " +
            "AS allowed_topic(topic_name) WHERE lower(btrim(topic_name)) = lower(t.name))) " +
            "AND (e.review_capacity IS NULL OR e.review_capacity = 0 OR " +
            "(SELECT COUNT(*) FROM review r WHERE r.expert_id = e.user_id " +
            "AND r.paper_id <> #{paperId} AND r.status = 'PENDING') " +
            "< e.review_capacity) ORDER BY m.o_score DESC, m.o_user_id LIMIT #{limit}")
    List<Map<String, Object>> matchExperts(@Param("paperId") Long paperId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM review WHERE expert_id = #{expertId} AND status = 'PENDING'")
    int countPendingByExpert(Long expertId);

    @Select("SELECT COUNT(*) FROM review WHERE expert_id = #{expertId} " +
            "AND paper_id <> #{paperId} AND status = 'PENDING'")
    int countPendingByExpertExcludingPaper(@Param("expertId") Long expertId,
                                            @Param("paperId") Long paperId);

    @Select("SELECT EXISTS (SELECT 1 FROM expert_profile e JOIN paper p ON p.id = #{paperId} " +
            "LEFT JOIN topic t ON t.id = p.topic_id WHERE e.user_id = #{expertId} " +
            "AND (e.review_topics IS NULL OR btrim(e.review_topics) = '' OR EXISTS (" +
            "SELECT 1 FROM unnest(string_to_array(replace(e.review_topics, '，', ','), ',')) " +
            "AS allowed_topic(topic_name) WHERE lower(btrim(topic_name)) = lower(t.name))))")
    boolean supportsTopic(@Param("expertId") Long expertId, @Param("paperId") Long paperId);

    /** 专家的评审任务列表(带论文信息与自己的评分) */
    @Select("SELECT r.id AS \"reviewId\", r.status AS \"myStatus\", r.result AS \"result\", " +
            SCORE_COLS +
            "p.id AS \"paperId\", p.title AS \"title\", p.status AS \"paperStatus\", p.keywords AS \"keywords\", " +
            "p.created_at AS \"createdAt\" " +
            "FROM review r JOIN paper p ON p.id = r.paper_id " +
            "WHERE r.expert_id = #{expertId} AND p.review_plan_published = TRUE " +
            "AND p.deleted_at IS NULL ORDER BY r.id DESC")
    List<Map<String, Object>> findTasksByExpertId(Long expertId);

    /** 论文详情页的评审列表(带专家姓名与五维评分) */
    @Select("SELECT r.id AS \"id\", r.expert_id AS \"expertId\", r.status AS \"status\", " +
            "r.overall_comment AS \"overallComment\", r.revision_comment AS \"revisionComment\", " +
            SCORE_COLS +
            "r.result AS \"result\", r.recommend_journal AS \"recommendJournal\", " +
            "r.rubric_json AS \"rubricJson\", r.recommendation_grade AS \"recommendationGrade\", " +
            "r.rejection_reason AS \"rejectionReason\", " +
            "r.submitted_at AS \"submittedAt\", u.real_name AS \"expertName\" " +
            "FROM review r JOIN sys_user u ON u.id = r.expert_id " +
            "WHERE r.paper_id = #{paperId} ORDER BY r.id")
    List<Map<String, Object>> findWithExpertByPaperId(Long paperId);

    /**
     * 提交评审:WHERE 限定 status='PENDING',并发/重复提交时影响行数为 0,
     * Service 层据此拒绝,保证一条评审只能提交一次。
     */
    @Update("UPDATE review SET status = 'SUBMITTED', overall_comment = #{overallComment}, " +
            "revision_comment = #{revisionComment}, result = #{result}, " +
            "score_fit = #{scoreFit}, score_innovation = #{scoreInnovation}, " +
            "score_complete = #{scoreComplete}, score_depth = #{scoreDepth}, " +
            "score_clarity = #{scoreClarity}, " +
            "recommend_journal = #{recommendJournal}, rubric_json = #{rubricJson}, " +
            "recommendation_grade = #{recommendationGrade}, rejection_reason = #{rejectionReason}, " +
            "submitted_at = now() " +
            "WHERE id = #{id} AND status = 'PENDING' " +
            "AND EXISTS (SELECT 1 FROM paper p WHERE p.id = review.paper_id " +
            "AND p.review_plan_published = TRUE AND p.status = 'REVIEWING')")
    int submit(@Param("id") Long id, @Param("overallComment") String overallComment,
               @Param("revisionComment") String revisionComment, @Param("result") String result,
               @Param("scoreFit") Integer scoreFit, @Param("scoreInnovation") Integer scoreInnovation,
               @Param("scoreComplete") Integer scoreComplete, @Param("scoreDepth") Integer scoreDepth,
               @Param("scoreClarity") Integer scoreClarity,
               @Param("recommendJournal") boolean recommendJournal,
               @Param("rubricJson") String rubricJson,
               @Param("recommendationGrade") String recommendationGrade,
               @Param("rejectionReason") String rejectionReason);
}
