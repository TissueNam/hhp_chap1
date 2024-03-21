package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserNotFoundException;
import io.hhplus.tdd.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class PointServiceUseTest {
    UserPointTable userPointTable;
    PointHistoryTable pointHistoryTable;
    PointService pointService;

    PointServiceUseTest() {
        this.userPointTable = mock(UserPointTable.class);
        this.pointHistoryTable = mock(PointHistoryTable.class);
        this.pointService = new PointService(userPointTable, pointHistoryTable);
    }

    // use 함수
    // 요구사항 분석
    // - 요청하는 유저 존재 여부 확인
    // - 해당 유저 존재한다면, amount 만큼 차감
    // - 차감된 유저의 정보 반환

    // 실패 TC
    // - 존재하지 않는 유저를 조회
    // - amount가 양수 일때
    // - 소유 포인트보다 더 많은 포인트를 차감하려 할 때
    // - 유저를 조회하다 DB 예외 발생
    // - 업데이트하다 DB 예외 발생 -> x

    @Test
    @DisplayName("use() 성공")
    void 성공(){
        // 셋업
        long userId = 123;
        long initialPoint = 100L;
        long amount = -30;

        PointHistory updatedHistory = new PointHistory(1L, userId, initialPoint + amount, TransactionType.USE, System.currentTimeMillis());
        when(pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis())).thenReturn(updatedHistory);

        UserPoint resultUserPoint = new UserPoint(userId, initialPoint + amount, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(resultUserPoint);
        // 실행
        UserPoint result = pointService.patchUse(userId, amount);

        // 검증
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(123);
        assertThat(result.point()).isEqualTo(70);
    }

    @Test
    @DisplayName("use() 존재하지 않는 유저에 대하여 요청하였을 때, UserNotFoundException 을 호출")
    void useNonExistentUser() {
        // 셋업
        Long userId = 999L;
        when(userPointTable.selectById(userId)).thenThrow(new UserNotFoundException("User not found"));

        // 실행, 검증
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            pointService.patchUse(userId, -1L);
        });

        // 추가 검증
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("amount가 양수 일때")
    void useInputPositiveNumTest(){
        // 실행, 검증
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.patchUse(123L, 100L);
        });

        // 추가 검증
        assertEquals("Amount must be a negative number.", exception.getMessage());
    }

    @Test
    @DisplayName("소유 포인트보다 더 많은 포인트를 차감하려 할 때")
    void useTooManyAmountTest(){
        // 셋업
        long userId = 123;
        long amount = -200;
        UserPoint userPoint = new UserPoint(userId, 100L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        // 실행, 검증
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.patchUse(userId, amount);
        });

        // 추가 검증
        assertEquals("You can't deduct more points than you own.", exception.getMessage());
    }

    @Test
    @DisplayName("유저를 조회하다 DB 예외 발생")
    void useSelectByIdDatabaseExceptionTest(){
        // 셋업
        long userId = 123L;
        long amount = -321L;

        doThrow(new RuntimeException("Database connection error")).when(userPointTable).selectById(userId);
        // 실행, 검증
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pointService.patchUse(userId, amount);
        });

        // 추가 검증
        assertEquals("Database connection error", exception.getMessage());
    }

}
