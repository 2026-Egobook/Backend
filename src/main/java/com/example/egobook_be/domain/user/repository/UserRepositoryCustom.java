package com.example.egobook_be.domain.user.repository;

import java.util.List;

public interface UserRepositoryCustom {
    List<Long> findHighReplyRateCandidates(Long excludeUserId, int limit);
}