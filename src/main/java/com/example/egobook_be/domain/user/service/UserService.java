package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.user.dto.UserNicknameResDto;
import com.example.egobook_be.domain.user.dto.UserNicknameUpdateReqDto;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserErrorCode;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public UserNicknameResDto updateNickname(Long userId, UserNicknameUpdateReqDto reqDto) {
        // 1. User 가져오기
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 2. 닉네임 업데이트 (해당 닉네임의 스타일은 이미 Dto 레벨에서 검증되었음)
        user.updateNickname(reqDto.nickname());

        return UserNicknameResDto.builder().newNickname(user.getNickname()).build();
    }
}
