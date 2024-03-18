package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserNotFoundException;
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
    private final PointHistoryTable pointHistoryTable;

    @Autowired
    public PointController(
            UserPointTable userPointTable,
            PointHistoryTable pointHistoryTable
    ){
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public UserPoint point(@PathVariable Long id) throws InterruptedException {
        if (id == null){
            throw new IllegalArgumentException("ID must be provided");
        }
        UserPoint userPoint = userPointTable.selectById(id);
        if (userPoint == null) {
            throw new UserNotFoundException("User not found");
        }
        return userPoint;
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(@PathVariable Long id) throws InterruptedException {
        UserPoint chkUser = userPointTable.selectById(id);
        if (chkUser == null){
            throw new UserNotFoundException("User not found");
        }
        return pointHistoryTable.selectAllByUserId(id);
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(@PathVariable Long id, @RequestBody Long amount) throws InterruptedException{
        if (amount < 0L){
            throw new IllegalArgumentException("Amount must be a positive number.");
        }
        UserPoint chkUser = userPointTable.selectById(id);
        if (chkUser == null) {
            throw new UserNotFoundException("User not found");
        }
        return userPointTable.insertOrUpdate(id, chkUser.point() + amount);
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public UserPoint use(@PathVariable Long id, @RequestBody Long amount) throws InterruptedException{
        if (amount > 0L){
            throw new IllegalArgumentException("Amount must be a negative number.");
        }
        UserPoint chkUser = userPointTable.selectById(id);
        if (chkUser == null) {
            throw new UserNotFoundException("User not found");
        }
        pointHistoryTable.insert(id, chkUser.point() - amount, TransactionType.USE, System.currentTimeMillis());
        return userPointTable.selectById(id);
    }
}
