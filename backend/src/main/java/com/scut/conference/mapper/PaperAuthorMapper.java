package com.scut.conference.mapper;

import com.scut.conference.entity.PaperAuthor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 论文作者表 Mapper */
public interface PaperAuthorMapper {

    /** 批量插入多作者(排序号 sort_no 由 Service 层按下标生成) */
    @Insert("<script>" +
            "INSERT INTO paper_author(paper_id, name, pinyin, email, affiliation, postal_code, sort_no, is_corresponding) VALUES " +
            "<foreach collection='list' item='a' separator=','>" +
            "(#{a.paperId}, #{a.name}, #{a.pinyin}, #{a.email}, #{a.affiliation}, " +
            "#{a.postalCode}, #{a.sortNo}, #{a.isCorresponding})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<PaperAuthor> authors);

    @Select("SELECT * FROM paper_author WHERE paper_id = #{paperId} ORDER BY sort_no")
    List<PaperAuthor> findByPaperId(Long paperId);

    @Delete("DELETE FROM paper_author WHERE paper_id = #{paperId}")
    int deleteByPaperId(Long paperId);
}
