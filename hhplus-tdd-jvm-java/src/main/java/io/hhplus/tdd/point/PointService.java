package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public UserPoint getPoint(final Long userId) {
        if (userId == null){
            throw new IllegalArgumentException("ID must be provided");
        }
        UserPoint userPoint = userPointTable.selectById(userId);
        if (userPoint == null) {
            throw new UserNotFoundException("User not found");
        }
        return userPoint;
    }

    public List<PointHistory> getHistory(final Long userId){
        if (userId == null){
            throw new IllegalArgumentException("ID must be provided");
        }
        UserPoint chkUser = userPointTable.selectById(userId);
        if (chkUser == null){
            throw new UserNotFoundException("User not found");
        }
        return pointHistoryTable.selectAllByUserId(chkUser.id());
    }

    public UserPoint patchCharge(final Long userId, final Long amount) {
        if (amount < 0L){
            throw new IllegalArgumentException("Amount must be a positive number.");
        }
        UserPoint chkUser = userPointTable.selectById(userId);
        if (chkUser == null) {
            throw new UserNotFoundException("User not found");
        }
        try {
            return userPointTable.insertOrUpdate(chkUser.id(), chkUser.point() + amount);
        } catch (RuntimeException e){
            throw new RuntimeException("Database connection error", e);
        }
    }

    public UserPoint patchUse(final Long userId, final Long amount){
        if (amount > 0L){
            throw new IllegalArgumentException("Amount must be a negative number.");
        }
        UserPoint chkUser = userPointTable.selectById(userId);
        if (chkUser == null) {
            throw new UserNotFoundException("User not found");
        }
        if (chkUser.point() + amount < 0){
            throw new IllegalArgumentException("You can't deduct more points than you own.");
        }
        try {
            pointHistoryTable.insert(chkUser.id(), chkUser.point() + amount, TransactionType.USE, System.currentTimeMillis());
            return userPointTable.selectById(chkUser.id());
        }catch (RuntimeException e){
            throw new RuntimeException("Database connection error", e);
        }
    }
}
