package com.scut.conference.mapper;

import com.scut.conference.entity.Paper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/** 论文表 Mapper */
public interface PaperMapper {

    /** 列 abstract 在 Java 中是关键字,统一用别名映射为 paperAbstract */
    String COLS = "id, title, title_en, abstract AS \"paperAbstract\", abstract_en, " +
            "keywords, keywords_en, topic_id, submitter_id, status, " +
            "file_path, final_file_path, review_plan_published, deleted_at, created_at, updated_at ";

    /** status 由调用方指定:DRAFT(存草稿,附件可空)或 SUBMITTED(正式投稿) */
    @Insert("INSERT INTO paper(title, title_en, abstract, abstract_en, keywords, keywords_en, " +
            "topic_id, submitter_id, status, file_path) " +
            "VALUES(#{title}, #{titleEn}, #{paperAbstract}, #{abstractEn}, #{keywords}, #{keywordsEn}, " +
            "#{topicId}, #{submitterId}, #{status}, #{filePath})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Paper paper);

    @Select("SELECT " + COLS + " FROM paper WHERE id = #{id} AND deleted_at IS NULL")
    Paper findById(Long id);

    /** 评审方案调整与发布共用论文行锁，避免发布后仍被并发请求改写。 */
    @Select("SELECT " + COLS + " FROM paper WHERE id = #{id} AND deleted_at IS NULL FOR UPDATE")
    Paper findByIdForUpdate(Long id);

    /**
     * 条件更新状态:WHERE 带上"期望的原状态"并返回影响行数。
     * 并发安全防线:两个请求同时到达时,只有第一个能匹配成功,第二个影响行数为 0,
     * Service 层据此判定"状态已被并发修改"并拒绝操作,避免重复流转。
     */
    @Update("UPDATE paper SET status = #{target}, updated_at = now() " +
            "WHERE id = #{id} AND status = #{expected} AND deleted_at IS NULL")
    int casUpdateStatus(@Param("id") Long id, @Param("expected") String expected, @Param("target") String target);

    @Update("UPDATE paper SET title = #{title}, title_en = #{titleEn}, " +
            "abstract = #{paperAbstract}, abstract_en = #{abstractEn}, " +
            "keywords = #{keywords}, keywords_en = #{keywordsEn}, " +
            "topic_id = #{topicId}, updated_at = now() " +
            "WHERE id = #{id} AND status IN ('DRAFT','SUBMITTED') AND deleted_at IS NULL")
    int updateDraftMeta(Paper paper);

    @Update("UPDATE paper SET review_plan_published = TRUE, updated_at = now() " +
            "WHERE id = #{id} AND status = 'REVIEWING' " +
            "AND review_plan_published = FALSE AND deleted_at IS NULL")
    int publishReviewPlan(Long id);

    @Update("UPDATE paper SET deleted_at = now(), updated_at = now() " +
            "WHERE id = #{id} AND submitter_id = #{submitterId} " +
            "AND status IN ('DRAFT','WITHDRAWN') AND deleted_at IS NULL")
    int moveToTrash(@Param("id") Long id, @Param("submitterId") Long submitterId);

    @Update("UPDATE paper SET deleted_at = NULL, updated_at = now() " +
            "WHERE id = #{id} AND submitter_id = #{submitterId} " +
            "AND status IN ('DRAFT','WITHDRAWN') AND deleted_at IS NOT NULL")
    int restoreFromTrash(@Param("id") Long id, @Param("submitterId") Long submitterId);

    @Update("UPDATE paper SET file_path = #{path}, updated_at = now() WHERE id = #{id}")
    int updateFilePath(@Param("id") Long id, @Param("path") String path);

    /** 终稿上传同样带状态条件,防止并发重复提交 */
    @Update("UPDATE paper SET final_file_path = #{path}, status = 'FINAL_SUBMITTED', updated_at = now() " +
            "WHERE id = #{id} AND status = 'ACCEPTED'")
    int updateFinalFile(@Param("id") Long id, @Param("path") String path);

    /** 管理后台论文列表:带投稿人、专题名、评审统计(分页) */
    @Select("<script>" +
            "SELECT p.id, p.title, p.status, p.keywords, " +
            "       p.review_plan_published AS \"reviewPlanPublished\", " +
            "       p.created_at AS \"createdAt\", " +
            "       u.real_name AS \"submitterName\", t.name AS \"topicName\", " +
            "       (SELECT COUNT(*) FROM review r WHERE r.paper_id = p.id) AS \"reviewCount\", " +
            "       (SELECT COUNT(*) FROM review r WHERE r.paper_id = p.id AND r.status = 'SUBMITTED') AS \"submittedCount\" " +
            "FROM paper p LEFT JOIN sys_user u ON u.id = p.submitter_id " +
            "             LEFT JOIN topic t ON t.id = p.topic_id " +
            "<where>p.deleted_at IS NULL" +
            "  <if test='status == \"REVIEW_RETURNED\"'>" +
            "    AND p.status = 'REVIEWING' AND p.review_plan_published = TRUE " +
            "    AND EXISTS (SELECT 1 FROM review r WHERE r.paper_id = p.id) " +
            "    AND NOT EXISTS (SELECT 1 FROM review r WHERE r.paper_id = p.id AND r.status = 'PENDING')" +
            "  </if>" +
            "  <if test='status != null and status != \"\" and status != \"REVIEW_RETURNED\"'>" +
            "    AND p.status = #{status}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (p.title LIKE '%'||#{keyword}||'%' OR u.real_name LIKE '%'||#{keyword}||'%'" +
            "        OR p.search_vector @@ plainto_tsquery('simple', #{keyword}))" +
            "  </if>" +
            "</where>" +
            " ORDER BY p.id DESC LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<Map<String, Object>> adminList(@Param("status") String status, @Param("keyword") String keyword,
                                         @Param("offset") int offset, @Param("size") int size);

    /** 管理后台论文总数(与 adminList 同条件) */
    @Select("<script>" +
            "SELECT COUNT(*) FROM paper p LEFT JOIN sys_user u ON u.id = p.submitter_id " +
            "<where>p.deleted_at IS NULL" +
            "  <if test='status == \"REVIEW_RETURNED\"'>" +
            "    AND p.status = 'REVIEWING' AND p.review_plan_published = TRUE " +
            "    AND EXISTS (SELECT 1 FROM review r WHERE r.paper_id = p.id) " +
            "    AND NOT EXISTS (SELECT 1 FROM review r WHERE r.paper_id = p.id AND r.status = 'PENDING')" +
            "  </if>" +
            "  <if test='status != null and status != \"\" and status != \"REVIEW_RETURNED\"'>" +
            "    AND p.status = #{status}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (p.title LIKE '%'||#{keyword}||'%' OR u.real_name LIKE '%'||#{keyword}||'%'" +
            "        OR p.search_vector @@ plainto_tsquery('simple', #{keyword}))" +
            "  </if>" +
            "</where>" +
            "</script>")
    long countAdminList(@Param("status") String status, @Param("keyword") String keyword);

    /** 前台"录用论文"页:聚合作者名单(string_agg 按署名顺序拼接) */
    @Select("SELECT p.id, p.title, p.keywords, p.status, t.name AS \"topicName\", " +
            "(SELECT string_agg(a.name, '、' ORDER BY a.sort_no) FROM paper_author a WHERE a.paper_id = p.id) AS \"authors\" " +
            "FROM paper p LEFT JOIN topic t ON t.id = p.topic_id " +
            "WHERE p.status IN ('ACCEPTED','FINAL_SUBMITTED') AND p.deleted_at IS NULL ORDER BY p.id DESC")
    List<Map<String, Object>> listAccepted();

    /** 作者"我的投稿"列表 */
    @Select("SELECT p.id, p.title, p.status, p.keywords, p.created_at AS \"createdAt\", " +
            "p.review_plan_published AS \"reviewPlanPublished\", " +
            "p.file_path IS NOT NULL AS \"hasMainFile\", t.name AS \"topicName\", " +
            "(SELECT string_agg(a.name, '、' ORDER BY a.sort_no) FROM paper_author a " +
            "WHERE a.paper_id = p.id) AS \"authors\", " +
            "(SELECT COUNT(*) FROM review r WHERE r.paper_id = p.id) AS \"reviewCount\", " +
            "(SELECT COUNT(*) FROM review r WHERE r.paper_id = p.id " +
            "AND r.status = 'SUBMITTED') AS \"submittedCount\" " +
            "FROM paper p LEFT JOIN topic t ON t.id = p.topic_id " +
            "WHERE p.submitter_id = #{submitterId} AND p.deleted_at IS NULL ORDER BY p.id DESC")
    List<Map<String, Object>> listBySubmitter(Long submitterId);

    @Select("SELECT id, title, status, deleted_at AS \"deletedAt\" FROM paper " +
            "WHERE submitter_id = #{submitterId} AND deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    List<Map<String, Object>> listDeletedBySubmitter(Long submitterId);

    @Select("SELECT COUNT(*) FROM paper WHERE topic_id = #{topicId}")
    int countByTopic(Long topicId);

    @Select("SELECT COUNT(*) FROM paper WHERE deleted_at IS NULL")
    int countAll();
}
