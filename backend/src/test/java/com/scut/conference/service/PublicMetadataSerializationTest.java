package com.scut.conference.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scut.conference.entity.Notification;
import com.scut.conference.entity.Paper;
import com.scut.conference.entity.PaperFile;
import com.scut.conference.entity.Topic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicMetadataSerializationTest {
    @Test
    void exposesDownloadAvailabilityWithoutServerFilePaths() throws Exception {
        Topic topic = new Topic();
        topic.setPptPath("C:/private/uploads/talk.pptx");
        Notification notice = new Notification();
        notice.setPdfPath("C:/private/uploads/notice.pdf");
        Paper paper = new Paper();
        paper.setFilePath("C:/private/uploads/paper.pdf");
        paper.setFinalFilePath("C:/private/uploads/final.pdf");
        PaperFile paperFile = new PaperFile();
        paperFile.setStoredPath("C:/private/uploads/supplement.zip");
        ObjectMapper json = new ObjectMapper();
        String topicJson = json.writeValueAsString(topic);
        String noticeJson = json.writeValueAsString(notice);
        String paperJson = json.writeValueAsString(paper);
        String fileJson = json.writeValueAsString(paperFile);
        assertTrue(topicJson.contains("\"hasPpt\":true"));
        assertTrue(noticeJson.contains("\"hasPdf\":true"));
        assertTrue(paperJson.contains("\"hasMainFile\":true"));
        assertTrue(paperJson.contains("\"hasFinalFile\":true"));
        assertFalse(topicJson.contains("private/uploads"));
        assertFalse(noticeJson.contains("private/uploads"));
        assertFalse(paperJson.contains("private/uploads"));
        assertFalse(fileJson.contains("private/uploads"));
    }
}
