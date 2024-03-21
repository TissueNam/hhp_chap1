package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.UserNotFoundException;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@AllArgsConstructor
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final ConcurrentHashMap<Long, Lock> lockMap = new ConcurrentHashMap<>();
    private static final long LOCK_TIMEOUT = 5L;

    public UserPoint getPoint(final Long userId) throws InterruptedException {
        if (userId == null){
            throw new IllegalArgumentException("ID must be provided");
        }
        Lock lock = lockMap.computeIfAbsent(userId, key -> new ReentrantLock());
        if(!lock.tryLock(LOCK_TIMEOUT, TimeUnit.SECONDS)){
            throw new RuntimeException("TimeOut! Failed to acquire lock for user: " + userId);
        } else {
            try {
                UserPoint userPoint = userPointTable.selectById(userId);
                if (userPoint == null) {
                    throw new UserNotFoundException("User not found");
                }
                return userPoint;
            } finally {
                lock.unlock();
            }
        }
    }

    public List<PointHistory> getHistory(final Long userId) throws InterruptedException {
        if (userId == null){
            throw new IllegalArgumentException("ID must be provided");
        }
        Lock lock = lockMap.computeIfAbsent(userId, key -> new ReentrantLock());
        if(!lock.tryLock(LOCK_TIMEOUT, TimeUnit.SECONDS)){
            throw new RuntimeException("TimeOut! Failed to acquire lock for user: " + userId);
        } else {
            try {
                UserPoint chkUser = userPointTable.selectById(userId);
                if (chkUser == null){
                    throw new UserNotFoundException("User not found");
                }
                return pointHistoryTable.selectAllByUserId(chkUser.id());
            } finally {
                lock.unlock();
            }
        }
    }

    public UserPoint patchCharge(final Long userId, final Long amount) throws InterruptedException {
        if (amount < 0L){
            throw new IllegalArgumentException("Amount must be a positive number.");
        }
        Lock lock = lockMap.computeIfAbsent(userId, key -> new ReentrantLock());
        if(!lock.tryLock(LOCK_TIMEOUT, TimeUnit.SECONDS)){
            throw new RuntimeException("TimeOut! Failed to acquire lock for user: " + userId);
        } else {
            try {
                UserPoint chkUser = userPointTable.selectById(userId);
                if (chkUser == null) {
                    throw new UserNotFoundException("User not found");
                }
                try {
                    return userPointTable.insertOrUpdate(chkUser.id(), chkUser.point() + amount);
                } catch (RuntimeException e){
                    throw new RuntimeException("Database connection error", e);
                }
            } finally {
                lock.unlock();
            }
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
