package com.scut.conference.service;

import com.scut.conference.common.BizException;
import com.scut.conference.dto.ReviewDtos.ReviewSubmitRequest;
import com.scut.conference.entity.Paper;
import com.scut.conference.entity.Review;
import com.scut.conference.mapper.PaperMapper;
import com.scut.conference.mapper.ReviewMapper;
import com.scut.conference.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewServiceRubricTest {
    private Object[] saved;
    private boolean planPublished = true;
    private final ReviewMapper reviews = (ReviewMapper) Proxy.newProxyInstance(
            ReviewMapper.class.getClassLoader(), new Class<?>[]{ReviewMapper.class}, (proxy, method, args) -> {
                if (method.getName().equals("findById")) {
                    Review review = new Review();
                    review.setId(8L);
                    review.setPaperId(7L);
                    review.setExpertId(22L);
                    review.setStatus("PENDING");
                    return review;
                }
                if (method.getName().equals("submit")) { saved = args; return 1; }
                throw new AssertionError(method.getName());
            });
    private final PaperMapper papers = (PaperMapper) Proxy.newProxyInstance(
            PaperMapper.class.getClassLoader(), new Class<?>[]{PaperMapper.class}, (proxy, method, args) -> {
                if (method.getName().equals("findById")) {
                    Paper paper = new Paper();
                    paper.setStatus("REVIEWING");
                    paper.setReviewPlanPublished(planPublished);
                    return paper;
                }
                throw new AssertionError(method.getName());
            });
    private final ReviewService service = new ReviewService(reviews, papers, null, null,
            new OperationLogService(null, null) {
                @Override public void log(String action, String detail) { }
            });

    @AfterEach void clearUser() { UserContext.clear(); }

    @Test
    void rejectsIncompleteRubric() {
        UserContext.set(22L, "EXPERT");
        ReviewSubmitRequest request = new ReviewSubmitRequest("总体评价", "", Map.of(),
                "ACCEPT", null, "ACCEPT", false);
        assertThrows(BizException.class, () -> service.submitReview(8L, request));
    }

    @Test
    void storesRubricAndDerivesLegacyScores() {
        UserContext.set(22L, "EXPERT");
        ReviewSubmitRequest request = new ReviewSubmitRequest("总体评价", "修改建议", Map.of(
                "topicConformity", "VERY_WELL", "topicCoverage", "BALANCED",
                "innovation", "HIGH", "integrity", "OUTSTANDING",
                "literary", "TOTAL", "technicalDepth", "FIELD",
                "presentation", "SATISFACTORY", "organization", "SATISFACTORY"),
                "STRONG_ACCEPT", null, "ACCEPT", true);
        service.submitReview(8L, request);
        assertEquals(5, saved[4]);
        assertEquals(5, saved[5]);
        assertEquals(5, saved[6]);
        assertEquals(4, saved[7]);
        assertEquals(5, saved[8]);
        assertTrue(((String) saved[10]).contains("topicConformity"));
        assertEquals("STRONG_ACCEPT", saved[11]);
    }

    @Test
    void unpublishedPlanCannotBeReviewed() {
        UserContext.set(22L, "EXPERT");
        planPublished = false;
        ReviewSubmitRequest request = new ReviewSubmitRequest("总体评价", "", Map.of(
                "topicConformity", "VERY_WELL", "topicCoverage", "BALANCED",
                "innovation", "HIGH", "integrity", "OUTSTANDING", "literary", "TOTAL",
                "technicalDepth", "FIELD", "presentation", "SATISFACTORY",
                "organization", "SATISFACTORY"), "ACCEPT", null, "ACCEPT", false);
        assertThrows(BizException.class, () -> service.submitReview(8L, request));
    }
}
