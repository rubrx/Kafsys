package com.kafsys.common.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PagedResponseTest {

    @Test
    void of_computesTotalPages_ceilingOnRemainder() {
        PagedResponse<String> page = PagedResponse.of(List.of("a", "b"), 0, 10, 25);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.last()).isFalse();
        assertThat(page.content()).containsExactly("a", "b");
    }

    @Test
    void of_marksLastPage_whenOnFinalIndex() {
        PagedResponse<String> page = PagedResponse.of(List.of("x"), 2, 10, 25);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.last()).isTrue();
    }

    @Test
    void of_handlesEmptyPage_beyondBounds() {
        PagedResponse<String> page = PagedResponse.of(List.of(), 5, 10, 25);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.last()).isTrue();
        assertThat(page.content()).isEmpty();
    }

    @Test
    void of_singleFullPage_hasOneTotalPage() {
        PagedResponse<String> page = PagedResponse.of(List.of("a", "b"), 0, 2, 2);

        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.last()).isTrue();
    }
}
