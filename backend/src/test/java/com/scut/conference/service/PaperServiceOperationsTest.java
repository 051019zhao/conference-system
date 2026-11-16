package com.scut.conference.service;

import com.scut.conference.common.BizException;
import com.scut.conference.dto.PaperDtos.AuthorItem;
import com.scut.conference.dto.PaperDtos.PaperSubmitRequest;
import com.scut.conference.entity.Paper;
import com.scut.conference.entity.Topic;
import com.scut.conference.mapper.ConferenceMapper;
import com.scut.conference.mapper.PaperAuthorMapper;
import com.scut.conference.mapper.PaperMapper;
import com.scut.conference.mapper.TopicMapper;
import com.scut.conference.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaperServiceOperationsTest {
    private final Paper paper = new Paper();
    private int edits;
    private int trashed;
    private int restored;
    private final PaperMapper papers = proxy(PaperMapper.class, (method, args) -> switch (method) {
        case "findById" -> paper;
        case "updateDraftMeta" -> { edits++; yield 1; }
        case "moveToTrash" -> { trashed++; yield 1; }
        case "restoreFromTrash" -> { restored++; yield 1; }
        default -> throw new AssertionError(method);
    });
    private final PaperAuthorMapper authors = proxy(PaperAuthorMapper.class, (method, args) -> switch (method) {
        case "deleteByPaperId", "insertBatch" -> 1;
        default -> throw new AssertionError(method);
    });
    private final TopicMapper topics = proxy(TopicMapper.class, (method, args) -> {
        if (method.equals("findById")) return new Topic();
        throw new AssertionError(method);
    });
    private final ConferenceMapper conference = proxy(ConferenceMapper.class, (method, args) -> {
        if (method.equals("find")) return null;
        throw new AssertionError(method);
    });
    private final PaperService service = new PaperService(papers, authors, null, topics,
            null, conference, null, new OperationLogService(null, null) {
                @Override public void log(String action, String detail) { }
            }, null, null);

    PaperServiceOperationsTest() {
        paper.setId(9L);
        paper.setSubmitterId(3L);
    }

    @AfterEach void clearUser() { UserContext.clear(); }

    @Test
    void authorCanEditBeforeReviewButNotAfterReviewStarts() {
        UserContext.set(3L, "AUTHOR");
        PaperSubmitRequest request = new PaperSubmitRequest("中文题目", "English title", "中文摘要",
                "English abstract", "数据库,系统,会议", "database,system,conference", 1L,
                List.of(new AuthorItem("作者", "Zuo Zhe", "author@example.com", "大学", "510000", true)));
        paper.setStatus("SUBMITTED");
        service.updateDraft(9L, request, null, null);
        assertEquals(1, edits);
        paper.setStatus("REVIEWING");
        assertThrows(BizException.class, () -> service.updateDraft(9L, request, null, null));
        assertEquals(1, edits);
    }

    @Test
    void trashOnlyAllowsOwnDraftOrWithdrawnPaperAndCanRestore() {
        UserContext.set(3L, "AUTHOR");
        paper.setStatus("DRAFT");
        service.moveToTrash(9L);
        service.restoreFromTrash(9L);
        assertEquals(1, trashed);
        assertEquals(1, restored);
        paper.setStatus("SUBMITTED");
        assertThrows(BizException.class, () -> service.moveToTrash(9L));
        assertEquals(1, trashed);
    }

    @FunctionalInterface private interface Handler { Object apply(String method, Object[] args); }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.apply(method.getName(), args));
    }
}
