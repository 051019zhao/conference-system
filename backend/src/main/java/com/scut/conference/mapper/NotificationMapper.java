package com.scut.conference.mapper;

import com.scut.conference.entity.Notification;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 通知公告表 Mapper */
public interface NotificationMapper {

    @Insert("INSERT INTO notification(title, content, expires_at, publisher, last_saved_at) " +
            "VALUES(#{title}, #{content}, #{expiresAt}, #{publisher}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    @Update("UPDATE notification SET title = #{title}, content = #{content}, expires_at = #{expiresAt}, " +
            "publisher = #{publisher}, last_saved_at = now() WHERE id = #{id}")
    int update(Notification notification);

    @Delete("DELETE FROM notification WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE notification SET is_published = true, publish_time = now() WHERE id = #{id}")
    int publish(Long id);

    @Update("UPDATE notification SET pdf_path = #{path}, last_saved_at = now() WHERE id = #{id}")
    int updatePdf(@Param("id") Long id, @Param("path") String path);

    @Select("SELECT * FROM notification WHERE id = #{id}")
    Notification findById(Long id);

    /** 前台只展示已发布通知 */
    @Select("SELECT * FROM notification WHERE is_published = true " +
            "AND (expires_at IS NULL OR expires_at > now()) ORDER BY publish_time DESC")
    List<Notification> findPublished();

    @Select("SELECT * FROM notification WHERE id = #{id} AND is_published = true " +
            "AND (expires_at IS NULL OR expires_at > now())")
    Notification findPublishedById(Long id);

    /** 后台管理列表:含草稿 */
    @Select("SELECT * FROM notification ORDER BY id DESC")
    List<Notification> findAll();
}
