package com.scut.conference.mapper;

import com.scut.conference.entity.OperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 操作审计日志 Mapper */
public interface OperationLogMapper {

    @Insert("INSERT INTO operation_log(user_id, username, role, action, detail, ip) " +
            "VALUES(#{userId}, #{username}, #{role}, #{action}, #{detail}, #{ip})")
    int insert(OperationLog log);

    /** 管理端分页查询(按用户名/动作筛选) */
    @Select("<script>" +
            "SELECT id, user_id AS \"userId\", username, role, action, detail, ip, " +
            "       created_at AS \"createdAt\" FROM operation_log " +
            "<where>" +
            "  <if test='username != null and username != \"\"'>username LIKE '%'||#{username}||'%'</if>" +
            "  <if test='action != null and action != \"\"'>AND action = #{action}</if>" +
            "</where>" +
            " ORDER BY id DESC LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<Map<String, Object>> listLogs(@Param("username") String username, @Param("action") String action,
                                        @Param("offset") int offset, @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM operation_log " +
            "<where>" +
            "  <if test='username != null and username != \"\"'>username LIKE '%'||#{username}||'%'</if>" +
            "  <if test='action != null and action != \"\"'>AND action = #{action}</if>" +
            "</where>" +
            "</script>")
    long countLogs(@Param("username") String username, @Param("action") String action);
}
