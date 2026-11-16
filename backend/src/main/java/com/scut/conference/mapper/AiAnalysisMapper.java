package com.scut.conference.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

/** AI 分析任务账本 Mapper */
public interface AiAnalysisMapper {

    @Insert("INSERT INTO ai_analysis(biz_type, biz_id, status, model, created_by) " +
            "VALUES(#{bizType}, #{bizId}, 'PENDING', #{model}, #{createdBy})")
    int insertTask(@Param("bizType") String bizType, @Param("bizId") Long bizId,
                   @Param("model") String model, @Param("createdBy") Long createdBy);

    /** 生成结束(成功或失败)更新结果 */
    @Update("UPDATE ai_analysis SET status = #{status}, content = #{content}, error = #{error}, " +
            "finished_at = now() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("content") String content, @Param("error") String error);

    /** 某业务对象最新一份分析(详情页展示;未生成过返回 null) */
    @Select("SELECT id, biz_type AS \"bizType\", biz_id AS \"bizId\", status, model, " +
            "content, error, created_at AS \"createdAt\", finished_at AS \"finishedAt\" " +
            "FROM ai_analysis WHERE biz_type = #{bizType} AND biz_id = #{bizId} " +
            "ORDER BY id DESC LIMIT 1")
    Map<String, Object> findLatest(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    @Select("SELECT COUNT(*) FROM ai_analysis WHERE biz_type = #{bizType} AND biz_id = #{bizId} " +
            "AND status IN ('PENDING','RUNNING')")
    int countRunning(@Param("bizType") String bizType, @Param("bizId") Long bizId);
}
