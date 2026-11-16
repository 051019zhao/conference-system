-- ============================================================================
-- 会议管理系统 建库脚本(openGauss 5.x)
-- 脚本幂等,可重复执行(已存在的对象自动跳过)
-- 说明:后端启动时 DataInitializer 会自动执行本脚本,也可在 Data Studio 手工执行
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. sys_user 系统用户表(管理员 ADMIN / 专家 EXPERT / 作者 AUTHOR)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    real_name     VARCHAR(64)  NOT NULL,
    email         VARCHAR(128) NOT NULL UNIQUE,
    gender        VARCHAR(16),
    education     VARCHAR(64),
    job_title     VARCHAR(64),
    phone         VARCHAR(32),
    work_unit     VARCHAR(128),
    postal_code   VARCHAR(16),
    role          VARCHAR(16)  NOT NULL CHECK (role IN ('ADMIN', 'EXPERT', 'AUTHOR')),
    status        SMALLINT     NOT NULL DEFAULT 2,          -- 2 待审核 1 正常 0 禁用(注册后需管理员审核)
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);
COMMENT ON TABLE  sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.role IS '角色:ADMIN管理员/EXPERT评审专家/AUTHOR投稿作者';
COMMENT ON COLUMN sys_user.status IS '状态:2待管理员审核(注册默认) 1正常 0禁用';

-- ----------------------------------------------------------------------------
-- 2. verify_code 邮箱验证码表(注册审核)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS verify_code (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(128) NOT NULL,
    code       VARCHAR(8)   NOT NULL,
    expire_at  TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_verify_code_email ON verify_code(email);

-- ----------------------------------------------------------------------------
-- 3. expert_profile 专家档案表(专家介绍/主旨报告专家信息)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS expert_profile (
    id                BIGSERIAL     PRIMARY KEY,
    user_id           BIGINT        NOT NULL UNIQUE REFERENCES sys_user(id),
    title             VARCHAR(64),                              -- 职称
    affiliation       VARCHAR(128),                             -- 所属单位
    bio               TEXT,                                     -- 个人简介
    research_keywords VARCHAR(256),                             -- 研究方向关键词,逗号分隔(用于自动分配评审)
    photo_url         VARCHAR(256),
    address           VARCHAR(256),
    website           VARCHAR(256),
    review_topics     VARCHAR(512),
    review_capacity   INT          NOT NULL DEFAULT 0,
    review_enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    is_keynote        BOOLEAN       NOT NULL DEFAULT FALSE      -- 是否主旨报告专家
);
COMMENT ON COLUMN expert_profile.research_keywords IS '研究方向关键词,英文逗号分隔,与论文关键词匹配用于自动分配评审专家';

-- ----------------------------------------------------------------------------
-- 4. conference_info 会议基本信息表(单行,id 恒为 1)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS conference_info (
    id           INT          PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    short_name   VARCHAR(32),
    venue        VARCHAR(128),                               -- 举办地点
    start_date   DATE,
    end_date     DATE,
    submit_deadline DATE,                                    -- 投稿截止(业务规则:过期禁止投稿/存草稿)
    notify_deadline DATE,                                    -- 录用通知截止(展示用)
    final_deadline DATE,                                     -- 终稿截止(业务规则:过期禁止提交终稿)
    intro        TEXT,                                       -- 会议简介
    cfp_text     TEXT,                                       -- 征文内容(Call for Papers)
    contact_email VARCHAR(128),
    organizer_name VARCHAR(128),
    organizer_url VARCHAR(512),
    organizer_logo_url VARCHAR(512),
    coorganizer_name VARCHAR(128),
    coorganizer_url VARCHAR(512),
    coorganizer_logo_url VARCHAR(512)
);

-- ----------------------------------------------------------------------------
-- 5. topic 征文专题表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS topic (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(128)  NOT NULL UNIQUE,
    description TEXT,
    report_limit INT,
    keynote_expert_id BIGINT REFERENCES sys_user(id),
    report_name VARCHAR(256),
    ppt_path VARCHAR(512),
    report_time_note VARCHAR(128),
    sort_no     INT           NOT NULL DEFAULT 0
);

-- ----------------------------------------------------------------------------
-- 6. paper 论文表
--    状态机:DRAFT 草稿 -> SUBMITTED 已投递 -> REVIEWING 外审中 -> ACCEPTED 已录用 / REJECTED 已退稿
--           ACCEPTED -> FINAL_SUBMITTED 终稿已提交;SUBMITTED -> WITHDRAWN 作者撤回
--    草稿在投稿截止前可反复编辑(含作者与附件),正式提交后进入评审流程
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS paper (
    id               BIGSERIAL     PRIMARY KEY,
    title            VARCHAR(256)  NOT NULL,
    title_en         VARCHAR(256),
    abstract         TEXT,                                        -- 摘要
    abstract_en      TEXT,
    keywords         VARCHAR(256),                                -- 关键词,英文逗号分隔
    keywords_en      VARCHAR(256),
    topic_id         BIGINT        REFERENCES topic(id),
    submitter_id     BIGINT        NOT NULL REFERENCES sys_user(id),
    status           VARCHAR(20)   NOT NULL DEFAULT 'SUBMITTED'
                     CHECK (status IN ('DRAFT','SUBMITTED','REVIEWING','ACCEPTED','REJECTED','FINAL_SUBMITTED','WITHDRAWN')),
    file_path        VARCHAR(512),                                -- 投稿原文路径
    final_file_path  VARCHAR(512),                                -- 录用后终稿路径
    review_plan_published BOOLEAN NOT NULL DEFAULT FALSE,          -- 评审分配方案发布后专家才可见
    deleted_at       TIMESTAMP,                                   -- 作者删除进入可恢复回收站
    search_vector    TSVECTOR,                                    -- 全文检索列(title+keywords,触发器维护)
    created_at       TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT now()
);
COMMENT ON COLUMN paper.status IS '论文状态:DRAFT草稿/SUBMITTED已投递/REVIEWING外审中/ACCEPTED已录用/REJECTED已退稿/FINAL_SUBMITTED终稿已提交/WITHDRAWN作者已撤回';
CREATE INDEX idx_paper_status ON paper(status);
CREATE INDEX idx_paper_topic ON paper(topic_id);
CREATE INDEX idx_paper_submitter ON paper(submitter_id);
-- 全文检索 GIN 倒排索引(配合 trg_paper_search 触发器,详见文末函数区)
CREATE INDEX idx_paper_search ON paper USING GIN(search_vector);

-- ----------------------------------------------------------------------------
-- 7. paper_author 论文作者表(支持多作者,sort_no 维护作者排序)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS paper_author (
    id              BIGSERIAL    PRIMARY KEY,
    paper_id        BIGINT       NOT NULL REFERENCES paper(id) ON DELETE CASCADE,
    name            VARCHAR(64)  NOT NULL,
    pinyin          VARCHAR(128),
    email           VARCHAR(128),
    affiliation     VARCHAR(128),                           -- 作者单位
    postal_code     VARCHAR(16),
    sort_no         INT          NOT NULL DEFAULT 1,        -- 作者排序,1 为第一作者
    is_corresponding BOOLEAN     NOT NULL DEFAULT FALSE,    -- 是否通讯作者
    -- 数据库层保证同一论文内作者顺序不重复(并发写入的最终防线)
    CONSTRAINT uk_paper_author_sort UNIQUE (paper_id, sort_no)
);
CREATE INDEX idx_paper_author_paper ON paper_author(paper_id);

-- ----------------------------------------------------------------------------
-- 7.1 paper_file 论文附件账本表(主稿/终稿/附属材料的统一元数据)
--     设计:文件内容存磁盘,本表只存元数据——大文件进库会让事务日志与备份膨胀,
--     与头像(小图,BYTEA)构成"大小分流"策略。
--     版本链:同一论文同一类型可有多个版本,旧版本保留供追溯,is_current 标记当前版。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS paper_file (
    id           BIGSERIAL    PRIMARY KEY,
    paper_id     BIGINT       NOT NULL REFERENCES paper(id) ON DELETE CASCADE,
    file_type    VARCHAR(16)  NOT NULL CHECK (file_type IN ('MAIN','FINAL','SUPPLEMENT')),
    file_name    VARCHAR(256) NOT NULL,              -- 原始文件名(下载时还原)
    stored_path  VARCHAR(512) NOT NULL,              -- 磁盘路径(UUID 重命名,防路径穿越)
    file_ext     VARCHAR(16),                        -- pdf / zip / rar / 7z / png ...
    file_size    BIGINT       NOT NULL DEFAULT 0,
    version_no   INT          NOT NULL DEFAULT 1,    -- 同类型第几版
    is_current   BOOLEAN      NOT NULL DEFAULT TRUE, -- 是否当前版本
    uploaded_by  BIGINT       NOT NULL REFERENCES sys_user(id),
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_paper_file_version UNIQUE (paper_id, file_type, version_no)
);
COMMENT ON TABLE paper_file IS '论文附件账本:MAIN投稿主稿/FINAL终稿/SUPPLEMENT附属资料(zip/rar/7z等),版本化保留';
COMMENT ON COLUMN paper_file.version_no IS '同类型文件第几版;重传主稿时旧版保留(is_current=false)供追溯';
CREATE INDEX idx_paper_file_paper ON paper_file(paper_id);

-- ----------------------------------------------------------------------------
-- 8. review 评审表(一篇论文分配给多位专家,一位专家对一篇论文只有一条评审)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS review (
    id                BIGSERIAL    PRIMARY KEY,
    paper_id          BIGINT       NOT NULL REFERENCES paper(id) ON DELETE CASCADE,
    expert_id         BIGINT       NOT NULL REFERENCES sys_user(id),
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SUBMITTED')),
    overall_comment   TEXT,                                    -- 总体评价
    revision_comment  TEXT,                                    -- 修改意见
    score_fit         SMALLINT CHECK (score_fit BETWEEN 1 AND 5),          -- 主题契合度
    score_innovation  SMALLINT CHECK (score_innovation BETWEEN 1 AND 5),   -- 创新性
    score_complete    SMALLINT CHECK (score_complete BETWEEN 1 AND 5),     -- 完整性
    score_depth       SMALLINT CHECK (score_depth BETWEEN 1 AND 5),        -- 技术深度
    score_clarity     SMALLINT CHECK (score_clarity BETWEEN 1 AND 5),      -- 表达质量
    result            VARCHAR(16) CHECK (result IN ('ACCEPT','REJECT')),  -- 是否录用建议
    recommend_journal BOOLEAN     NOT NULL DEFAULT FALSE,     -- 是否推荐到期刊
    rubric_json       TEXT,
    recommendation_grade VARCHAR(32),
    rejection_reason VARCHAR(64),
    created_at        TIMESTAMP   NOT NULL DEFAULT now(),
    submitted_at      TIMESTAMP,
    CONSTRAINT uk_review_paper_expert UNIQUE (paper_id, expert_id)
);
COMMENT ON COLUMN review.result IS '评审结论:ACCEPT建议录用/REJECT建议退稿';
COMMENT ON COLUMN review.score_fit IS '评审表五维评分(1-5):契合度/创新性/完整性/深度/表达';
CREATE INDEX idx_review_paper ON review(paper_id);
CREATE INDEX idx_review_expert ON review(expert_id);

-- ----------------------------------------------------------------------------
-- 9. keynote_report 主旨报告表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS keynote_report (
    id         BIGSERIAL     PRIMARY KEY,
    expert_id  BIGINT        NOT NULL REFERENCES expert_profile(id),
    title      VARCHAR(256)  NOT NULL,
    abstract   TEXT,
    time_note  VARCHAR(128),                                 -- 报告时间地点简述
    sort_no    INT           NOT NULL DEFAULT 0
);

-- ----------------------------------------------------------------------------
-- 10. schedule_item 会议日程表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schedule_item (
    id         BIGSERIAL     PRIMARY KEY,
    day        DATE          NOT NULL,                       -- 日程日期
    start_time VARCHAR(16),                                  -- 开始时间 HH:MI
    end_time   VARCHAR(16),
    title      VARCHAR(256)  NOT NULL,                       -- 日程标题
    speaker    VARCHAR(128),                                 -- 演讲人/负责人
    location   VARCHAR(128),                                 -- 地点
    type       VARCHAR(32)   NOT NULL DEFAULT '常规',         -- 开幕式/主旨报告/分论坛/茶歇...
    sort_no    INT           NOT NULL DEFAULT 0
);
CREATE INDEX idx_schedule_day ON schedule_item(day);

-- ----------------------------------------------------------------------------
-- 11. notification 通知公告表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification (
    id           BIGSERIAL     PRIMARY KEY,
    title        VARCHAR(128)  NOT NULL,
    content      TEXT,
    last_saved_at TIMESTAMP,
    expires_at   TIMESTAMP,
    publisher    VARCHAR(128),
    pdf_path     VARCHAR(512),
    is_published BOOLEAN       NOT NULL DEFAULT FALSE,     -- 是否发布(草稿/已发布)
    publish_time TIMESTAMP,
    created_at   TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_publish ON notification(is_published, publish_time);

-- ----------------------------------------------------------------------------
-- 12. material 会议资料表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS material (
    id             BIGSERIAL     PRIMARY KEY,
    name           VARCHAR(128)  NOT NULL,
    description    VARCHAR(256),
    file_path      VARCHAR(512)  NOT NULL,
    file_size      BIGINT        NOT NULL DEFAULT 0,        -- 字节
    download_count INT           NOT NULL DEFAULT 0,
    created_at     TIMESTAMP     NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 13. paper_status_log 论文状态变更日志表(由触发器自动写入)
--     openGauss RANGE 分区表:按月分区,审计日志只增不改,分区裁剪加速查询、
--     历史分区可整体卸载归档——海量日志的标准方案(openGauss 数据库特色能力)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS paper_status_log (
    id         BIGSERIAL,
    paper_id   BIGINT      NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    changed_at TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT pk_psl PRIMARY KEY (id, changed_at)
)
PARTITION BY RANGE (changed_at) (
    PARTITION p202611 VALUES LESS THAN ('2026-11-01'),
    PARTITION p202612 VALUES LESS THAN ('2026-12-01'),
    PARTITION p202701 VALUES LESS THAN ('2027-01-01'),
    PARTITION pmax    VALUES LESS THAN (MAXVALUE)
);
COMMENT ON TABLE paper_status_log IS '论文状态变更日志,openGauss RANGE 按月分区(分区裁剪+归档友好)';
CREATE INDEX idx_psl_paper ON paper_status_log(paper_id);

-- ----------------------------------------------------------------------------
-- 14. operation_log 操作审计日志表(应用层关键动作埋点:登录/投稿/评审/终审/用户管理)
--     与 paper_status_log 互补:触发器记录数据变化,本表记录"谁在何时做了什么"
--     同样采用 openGauss RANGE 按月分区
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS operation_log (
    id         BIGSERIAL,
    user_id    BIGINT,
    username   VARCHAR(64),
    role       VARCHAR(16),
    action     VARCHAR(64)  NOT NULL,
    detail     VARCHAR(512),
    ip         VARCHAR(64),
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT pk_oplog PRIMARY KEY (id, created_at)
)
PARTITION BY RANGE (created_at) (
    PARTITION p202611 VALUES LESS THAN ('2026-11-01'),
    PARTITION p202612 VALUES LESS THAN ('2026-12-01'),
    PARTITION p202701 VALUES LESS THAN ('2027-01-01'),
    PARTITION pmax    VALUES LESS THAN (MAXVALUE)
);
COMMENT ON TABLE operation_log IS '操作审计日志:应用层记录关键业务动作';
COMMENT ON COLUMN operation_log.action IS '动作:LOGIN登录/REGISTER注册/SUBMIT投稿/WITHDRAW撤回/FINAL终稿/FIRST_PASS初审/REJECT退稿/ASSIGN分配评审/REVIEW提交评审/DECIDE终审/USER_CREATE创建账号/USER_STATUS启用禁用/USER_RESET_PWD重置密码';
CREATE INDEX idx_oplog_created ON operation_log(created_at);
CREATE INDEX idx_oplog_user ON operation_log(user_id);

-- ----------------------------------------------------------------------------
-- 15. user_avatar 用户头像表(BYTEA 大对象存储)
--     大字段垂直拆分到独立表,避免主表膨胀;课程演示 openGauss 二进制大对象能力
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_avatar (
    user_id      BIGINT       PRIMARY KEY REFERENCES sys_user(id) ON DELETE CASCADE,
    content      BYTEA        NOT NULL,          -- 图片二进制内容(限制 2MB,应用层校验)
    content_type VARCHAR(64)  NOT NULL,          -- image/png / image/jpeg
    updated_at   TIMESTAMP    NOT NULL DEFAULT now()
);
COMMENT ON TABLE user_avatar IS '用户头像(BYTEA 大对象,与用户表垂直拆分)';

-- ----------------------------------------------------------------------------
-- 16. ai_analysis AI 分析任务表(异步账本)
--     LLM 调用耗时数秒到数十秒,采用"提交任务立即返回,后台生成,前端轮询/重查"模式。
--     状态机:PENDING 已创建 -> RUNNING 生成中 -> READY 完成 / FAILED 失败
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_analysis (
    id          BIGSERIAL    PRIMARY KEY,
    biz_type    VARCHAR(32)  NOT NULL,               -- PAPER_PRECHECK 论文AI预审
    biz_id      BIGINT       NOT NULL,               -- 业务对象 id(论文 id)
    status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING'
                CHECK (status IN ('PENDING','RUNNING','READY','FAILED')),
    model       VARCHAR(64),
    content     TEXT,                                 -- AI 输出(结构化纯文本)
    error       VARCHAR(512),
    created_by  BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    finished_at TIMESTAMP
);
COMMENT ON TABLE ai_analysis IS 'AI 异步分析任务账本:当前用于论文AI预审(PAPER_PRECHECK),仅评审专家与管理员可见';
CREATE INDEX idx_ai_analysis_biz ON ai_analysis(biz_type, biz_id);

-- ----------------------------------------------------------------------------
-- 视图 v_paper_stats:各专题各状态的论文统计(供管理后台仪表盘使用)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_paper_stats AS
SELECT t.name  AS topic_name,
       p.status,
       COUNT(*) AS cnt
FROM paper p
LEFT JOIN topic t ON p.topic_id = t.id
WHERE p.deleted_at IS NULL
GROUP BY t.name, p.status;

-- ----------------------------------------------------------------------------
-- 触发器:论文状态变化时自动写日志(数据库层面的审计,不依赖应用代码)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_log_paper_status() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status <> OLD.status THEN
        INSERT INTO paper_status_log(paper_id, old_status, new_status)
        VALUES (NEW.id, OLD.status, NEW.status);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_paper_status ON paper;
CREATE TRIGGER trg_paper_status
AFTER UPDATE ON paper
FOR EACH ROW EXECUTE PROCEDURE fn_log_paper_status();

-- ----------------------------------------------------------------------------
-- 触发器:论文标题/关键词变化时自动维护全文检索列(标准 PG 式 tsvector 维护)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_paper_search_update() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('simple',
        coalesce(NEW.title, '') || ' ' || coalesce(NEW.keywords, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_paper_search ON paper;
CREATE TRIGGER trg_paper_search
BEFORE INSERT OR UPDATE OF title, keywords ON paper
FOR EACH ROW EXECUTE PROCEDURE fn_paper_search_update();

-- ----------------------------------------------------------------------------
-- 函数 fn_match_experts:评审专家自动匹配(业务规则下沉到数据库层)
--   按论文关键词与专家研究方向的重合度打分,排除投稿人本人(回避原则)
--   与已提交评审(status=SUBMITTED)的专家,返回按匹配度降序的候选表。
--   应用层 LIMIT 取前 N 即可,匹配逻辑在库内原子完成。
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_match_experts(_paper_id BIGINT)
RETURNS TABLE (o_user_id BIGINT, o_real_name VARCHAR(64), o_affiliation VARCHAR(128),
               o_keywords VARCHAR(256), o_score BIGINT) AS $$
DECLARE
    _submitter BIGINT;
BEGIN
    SELECT submitter_id INTO _submitter FROM paper WHERE id = _paper_id;
    RETURN QUERY
    WITH pk AS (
        SELECT DISTINCT lower(btrim(kw)) AS kw
        FROM unnest(string_to_array((SELECT keywords FROM paper WHERE id = _paper_id), ',')) AS kw
        WHERE btrim(kw) <> ''
    )
    SELECT e.user_id, u.real_name, e.affiliation, e.research_keywords,
           (SELECT count(*) FROM pk
             WHERE pk.kw <> '' AND position(pk.kw in lower(e.research_keywords)) > 0) AS score
    FROM expert_profile e
    JOIN sys_user u ON u.id = e.user_id AND u.role = 'EXPERT' AND u.status = 1
    WHERE e.user_id <> _submitter          -- 回避原则:投稿人自己的论文不参与评审
      AND NOT EXISTS (SELECT 1 FROM review r      -- 已提交评审的专家不再进入候选
                      WHERE r.paper_id = _paper_id AND r.expert_id = e.user_id
                        AND r.status = 'SUBMITTED')
    ORDER BY score DESC, e.user_id;
END;
$$ LANGUAGE plpgsql;
