package com.kafsys.common.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ok_wrapsPayload_marksSuccess_populatesTimestamp() {
        ApiResponse<String> response = ApiResponse.ok("hello");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("hello");
        assertThat(response.error()).isNull();
        assertThat(response.message()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void ok_withMessage_carriesMessage() {
        ApiResponse<Integer> response = ApiResponse.ok(42, "counted");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(42);
        assertThat(response.message()).isEqualTo("counted");
    }

    @Test
    void error_hasNullData_andErrorText() {
        ApiResponse<String> response = ApiResponse.error("something broke");

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error()).isEqualTo("something broke");
    }

    @Test
    void created_flagsSuccess_andSetsDefaultMessage() {
        ApiResponse<List<Integer>> response = ApiResponse.created(List.of(1, 2, 3));

        assertThat(response.success()).isTrue();
        assertThat(response.data()).containsExactly(1, 2, 3);
        assertThat(response.message()).isEqualTo("Resource created successfully");
    }
}
