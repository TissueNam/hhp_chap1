package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserNotFoundException;
import io.hhplus.tdd.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class PointServiceHistoryTest {
    UserPointTable userPointTable;
    PointHistoryTable pointHistoryTable;
    PointService pointService;

    PointServiceHistoryTest(){
        this.userPointTable = mock(UserPointTable.class);
        this.pointHistoryTable = mock(PointHistoryTable.class);
        this.pointService = new PointService(userPointTable, pointHistoryTable);

        when(userPointTable.selectById(999L)).thenReturn(null);
        when(userPointTable.selectById(123L)).thenReturn(new UserPoint(123L, 80L, System.currentTimeMillis()));
        List<PointHistory> mockPointHistories = new ArrayList<>();
        mockPointHistories.add(new PointHistory(1L, 123L, 100L, TransactionType.CHARGE, System.currentTimeMillis()));
        mockPointHistories.add(new PointHistory(2L, 123L, 20L, TransactionType.USE, System.currentTimeMillis()+1));

        when(pointHistoryTable.selectAllByUserId(123L)).thenReturn(mockPointHistories);
    }


    // history 함수
    // 요구사항 분석
    // - 요청한 유저가 테이블에 존재하는 유저인지 확인
    // - 유저가 존재한다면 유저에 대한 내역 리스트 조회

    // 실패 TC
    // - 존재하지 않는 유저를 조회함
    // - userId를 요청에 넣지 않음

    @Test
    @DisplayName("history() 성공")
    void 성공() throws InterruptedException {
        // 셋업
        Long userId = 123L;

        // 실행
        List<PointHistory> result = pointService.getHistory(userId);

        // 검증
        assertThat(result).isNotNull();
        assertThat(result.get(0).userId()).isEqualTo(123);
        assertThat(result.get(0).amount()).isEqualTo(100);
        assertThat(result.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(result.get(1).userId()).isEqualTo(123);
        assertThat(result.get(1).amount()).isEqualTo(20);
        assertThat(result.get(1).type()).isEqualTo(TransactionType.USE);
    }

    @Test
    @DisplayName("history()을 사용하였을때, ID가 주어지지 않았을 때 Bad Request 응답")
    void historyByNullIdTest() throws InterruptedException{
        // 실행, 검증
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.getHistory(null);
        });

        // 추가 검증
        assertEquals("ID must be provided", exception.getMessage());
    }

    @Test
    @DisplayName("history()을 사용하였을때, 유저의 내역이 없는 경우 UserNotFoundException 을 호출")
    void historyWithNoUserHistoryTest() throws InterruptedException {
        // 셋업
        Long userId = 999L;
        when(userPointTable.selectById(userId)).thenThrow(new UserNotFoundException("User not found"));

        // 실행
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            pointService.getHistory(userId);
        });

        // 검증
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("동시에 여러 스레드에서 사용자 충전/사용 내역 조회")
    void historyTooManyAccessTest() throws InterruptedException, ExecutionException {
        // 셋업
        long userId = 123;
        int threadCount = 10;
        List<PointHistory> expectedPointHistory = new ArrayList<>();
        expectedPointHistory.add(new PointHistory(1, userId, 50, TransactionType.CHARGE, System.currentTimeMillis()));
        expectedPointHistory.add(new PointHistory(2, userId, -20, TransactionType.USE, System.currentTimeMillis()));

        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 100L, System.currentTimeMillis()));
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expectedPointHistory);

        // 실행
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<List<PointHistory>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Future<List<PointHistory>> future = executor.submit(() -> {
                List<PointHistory> history = pointService.getHistory(userId);
                System.out.println("Thread " + Thread.currentThread().getName() + " ->: " + history);
                latch.countDown();
                return history;
            });
            futures.add(future);
        }
        latch.await();

        // 검증
        for (Future<List<PointHistory>> future : futures) {
            try {
                List<PointHistory> res = future.get();
                assertThat(res).isNotNull();
                assertThat(res).containsExactlyInAnyOrderElementsOf(expectedPointHistory);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        verify(userPointTable, times(threadCount)).selectById(userId);
        verify(pointHistoryTable, times(threadCount)).selectAllByUserId(userId);

        executor.shutdown();
    }
}
