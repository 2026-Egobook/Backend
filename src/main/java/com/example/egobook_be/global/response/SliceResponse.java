package com.example.egobook_be.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.function.Function;

@Builder
@Schema(title = "SliceResponse Dto", description = "무한 스크롤 시 Slice된 데이터들 반환 Record")
public record SliceResponse<T>(
        List<T> content,    // Slicing된 데이터 리스트
        int currentSlice,   // 현재 slice 번호
        int size,           // 현재 slice 크기(개수)
        boolean hasNext     // 다음 Slice 존재 여부
) {

    /**
     * Slice<Dto> -> SliceResponse<Dto>로 변환하여 반환하는 함수
     * Slice<Dto>에 있는 데이터 값을 옮기기만 한다.
     * @param slice Slice<Dto> 형식의 데이터. Slicing 해온 데이터가 담겨있다.
     * @return SliceResponse<T> Slicing 정보와 Slicing된 데이터들이 담긴 SliceResponse 객체
     * @param <T> 데이터의 타입
     */
    public static <T> SliceResponse<T> of(Slice<T> slice) {
        return SliceResponse.<T>builder()
                .content(slice.getContent())
                .currentSlice(slice.getNumber()+1) // 프론트 기준이므로, 실제보다 1 더 많게 반환한다.
                .size(slice.getSize())
                .hasNext(slice.hasNext())
                .build();
    }

    /**
     * Slice<Entity> -> mapper(Entity) -> Dto -> SliceResponse<Dto>로 변환하는 함수.
     * 사용 예시:
     *      SliceResponse.of(slice, TestMapper::toDto)
     * @param slice Slice<Entity> 객체
     * @param mapper Slice에 들어있는 Entity를 Dto로 변환하는 mapper 함수
     * @return 해당 Slice의 정보들이 담긴 SliceResponse<Dto>
     * @param <E> Entity
     * @param <T> Dto
     */
    public static <E, T> SliceResponse<T> of(Slice<E> slice, Function<E, T> mapper){
        /**
         * Function<E, T> mapper: Entity -> mapper -> Dto
         * Function<E, T>은 함수 자체를 인자로 전달받기 위한 타입이다.(자바의 함수형 인터페이스 중 하나임)
         * @FunctionalInterface
         * public interface Function<T, R>{
         *      R apply(T t);
         * }
         * => 입력(T)을 넣으면 출력(R)을 반환하는 함수
         */

        // 1. 전달 받은 mapper 함수를 이용하여, Slice<E>에 들어있는 Entity들을 Dto로 변환하여 List로 변환한다.
        List<T> content = slice.getContent().stream().map(mapper).toList();

        // 2. 변환한 content를 SliceResponse<T>에 넣어 설정한 뒤, 다른 세부 설정들도 할당해준다.
        return SliceResponse.<T>builder()
                .content(content)
                .currentSlice(slice.getNumber()+1)
                .size(slice.getSize())
                .hasNext(slice.hasNext())
                .build();
    }

}
