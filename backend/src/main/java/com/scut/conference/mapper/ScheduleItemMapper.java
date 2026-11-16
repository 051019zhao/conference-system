package com.scut.conference.mapper;

import com.scut.conference.entity.ScheduleItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 会议日程表 Mapper */
public interface ScheduleItemMapper {

    @Insert("INSERT INTO schedule_item(day, start_time, end_time, title, speaker, location, type, sort_no) " +
            "VALUES(#{day}, #{startTime}, #{endTime}, #{title}, #{speaker}, #{location}, COALESCE(#{type}, '常规'), COALESCE(#{sortNo}, 0))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScheduleItem item);

    @Update("UPDATE schedule_item SET day = #{day}, start_time = #{startTime}, end_time = #{endTime}, " +
            "title = #{title}, speaker = #{speaker}, location = #{location}, type = #{type}, sort_no = #{sortNo} " +
            "WHERE id = #{id}")
    int update(ScheduleItem item);

    @Delete("DELETE FROM schedule_item WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM schedule_item ORDER BY day, start_time, sort_no")
    List<ScheduleItem> findAll();
}
