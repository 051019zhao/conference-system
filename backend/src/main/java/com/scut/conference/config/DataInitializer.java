package com.scut.conference.config;

import com.scut.conference.entity.ConferenceInfo;
import com.scut.conference.entity.ExpertProfile;
import com.scut.conference.entity.KeynoteReport;
import com.scut.conference.entity.Notification;
import com.scut.conference.entity.ScheduleItem;
import com.scut.conference.entity.SysUser;
import com.scut.conference.entity.Topic;
import com.scut.conference.mapper.ConferenceMapper;
import com.scut.conference.mapper.ExpertProfileMapper;
import com.scut.conference.mapper.KeynoteReportMapper;
import com.scut.conference.mapper.NotificationMapper;
import com.scut.conference.mapper.ScheduleItemMapper;
import com.scut.conference.mapper.TopicMapper;
import com.scut.conference.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 启动初始化:
 * 1. 自动执行 schema.sql 建表(幂等,可重复执行);
 * 2. 首次启动(用户表为空)时写入演示数据:管理员/专家/作者账号、会议信息、专题、日程、通知、主旨报告。
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;
    private final ExpertProfileMapper expertProfileMapper;
    private final TopicMapper topicMapper;
    private final ConferenceMapper conferenceMapper;
    private final NotificationMapper notificationMapper;
    private final ScheduleItemMapper scheduleItemMapper;
    private final KeynoteReportMapper keynoteReportMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public DataInitializer(JdbcTemplate jdbcTemplate, UserMapper userMapper,
                           ExpertProfileMapper expertProfileMapper, TopicMapper topicMapper,
                           ConferenceMapper conferenceMapper, NotificationMapper notificationMapper,
                           ScheduleItemMapper scheduleItemMapper, KeynoteReportMapper keynoteReportMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userMapper = userMapper;
        this.expertProfileMapper = expertProfileMapper;
        this.topicMapper = topicMapper;
        this.conferenceMapper = conferenceMapper;
        this.notificationMapper = notificationMapper;
        this.scheduleItemMapper = scheduleItemMapper;
        this.keynoteReportMapper = keynoteReportMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        String statsViewSql = initSchema();
        migrateColumns();
        // 旧库的 paper 表可能尚无 deleted_at；先补列，再更新依赖该列的视图。
        jdbcTemplate.execute(statsViewSql);
        log.info("数据库结构初始化完成(schema.sql)");
        if (userMapper.countAll() == 0) {
            seed();
        }
    }

    // ---------------- schema 自动执行 ----------------

    private String initSchema() throws IOException {
        String sql = new String(new ClassPathResource("schema.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        String statsViewSql = null;
        for (String stmt : splitStatements(sql)) {
            if (stmt.contains("CREATE OR REPLACE VIEW v_paper_stats AS")) {
                statsViewSql = stmt;
                continue;
            }
            try {
                jdbcTemplate.execute(stmt);
            } catch (DataAccessException e) {
                if (isAlreadyExists(e)) {
                    log.debug("跳过已存在的数据库对象");
                } else {
                    throw e;
                }
            }
        }
        if (statsViewSql == null) {
            throw new IllegalStateException("schema.sql 缺少论文统计视图定义");
        }
        return statsViewSql;
    }

    /** CREATE TABLE IF NOT EXISTS 不会给旧数据库加列，启动时增量补齐本项目的新字段。 */
    private void migrateColumns() {
        addColumn("sys_user", "gender", "VARCHAR(16)");
        addColumn("sys_user", "education", "VARCHAR(64)");
        addColumn("sys_user", "job_title", "VARCHAR(64)");
        addColumn("sys_user", "phone", "VARCHAR(32)");
        addColumn("sys_user", "work_unit", "VARCHAR(128)");
        addColumn("sys_user", "postal_code", "VARCHAR(16)");
        addColumn("expert_profile", "address", "VARCHAR(256)");
        addColumn("expert_profile", "website", "VARCHAR(256)");
        addColumn("expert_profile", "review_topics", "VARCHAR(512)");
        addColumn("expert_profile", "review_capacity", "INT DEFAULT 0");
        addColumn("expert_profile", "review_enabled", "BOOLEAN DEFAULT TRUE");
        addColumn("conference_info", "organizer_name", "VARCHAR(128)");
        addColumn("conference_info", "organizer_url", "VARCHAR(512)");
        addColumn("conference_info", "organizer_logo_url", "VARCHAR(512)");
        addColumn("conference_info", "coorganizer_name", "VARCHAR(128)");
        addColumn("conference_info", "coorganizer_url", "VARCHAR(512)");
        addColumn("conference_info", "coorganizer_logo_url", "VARCHAR(512)");
        addColumn("topic", "report_limit", "INT");
        addColumn("topic", "keynote_expert_id", "BIGINT");
        addColumn("topic", "report_name", "VARCHAR(256)");
        addColumn("topic", "ppt_path", "VARCHAR(512)");
        addColumn("topic", "report_time_note", "VARCHAR(128)");
        addColumn("paper", "title_en", "VARCHAR(256)");
        addColumn("paper", "abstract_en", "TEXT");
        addColumn("paper", "keywords_en", "VARCHAR(256)");
        boolean addedReviewPlan = addColumn("paper", "review_plan_published", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumn("paper", "deleted_at", "TIMESTAMP");
        if (addedReviewPlan) {
            // 升级旧库时保留已经分配出去的评审任务；仅首次新增该列时执行。
            jdbcTemplate.execute("UPDATE paper SET review_plan_published = TRUE " +
                    "WHERE EXISTS (SELECT 1 FROM review r WHERE r.paper_id = paper.id)");
        }
        addColumn("paper_author", "pinyin", "VARCHAR(128)");
        addColumn("paper_author", "postal_code", "VARCHAR(16)");
        addColumn("review", "rubric_json", "TEXT");
        addColumn("review", "recommendation_grade", "VARCHAR(32)");
        addColumn("review", "rejection_reason", "VARCHAR(64)");
        addColumn("notification", "last_saved_at", "TIMESTAMP");
        addColumn("notification", "expires_at", "TIMESTAMP");
        addColumn("notification", "publisher", "VARCHAR(128)");
        addColumn("notification", "pdf_path", "VARCHAR(512)");
    }

    private boolean addColumn(String table, String column, String definition) {
        Integer found = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        if (found == null || found == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("数据库迁移: {}.{}", table, column);
            return true;
        }
        return false;
    }

    /**
     * 按 ';' 切分语句。以下三种上下文中的分号不切分:
     * 1. $$...$$ 函数体;2. -- 行注释;3. '...' 字符串字面量
     */
    private List<String> splitStatements(String sql) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDollar = false;
        boolean inString = false;
        boolean inComment = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (inComment) {
                current.append(c);
                if (c == '\n') {
                    inComment = false;
                }
            } else if (!inDollar && !inString && c == '-' && next == '-') {
                inComment = true;
                current.append(c);
            } else if (!inDollar && c == '\'') {
                inString = !inString;
                current.append(c);
            } else if (!inString && c == '$' && next == '$') {
                inDollar = !inDollar;
                current.append("$$");
                i++;
            } else if (c == ';' && !inDollar && !inString) {
                addIfEffective(result, current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        addIfEffective(result, current.toString());
        return result;
    }

    /** 过滤空语句与纯注释片段 */
    private void addIfEffective(List<String> list, String stmt) {
        String trimmed = stmt.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        for (String line : trimmed.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("--")) {
                list.add(trimmed);
                return;
            }
        }
    }

    /** openGauss/PostgreSQL "对象已存在" 的 SQLState:表/触发器/列/约束重复 */
    private boolean isAlreadyExists(DataAccessException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SQLException se) {
                String state = se.getSQLState();
                if ("42P07".equals(state) || "42710".equals(state)
                        || "42701".equals(state) || "42P16".equals(state)) {
                    return true;
                }
            }
            String msg = cause.getMessage();
            if (msg != null && (msg.contains("already exists") || msg.contains("已存在"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    // ---------------- 演示数据 ----------------

    private void seed() {
        log.info("首次启动,正在写入演示数据(账号见 README)...");
        createUser("admin", "Admin@123", "系统管理员", "admin@sedb.edu.cn", "ADMIN");

        Long zhang = createUser("expert01", "Expert@123", "张伟", "zhangwei@scut.edu.cn", "EXPERT");
        insertProfile(zhang, "教授", "华南理工大学",
                "长期从事数据库系统与查询优化方向研究,主持多项国家自然科学基金项目,担任多个国际会议程序委员。",
                "数据库,数据挖掘,机器学习", true);
        Long li = createUser("expert02", "Expert@123", "李静", "lijing@sysu.edu.cn", "EXPERT");
        insertProfile(li, "教授", "中山大学",
                "研究方向为自然语言处理与信息检索,CCF 高级会员,在 ACL/SIGIR 等会议发表论文六十余篇。",
                "自然语言处理,信息检索,大模型", true);
        Long wang = createUser("expert03", "Expert@123", "王强", "wangqiang@scut.edu.cn", "EXPERT");
        insertProfile(wang, "副教授", "华南理工大学",
                "聚焦计算机视觉与深度学习在工业质检场景中的应用,与多家制造企业开展产学研合作。",
                "计算机视觉,深度学习", false);

        createUser("author01", "Author@123", "陈晨", "chenchen@stu.edu.cn", "AUTHOR");

        seedConference();
        seedTopics();
        seedNotifications();
        seedSchedule();
        seedKeynotes(zhang, li);
        log.info("演示数据写入完成");
    }

    private void seedConference() {
        // 仅在首次启动(seed 前置条件:用户表为空)执行,直接插入即可
        jdbcTemplate.update(
                "INSERT INTO conference_info(id, name, short_name, venue, start_date, end_date, " +
                        "submit_deadline, notify_deadline, final_deadline, intro, cfp_text, contact_email) " +
                        "VALUES(1,?,?,?,?,?,?,?,?,?,?,?)",
                "2026年软件工程与数据库技术学术会议", "SEDB 2026", "广州 · 大学城中心学术交流中心",
                LocalDate.of(2026, 12, 18), LocalDate.of(2026, 12, 20),
                LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 8), LocalDate.of(2027, 1, 12),
                "SEDB 2026 由华南地区高校联合主办,聚焦软件工程与数据库技术的前沿研究与实践," +
                        "设主旨报告、分论坛报告与青年论坛,预计参会 500 人。",
                "一、征文范围\n" +
                        "1. 数据库与信息系统:数据模型、查询处理与优化、事务与并发控制、分布式与云原生数据库\n" +
                        "2. 软件工程与智能化:需求工程、软件测试与维护、智能化软件开发\n" +
                        "3. 人工智能与机器学习:机器学习理论、大模型与系统、智能应用\n" +
                        "4. 数据安全与隐私:访问控制、隐私计算、数据治理与合规\n\n" +
                        "二、投稿要求\n" +
                        "1. 论文未在其他会议或期刊发表过,一稿多投将被记录并通报;\n" +
                        "2. 投稿请注册作者账号后在线提交,稿件 PDF 不超过 20MB;\n" +
                        "3. 论文须经专家评审,录用后按要求提交终稿。\n\n" +
                        "三、重要日期\n" +
                        "投稿截止:2026-11-30　录用通知:2026-12-08　终稿提交:2026-12-12",
                "sedb2026@conf.edu.cn");
    }

    private void seedTopics() {
        insertTopic("数据库与信息系统", "数据模型、查询处理与优化、事务与并发控制、分布式与云原生数据库", 1);
        insertTopic("软件工程与智能化", "需求工程、软件测试与维护、智能化软件开发与 DevOps", 2);
        insertTopic("人工智能与机器学习", "机器学习理论、大模型与系统、智能应用", 3);
        insertTopic("数据安全与隐私", "访问控制、隐私计算、数据治理与合规", 4);
    }

    private void seedNotifications() {
        insertNotification("SEDB 2026 征稿正式启动",
                "2026年软件工程与数据库技术学术会议(SEDB 2026)现已开放投稿,欢迎广大学者与研究生踊跃投稿。投稿请先注册作者账号。",
                true);
        insertNotification("论文投稿截止时间延长通知",
                "应广大作者要求,论文投稿截止时间由 2026-11-25 延长至 2026-11-30,请合理安排时间。",
                true);
        insertNotification("【草稿】评审专家名单(待确认)",
                "本届会议拟邀请 30 位领域专家担任评审,名单确认后公布。", false);
    }

    private void seedSchedule() {
        insertSchedule(LocalDate.of(2026, 12, 18), "08:30", "09:00", "开幕式及致辞", "组委会", "一层报告厅", "开幕式", 1);
        insertSchedule(LocalDate.of(2026, 12, 18), "09:00", "10:00", "主旨报告:面向大模型的向量数据库关键技术", "张伟", "一层报告厅", "主旨报告", 2);
        insertSchedule(LocalDate.of(2026, 12, 18), "10:20", "12:00", "分论坛A:数据库新技术", "多报告人", "二层201", "分论坛", 3);
        insertSchedule(LocalDate.of(2026, 12, 18), "14:00", "16:00", "分论坛B:智能化软件工程", "多报告人", "二层203", "分论坛", 4);
        insertSchedule(LocalDate.of(2026, 12, 19), "09:00", "10:00", "主旨报告:检索增强生成的回顾与展望", "李静", "一层报告厅", "主旨报告", 1);
        insertSchedule(LocalDate.of(2026, 12, 19), "10:20", "12:00", "分论坛C:人工智能应用", "多报告人", "二层205", "分论坛", 2);
        insertSchedule(LocalDate.of(2026, 12, 19), "16:00", "17:00", "最佳论文颁奖及闭幕式", "组委会", "一层报告厅", "闭幕式", 3);
    }

    private void seedKeynotes(Long expertUser1, Long expertUser2) {
        ExpertProfile profile1 = expertProfileMapper.findByUserId(expertUser1);
        ExpertProfile profile2 = expertProfileMapper.findByUserId(expertUser2);
        insertKeynote(profile1.getId(), "面向大模型的向量数据库关键技术",
                "大模型应用推动向量检索成为数据库新的核心负载。本报告回顾向量索引技术的发展," +
                        "分享我们在高维索引压缩、混合查询与 GPU 检索加速方面的最新工作。",
                "12月18日 09:00-10:00 · 一层报告厅", 1);
        insertKeynote(profile2.getId(), "检索增强生成的回顾与展望",
                "检索增强生成(RAG)将外部知识与生成模型结合,是当前落地最广的大模型应用范式。" +
                        "本报告梳理 RAG 的技术演进,讨论检索质量评估与可信性挑战。",
                "12月19日 09:00-10:00 · 一层报告厅", 2);
    }

    // ---------------- 种子数据辅助 ----------------

    private Long createUser(String username, String password, String realName, String email, String role) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setRealName(realName);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(1);   // 演示账号直接可用(注册流程的新用户才是待审核状态)
        userMapper.insert(user);
        return user.getId();
    }

    private void insertProfile(Long userId, String title, String affiliation, String bio,
                               String keywords, boolean keynote) {
        ExpertProfile profile = new ExpertProfile();
        profile.setUserId(userId);
        profile.setTitle(title);
        profile.setAffiliation(affiliation);
        profile.setBio(bio);
        profile.setResearchKeywords(keywords);
        profile.setIsKeynote(keynote);
        expertProfileMapper.insert(profile);
    }

    private void insertTopic(String name, String description, int sortNo) {
        Topic topic = new Topic();
        topic.setName(name);
        topic.setDescription(description);
        topic.setSortNo(sortNo);
        topicMapper.insert(topic);
    }

    private void insertNotification(String title, String content, boolean published) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notificationMapper.insert(notification);
        if (published) {
            notificationMapper.publish(notification.getId());
        }
    }

    private void insertSchedule(LocalDate day, String start, String end, String title,
                                String speaker, String location, String type, int sortNo) {
        ScheduleItem item = new ScheduleItem();
        item.setDay(day);
        item.setStartTime(start);
        item.setEndTime(end);
        item.setTitle(title);
        item.setSpeaker(speaker);
        item.setLocation(location);
        item.setType(type);
        item.setSortNo(sortNo);
        scheduleItemMapper.insert(item);
    }

    private void insertKeynote(Long profileId, String title, String abstractText, String timeNote, int sortNo) {
        KeynoteReport report = new KeynoteReport();
        report.setExpertId(profileId);
        report.setTitle(title);
        report.setPaperAbstract(abstractText);
        report.setTimeNote(timeNote);
        report.setSortNo(sortNo);
        keynoteReportMapper.insert(report);
    }
}
