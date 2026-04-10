package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReplyReport;
import com.example.egobook_be.domain.letters.entity.QPlazaLetterReplyReport;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import java.util.List;

@RequiredArgsConstructor
public class PlazaLetterReplyReportRepositoryCustomImpl implements PlazaLetterReplyReportRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QPlazaLetterReplyReport qPlazaLetterReplyReport = QPlazaLetterReplyReport.plazaLetterReplyReport;

    // 신고자 PK로 신고한 누적 횟수 카운트
    // Letter와 동일하게 reporterId 필드를 직접 보유
    @Override
    public long countByReporterId(Long reporterId, ReportReason reportReason, ReportStatus reportStatus) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qPlazaLetterReplyReport.reporterId.eq(reporterId));
        if (reportReason != null) builder.and(qPlazaLetterReplyReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qPlazaLetterReplyReport.status.eq(reportStatus));

        Long count = queryFactory
                .select(qPlazaLetterReplyReport.count())
                .from(qPlazaLetterReplyReport)
                .where(builder)
                .fetchOne();

        return count != null ? count : 0L;
    }

    // 답장 작성자(replierId) PK로 신고받은 누적 횟수 카운트
    // Letter의 senderId에 대응하는 개념으로, reply 연관관계를 통해 접근
    @Override
    public long countByReplierId(Long replierId, ReportReason reportReason, ReportStatus reportStatus) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qPlazaLetterReplyReport.replierId.eq(replierId));
        if (reportReason != null) builder.and(qPlazaLetterReplyReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qPlazaLetterReplyReport.status.eq(reportStatus));

        Long count = queryFactory
                .select(qPlazaLetterReplyReport.count())
                .from(qPlazaLetterReplyReport)
                .join(qPlazaLetterReplyReport.reply)
                .where(builder)
                .fetchOne();

        return count != null ? count : 0L;
    }

    // ReportType 구분 없이 전체 카운트 (신고자 or 피신고자)
    @Override
    public long countByUserId(Long userId, ReportReason reportReason, ReportStatus reportStatus) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(
                qPlazaLetterReplyReport.reporterId.eq(userId)
                        .or(qPlazaLetterReplyReport.replierId.eq(userId))
        );
        if (reportReason != null) builder.and(qPlazaLetterReplyReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qPlazaLetterReplyReport.status.eq(reportStatus));

        Long count = queryFactory
                .select(qPlazaLetterReplyReport.count())
                .from(qPlazaLetterReplyReport)
                .join(qPlazaLetterReplyReport.reply)
                .where(builder)
                .fetchOne();

        return count != null ? count : 0L;
    }

    // 신고자 PK로 Slice 조회
    @Override
    public Slice<PlazaLetterReplyReport> findPlazaLetterReplyReportsByReporterId(
            Long reporterId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qPlazaLetterReplyReport.reporterId.eq(reporterId));
        if (reportReason != null) builder.and(qPlazaLetterReplyReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qPlazaLetterReplyReport.status.eq(reportStatus));

        List<PlazaLetterReplyReport> content = queryFactory
                .selectFrom(qPlazaLetterReplyReport)
                .join(qPlazaLetterReplyReport.reply).fetchJoin()
                .where(builder)
                .orderBy(getOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) content.removeLast();
        return new SliceImpl<>(content, pageable, hasNext);
    }

    // 답장 작성자 PK(replierId)로 Slice 조회 (자신이 작성한 답장에 대해 신고를 받은 이력)
    @Override
    public Slice<PlazaLetterReplyReport> findPlazaLetterReplyReportsByReplierId(
            Long replierId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qPlazaLetterReplyReport.replierId.eq(replierId));
        if (reportReason != null) builder.and(qPlazaLetterReplyReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qPlazaLetterReplyReport.status.eq(reportStatus));

        List<PlazaLetterReplyReport> content = queryFactory
                .selectFrom(qPlazaLetterReplyReport)
                .join(qPlazaLetterReplyReport.reply).fetchJoin()
                .where(builder)
                .orderBy(getOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) content.removeLast();
        return new SliceImpl<>(content, pageable, hasNext);
    }

    // ReportType 구분 없이 전체 Slice 조회
    @Override
    public Slice<PlazaLetterReplyReport> findPlazaLetterReplyReportsByUserIdWithoutReportType(
            Long userId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(
                qPlazaLetterReplyReport.reporterId.eq(userId)
                        .or(qPlazaLetterReplyReport.replierId.eq(userId))
        );
        if (reportReason != null) builder.and(qPlazaLetterReplyReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qPlazaLetterReplyReport.status.eq(reportStatus));

        List<PlazaLetterReplyReport> content = queryFactory
                .selectFrom(qPlazaLetterReplyReport)
                .join(qPlazaLetterReplyReport.reply).fetchJoin()
                .where(builder)
                .orderBy(getOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) content.removeLast();
        return new SliceImpl<>(content, pageable, hasNext);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
        return sort.stream()
                .map(order -> {
                    PathBuilder<PlazaLetterReplyReport> path = new PathBuilder<>(PlazaLetterReplyReport.class, "plazaLetterReplyReport");
                    return new OrderSpecifier(
                            order.isAscending() ? Order.ASC : Order.DESC,
                            path.get(order.getProperty())
                    );
                })
                .toArray(OrderSpecifier[]::new);
    }
}