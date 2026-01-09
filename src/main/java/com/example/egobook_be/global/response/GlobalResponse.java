package com.example.egobook_be.global.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
@Schema(title="GlobalResponse Dto", description = "공통 API 응답 형식 Record")
public record GlobalResponse<T>(
        // json 속성 지정
        @JsonProperty("isSuccess")
        @Schema(description = "요청 성공 여부", example = "true")
        Boolean isSuccess,

        @JsonProperty("code")
        @Schema(description = "HTTP 상태 코드", example = "200")
        String code,

        @JsonProperty("message")
        @Schema(description = "HTTP 응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
        String message,

        @JsonInclude(JsonInclude.Include.NON_NULL) // 값이 null이면 결과에 포함하지 않음
        @Schema(description = "응답 데이터")
        T data
) {
    // success - 성공 응답 생성 함수
    public static <T> GlobalResponse<T> success(T data){
        return new GlobalResponse<>(true, "200", "요청이 성공적으로 처리되었습니다!", data);
    }
    public static <T> GlobalResponse<T> success(String message, T data){
        return new GlobalResponse<>(true, "200", message, data);
    }
    public static <T> GlobalResponse<T> success(String code, String message, T data){
        return new GlobalResponse<>(true, code, message, data);
    }

    // error - 실패 응답 생성 함수
    public static <T> GlobalResponse<T> error(String code, String message){
        return new GlobalResponse<>(false, code , message, null);
    }
}
