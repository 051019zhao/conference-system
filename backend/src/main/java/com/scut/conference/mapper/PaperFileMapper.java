package com.scut.conference.mapper;

import com.scut.conference.entity.PaperFile;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 论文附件账本 Mapper */
public interface PaperFileMapper {

    @Insert("INSERT INTO paper_file(paper_id, file_type, file_name, stored_path, file_ext, file_size, " +
            "version_no, is_current, uploaded_by) " +
            "VALUES(#{paperId}, #{fileType}, #{fileName}, #{storedPath}, #{fileExt}, #{fileSize}, " +
            "#{versionNo}, #{isCurrent}, #{uploadedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PaperFile file);

    /** 论文全部附件(新版本在前) */
    @Select("SELECT * FROM paper_file WHERE paper_id = #{paperId} " +
            "ORDER BY file_type, version_no DESC")
    List<PaperFile> findByPaperId(Long paperId);

    @Select("SELECT * FROM paper_file WHERE id = #{id}")
    PaperFile findById(Long id);

    /** 同类型当前最大版本号(无记录返回 0) */
    @Select("SELECT COALESCE(MAX(version_no), 0) FROM paper_file " +
            "WHERE paper_id = #{paperId} AND file_type = #{fileType}")
    int maxVersion(@Param("paperId") Long paperId, @Param("fileType") String fileType);

    /** 重传新版本时,把该类型旧版本全部置为非当前 */
    @Update("UPDATE paper_file SET is_current = false " +
            "WHERE paper_id = #{paperId} AND file_type = #{fileType}")
    int markOldVersionsNotCurrent(@Param("paperId") Long paperId, @Param("fileType") String fileType);

    @Delete("DELETE FROM paper_file WHERE id = #{id}")
    int deleteById(Long id);
}
