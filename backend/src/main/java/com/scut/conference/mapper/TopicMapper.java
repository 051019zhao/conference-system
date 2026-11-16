package com.scut.conference.mapper;

import com.scut.conference.entity.Topic;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 征文专题表 Mapper */
public interface TopicMapper {

    @Insert("INSERT INTO topic(name, description, sort_no, report_limit, keynote_expert_id, " +
            "report_name, report_time_note) VALUES(#{name}, #{description}, COALESCE(#{sortNo}, 0), " +
            "#{reportLimit}, #{keynoteExpertId}, #{reportName}, #{reportTimeNote})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Topic topic);

    @Select("SELECT t.*, u.real_name AS \"keynoteExpertName\" FROM topic t " +
            "LEFT JOIN sys_user u ON u.id = t.keynote_expert_id ORDER BY t.sort_no, t.id")
    List<Topic> findAll();

    @Select("SELECT * FROM topic WHERE id = #{id}")
    Topic findById(Long id);

    @Update("UPDATE topic SET name = #{name}, description = #{description}, sort_no = #{sortNo}, " +
            "report_limit = #{reportLimit}, keynote_expert_id = #{keynoteExpertId}, " +
            "report_name = #{reportName}, report_time_note = #{reportTimeNote} WHERE id = #{id}")
    int update(Topic topic);

    @Update("UPDATE topic SET ppt_path = #{path} WHERE id = #{id}")
    int updatePpt(@Param("id") Long id, @Param("path") String path);

    @Delete("DELETE FROM topic WHERE id = #{id}")
    int deleteById(Long id);
}
