package com.example.egobook_be.global.response;


import com.example.egobook_be.global.enums.GlobalResponseCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
@Schema(title="GlobalResponse Dto", description = "공통 API 응답 형식 Record")
public record GlobalResponse<T>(
        @JsonProperty("status")
        @Schema(description = "HTTP 상태 번호", example = "200")
        Integer status,

        @JsonProperty("code")
        @Schema(description = "성공/실패 여부", example = "SUCCESS")
        GlobalResponseCode code,

        @JsonProperty("message")
        @Schema(description = "HTTP 응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
        String message,

        @JsonInclude(JsonInclude.Include.NON_NULL) // 값이 null이면 결과에 포함하지 않음
        @Schema(description = "응답 데이터")
        T data
) {
    // success - 성공 응답 생성 함수
    public static <T> GlobalResponse<T> success(T data){
        return new GlobalResponse<>(200, GlobalResponseCode.SUCCESS, "요청이 성공적으로 처리되었습니다!", data);
    }
    public static <T> GlobalResponse<T> success(String message, T data){
        return new GlobalResponse<>(200, GlobalResponseCode.SUCCESS, message, data);
    }
    public static <T> GlobalResponse<T> success(Integer status, String message, T data){
        return new GlobalResponse<>(status, GlobalResponseCode.SUCCESS, message, data);
    }


    // error - 실패 응답 생성 함수
    public static <T> GlobalResponse<T> error(Integer status, String message){
        return new GlobalResponse<>(status, GlobalResponseCode.FAIL, message, null);
    }
}
