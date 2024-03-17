package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class PointControllerTest {
    private PointController pointController;

    @Mock
    private UserPointTable userPointTable;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        pointController = new PointController(userPointTable);
    }

    /* ----------- GET: point() ---------------
     * 1. getPointForNotExistingUser()
     *  // given, when : 존재하지 않는 유저에 대해 조회를 요청하였을 때
     *  // then : UserNotFoundException 에러 메세지를 호출
     *  1-1. 기존 메소드로 실험 -> 실패
     *   -> point() 미구현 실패
     *  1-2. point() 구현 -> 성공
     *  1-3. UserNotFoundException 작성, 호출되도록 변경
    */
    @Test
    void getPointForNotExistingUser() throws Exception {
        // given
        Long userId = 123L;
        when(userPointTable.selectById(userId)).thenThrow(new UserNotFoundException("User not found"));

        // when
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            pointController.point(userId);
        });

        // then
        assertEquals("User not found", exception.getMessage());
    }
}
