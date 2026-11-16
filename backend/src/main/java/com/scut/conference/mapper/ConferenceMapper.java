package com.scut.conference.mapper;

import com.scut.conference.entity.ConferenceInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 会议信息表 Mapper(单行,id=1) */
public interface ConferenceMapper {

    @Select("SELECT * FROM conference_info WHERE id = 1")
    ConferenceInfo find();

    @Update("<script>" +
            "UPDATE conference_info" +
            "<set>" +
            "  <if test='c.name != null'>name = #{c.name},</if>" +
            "  <if test='c.shortName != null'>short_name = #{c.shortName},</if>" +
            "  <if test='c.venue != null'>venue = #{c.venue},</if>" +
            "  <if test='c.startDate != null'>start_date = #{c.startDate},</if>" +
            "  <if test='c.endDate != null'>end_date = #{c.endDate},</if>" +
            "  <if test='c.submitDeadline != null'>submit_deadline = #{c.submitDeadline},</if>" +
            "  <if test='c.notifyDeadline != null'>notify_deadline = #{c.notifyDeadline},</if>" +
            "  <if test='c.finalDeadline != null'>final_deadline = #{c.finalDeadline},</if>" +
            "  <if test='c.intro != null'>intro = #{c.intro},</if>" +
            "  <if test='c.cfpText != null'>cfp_text = #{c.cfpText},</if>" +
            "  <if test='c.contactEmail != null'>contact_email = #{c.contactEmail},</if>" +
            "  <if test='c.organizerName != null'>organizer_name = #{c.organizerName},</if>" +
            "  <if test='c.organizerUrl != null'>organizer_url = #{c.organizerUrl},</if>" +
            "  <if test='c.organizerLogoUrl != null'>organizer_logo_url = #{c.organizerLogoUrl},</if>" +
            "  <if test='c.coorganizerName != null'>coorganizer_name = #{c.coorganizerName},</if>" +
            "  <if test='c.coorganizerUrl != null'>coorganizer_url = #{c.coorganizerUrl},</if>" +
            "  <if test='c.coorganizerLogoUrl != null'>coorganizer_logo_url = #{c.coorganizerLogoUrl},</if>" +
            "</set>" +
            " WHERE id = 1" +
            "</script>")
    int update(@Param("c") ConferenceInfo info);
}
