package com.example.egobook_be.global.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class SliceResponseTest {
    static class TestEntity{
        String name;
        int age;
        TestEntity(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
    // 테스트를 위한 간단한 DTO 클래스
    static record TestDto(String name) {
        // Entity -> DTO 변환 메서드
        static TestDto from(TestEntity entity) {
            return new TestDto(entity.name);
        }
    }
    @Test
    @DisplayName("Slice<T>를 그대로 SliceResponse<T>로 변환한다.")
    void of_Success_Same_Type() {
        // given
        List<String> content = List.of("Apple", "Banana", "Cherry");
        Pageable pageable = PageRequest.of(0, 3); // 0번 페이지, 사이즈 3
        boolean hasNext = true;

        // SliceImpl을 사용하여 가짜 Slice 객체 생성
        Slice<String> stringSlice = new SliceImpl<>(content, pageable, hasNext);

        // when
        SliceResponse<String> response = SliceResponse.of(stringSlice);

        // then
        assertThat(response.content()).hasSize(3);
        assertThat(response.content()).containsExactly("Apple", "Banana", "Cherry");
        assertThat(response.page()).isEqualTo(1); // 프론트 기준으로 +1 하므로 1이어야 맞는 것이다.
        assertThat(response.size()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Slice<Entity>와 Mapper를 사용하여 SliceResponse<Dto>로 변환한다.")
    void of_Success_With_Mapper() {
        // given
        // 1. Entity 리스트 준비
        List<TestEntity> entities = List.of(
                new TestEntity("User1", 20),
                new TestEntity("User2", 25)
        );
        Pageable pageable = PageRequest.of(1, 2); // 1번 페이지, 사이즈 2
        boolean hasNext = false; // 마지막 페이지라고 가정

        // 2. Entity Slice 생성
        Slice<TestEntity> entitySlice = new SliceImpl<>(entities, pageable, hasNext);

        // when
        // 3. Entity Slice -> DTO SliceResponse 변환 (Mapper 사용: TestDto::from)
        SliceResponse<TestDto> response = SliceResponse.of(entitySlice, TestDto::from);

        // then
        // 1. 내용물(Content) 검증
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).name()).isEqualTo("User1"); // DTO로 잘 변환되었는지 확인
        assertThat(response.content().get(1).name()).isEqualTo("User2");

        // 2. 메타데이터(Slice 정보) 검증
        assertThat(response.page()).isEqualTo(2); // 페이지 번호
        assertThat(response.size()).isEqualTo(2);         // 페이지 크기
        assertThat(response.hasNext()).isFalse();         // 다음 페이지 여부
    }

    @Test
    @DisplayName("빈 Slice가 들어왔을 때도 정상적으로 빈 리스트를 반환해야 한다.")
    void of_Success_Empty_Slice() {
        // given
        List<TestEntity> emptyList = List.of();
        Pageable pageable = PageRequest.of(0, 10);
        Slice<TestEntity> emptySlice = new SliceImpl<>(emptyList, pageable, false);

        // when
        SliceResponse<TestDto> response = SliceResponse.of(emptySlice, TestDto::from);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.size()).isEqualTo(10); // 요청한 사이즈는 10
    }

}
