package com.scut.conference.mapper;

import com.scut.conference.entity.KeynoteReport;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/** 主旨报告表 Mapper */
public interface KeynoteReportMapper {

    @Insert("INSERT INTO keynote_report(expert_id, title, abstract, time_note, sort_no) " +
            "VALUES(#{expertId}, #{title}, #{paperAbstract}, #{timeNote}, COALESCE(#{sortNo}, 0))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KeynoteReport report);

    @Update("UPDATE keynote_report SET expert_id = #{expertId}, title = #{title}, " +
            "abstract = #{paperAbstract}, time_note = #{timeNote}, sort_no = #{sortNo} WHERE id = #{id}")
    int update(KeynoteReport report);

    @Delete("DELETE FROM keynote_report WHERE id = #{id}")
    int deleteById(Long id);

    /** 前台/后台共用的列表,关联专家姓名职称 */
    @Select("SELECT k.id AS \"id\", k.expert_id AS \"expertId\", k.title AS \"title\", " +
            "k.abstract AS \"abstractText\", k.time_note AS \"timeNote\", k.sort_no AS \"sortNo\", " +
            "u.real_name AS \"expertName\", e.title AS \"expertTitle\", e.affiliation AS \"affiliation\" " +
            "FROM keynote_report k " +
            "JOIN expert_profile e ON e.id = k.expert_id " +
            "JOIN sys_user u ON u.id = e.user_id " +
            "ORDER BY k.sort_no, k.id")
    List<Map<String, Object>> listAll();
}
