package com.scut.conference.mapper;

import com.scut.conference.entity.Material;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 会议资料表 Mapper */
public interface MaterialMapper {

    @Insert("INSERT INTO material(name, description, file_path, file_size) VALUES(#{name}, #{description}, #{filePath}, #{fileSize})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Material material);

    @Update("UPDATE material SET name = COALESCE(#{name}, name), description = COALESCE(#{description}, description) WHERE id = #{id}")
    int updateMeta(@Param("id") Long id, @Param("name") String name, @Param("description") String description);

    @Delete("DELETE FROM material WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM material ORDER BY id DESC")
    List<Material> findAll();

    @Select("SELECT * FROM material WHERE id = #{id}")
    Material findById(Long id);

    /** 下载计数(原子自增,并发安全) */
    @Update("UPDATE material SET download_count = download_count + 1 WHERE id = #{id}")
    int increaseDownloadCount(Long id);
}
