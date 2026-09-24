package com.example.egobook_be.domain.moderation.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 관리자용 일반/보관 신고를 시간순으로 합쳐 정확한 페이지를 반환한다. */
public final class AdminReportMerge {
    private AdminReportMerge() {}

    /**
     * 각각의 저장소에서 0부터 offset+size+1개를 읽어온 다음 합친다.
     * 신고량이 많아질 경우 DB UNION + 키셋 페이지네이션으로 교체할 수 있다.
     */
    public static <T> Slice<T> merge(List<T> live, List<T> archived,
                                     Comparator<T> newestFirst, int page, int size) {
        int offset = Math.multiplyExact(page - 1, size);
        List<T> combined = new ArrayList<>(live.size() + archived.size());
        combined.addAll(live);
        combined.addAll(archived);
        combined.sort(newestFirst);
        int start = Math.min(offset, combined.size());
        int end = Math.min(Math.addExact(offset, size), combined.size());
        boolean hasNext = combined.size() > end;
        Pageable pageable = PageRequest.of(page - 1, size);
        return new SliceImpl<>(combined.subList(start, end), pageable, hasNext);
    }

    public static int fetchSize(int page, int size) {
        return Math.addExact(Math.multiplyExact(page, size), 1);
    }
}
