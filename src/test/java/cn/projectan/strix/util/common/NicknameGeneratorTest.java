package cn.projectan.strix.util.common;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 昵称生成器测试
 *
 * @author ProjectAn
 * @since 2025/1/29
 */
@Slf4j
class NicknameGeneratorTest {

    @Test
    void testGenerate() {
        log.info("测试基础生成功能");
        for (int i = 0; i < 10; i++) {
            String nickname = NicknameGenerator.generate();
            log.info("生成昵称: {}", nickname);
            assertNotNull(nickname);
            assertTrue(nickname.contains("的"));
        }
    }

    @Test
    void testGenerateWithSuffix() {
        log.info("测试带数字后缀生成功能");
        for (int i = 0; i < 10; i++) {
            String nickname = NicknameGenerator.generateWithSuffix(10000);
            log.info("生成昵称(带后缀): {}", nickname);
            assertNotNull(nickname);
            assertTrue(nickname.contains("的"));
        }
    }

    @Test
    void testGenerateWithPaddedSuffix() {
        log.info("测试带固定长度数字后缀生成功能");
        for (int i = 0; i < 10; i++) {
            String nickname = NicknameGenerator.generateWithPaddedSuffix();
            log.info("生成昵称(4位后缀): {}", nickname);
            assertNotNull(nickname);
            assertTrue(nickname.contains("的"));
            assertTrue(nickname.matches(".*\\d{4}$"));
        }
    }

    @Test
    void testGenerateSimple() {
        log.info("测试简洁生成功能");
        for (int i = 0; i < 10; i++) {
            String nickname = NicknameGenerator.generateSimple();
            log.info("生成昵称(简洁): {}", nickname);
            assertNotNull(nickname);
            assertFalse(nickname.contains("的"));
        }
    }

    @Test
    void testCombinationCount() {
        long count = NicknameGenerator.getCombinationCount();
        long countWithSuffix = NicknameGenerator.getCombinationCountWithSuffix(10000);

        log.info("形容词数量: {}", NicknameGenerator.getAdjectiveCount());
        log.info("名词数量: {}", NicknameGenerator.getNounCount());
        log.info("不带后缀组合数: {}", count);
        log.info("带后缀组合数(0-9999): {}", countWithSuffix);

        assertTrue(count > 1_000_000, "组合数应超过100万");
        assertTrue(countWithSuffix > 10_000_000_000L, "带后缀组合数应超过100亿");
    }

    @Test
    void testUniqueness() {
        log.info("测试生成唯一性(带后缀)");
        Set<String> nicknames = new HashSet<>();
        int total = 1000000;

        for (int i = 0; i < total; i++) {
            nicknames.add(NicknameGenerator.generateWithSuffix(10000));
        }

        double uniqueRate = (double) nicknames.size() / total * 100;
        log.info("生成{}个昵称, 唯一数: {}, 唯一率: {}%", total, nicknames.size(), String.format("%.2f", uniqueRate));

        assertTrue(nicknames.size() > total * 0.99, "唯一率应超过99%");
    }

    @Test
    void testPerformance() {
        log.info("测试生成性能");

        // 预热
        for (int i = 0; i < 10000; i++) {
            NicknameGenerator.generate();
        }

        int iterations = 100_000_000;
        long start = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            NicknameGenerator.generateWithSuffix(10000);
        }

        long elapsed = System.nanoTime() - start;
        double avgNanos = (double) elapsed / iterations;
        double opsPerSecond = 1_000_000_000.0 / avgNanos;

        log.info("生成{}次, 总耗时: {}ms", iterations, elapsed / 1_000_000);
        log.info("平均每次: {}ns", String.format("%.2f", avgNanos));
        log.info("每秒可生成: {} 次", String.format("%.0f", opsPerSecond));

        assertTrue(avgNanos < 1000, "平均生成时间应小于1微秒");
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        log.info("测试多线程安全性");
        Set<String> allNicknames = java.util.Collections.synchronizedSet(new HashSet<>());
        int threadCount = 32;
        int perThread = 10000;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    allNicknames.add(NicknameGenerator.generateWithSuffix(10000));
                }
            });
        }

        long start = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long elapsed = System.currentTimeMillis() - start;

        log.info("{}个线程各生成{}次, 总耗时: {}ms", threadCount, perThread, elapsed);
        log.info("唯一昵称数: {}", allNicknames.size());

        assertTrue(allNicknames.size() > threadCount * perThread * 0.99);
    }

}
