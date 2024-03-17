package io.hhplus.tdd.point;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public record UserPoint(
        Long id,
        Long point,
        Long updateMillis
) {
}
