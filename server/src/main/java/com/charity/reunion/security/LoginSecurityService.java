package com.charity.reunion.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防爆破:同一账号连续失败 5 次锁定 5 分钟。
 * 锁定期间登录直接拒绝并返回剩余秒数,成功登录后计数清零。
 */
@Service
public class LoginSecurityService {

    private static final int MAX_FAIL = 5;
    private static final long LOCK_SECONDS = 5 * 60L;

    private record FailRecord(int count, LocalDateTime lockUntil) { }

    private final Map<String, FailRecord> fails = new ConcurrentHashMap<>();

    /** 账号是否处于锁定中,返回剩余秒数(0 表示未锁定) */
    public long lockedSeconds(String username) {
        FailRecord r = fails.get(username);
        if (r == null || r.lockUntil() == null) {
            return 0;
        }
        long left = Duration.between(LocalDateTime.now(), r.lockUntil()).getSeconds();
        if (left <= 0) {
            fails.remove(username);   // 锁定期已过,自动解锁
            return 0;
        }
        return left;
    }

    /** 记录一次失败,返回锁定后剩余秒数(未触发锁定则为 0) */
    public long recordFail(String username) {
        fails.compute(username, (k, old) -> {
            int count = (old == null || isExpired(old)) ? 1 : old.count() + 1;
            LocalDateTime until = count >= MAX_FAIL ? LocalDateTime.now().plusSeconds(LOCK_SECONDS) : null;
            return new FailRecord(count, until);
        });
        return lockedSeconds(username);
    }

    /** 失败次数(前端展示"还剩 N 次机会") */
    public int failCount(String username) {
        FailRecord r = fails.get(username);
        return r == null || isExpired(r) ? 0 : r.count();
    }

    public void clear(String username) {
        fails.remove(username);
    }

    private boolean isExpired(FailRecord r) {
        return r.lockUntil() != null && r.lockUntil().isBefore(LocalDateTime.now());
    }
}
