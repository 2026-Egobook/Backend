package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import com.example.egobook_be.domain.letters.entity.QPlazaLetterReport;
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
public class PlazaLetterReportRepositoryCustomImpl implements PlazaLetterReportRepositoryCustom {
    private final JPAQueryFactory queryFactory; // QueryDSL을 사용해서 JPA 쿼리를 생성, 실행하는 빌더 역할
    private final QPlazaLetterReport qPlazaLetterReport = QPlazaLetterReport.plazaLetterReport;

    // 신고자 PK로 신고한 누적 횟수를 카운트하는 함수
    @Override
    public long countByReporterId(Long reporterId, ReportReason reportReason, ReportStatus reportStatus) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qPlazaLetterReport.reporterId.eq(reporterId));
        if (reportReason != null) builder.and(qPlazaLetterReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qPlazaLetterReport.status.eq(reportStatus));

        Long count = queryFactory
                .select(qPlazaLetterReport.count())
                .from(qPlazaLetterReport)
                .where(builder)
                .fetchOne();

        return count != null ? count : 0L;
    }

    // 편지 작성자 PK로 신고받은 누적 횟수를 카운트하는 함수
    @Override
    public long countBySenderId(Long senderId, ReportReason reportReason, ReportStatus reportStatus) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qPlazaLetterReport.letter.senderId.eq(senderId));
        if (reportReason != null) builder.and(qPlazaLetterReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qPlazaLetterReport.status.eq(reportStatus));

        Long count = queryFactory
                .select(qPlazaLetterReport.count())
                .from(qPlazaLetterReport)
                .join(qPlazaLetterReport.letter)
                .where(builder)
                .fetchOne();

        return count != null ? count : 0L;
    }

    // ReportType 구분 없이 전체 조회
    @Override
    public long countByUserId(Long userId, ReportReason reportReason, ReportStatus reportStatus) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qPlazaLetterReport.reporterId.eq(userId)
                .or(qPlazaLetterReport.letter.senderId.eq(userId)));
        if (reportReason != null) builder.and(qPlazaLetterReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qPlazaLetterReport.status.eq(reportStatus));

        Long count = queryFactory
                .select(qPlazaLetterReport.count())
                .from(qPlazaLetterReport)
                .join(qPlazaLetterReport.letter)
                .where(builder)
                .fetchOne();

        return count != null ? count : 0L;
    }

    // 신고자 PK로 필터링을 통해 데이터들을 Slicing하는 함수
    @Override
    public Slice<PlazaLetterReport> findPlazaLetterReportsByReporterId(Long reporterId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable){
        // WHERE 절을 동적으로 빌드해주는 역할
        BooleanBuilder builder = new BooleanBuilder();

        /*
          1. 조건 Build
           - reporterId로 검색 (공통)
           - reportReason이 null이 아닌 경우 조건에 추가
           - reportStatus가 null이 아닌 경우 조건에 추가
         */
        builder.and(qPlazaLetterReport.reporterId.eq(reporterId));
        if(reportReason != null) builder.and(qPlazaLetterReport.reason.eq(reportReason));
        if(reportStatus != null) builder.and(qPlazaLetterReport.status.eq(reportStatus));

        // 2. 조건에 맞는 데이터 조회
        List<PlazaLetterReport> content = queryFactory
                .selectFrom(qPlazaLetterReport)
                .join(qPlazaLetterReport.letter).fetchJoin()
                .where(builder)
                .orderBy(getOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset()) // 데이터를 조회할 시작 위치
                .limit(pageable.getPageSize()+1L) // 조회할 데이터 개수 지정 (+1은 다음 페이지 존재 여부를 확인하기 위해서이다.)
                .fetch();

        // 3. 다음 페이지 존재하는지 여부 확인 및 hasNext 판별용으로 추가로 가져온 데이터 결과에서 삭제
        boolean hasNext = content.size() > pageable.getPageSize();
        if(hasNext) content.removeLast();
        return new SliceImpl<>(content, pageable, hasNext);
    }

    // 편지 작성자 PK(senderId)로 필터링을 통해 데이터들을 Slicing하는 함수 (자신이 작성한 편지에 대해 신고를 받은 이력을 조회하는 함수)
    @Override
    public Slice<PlazaLetterReport> findPlazaLetterReportsBySenderId(Long senderId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable){
        // WHERE 절을 동적으로 빌드해주는 역할
        BooleanBuilder builder = new BooleanBuilder();

        /*
          1. 조건 Build
           - senderId 검색 (공통)
           - reportReason이 null이 아닌 경우 조건에 추가
           - reportStatus가 null이 아닌 경우 조건에 추가
         */
        builder.and(qPlazaLetterReport.senderId.eq(senderId));
        if(reportReason != null) builder.and(qPlazaLetterReport.reason.eq(reportReason));
        if(reportStatus != null) builder.and(qPlazaLetterReport.status.eq(reportStatus));

        // 2. 조건에 맞는 데이터 조회
        List<PlazaLetterReport> content = queryFactory
                .selectFrom(qPlazaLetterReport)
                .join(qPlazaLetterReport.letter).fetchJoin()
                .where(builder)
                .orderBy(getOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset()) // 데이터를 조회할 시작 위치
                .limit(pageable.getPageSize()+1L) // 조회할 데이터 개수 지정 (+1은 다음 페이지 존재 여부를 확인하기 위해서이다.)
                .fetch();

        // 3. 다음 페이지 존재하는지 여부 확인 및 hasNext 판별용으로 추가로 가져온 데이터 결과에서 삭제
        boolean hasNext = content.size() > pageable.getPageSize();
        if(hasNext) content.removeLast();
        return new SliceImpl<>(content, pageable, hasNext);
    }

    // ReportType 구분 없이 전체 조회
    @Override
    public Slice<PlazaLetterReport> findPlazaLetterReportsByUserIdWithoutReportType(Long userId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        // reporterId가 userId이거나 letter의 senderId가 userId인 경우 모두 조회
        builder.and(qPlazaLetterReport.reporterId.eq(userId)
                .or(qPlazaLetterReport.letter.senderId.eq(userId)));
        if (reportReason != null) builder.and(qPlazaLetterReport.reason.eq(reportReason));
        if (reportStatus != null) builder.and(qPlazaLetterReport.status.eq(reportStatus));

        List<PlazaLetterReport> content = queryFactory
                .selectFrom(qPlazaLetterReport)
                .join(qPlazaLetterReport.letter).fetchJoin()
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
                    PathBuilder<PlazaLetterReport> path = new PathBuilder<>(PlazaLetterReport.class, "plazaLetterReport");
                    return new OrderSpecifier(
                            order.isAscending() ? Order.ASC : Order.DESC,
                            path.get(order.getProperty())
                    );
                })
                .toArray(OrderSpecifier[]::new);
    }
}
