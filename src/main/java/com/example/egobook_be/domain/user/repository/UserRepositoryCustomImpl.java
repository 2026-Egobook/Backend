package com.example.egobook_be.domain.user.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final EntityManager em;

    @Override
    public List<Long> findHighReplyRateCandidates(Long excludeUserId, int limit) {
        // 1) ACTIVE 우선
        List<Long> active = queryCandidates(excludeUserId, limit, true);
        if (!active.isEmpty()) return active;

        // 2) fallback: ACTIVE가 아무도 없으면 status 제한 풀어서 뽑기(NEW 포함)
        // 나중에 접속률이 높은 후보를 뽑는 기준은 pm과 이야기해봐야함.
        return queryCandidates(excludeUserId, limit, false);
    }

    private List<Long> queryCandidates(Long excludeUserId, int limit, boolean onlyActive) {

        String statusWhere = onlyActive
                ? " AND u.status = 'ACTIVE' "
                : " AND u.status <> 'DELETED_PENDING' ";

        String sql = """
            SELECT u.id
            FROM `user` u
            LEFT JOIN (
                SELECT receiver_id AS uid, COUNT(*) AS received_cnt
                FROM plaza_letters
                WHERE receiver_id IS NOT NULL
                GROUP BY receiver_id
            ) r ON r.uid = u.id
            LEFT JOIN (
                SELECT replier_id AS uid, COUNT(*) AS reply_cnt
                FROM plaza_letter_replies
                GROUP BY replier_id
            ) p ON p.uid = u.id
            WHERE u.id <> :excludeUserId
                AND (u.letter_receive_blocked_until IS NULL OR u.letter_receive_blocked_until <= NOW())
        """ + statusWhere + """
            ORDER BY (COALESCE(p.reply_cnt,0) / NULLIF(COALESCE(r.received_cnt,0),0)) DESC,
                     COALESCE(p.reply_cnt,0) DESC,
                     u.last_login_at DESC
            LIMIT :limit
        """;

        @SuppressWarnings("unchecked")
        List<Number> rows = em.createNativeQuery(sql)
                .setParameter("excludeUserId", excludeUserId)
                .setParameter("limit", limit)
                .getResultList();

        return rows.stream().map(Number::longValue).toList();
    }
}