package com.scut.conference.service;

import com.scut.conference.common.BizException;
import com.scut.conference.entity.Paper;
import com.scut.conference.mapper.PaperMapper;
import com.scut.conference.mapper.ReviewMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewServicePlanTest {
    private final List<Long> published = new ArrayList<>();
    private final List<Long> locked = new ArrayList<>();
    private final PaperMapper papers = (PaperMapper) Proxy.newProxyInstance(
            PaperMapper.class.getClassLoader(), new Class<?>[]{PaperMapper.class}, (proxy, method, args) -> {
                if (method.getName().equals("findByIdForUpdate")) {
                    locked.add((Long) args[0]);
                    Paper paper = new Paper();
                    paper.setId((Long) args[0]);
                    paper.setStatus("REVIEWING");
                    paper.setReviewPlanPublished(false);
                    return paper;
                }
                if (method.getName().equals("publishReviewPlan")) {
                    published.add((Long) args[0]);
                    return 1;
                }
                throw new AssertionError(method.getName());
            });
    private final ReviewMapper reviews = (ReviewMapper) Proxy.newProxyInstance(
            ReviewMapper.class.getClassLoader(), new Class<?>[]{ReviewMapper.class}, (proxy, method, args) -> {
                if (method.getName().equals("countByPaperId")) return ((Long) args[0]) == 1L ? 2 : 1;
                throw new AssertionError(method.getName());
            });
    private final ReviewService service = new ReviewService(reviews, papers, null, null,
            new OperationLogService(null, null) {
                @Override public void log(String action, String detail) { }
            });

    @Test
    void publishesCompletePlansAndReportsShortages() {
        Map<String, Object> result = service.publishPlans(List.of(2L, 1L, 2L));
        assertEquals(List.of(1L), result.get("publishedIds"));
        assertEquals(List.of(2L), result.get("insufficientIds"));
        assertEquals(List.of(1L), published);
        assertEquals(List.of(1L, 2L), locked);
    }

    @Test
    void refusesEmptySelection() {
        assertThrows(BizException.class, () -> service.publishPlans(List.of()));
        assertEquals(List.of(), published);
    }
}
