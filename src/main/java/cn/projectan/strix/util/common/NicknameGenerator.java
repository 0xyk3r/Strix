package cn.projectan.strix.util.common;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 中文昵称生成器
 * <p>
 * 通过组合形容词和名词生成随机中文昵称，用于新注册用户的默认昵称。
 * <p>
 * 组合数量：约1000+形容词 × 1500+名词 × 10000数字后缀 = 百亿级别组合
 * <p>
 * 特性：
 * <ul>
 *   <li>高性能：使用数组存储词汇，O(1)随机访问</li>
 *   <li>线程安全：使用ThreadLocalRandom避免竞争</li>
 *   <li>懒加载：首次使用时加载词库到内存</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2025/1/29
 */
public class NicknameGenerator {

    private static final String ADJECTIVES_PATH = "nickname/adjectives.txt";
    private static final String NOUNS_PATH = "nickname/nouns.txt";

    /**
     * 使用静态内部类实现线程安全的懒加载（Initialization-on-demand holder idiom）
     */
    private static class WordHolder {
        static final String[] adjectives;
        static final String[] nouns;

        static {
            adjectives = loadWordList(ADJECTIVES_PATH);
            nouns = loadWordList(NOUNS_PATH);
            if (adjectives.length == 0 || nouns.length == 0) {
                throw new IllegalStateException(I18nUtil.get("error.nickname.dictionaryLoadFailed"));
            }
        }
    }

    /**
     * 生成随机昵称（不带数字后缀）
     * <p>
     * 格式：形容词 + 的 + 名词
     * 示例：快乐的小猫、温柔的云朵
     *
     * @return 随机昵称
     */
    public static String generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String adjective = WordHolder.adjectives[random.nextInt(WordHolder.adjectives.length)];
        String noun = WordHolder.nouns[random.nextInt(WordHolder.nouns.length)];
        return adjective + "的" + noun;
    }

    /**
     * 生成随机昵称（带数字后缀）
     * <p>
     * 格式：形容词 + 的 + 名词 + 数字
     * 示例：快乐的小猫2048、温柔的云朵666
     *
     * @param maxSuffix 数字后缀最大值（不包含），建议10000
     * @return 随机昵称
     */
    public static String generateWithSuffix(int maxSuffix) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String adjective = WordHolder.adjectives[random.nextInt(WordHolder.adjectives.length)];
        String noun = WordHolder.nouns[random.nextInt(WordHolder.nouns.length)];
        int suffix = random.nextInt(maxSuffix);
        return adjective + "的" + noun + suffix;
    }

    /**
     * 生成随机昵称（带4位数字后缀）
     * <p>
     * 格式：形容词 + 的 + 名词 + 4位数字
     * 示例：快乐的小猫2048、温柔的云朵0666
     *
     * @return 随机昵称
     */
    public static String generateWithPaddedSuffix() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String adjective = WordHolder.adjectives[random.nextInt(WordHolder.adjectives.length)];
        String noun = WordHolder.nouns[random.nextInt(WordHolder.nouns.length)];
        int suffix = random.nextInt(10000);
        return adjective + "的" + noun + String.format("%04d", suffix);
    }

    /**
     * 生成简洁昵称（无连接词）
     * <p>
     * 格式：形容词 + 名词
     * 示例：快乐小猫、温柔云朵
     *
     * @return 随机昵称
     */
    public static String generateSimple() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String adjective = WordHolder.adjectives[random.nextInt(WordHolder.adjectives.length)];
        String noun = WordHolder.nouns[random.nextInt(WordHolder.nouns.length)];
        return adjective + noun;
    }

    /**
     * 生成简洁昵称（带数字后缀）
     * <p>
     * 格式：形容词 + 名词 + 数字
     * 示例：快乐小猫2048、温柔云朵666
     *
     * @param maxSuffix 数字后缀最大值（不包含）
     * @return 随机昵称
     */
    public static String generateSimpleWithSuffix(int maxSuffix) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String adjective = WordHolder.adjectives[random.nextInt(WordHolder.adjectives.length)];
        String noun = WordHolder.nouns[random.nextInt(WordHolder.nouns.length)];
        int suffix = random.nextInt(maxSuffix);
        return adjective + noun + suffix;
    }

    /**
     * 获取可能的组合数量（不含数字后缀）
     *
     * @return 组合数量
     */
    public static long getCombinationCount() {
        return (long) WordHolder.adjectives.length * WordHolder.nouns.length;
    }

    /**
     * 获取可能的组合数量（含数字后缀）
     *
     * @param maxSuffix 数字后缀最大值
     * @return 组合数量
     */
    public static long getCombinationCountWithSuffix(int maxSuffix) {
        return (long) WordHolder.adjectives.length * WordHolder.nouns.length * maxSuffix;
    }

    /**
     * 获取形容词数量
     *
     * @return 形容词数量
     */
    public static int getAdjectiveCount() {
        return WordHolder.adjectives.length;
    }

    /**
     * 获取名词数量
     *
     * @return 名词数量
     */
    public static int getNounCount() {
        return WordHolder.nouns.length;
    }

    private static String[] loadWordList(String resourcePath) {
        List<String> words = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        words.add(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(I18nUtil.get("error.nickname.loadFailed", resourcePath), e);
        }
        return words.toArray(new String[0]);
    }

}
