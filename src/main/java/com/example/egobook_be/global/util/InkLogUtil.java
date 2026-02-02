package com.example.egobook_be.global.util;

import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class InkLogUtil {
    // 해당 사용자의 잉크값을 증가시키고 InkLog를 남기는 함수
    public void addInkLog(List<InkLog> inkLogs, User user, Integer amount, InkLogType inkLogType) {
        user.addInk(amount);
        inkLogs.add(InkLog.builder()
                .user(user)
                .amount(amount)
                .reason(inkLogType)
                .build()
        );
    }
}
