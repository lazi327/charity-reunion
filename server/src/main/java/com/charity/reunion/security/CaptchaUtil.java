package com.charity.reunion.security;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * 服务端算术图形验证码:
 * - 随机生成加法/减法/乘法算式,AWT 绘制成带干扰线、噪点、字符旋转的 PNG(base64)
 * - 答案存内存 Map,5 分钟过期,一次性消费(验证即删除,防重放)
 * - 每次生成时顺带惰性清理过期条目,无需定时任务
 */
public class CaptchaUtil {

    private static final long TTL_MILLIS = 5 * 60 * 1000L;
    private static final int WIDTH = 130, HEIGHT = 46;
    private static final Map<String, Entry> STORE = new ConcurrentHashMap<>();
    private static final Random RND = new Random();

    private record Entry(int answer, long expireAt) { }

    /** 生成验证码,返回 [id, dataUrl] */
    public static String[] create() {
        cleanupExpired();
        int a = RND.nextInt(1, 10), b = RND.nextInt(1, 10);
        String op = new String[]{"+", "-", "×"}[RND.nextInt(3)];
        int answer = switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            default -> a * b;
        };
        String question = a + " " + op + " " + b + " = ?";

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        STORE.put(id, new Entry(answer, System.currentTimeMillis() + TTL_MILLIS));
        return new String[]{id, "data:image/png;base64," + draw(question)};
    }

    /** 校验(一次性):无论成败都删除该 id,防止暴力重试同一验证码 */
    public static boolean verify(String id, String input) {
        if (id == null || input == null) {
            return false;
        }
        Entry entry = STORE.remove(id);
        if (entry == null || entry.expireAt() < System.currentTimeMillis()) {
            return false;
        }
        try {
            return Integer.parseInt(input.trim()) == entry.answer();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void cleanupExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = STORE.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expireAt() < now) {
                it.remove();
            }
        }
    }

    /** 绘制算式图片:渐变底 + 噪点 + 干扰线 + 逐字符随机旋转 */
    private static String draw(String question) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 浅色渐变背景
        g.setPaint(new GradientPaint(0, 0, new Color(0xf7f9fc), WIDTH, HEIGHT, new Color(0xe9f0fa)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // 噪点
        for (int i = 0; i < 60; i++) {
            g.setColor(randomColor(150, 220));
            g.fillOval(RND.nextInt(WIDTH), RND.nextInt(HEIGHT), 2, 2);
        }
        // 干扰线
        for (int i = 0; i < 4; i++) {
            g.setColor(randomColor(170, 230));
            g.drawLine(RND.nextInt(WIDTH), RND.nextInt(HEIGHT), RND.nextInt(WIDTH), RND.nextInt(HEIGHT));
        }
        // 逐字符绘制,随机颜色/字号/微旋转
        int x = 8;
        for (char c : question.toCharArray()) {
            g.setColor(randomColor(20, 110));
            g.setFont(new Font("Arial Black", Font.BOLD, 17 + RND.nextInt(5)));
            double angle = (RND.nextDouble() - 0.5) * 0.5;
            Graphics2D gc = (Graphics2D) g.create();
            gc.rotate(angle, x + 6, HEIGHT / 2.0);
            gc.drawString(String.valueOf(c), x, 30 + RND.nextInt(6));
            gc.dispose();
            x += g.getFontMetrics().charWidth(c) + 1;
        }
        g.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码生成失败", e);
        }
    }

    private static Color randomColor(int min, int max) {
        return new Color(min + RND.nextInt(max - min), min + RND.nextInt(max - min), min + RND.nextInt(max - min));
    }
}
