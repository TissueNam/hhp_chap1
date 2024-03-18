package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class PointTest {
    @Mock
    private UserPointTable userPointTable;
    @Mock
    private PointHistoryTable pointHistoryTable;
    private PointController pointController;
    @BeforeEach
    void init(){
        MockitoAnnotations.openMocks(this);
        pointController = new PointController(userPointTable, pointHistoryTable);
    }
    @Test
    @DisplayName("유저의 포인트를 조회하는 point()를 사용하였을때, 유저가 존재하지 않아 UserNotFoundException 을 호출하여야 한다.")
    void pointGetNotExistentUser() throws InterruptedException {
        // given
        Long userId = 123L;
        when(userPointTable.selectById(userId)).thenThrow(new UserNotFoundException("User not found"));

        // when, then
        assertThrows(UserNotFoundException.class, () -> {
            pointController.point(userId);
        });
    }

    @Test
    @DisplayName("point()를 요청하였을때, ID가 주어지지 않았을 때 Bad Request를 던져야 한다.")
    void pointWithoutIdBadRequest() {
        // when, then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointController.point(null);
        });

        // then
        assertEquals("ID must be provided", exception.getMessage());
    }

    @Test
    @DisplayName("history() 빈값 테스트")
    void historyInitTest() throws InterruptedException {
        // given
        Long userId = 123L;
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(Collections.emptyList());

        // when
        List<PointHistory> result = pointController.history(userId);

        // then
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("history() 을 사용하였을때, 유저의 내역이 없는 경우")
    void historyWithNoUserHistoryTest() throws InterruptedException {
        // given
        Long userId = 123L;
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(Collections.emptyList());

        // when
        List<PointHistory> result = pointController.history(userId);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("charge(), 존재하지 않는 유저를 조회하였을 때, UserNotFoundException 을 반환")
    void chargeNotExistentUser() throws InterruptedException {
        // given
        Long userId = 123L;
        when(userPointTable.selectById(userId)).thenThrow(new UserNotFoundException("User not found"));

        // when
        assertThrows(UserNotFoundException.class, () -> {
            pointController.charge(userId, 0L);
        });
    }

    @Test
    @DisplayName("charge(), amount의 값이 음수를 입력하였을 때, IllegalArgumentException(\"Amount must be a positive number.\") 반환")
    void chargeInputNegativeNum(){
        // when, then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointController.charge(123L, -1L);
        });

        // then
        assertEquals("Amount must be a positive number.", exception.getMessage());
    }

    @Test
    @DisplayName("charge(), insertOrUpdate()에서 데이터베이스 예외 발생")
    void chargeInsertOrUpdateDatabaseExceptionTest() throws InterruptedException{
        // given
        Long userId = 123L;
        Long amount = 321L;
        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, amount, 123L));

        // 모킹된 userPointTable의 insertOrUpdate() 메소드가 예외를 던지도록 설정
        doThrow(new RuntimeException("Database connection error")).when(userPointTable).insertOrUpdate(userId, amount);

        // when, then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pointController.charge(userId, amount);
        });

        assertEquals("Database connection error", exception.getMessage());
    }

    @Test
    @DisplayName("use(): 존재하지 않는 유저에 대하여 요청하였을 때")
    void useNonExistentUser() throws InterruptedException{
        // given
        Long userId = 123L;
        when(userPointTable.selectById(userId)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> {
            pointController.use(userId, 123L);
        });
    }
    @Test
    @DisplayName("use(): amount의 값이 양수를 입력하였을 때, IllegalArgumentException(\"Amount must be a negative number.\") 반환")
    void useInputPositiveNum(){
        //when, then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointController.use(123L, 1L);
        });

        // then
        assertEquals("Amount must be a negative number.", exception.getMessage());
    }

}
