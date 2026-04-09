package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.domain.question.entity.QAnswerReport;
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
public class AnswerReportRepositoryCustomImpl implements AnswerReportRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QAnswerReport qReport = QAnswerReport.answerReport;

    // AnswerReport는 reporterId 필드 없이 user 연관관계로 신고자를 참조
    // → qReport.user.id 로 신고자 접근
    @Override
    public long countByReporterId(Long reporterId, ReportReason reportReason, ReportStatus reportStatus) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qReport.user.id.eq(reporterId)); // reporterId 필드 X → user 연관관계로 접근
        if (reportReason != null) builder.and(qReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qReport.status.eq(reportStatus));

        Long count = queryFactory
                .select(qReport.count())
                .from(qReport)
                .join(qReport.user)  // user join 필요
                .where(builder)
                .fetchOne();

        return count != null ? count : 0L;
    }

    // 답변 작성자(answererId) PK로 신고받은 누적 횟수 카운트
    // answer 연관관계를 통해 답변 작성자에 접근 → qReport.answer.user.id
    @Override
    public long countByAnswererId(Long answererId, ReportReason reportReason, ReportStatus reportStatus) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qReport.answer.user.id.eq(answererId)); // answer → user 로 피신고자 접근
        if (reportReason != null) builder.and(qReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qReport.status.eq(reportStatus));

        Long count = queryFactory
                .select(qReport.count())
                .from(qReport)
                .join(qReport.answer)
                .join(qReport.answer.user) // 피신고자 접근을 위한 answer.user join
                .where(builder)
                .fetchOne();

        return count != null ? count : 0L;
    }

    // ReportType 구분 없이 전체 카운트
    // user.id(신고자) or answer.user.id(피신고자) 둘 중 하나라도 userId와 일치하면 조회
    @Override
    public long countByUserId(Long userId, ReportReason reportReason, ReportStatus reportStatus) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(
                qReport.user.id.eq(userId)
                        .or(qReport.answer.user.id.eq(userId))
        );
        if (reportReason != null) builder.and(qReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qReport.status.eq(reportStatus));

        Long count = queryFactory
                .select(qReport.count())
                .from(qReport)
                .join(qReport.user) // count 쿼리는 n+1 문제가 발생하지 않는다

                .join(qReport.answer)
                .join(qReport.answer.user)
                .where(builder)
                .fetchOne();

        return count != null ? count : 0L;
    }

    // 신고자(user.id)로 Slice 조회
    @Override
    public Slice<AnswerReport> findAnswerReportsByReporterId(
            Long reporterId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qReport.user.id.eq(reporterId));
        if (reportReason != null) builder.and(qReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qReport.status.eq(reportStatus));

        List<AnswerReport> content = queryFactory
                .selectFrom(qReport)
                .join(qReport.user).fetchJoin()
                .join(qReport.answer).fetchJoin()
                .where(builder)
                .orderBy(getOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) content.removeLast();
        return new SliceImpl<>(content, pageable, hasNext);
    }

    // 피신고자(answer.user.id)로 Slice 조회
    @Override
    public Slice<AnswerReport> findAnswerReportsByAnswererId(
            Long answererId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qReport.answer.user.id.eq(answererId));
        if (reportReason != null) builder.and(qReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qReport.status.eq(reportStatus));

        List<AnswerReport> content = queryFactory
                .selectFrom(qReport)
                .join(qReport.user).fetchJoin()
                .join(qReport.answer).fetchJoin()
                .join(qReport.answer.user).fetchJoin() // 피신고자 접근을 위해 answer.user도 fetchJoin
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
    public Slice<AnswerReport> findAnswerReportsByUserIdWithoutReportType(
            Long userId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(
                qReport.user.id.eq(userId)
                        .or(qReport.answer.user.id.eq(userId))
        );
        if (reportReason != null) builder.and(qReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qReport.status.eq(reportStatus));

        List<AnswerReport> content = queryFactory
                .selectFrom(qReport)
                .join(qReport.user).fetchJoin()
                .join(qReport.answer).fetchJoin()
                .join(qReport.answer.user).fetchJoin()
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
                    PathBuilder<AnswerReport> path = new PathBuilder<>(AnswerReport.class, "answerReport");
                    return new OrderSpecifier(
                            order.isAscending() ? Order.ASC : Order.DESC,
                            path.get(order.getProperty())
                    );
                })
                .toArray(OrderSpecifier[]::new);
    }
}