package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class PointServiceChargeTest {
    UserPointTable userPointTable;
    PointHistoryTable pointHistoryTable;
    PointService pointService;

    PointServiceChargeTest() {
        this.userPointTable = mock(UserPointTable.class);
        this.pointHistoryTable = mock(PointHistoryTable.class);
        this.pointService = new PointService(userPointTable, pointHistoryTable);

        when(userPointTable.selectById(123L)).thenReturn(new UserPoint(123L, 100L, System.currentTimeMillis()));
        when(userPointTable.selectById(999L)).thenReturn(null);
    }

    // charge 함수
    // 요구사항 분석
    // - 요청하는 유저 존재 여부 확인
    // - 존재한다면, amount 더해서 save

    // 실패 TC
    // - 존재하지 않는 유저를 조회
    // - amount가 음수 일때
    // - 유저를 조회하다 DB 예외 발생
    // - 업데이트하다 DB 예외 발생
    @Test
    @DisplayName("charge() 성공")
    void 성공() {
        // 셋업
        long userId = 123L;
        long amount = 100L;
        UserPoint tmpUser = new UserPoint(123L, 100L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(tmpUser);
        when(userPointTable.insertOrUpdate(userId, tmpUser.point() + amount))
                .thenReturn(new UserPoint(userId, tmpUser.point() + amount, System.currentTimeMillis()));

        // 실행
        UserPoint result = pointService.patchCharge(userId, amount);

        // 검증
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(123);
        assertThat(result.point()).isEqualTo(200);
    }
    @Test
    @DisplayName("charge()를 사용하였을때, 유저가 존재하지 않아 UserNotFoundException 을 호출하여야 한다.")
    void pointGetNotExistentUser() {
        // 셋업
        Long userId = 999L;
        when(userPointTable.selectById(userId)).thenThrow(new UserNotFoundException("User not found"));

        // 실행, 검증
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            pointService.patchCharge(userId, 123L);
        });

        // 추가 검증
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("charge(), amount의 값이 음수를 입력하였을 때, IllegalArgumentException(\"Amount must be a positive number.\") 반환")
    void chargeInputNegativeNum(){
        // 실행, 검증
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.patchCharge(123L, -1L);
        });

        // 추가 검증
        assertEquals("Amount must be a positive number.", exception.getMessage());
    }

    @Test
    @DisplayName("charge(), selectById()에서 데이터베이스 예외 발생")
    void chargeSelectByIdDatabaseExceptionTest() {
        // 셋업
        long userId = 123L;
        long amount = 321L;

        doThrow(new RuntimeException("Database connection error")).when(userPointTable).selectById(userId);

        // 실행, 검증
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pointService.patchCharge(userId, amount);
        });

        // 추가 검증
        assertEquals("Database connection error", exception.getMessage());
    }

    @Test
    @DisplayName("charge(), insertOrUpdate()에서 데이터베이스 예외 발생")
    void chargeInsertOrUpdateDatabaseExceptionTest() {
        // 셋업
        long userId = 123L;
        long amount = 100L;
        UserPoint tmpUser = new UserPoint(123L, 100L, System.currentTimeMillis());
        doThrow(new RuntimeException("Database connection error")).when(userPointTable).insertOrUpdate(userId, tmpUser.point() + amount);

        // 실행, 검증
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pointService.patchCharge(userId, amount);
        });

        // 추가 검증
        assertEquals("Database connection error", exception.getMessage());
    }

}
