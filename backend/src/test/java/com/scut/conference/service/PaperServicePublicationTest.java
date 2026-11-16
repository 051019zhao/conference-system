package com.scut.conference.service;

import com.scut.conference.common.BizException;
import com.scut.conference.common.DownloadFile;
import com.scut.conference.entity.Paper;
import com.scut.conference.entity.SysUser;
import com.scut.conference.mapper.PaperMapper;
import com.scut.conference.mapper.ReviewMapper;
import com.scut.conference.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperServicePublicationTest {
    private Paper paper;
    private int assignedReviews = 1;
    private int submittedReviews = 1;
    private final List<String> loadedPaths = new ArrayList<>();
    private final List<String> emails = new ArrayList<>();
    private final PaperMapper papers = proxy(PaperMapper.class, (method, args) -> switch (method) {
        case "findById" -> paper;
        case "casUpdateStatus" -> 1;
        default -> throw new AssertionError("Unexpected paper query: " + method);
    });
    private final ReviewMapper reviews = proxy(ReviewMapper.class, (method, args) -> {
        if ("countByPaperId".equals(method)) return assignedReviews;
        if ("countSubmittedByPaperId".equals(method)) return submittedReviews;
        throw new AssertionError("Unexpected review query: " + method);
    });
    private final UserMapper users = proxy(UserMapper.class, (method, args) -> {
        if ("findById".equals(method)) {
            SysUser author = new SysUser();
            author.setEmail("author@example.com");
            return author;
        }
        throw new AssertionError("Unexpected user query: " + method);
    });
    private final FileStorageService files = new FileStorageService() {
        @Override
        public org.springframework.core.io.Resource loadAsResource(String path) {
            loadedPaths.add(path);
            return new ByteArrayResource(path.getBytes(StandardCharsets.UTF_8));
        }
    };
    private final MailService mail = new MailService(null) {
        @Override
        public void send(String to, String subject, String body) {
            emails.add(to + "|" + subject + "|" + body);
        }
    };
    private final OperationLogService logs = new OperationLogService(null, null) {
        @Override
        public void log(String action, String detail) { }
    };
    private final PaperService service = new PaperService(papers, null, null, null,
            reviews, null, files, logs, users, mail);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void onlyAcceptedPapersCanBeDownloadedPublicly() {
        paper = paper("REVIEWING");
        assertThrows(BizException.class, () -> service.downloadAcceptedPublic(7L));
        assertTrue(loadedPaths.isEmpty());
    }

    @Test
    void publicDownloadSwitchesFromMainToFinal() throws Exception {
        paper = paper("ACCEPTED");

        DownloadFile first = service.downloadAcceptedPublic(7L);
        assertEquals("main.pdf", new String(first.resource().getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        assertEquals("测试论文-主稿.pdf", first.fileName());

        paper.setStatus("FINAL_SUBMITTED");
        paper.setFinalFilePath("final.pdf");
        DownloadFile second = service.downloadAcceptedPublic(7L);
        assertEquals("final.pdf", new String(second.resource().getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        assertEquals("测试论文-终稿.pdf", second.fileName());
        assertEquals(List.of("main.pdf", "final.pdf"), loadedPaths);
    }

    @Test
    void acceptanceEmailIsSentOnlyAfterDecisionCommits() {
        paper = paper("REVIEWING");
        TransactionSynchronizationManager.initSynchronization();

        service.decide(7L, "ACCEPTED");
        assertTrue(emails.isEmpty());
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
        assertEquals(List.of("author@example.com|论文录用通知：《测试论文》|"
                + "您好，您投稿的论文《测试论文》已被会议录用。请登录会议系统查看评审意见，并按会议要求提交终稿。"), emails);
    }

    @Test
    void cannotDecideWhileAnAssignedExpertHasNotReturnedAnOpinion() {
        paper = paper("REVIEWING");
        assignedReviews = 2;
        submittedReviews = 1;
        assertThrows(BizException.class, () -> service.decide(7L, "ACCEPTED"));
        assertTrue(emails.isEmpty());
    }

    private Paper paper(String status) {
        Paper paper = new Paper();
        paper.setId(7L);
        paper.setTitle("测试论文");
        paper.setSubmitterId(3L);
        paper.setStatus(status);
        paper.setReviewPlanPublished(true);
        paper.setFilePath("main.pdf");
        return paper;
    }

    private interface Handler {
        Object apply(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (instance, method, args) -> handler.apply(method.getName(), args));
    }
}
