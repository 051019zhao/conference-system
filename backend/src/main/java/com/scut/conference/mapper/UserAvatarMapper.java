package com.scut.conference.mapper;

import com.scut.conference.entity.UserAvatar;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/** 用户头像 Mapper(BYTEA 大对象) */
public interface UserAvatarMapper {

    /** 头像更换采用"先删后插",由 Service 层事务保证原子性(openGauss 不支持 ON CONFLICT) */
    @Delete("DELETE FROM user_avatar WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    @Insert("INSERT INTO user_avatar(user_id, content, content_type, updated_at) " +
            "VALUES(#{userId}, #{content}, #{contentType}, now())")
    int insert(UserAvatar avatar);

    @Select("SELECT user_id, content, content_type, updated_at FROM user_avatar WHERE user_id = #{userId}")
    UserAvatar findByUserId(Long userId);
}
