package com.charity.reunion.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 亲缘雷达 · 本地特征抽取器(NLP-lite):
 * 不依赖任何大模型,用正则 + 词典 + 常识规则从自然语言描述中提取结构化寻亲特征。
 * 提取维度:性别 / 出生年 / 失踪年 / 失踪省份 / 体貌特征标签 / 姓名线索
 */
public final class FeatureExtractor {

    private FeatureExtractor() { }

    /** 省份及其常见别称/简称 */
    private static final Map<String, String[]> PROVINCE_ALIASES = Map.ofEntries(
            Map.entry("北京", new String[]{"北京", "京"}),
            Map.entry("天津", new String[]{"天津", "津"}),
            Map.entry("河北", new String[]{"河北", "冀", "石家庄", "保定", "唐山"}),
            Map.entry("山西", new String[]{"山西", "晋", "太原", "大同"}),
            Map.entry("内蒙古", new String[]{"内蒙古", "呼和浩特", "赤峰", "通辽"}),
            Map.entry("辽宁", new String[]{"辽宁", "辽", "沈阳", "大连"}),
            Map.entry("吉林", new String[]{"吉林", "长春"}),
            Map.entry("黑龙江", new String[]{"黑龙江", "黑", "哈尔滨", "齐齐哈尔"}),
            Map.entry("上海", new String[]{"上海", "沪", "浦东"}),
            Map.entry("江苏", new String[]{"江苏", "苏", "南京", "苏州", "无锡", "徐州", "南通"}),
            Map.entry("浙江", new String[]{"浙江", "浙", "杭州", "宁波", "温州", "金华"}),
            Map.entry("安徽", new String[]{"安徽", "皖", "合肥", "阜阳", "亳州"}),
            Map.entry("福建", new String[]{"福建", "闽", "福州", "厦门", "泉州"}),
            Map.entry("江西", new String[]{"江西", "赣", "南昌", "赣州"}),
            Map.entry("山东", new String[]{"山东", "鲁", "济南", "青岛", "临沂", "菏泽"}),
            Map.entry("河南", new String[]{"河南", "豫", "郑州", "南阳", "商丘", "周口"}),
            Map.entry("湖北", new String[]{"湖北", "鄂", "武汉", "襄阳", "黄冈"}),
            Map.entry("湖南", new String[]{"湖南", "湘", "长沙", "邵阳", "衡阳"}),
            Map.entry("广东", new String[]{"广东", "粤", "广州", "深圳", "东莞", "佛山", "天河", "潮汕"}),
            Map.entry("广西", new String[]{"广西", "桂", "南宁", "桂林", "玉林"}),
            Map.entry("海南", new String[]{"海南", "琼", "海口", "三亚"}),
            Map.entry("重庆", new String[]{"重庆"}),
            Map.entry("四川", new String[]{"四川", "川", "蜀", "成都", "绵阳", "南充"}),
            Map.entry("贵州", new String[]{"贵州", "黔", "贵阳", "毕节"}),
            Map.entry("云南", new String[]{"云南", "滇", "昆明", "曲靖"}),
            Map.entry("西藏", new String[]{"西藏", "藏", "拉萨"}),
            Map.entry("陕西", new String[]{"陕西", "陕", "秦", "西安", "咸阳"}),
            Map.entry("甘肃", new String[]{"甘肃", "陇", "兰州", "天水"}),
            Map.entry("青海", new String[]{"青海", "西宁"}),
            Map.entry("宁夏", new String[]{"宁夏", "银川"}),
            Map.entry("新疆", new String[]{"新疆", "乌鲁木齐", "喀什", "伊犁"}));

    /** 体貌/口音特征词典:命中即产出标签 */
    private static final Map<String, String[]> FEATURE_LEXICON = Map.ofEntries(
            Map.entry("高个子", new String[]{"高个", "较高", "一米七", "1.7", "1米7", "一米八", "1.8", "1米8", "身高"}),
            Map.entry("矮个子", new String[]{"矮", "一米六", "1.6", "1米6", "一米五", "1.5"}),
            Map.entry("圆脸", new String[]{"圆脸", "脸圆"}),
            Map.entry("瓜子脸", new String[]{"瓜子脸", "瘦脸", "脸瘦"}),
            Map.entry("黑皮肤", new String[]{"皮肤黑", "黝黑", "晒黑"}),
            Map.entry("左撇子", new String[]{"左撇", "左手干活", "惯用左手"}),
            Map.entry("疤痕", new String[]{"疤", "伤疤", "刀疤", "烫伤", "烧伤"}),
            Map.entry("痣", new String[]{"痣", "黑痣", "胎记"}),
            Map.entry("龅牙缺牙", new String[]{"龅牙", "缺牙", "掉牙", "兔牙", "假牙"}),
            Map.entry("戴眼镜", new String[]{"眼镜", "近视", "散光"}),
            Map.entry("口吃", new String[]{"口吃", "结巴", "说话不利索"}),
            Map.entry("听力障碍", new String[]{"聋", "听力", "耳背", "助听器"}),
            Map.entry("智力障碍", new String[]{"智力", "痴呆", "精神", "意识不清", "幼稚"}),
            Map.entry("方言口音", new String[]{"口音", "方言", "话", "乡音"}),
            Map.entry("跛脚步态", new String[]{"跛", "瘸", "走路姿势", "步态", "腿脚", "拐"}),
            Map.entry("文身", new String[]{"文身", "纹身"}),
            Map.entry("断指", new String[]{"断指", "缺指", "手指断"}),
            Map.entry("走失于车站", new String[]{"车站", "火车站", "汽车站", "码头", "集市", "庙会"})
    );

    private static final Pattern P_BIRTH = Pattern.compile("(19\\d{2}|20\\d{2})\\s*年(前后)?(出生|生人?|生的)?");
    private static final Pattern P_AGE_NOW = Pattern.compile("(今年|时年|年方|\\d{1,2}\\s*岁)");
    private static final Pattern P_AGE = Pattern.compile("(今年|时年|年方|走失时|当时)?\\s*(\\d{1,2})\\s*岁");
    private static final Pattern P_LOST_YEAR = Pattern.compile("(19\\d{2}|20\\d{2})\\s*年[^。;]{0,10}?(走失|失踪|离开家|离家|出走|丢失)");
    private static final Pattern P_YEARS_AGO = Pattern.compile("(\\d{1,2})\\s*年(前|多前)(走失|失踪)?");
    private static final Pattern P_NAME = Pattern.compile("(叫|名叫|名为|名字是|系)([\u4e00-\u9fa5]{2,4})");
    /** 称谓直接接姓名(如"弟弟张小明,1995年出生"),后接标点/年份/关键字才认定,防误抽 */
    private static final Pattern P_NAME_AFTER_TITLE = Pattern.compile(
            "(弟弟|妹妹|哥哥|姐姐|儿子|女儿|孩子|侄儿|外甥)([\u4e00-\u9fa5]{2,3})(?=[,，、;；]|19|20|今年|出生|走失|失踪)");
    private static final Pattern P_XING = Pattern.compile("([⻰龙王李张刘陈杨黄赵吴周徐孙马朱胡郭何高林罗郑梁谢宋唐许韩冯邓曹彭曾肖田董袁潘蒋蔡余杜叶程苏魏吕丁任沈姚卢姜崔钟谭陆汪范金石廖贾夏韦付方白邹孟熊秦邱江尹薛闫段雷侯龙史陶黎贺顾毛郝龚邵万钱严覃武戴莫孔向汤])姓");

    /** 抽取结果 */
    public record Extracted(Integer gender, Integer birthYear, Integer lostYear,
                            String province, List<String> tags, String nameHint) {

        public boolean isEmpty() {
            return gender == null && birthYear == null && lostYear == null
                    && province == null && tags.isEmpty() && nameHint == null;
        }
    }

    /** 从自由文本中抽取结构化特征 */
    public static Extracted extract(String text) {
        if (text == null || text.isBlank()) {
            return new Extracted(null, null, null, null, List.of(), null);
        }
        String t = text.trim();
        int currentYear = java.time.Year.now().getValue();

        // 性别:代词与亲属称谓加权投票
        int gScore = count(t, "男", "他", "儿子", "兄弟", "父亲", "爸", "哥哥", "弟弟", "叔", "爷爷")
                - count(t, "女", "她", "女儿", "姐妹", "母亲", "妈", "姐姐", "妹妹", "姑", "奶奶");
        Integer gender = gScore > 0 ? 1 : (gScore < 0 ? 2 : null);

        // 出生年:优先 "今年X岁" 反推,其次 "19xx年出生"
        Integer birthYear = null;
        Matcher mA = P_AGE.matcher(t);
        if (mA.find() && !mA.group(1).contains("19") && !mA.group(1).contains("20")) {
            int age = Integer.parseInt(mA.group(2));
            if (age < 110) {
                birthYear = currentYear - age;
            }
        }
        if (birthYear == null) {
            Matcher mB = P_BIRTH.matcher(t);
            while (mB.find()) {
                int y = Integer.parseInt(mB.group(1));
                // 紧跟"走失/失踪"的年份跳过(那是失踪年)
                String tail = t.substring(mB.end(), Math.min(t.length(), mB.end() + 6));
                if (!tail.contains("走失") && !tail.contains("失踪") && !tail.contains("离家")) {
                    birthYear = y;
                    break;
                }
            }
        }

        // 失踪年:"19xx年走失" 或 "N年前走失"
        Integer lostYear = null;
        Matcher mL = P_LOST_YEAR.matcher(t);
        if (mL.find()) {
            lostYear = Integer.parseInt(mL.group(1));
        } else {
            Matcher mY = P_YEARS_AGO.matcher(t);
            if (mY.find()) {
                lostYear = currentYear - Integer.parseInt(mY.group(1));
            }
        }
        if (lostYear != null && birthYear != null && lostYear < birthYear) {
            lostYear = null;   // 数据矛盾则丢弃
        }

        // 省份:别称命中最多者
        String province = null;
        int best = 0;
        for (Map.Entry<String, String[]> e : PROVINCE_ALIASES.entrySet()) {
            int hits = 0;
            for (String alias : e.getValue()) {
                if (t.contains(alias)) {
                    hits += alias.length() >= 2 ? 2 : 1;   // 全称权重高于单字简称
                }
            }
            if (hits > best) {
                best = hits;
                province = e.getKey();
            }
        }

        // 特征标签:词典命中
        Set<String> tags = new LinkedHashSet<>();
        for (Map.Entry<String, String[]> e : FEATURE_LEXICON.entrySet()) {
            for (String kw : e.getValue()) {
                if (t.contains(kw)) {
                    tags.add(e.getKey());
                    break;
                }
            }
        }

        // 姓名线索:优先"叫X"式,次选"称谓+X"式,最后"X姓"式兜底
        String nameHint = null;
        Matcher mN = P_NAME.matcher(t);
        if (mN.find()) {
            nameHint = mN.group(2);
        } else {
            Matcher mT = P_NAME_AFTER_TITLE.matcher(t);
            if (mT.find()) {
                nameHint = mT.group(2);
            } else {
                Matcher mX = P_XING.matcher(t);
                if (mX.find()) {
                    nameHint = mX.group(1);
                }
            }
        }

        return new Extracted(gender, birthYear, lostYear, province, new ArrayList<>(tags), nameHint);
    }

    /** 从档案文本(features+story+name)提取标签集合,供 Jaccard 比对 */
    public static List<String> tagsOf(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null) {
                sb.append(p).append(' ');
            }
        }
        Set<String> tags = new LinkedHashSet<>();
        String t = sb.toString();
        for (Map.Entry<String, String[]> e : FEATURE_LEXICON.entrySet()) {
            for (String kw : e.getValue()) {
                if (t.contains(kw)) {
                    tags.add(e.getKey());
                    break;
                }
            }
        }
        return new ArrayList<>(tags);
    }

    /** 提取省份(供档案向量归一化) */
    public static String provinceOf(String place) {
        if (place == null) {
            return null;
        }
        for (Map.Entry<String, String[]> e : PROVINCE_ALIASES.entrySet()) {
            for (String alias : e.getValue()) {
                if (place.contains(alias)) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    private static int count(String t, String... keys) {
        int n = 0;
        for (String k : keys) {
            if (t.contains(k)) {
                n++;
            }
        }
        return n;
    }
}
