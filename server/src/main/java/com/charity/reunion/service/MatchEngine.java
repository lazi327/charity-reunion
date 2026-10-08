package com.charity.reunion.service;

import com.charity.reunion.entity.SearchCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 亲缘雷达 · 自研五维匹配引擎(纯本地计算,无外部依赖):
 * <pre>
 * 维度          权重   算法
 * 姓名相似度    15%    Levenshtein 编辑距离归一化
 * 年龄吻合度    20%    高斯衰减 exp(-(Δ岁/8)^2)
 * 地理邻近度    25%    省份邻接图:同省100 / 邻省65 / 同大区40 / 其他10
 * 特征重合度    25%    体貌标签 Jaccard 相似系数
 * 时间接近度    15%    失踪年份差的分段衰减
 * </pre>
 * 某维度输入缺失时,该权重按比例摊给其余维度,保证总分始终归一 0-100。
 */
public final class MatchEngine {

    private MatchEngine() { }

    /** 单条档案的匹配结果 */
    public record Hit(SearchCase caseRow, int score, Map<String, Integer> dims, List<String> reasons) { }

    /** 查询向量 */
    public record QueryVector(String nameHint, Integer gender, Integer birthYear,
                              Integer lostYear, String province, List<String> tags) { }

    /** 31 省邻接表(简化地理图) */
    private static final Map<String, Set<String>> ADJACENT = new HashMap<>();
    /** 大区归属 */
    private static final Map<String, String> REGION = new HashMap<>();

    static {
        String[][] adj = {
                {"北京", "天津", "河北"}, {"天津", "北京", "河北", "山东"},
                {"河北", "北京", "天津", "山西", "山东", "河南", "辽宁", "内蒙古"},
                {"山西", "河北", "内蒙古", "陕西", "河南"}, {"内蒙古", "山西", "河北", "辽宁", "吉林", "黑龙江", "陕西", "宁夏", "甘肃"},
                {"辽宁", "吉林", "河北", "内蒙古"}, {"吉林", "黑龙江", "辽宁", "内蒙古"},
                {"黑龙江", "吉林", "内蒙古"}, {"上海", "江苏", "浙江"},
                {"江苏", "上海", "浙江", "安徽", "山东"}, {"浙江", "上海", "江苏", "安徽", "江西", "福建"},
                {"安徽", "江苏", "浙江", "江西", "湖北", "河南", "山东"}, {"福建", "浙江", "江西", "广东"},
                {"江西", "安徽", "浙江", "福建", "广东", "湖南", "湖北"}, {"山东", "河北", "河南", "安徽", "江苏"},
                {"河南", "河北", "山西", "陕西", "湖北", "安徽", "山东", "江苏"}, {"湖北", "河南", "陕西", "重庆", "湖南", "江西", "安徽"},
                {"湖南", "湖北", "重庆", "贵州", "广西", "广东", "江西"}, {"广东", "福建", "江西", "湖南", "广西", "海南"},
                {"广西", "广东", "湖南", "贵州", "云南"}, {"海南", "广东"},
                {"重庆", "四川", "贵州", "湖南", "湖北", "陕西"}, {"四川", "重庆", "陕西", "甘肃", "青海", "云南", "贵州", "西藏"},
                {"贵州", "四川", "重庆", "湖南", "广西", "云南"}, {"云南", "四川", "贵州", "广西", "西藏"},
                {"西藏", "四川", "云南", "青海", "新疆"}, {"陕西", "山西", "河南", "湖北", "重庆", "四川", "甘肃", "宁夏", "内蒙古"},
                {"甘肃", "陕西", "四川", "青海", "新疆", "内蒙古", "宁夏"}, {"青海", "甘肃", "四川", "西藏", "新疆"},
                {"宁夏", "甘肃", "陕西", "内蒙古"}, {"新疆", "甘肃", "青海", "西藏"}
        };
        for (String[] row : adj) {
            Set<String> neighbors = ADJACENT.computeIfAbsent(row[0], k -> new HashSet<>());
            for (int i = 1; i < row.length; i++) {
                neighbors.add(row[i]);
                ADJACENT.computeIfAbsent(row[i], k -> new HashSet<>()).add(row[0]);
            }
        }
        Map.of("东北", List.of("辽宁", "吉林", "黑龙江"), "华北", List.of("北京", "天津", "河北", "山西", "内蒙古"),
                "华东", List.of("上海", "江苏", "浙江", "安徽", "福建", "江西", "山东"),
                "华中", List.of("河南", "湖北", "湖南"), "华南", List.of("广东", "广西", "海南"),
                "西南", List.of("重庆", "四川", "贵州", "云南", "西藏"),
                "西北", List.of("陕西", "甘肃", "青海", "宁夏", "新疆"))
                .forEach((region, provs) -> provs.forEach(p -> REGION.put(p, region)));
    }

    /**
     * 计算档案与查询向量的匹配度。
     *
     * @return 总分 0-100 + 五维分解 + 中文匹配理由
     */
    public static Hit score(SearchCase c, QueryVector q) {
        Map<String, Integer> dims = new HashMap<>();
        Map<String, Integer> weights = new HashMap<>();
        List<String> reasons = new ArrayList<>();
        int currentYear = java.time.Year.now().getValue();

        // --- 1. 姓名相似度(Levenshtein) ---
        String caseName = c.getName() == null ? "" : c.getName().trim();
        if (q.nameHint() != null && !q.nameHint().isBlank() && !caseName.isEmpty()) {
            double sim = levenshteinSim(q.nameHint(), caseName);
            dims.put("name", (int) Math.round(sim * 100));
            weights.put("name", 15);
            if (sim >= 0.99) {
                reasons.add("姓名完全一致");
            } else if (sim >= 0.5) {
                reasons.add(String.format("姓名高度相似(%s↔%s)", q.nameHint(), caseName));
            }
        } else {
            dims.put("name", 0);   // 缺失:不计权
        }

        // --- 2. 年龄吻合度(高斯衰减) ---
        if (q.birthYear() != null && c.getBirthYear() != null) {
            int diff = Math.abs(q.birthYear() - c.getBirthYear());
            int s = (int) Math.round(100 * Math.exp(-Math.pow(diff / 8.0, 2)));
            dims.put("age", s);
            weights.put("age", 20);
            if (diff == 0) {
                reasons.add("出生年份完全吻合");
            } else if (diff <= 3) {
                reasons.add("年龄仅相差 " + diff + " 岁");
            }
        } else {
            dims.put("age", 0);
        }

        // --- 3. 地理邻近度(省份邻接图) ---
        String caseProvince = FeatureExtractor.provinceOf(c.getLostPlace());
        if (q.province() != null && caseProvince != null) {
            int s;
            if (q.province().equals(caseProvince)) {
                s = 100;
                reasons.add("失踪省份相同(" + caseProvince + ")");
            } else if (ADJACENT.getOrDefault(q.province(), Set.of()).contains(caseProvince)) {
                s = 65;
                reasons.add("失踪地为邻省(" + q.province() + "↔" + caseProvince + ")");
            } else if (REGION.getOrDefault(q.province(), "?").equals(REGION.get(caseProvince))) {
                s = 40;
                reasons.add("同属" + REGION.get(q.province()) + "地区");
            } else {
                s = 10;
            }
            dims.put("geo", s);
            weights.put("geo", 25);
        } else {
            dims.put("geo", 0);
        }

        // --- 4. 特征重合度(Jaccard) ---
        List<String> caseTags = FeatureExtractor.tagsOf(c.getFeatures(), c.getStory());
        if (q.tags() != null && !q.tags().isEmpty() && !caseTags.isEmpty()) {
            Set<String> inter = new HashSet<>(q.tags());
            inter.retainAll(caseTags);
            Set<String> union = new HashSet<>(q.tags());
            union.addAll(caseTags);
            double jac = (double) inter.size() / union.size();
            // Jaccard 对小集合过苛,命中任一标签即给基础分 40 再按重合放大
            int s = inter.isEmpty() ? 0 : (int) Math.min(100, 40 + jac * 100);
            dims.put("feature", s);
            weights.put("feature", 25);
            if (!inter.isEmpty()) {
                reasons.add("共同特征:" + String.join("、", inter));
            }
        } else {
            dims.put("feature", 0);
        }

        // --- 5. 时间接近度(分段衰减) ---
        int caseLostYear = c.getLostDate() != null ? c.getLostDate().getYear()
                : (c.getCreateTime() != null ? c.getCreateTime().getYear() : currentYear);
        if (q.lostYear() != null) {
            int diff = Math.abs(q.lostYear() - caseLostYear);
            int s = diff == 0 ? 100 : diff == 1 ? 85 : diff == 2 ? 70 : diff == 3 ? 55 : diff <= 5 ? 35 : diff <= 9 ? 22 : 15;
            dims.put("time", s);
            weights.put("time", 15);
            if (diff <= 2) {
                reasons.add("走失时间与 " + q.lostYear() + " 年接近");
            }
        } else {
            dims.put("time", 0);
        }

        // --- 性别硬性过滤维度:不符直接腰斩总分 ---
        boolean genderMismatch = q.gender() != null && c.getGender() != null && !q.gender().equals(c.getGender());

        // --- 加权归一:缺失维度权重摊给已有维度 ---
        int weightSum = weights.values().stream().mapToInt(Integer::intValue).sum();
        int total;
        if (weightSum == 0) {
            total = 5;   // 什么都没填,给个底分
            reasons.add("可补充更多特征以提高匹配精度");
        } else {
            double acc = 0;
            for (Map.Entry<String, Integer> e : weights.entrySet()) {
                acc += dims.getOrDefault(e.getKey(), 0) * e.getValue();
            }
            total = (int) Math.round(acc / weightSum);
        }
        if (genderMismatch) {
            total = total / 2;
            reasons.add("注意:性别与档案不符,得分已折半");
        }
        if (reasons.isEmpty()) {
            reasons.add("文本存在相近描述");
        }
        return new Hit(c, total, dims, reasons);
    }

    /** 字符级 Levenshtein 相似度(中文按字比较) */
    public static double levenshteinSim(String a, String b) {
        int n = a.length(), m = b.length();
        if (n == 0 || m == 0) {
            return 0;
        }
        // 短名包含长姓(如"张" vs "张小明")视为部分匹配
        if (a.contains(b) || b.contains(a)) {
            return 0.8 + 0.2 * Math.min(n, m) / Math.max(n, m);
        }
        int[] prev = new int[m + 1];
        int[] cur = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            cur[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return 1 - (double) prev[m] / Math.max(n, m);
    }
}
