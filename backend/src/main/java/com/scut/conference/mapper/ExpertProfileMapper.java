package com.scut.conference.mapper;

import com.scut.conference.entity.ExpertProfile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/** 专家档案表 Mapper */
public interface ExpertProfileMapper {

    // openGauss 驱动对 COALESCE 中的布尔参数会推断成 smallint,必须 CAST 钉死类型
    @Insert("INSERT INTO expert_profile(user_id, title, affiliation, bio, research_keywords, photo_url, " +
            "address, website, review_topics, review_capacity, review_enabled, is_keynote) " +
            "VALUES(#{userId}, #{title}, #{affiliation}, #{bio}, #{researchKeywords}, #{photoUrl}, " +
            "#{address}, #{website}, #{reviewTopics}, COALESCE(#{reviewCapacity}, 0), " +
            "COALESCE(CAST(#{reviewEnabled} AS boolean), true), COALESCE(CAST(#{isKeynote} AS boolean), false))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExpertProfile profile);

    @Select("SELECT * FROM expert_profile WHERE user_id = #{userId}")
    ExpertProfile findByUserId(Long userId);

    @Select("SELECT * FROM expert_profile WHERE id = #{id}")
    ExpertProfile findById(Long id);

    @Update("UPDATE expert_profile SET title = COALESCE(#{title}, title), " +
            "affiliation = COALESCE(#{affiliation}, affiliation), bio = COALESCE(#{bio}, bio), " +
            "research_keywords = COALESCE(#{researchKeywords}, research_keywords), " +
            "address = COALESCE(#{address}, address), website = COALESCE(#{website}, website), " +
            "review_topics = COALESCE(#{reviewTopics}, review_topics), " +
            "review_capacity = COALESCE(#{reviewCapacity}, review_capacity), " +
            "review_enabled = COALESCE(CAST(#{reviewEnabled} AS boolean), review_enabled), " +
            "is_keynote = COALESCE(CAST(#{isKeynote} AS boolean), is_keynote) WHERE user_id = #{userId}")
    int updateByUserId(ExpertProfile profile);

    /** 前台"专家介绍"页数据 */
    @Select("SELECT e.user_id AS \"userId\", u.real_name AS \"realName\", u.gender, e.title, e.affiliation, " +
            "e.research_keywords AS \"researchKeywords\", e.bio, e.website, " +
            "e.review_topics AS \"reviewTopics\", e.is_keynote AS \"isKeynote\" " +
            "FROM expert_profile e JOIN sys_user u ON u.id = e.user_id " +
            "WHERE u.status = 1 ORDER BY e.id")
    List<Map<String, Object>> listPublicExperts();

    /** 评审分配对话框的精简专家列表(profileId 供主旨报告关联) */
    @Select("SELECT e.id AS \"profileId\", e.user_id AS \"userId\", u.real_name AS \"realName\", " +
            "e.title, e.affiliation, e.research_keywords AS \"researchKeywords\", " +
            "e.review_enabled AS \"reviewEnabled\" " +
            "FROM expert_profile e JOIN sys_user u ON u.id = e.user_id " +
            "WHERE u.role = 'EXPERT' AND u.status = 1 ORDER BY e.id")
    List<Map<String, Object>> listExpertsLite();
}
