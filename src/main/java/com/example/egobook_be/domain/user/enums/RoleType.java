package com.example.egobook_be.domain.user.enums;

/**
 * 사용자의 권한을 담고 있는 Enum Class
 * - Spring Security 표준으로, "ROLE_" 접두사를 붙여놓았음
 */
public enum RoleType {
    ROLE_ADMIN,
    ROLE_USER
}
