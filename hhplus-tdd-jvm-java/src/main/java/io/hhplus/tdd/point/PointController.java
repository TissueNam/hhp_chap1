package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequestMapping("/point")
@RestController
public class PointController {
    private final UserPointTable userPointTable;
    @Autowired
    public PointController(UserPointTable userPointTable){
        this.userPointTable = userPointTable;
    }

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public UserPoint point(@PathVariable Long id)  {
        try {
            return userPointTable.selectById(id);
        } catch (InterruptedException e) {
            log.error("Error retrieving user point for id: {}", id, e);
            return new UserPoint(id, 0L, System.currentTimeMillis());
        }
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(@PathVariable Long id) {
        return Collections.emptyList();
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(@PathVariable Long id, @RequestBody Long amount) {
        return new UserPoint(0L, 0L, 0L);
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public UserPoint use(@PathVariable Long id, @RequestBody Long amount) {
        return new UserPoint(0L, 0L, 0L);
    }
}
