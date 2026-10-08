-- =====================================================
-- 公益寻亲网站 数据库初始化脚本
-- 数据库: cf_reunion   MySQL 8.0+
-- 执行方式: mysql -uroot -p < init.sql
-- 角色: ADMIN 管理员 / USER 寻亲者
-- =====================================================
CREATE DATABASE IF NOT EXISTS cf_reunion DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE cf_reunion;

DROP TABLE IF EXISTS t_vote, t_wish, t_login_log, t_answer, t_question, t_guide, t_clue, t_case, t_banner, t_message, t_user;

-- 用户表(寻亲者 + 管理员)
CREATE TABLE t_user (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
  password    VARCHAR(120) NOT NULL COMMENT '密码(加密存储)',
  nickname    VARCHAR(50)  NOT NULL COMMENT '昵称',
  gender      TINYINT      NOT NULL COMMENT '1男 2女',
  birth_year  INT          COMMENT '出生年份',
  city        VARCHAR(50)  COMMENT '所在城市',
  about_me    VARCHAR(500) COMMENT '联系说明',
  avatar      VARCHAR(255) COMMENT '头像地址',
  phone       VARCHAR(20)  COMMENT '联系电话',
  role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER寻亲者/ADMIN管理员',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0待审核 1正常 2未通过 3已停用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户';

-- 首页轮播图
CREATE TABLE t_banner (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  title      VARCHAR(120) NOT NULL COMMENT '标题',
  image      VARCHAR(255) NOT NULL COMMENT '图片地址',
  link_url   VARCHAR(255) COMMENT '跳转地址(站内路径或外链)',
  sort_order INT          NOT NULL DEFAULT 0 COMMENT '排序,越小越前',
  status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '首页轮播图';

-- 寻亲信息
CREATE TABLE t_case (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT       NOT NULL COMMENT '发布寻亲者',
  relation     VARCHAR(20)  NOT NULL COMMENT '寻亲类型:寻子女/寻父母/寻配偶/寻其他亲属',
  name         VARCHAR(50)  NOT NULL COMMENT '失踪人姓名',
  gender       TINYINT      NOT NULL COMMENT '失踪人性别 1男 2女',
  birth_year   INT          COMMENT '失踪人出生年份',
  photo        VARCHAR(255) COMMENT '失踪人照片',
  lost_place   VARCHAR(120) NOT NULL COMMENT '失踪地区(省 市)',
  lost_date    DATE         COMMENT '失踪日期',
  features     VARCHAR(1000) COMMENT '体貌特征',
  story        TEXT          COMMENT '失踪经过描述',
  contact_phone VARCHAR(20)  COMMENT '联系电话',
  top_flag     TINYINT      NOT NULL DEFAULT 0 COMMENT '1管理员置顶',
  status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核 1已发布 2驳回',
  view_count   INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_case_status (status),
  KEY idx_case_user (user_id)
) COMMENT '寻亲信息';

-- 寻亲线索
CREATE TABLE t_clue (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  case_id     BIGINT        NOT NULL COMMENT '关联寻亲信息',
  user_id     BIGINT        NOT NULL COMMENT '线索提供人',
  content     VARCHAR(1000) NOT NULL COMMENT '线索内容',
  contact     VARCHAR(100)  COMMENT '线索提供人联络方式(发布者可见)',
  status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0待处理 1有效 2虚假',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_clue_case (case_id)
) COMMENT '寻亲线索';

-- 寻亲指南(富文本)
CREATE TABLE t_guide (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(120) NOT NULL,
  cover       VARCHAR(255) COMMENT '封面图',
  content     MEDIUMTEXT   NOT NULL COMMENT '富文本HTML',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1发布 0下架',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '寻亲指南';

-- 寻亲问答
CREATE TABLE t_question (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT        NOT NULL COMMENT '提问人',
  title       VARCHAR(120)  NOT NULL,
  content     VARCHAR(1000) NOT NULL COMMENT '问题描述',
  status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0正常 1已屏蔽',
  view_count  INT           NOT NULL DEFAULT 0,
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '寻亲问答-问题';

-- 问答回复
CREATE TABLE t_answer (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT        NOT NULL,
  user_id     BIGINT        NOT NULL COMMENT '回答人',
  content     VARCHAR(1000) NOT NULL,
  status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0正常 1已屏蔽',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_answer_question (question_id)
) COMMENT '寻亲问答-回复';

-- 站内消息(聊天)
CREATE TABLE t_message (
  id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  from_id     BIGINT        NOT NULL,
  to_id       BIGINT        NOT NULL,
  content     VARCHAR(1000) NOT NULL,
  read_flag   TINYINT       NOT NULL DEFAULT 0 COMMENT '接收方是否已读',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_pair (from_id, to_id)
) COMMENT '站内消息';

-- 登录安全日志
CREATE TABLE t_login_log (
  id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username     VARCHAR(50) NOT NULL,
  ip           VARCHAR(64),
  success      TINYINT     NOT NULL DEFAULT 0 COMMENT '1成功 0失败',
  msg          VARCHAR(120) COMMENT '结果描述',
  lock_seconds BIGINT      COMMENT '锁定时剩余秒数',
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_login_user (username)
) COMMENT '登录日志';

-- 回家灯(为寻亲档案点灯)
CREATE TABLE t_wish (
  id          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT   NOT NULL,
  case_id     BIGINT   NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_wish (user_id, case_id)
) COMMENT '点亮回家灯';

-- 问答点赞
CREATE TABLE t_vote (
  id          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT   NOT NULL,
  question_id BIGINT   NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_vote (user_id, question_id)
) COMMENT '问答点赞';

-- ============ 演示数据 ============
-- 管理员: admin / admin123   寻亲者密码均为 123456
INSERT INTO t_user (username, password, nickname, gender, birth_year, city, about_me, phone, role, status) VALUES
('admin',   '{noop}admin123', '平台管理员', 1, 1988, '北京', '负责寻亲信息审核与线索核实。', '13800000000', 'ADMIN', 1),
('s_zhang', '{noop}123456',   '张父',       1, 1965, '广东广州', '寻找走失多年的儿子,必有重谢。', '13900000001', 'USER', 1),
('s_li',    '{noop}123456',   '李秀兰',     2, 1958, '四川成都', '寻找被送养的妹妹。',             '13900000002', 'USER', 1),
('s_wang',  '{noop}123456',   '老王',       1, 1972, '浙江杭州', '寻找失散的母亲。',               '13900000003', 'USER', 1),
('s_chen',  '{noop}123456',   '陈姐',       2, 1980, '河南郑州', '寻找弟弟陈小龙。',               '13900000004', 'USER', 1),
('s_new',   '{noop}123456',   '待审核用户', 1, 1990, '江苏南京', '刚注册,等待审核。',               '13900000005', 'USER', 0);

-- 轮播图(图片由后端 /upload 目录提供,可自行替换)
INSERT INTO t_banner (title, image, link_url, sort_order, status) VALUES
('团圆行动 · 让爱回家', '/upload/demo-banner1.svg', '/cases', 1, 1),
('如何科学寻亲:黄金72小时指南', '/upload/demo-banner2.svg', '/guides/1', 2, 1),
('亲缘雷达 · 五维算法比对相似档案', '/upload/demo-banner3.svg', '/radar', 3, 1);

-- 寻亲信息(status: 0待审核 1已发布 2驳回)
INSERT INTO t_case (user_id, relation, name, gender, birth_year, photo, lost_place, lost_date, features, story, contact_phone, top_flag, status, view_count) VALUES
(2, '寻子女', '张小明', 1, 1995, NULL, '广东广州', '2001-05-20', '走失时6岁,圆脸,左眉尾有一道小疤,说普通话带广州口音。', '2001年5月20日傍晚,孩子在天河区菜市场附近走失,当时穿蓝色条纹短袖。多年来家人从未放弃寻找。', '13900000001', 1, 1, 356),
(3, '寻其他亲属', '李秀英', 2, 1965, NULL, '四川成都', '1972-11-03', '走失时7岁,扎双辫,右手有胎记,身高约1.2米。', '妹妹1972年被父母带去赶集时在青羊宫附近走散,如今父母年迈,唯一的愿望是有生之年再见她一面。', '13900000002', 0, 1, 128),
(4, '寻父母', '王德福', 1, 1930, NULL, '浙江杭州', '1998-03-15', '走失时68岁,身高1.68米,微驼背,患阿尔茨海默症,穿灰色中山装。', '母亲1998年3月从养老院出走,至今下落不明。儿子希望能得到线索,让老人落叶归根。', '13900000003', 0, 1, 96),
(5, '寻兄弟姐妹', '陈小龙', 1, 1990, NULL, '河南郑州', '2003-09-01', '走失时13岁,瘦高,戴近视眼镜,右耳有听力障碍。', '弟弟2003年放学后未到家,搜救无果。姐姐希望能借助平台力量扩散信息。', '13900000004', 0, 1, 210),
(2, '寻子女', '张小红', 2, 1998, NULL, '湖南长沙', '2005-12-24', '走失时7岁,门牙有缝隙,喜欢穿红衣服。', '女儿在长沙火车站与家人走散,监控显示她曾独自向东行走。', '13900000001', 0, 0, 0),
(4, '寻父母', '李桂芳', 2, 1935, NULL, '安徽合肥', '2010-06-18', '走失时75岁,身高1.55米,白发,讲合肥方言。', '奶奶在公园散步后走失,家人万分焦急,恳请好心人提供线索。', '13900000003', 0, 2, 0);

-- 线索
INSERT INTO t_clue (case_id, user_id, content, contact, status) VALUES
(1, 3, '广州天河区一家超市见过一个左眉尾有疤的青年,像在找什么,约25岁。', '13900000002', 0),
(4, 5, '我邻居家有个戴眼镜的男孩,右耳好像有听力障碍,2003年领养的,建议核实。', NULL, 0),
(1, 4, '多年前在佛山见到相似特征的人在打工,可以提供当时工友联系方式。', 'wxyz@example.com', 1);

-- 寻亲指南(富文本)
INSERT INTO t_guide (title, cover, content, status) VALUES
('亲人走失怎么办?黄金72小时自救指南', NULL,
 '<h2>一、立即报警,不必等24小时</h2><p>儿童、老人、残障人士走失,<b>立即拨打110</b>,警方须即时受理。保留报警回执。</p><h2>二、五步自救法</h2><ol><li>确认最后出现地点与时间,调取周边监控;</li><li>沿着走失方向扩大搜索半径,重点车站、市场、公园;</li><li>发动亲友分片寻找,统一联络人汇总信息;</li><li>在公安打拐系统录入<strong>DNA血样</strong>;</li><li>通过寻亲平台发布档案,扩散走失者照片与特征。</li></ol><h2>三、防骗提醒</h2><p style="color:#e05a6d">凡是索要"找到保证金""封口费"的线索,一律视为诈骗,请立即举报。</p>', 1),
('采血入库与DNA比对全流程图解', NULL,
 '<h2>谁可以采血?</h2><p>丢失儿童的父母、兄弟姐妹等亲生直系亲属,可到<strong>全国公安DNA实验室</strong>免费采血入库。</p><h2>流程</h2><ul><li>携带身份证与亲属关系证明;</li><li>现场采集血样(约2ml)或口腔拭子;</li><li>数据自动比对被拐、走失及无名尸源;</li><li>命中后由警方通知复核,通常30个工作日内出结果。</li></ul><blockquote>提示:血样长期有效,一次采血,终身受益。</blockquote>', 1);

-- 问答
INSERT INTO t_question (user_id, title, content, status, view_count) VALUES
(2, '采血入库需要带什么材料?', '我在广州,想把走失儿子的信息录入DNA库,需要带哪些证明?异地可以办吗?', 0, 45),
(3, '发布寻亲档案后多久能审核通过?', '第一次在平台发布妹妹的寻亲档案,审核一般需要多长时间?需要补充什么材料吗?', 0, 20);
INSERT INTO t_answer (question_id, user_id, content, status) VALUES
(1, 5, '我们这边派出所就可以采血,带上身份证和能证明亲子关系的材料(出生证、户口本)即可,免费的。', 0),
(1, 4, '提示:父母双方都采血命中率更高,异地可以办,全国库是联网的。', 0),
(2, 4, '我上次当天就过了,应该不超过一个工作日,平台志愿者很给力。', 0);

-- 管理员与寻亲者沟通示例
INSERT INTO t_message (from_id, to_id, content, read_flag) VALUES
(1, 2, '您好,您发布的张小明档案已通过审核,建议补充一张近照合成图提高辨认率。', 0),
(2, 1, '好的,谢谢!请问补充照片是在原档案上修改吗?', 1);
