package com.scut.conference.mapper;

import com.scut.conference.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/** 用户表 Mapper */
public interface UserMapper {

    /** status 由调用方显式指定:注册=2(待审核),管理员/种子创建=1(正常) */
    @Insert("INSERT INTO sys_user(username, password_hash, real_name, email, role, status, " +
            "gender, phone, work_unit, postal_code) " +
            "VALUES(#{username}, #{passwordHash}, #{realName}, #{email}, #{role}, #{status}, " +
            "#{gender}, #{phone}, #{workUnit}, #{postalCode})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE email = #{email}")
    SysUser findByEmail(String email);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser findById(Long id);

    /** 管理后台用户列表,LEFT JOIN 带出专家档案信息(Map 键为驼峰别名),分页 */
    @Select("<script>" +
            "SELECT u.id, u.username, u.real_name AS \"realName\", u.email, u.role, u.status, " +
            "       u.gender, u.phone, u.postal_code AS \"postalCode\", " +
            "       u.created_at AS \"createdAt\", e.title, e.affiliation, e.bio, e.address, e.website, " +
            "       e.research_keywords AS \"researchKeywords\", e.review_topics AS \"reviewTopics\", " +
            "       e.review_capacity AS \"reviewCapacity\", e.review_enabled AS \"reviewEnabled\", " +
            "       e.is_keynote AS \"isKeynote\" " +
            "FROM sys_user u LEFT JOIN expert_profile e ON e.user_id = u.id " +
            "<where>" +
            "  <if test='role != null and role != \"\"'>u.role = #{role}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (u.real_name LIKE '%'||#{keyword}||'%' OR u.username LIKE '%'||#{keyword}||'%' " +
            "         OR u.email LIKE '%'||#{keyword}||'%')" +
            "  </if>" +
            "</where>" +
            " ORDER BY u.id LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<Map<String, Object>> listUsers(@Param("role") String role, @Param("keyword") String keyword,
                                         @Param("offset") int offset, @Param("size") int size);

    /** 用户总数(与 listUsers 同条件) */
    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_user u " +
            "<where>" +
            "  <if test='role != null and role != \"\"'>u.role = #{role}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (u.real_name LIKE '%'||#{keyword}||'%' OR u.username LIKE '%'||#{keyword}||'%' " +
            "         OR u.email LIKE '%'||#{keyword}||'%')" +
            "  </if>" +
            "</where>" +
            "</script>")
    long countUsers(@Param("role") String role, @Param("keyword") String keyword);

    @Update("UPDATE sys_user SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE sys_user SET password_hash = #{passwordHash} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @Update("UPDATE sys_user SET real_name = COALESCE(#{realName}, real_name), email = COALESCE(#{email}, email) " +
            "WHERE id = #{id}")
    int updateBasic(@Param("id") Long id, @Param("realName") String realName, @Param("email") String email);

    @Update("UPDATE sys_user SET real_name = #{realName}, email = #{email}, gender = #{gender}, " +
            "education = #{education}, job_title = #{jobTitle}, phone = #{phone}, " +
            "work_unit = #{workUnit}, postal_code = #{postalCode} WHERE id = #{id}")
    int updateProfile(@Param("id") Long id, @Param("realName") String realName,
                      @Param("email") String email, @Param("gender") String gender,
                      @Param("education") String education, @Param("jobTitle") String jobTitle,
                      @Param("phone") String phone, @Param("workUnit") String workUnit,
                      @Param("postalCode") String postalCode);

    @Update("UPDATE sys_user SET gender = #{gender}, phone = #{phone}, postal_code = #{postalCode}, " +
            "work_unit = #{workUnit} WHERE id = #{id}")
    int updateExpertContact(@Param("id") Long id, @Param("gender") String gender,
                            @Param("phone") String phone, @Param("postalCode") String postalCode,
                            @Param("workUnit") String workUnit);

    @Select("SELECT COUNT(*) FROM sys_user")
    int countAll();
}
