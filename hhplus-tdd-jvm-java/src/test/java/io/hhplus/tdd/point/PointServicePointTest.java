package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserNotFoundException;
import io.hhplus.tdd.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class PointServicePointTest {
    UserPointTable userPointTable;
    PointHistoryTable pointHistoryTable;
    PointService pointService;

    PointServicePointTest() {
        this.userPointTable = mock(UserPointTable.class);
        this.pointHistoryTable = mock(PointHistoryTable.class);
        this.pointService = new PointService(userPointTable, pointHistoryTable);

        when(userPointTable.selectById(123L)).thenReturn(new UserPoint(123L, 100L, System.currentTimeMillis()));
        when(userPointTable.selectById(999L)).thenReturn(null);
    }

    // point 함수
    // 요구사항 분석
    // - 요청 유저 아이디로 테이블내 유저 정보 조회

    // 실패 TC
    // - userId를 요청에 넣지 않음
    // - 존재하지 않는 유저를 조회함
    @Test
    @DisplayName("point() 성공")
    void 성공() throws InterruptedException {
        // 셋업
        Long userId = 123L;

        // 실행
        UserPoint result = pointService.getPoint(userId);

        // 검증
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(123);
        assertThat(result.point()).isEqualTo(100);
    }

    @Test
    @DisplayName("point()를 요청하였을때, ID가 주어지지 않았을 때 Bad Request를 던져야 한다.")
    void getPointByNullIdTest() throws InterruptedException{
        // 실행, 검증
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.getPoint(null);
        });

        // 추가 검증
        assertEquals("ID must be provided", exception.getMessage());
    }

    @Test
    @DisplayName("유저의 포인트를 조회하는 point()를 사용하였을때, 유저가 존재하지 않아 UserNotFoundException 을 호출하여야 한다.")
    void pointGetNotExistentUser() throws InterruptedException {
        // 셋업
        Long userId = 999L;
        when(userPointTable.selectById(userId)).thenThrow(new UserNotFoundException("User not found"));

        // 실행, 검증
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            pointService.getPoint(userId);
        });

        // 추가 검증
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("동시에 여러 스레드에서 사용자 조회")
    void pointTooManyAccessTest() throws InterruptedException {
        // 셋업
        long userId = 123;
        int threadCount = 10;

        // 실행
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    UserPoint result = pointService.getPoint(userId);
                    System.out.println(Thread.currentThread().getName()  + "->: " + result);
                    assertThat(result).isNotNull();
                    assertThat(result.id()).isEqualTo(123);
                    assertThat(result.point()).isEqualTo(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // 검증
        verify(userPointTable, times(threadCount)).selectById(userId);

        executor.shutdown();
    }
}
