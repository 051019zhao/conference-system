package com.scut.conference.service;

import com.scut.conference.common.BizException;
import com.scut.conference.common.DownloadFile;
import com.scut.conference.dto.AdminDtos.ConferenceRequest;
import com.scut.conference.dto.AdminDtos.KeynoteRequest;
import com.scut.conference.dto.AdminDtos.MaterialMetaRequest;
import com.scut.conference.dto.AdminDtos.NotificationRequest;
import com.scut.conference.dto.AdminDtos.ScheduleRequest;
import com.scut.conference.dto.AdminDtos.TopicRequest;
import com.scut.conference.entity.ConferenceInfo;
import com.scut.conference.entity.KeynoteReport;
import com.scut.conference.entity.Material;
import com.scut.conference.entity.Notification;
import com.scut.conference.entity.ScheduleItem;
import com.scut.conference.entity.Topic;
import com.scut.conference.mapper.ConferenceMapper;
import com.scut.conference.mapper.ExpertProfileMapper;
import com.scut.conference.mapper.KeynoteReportMapper;
import com.scut.conference.mapper.MaterialMapper;
import com.scut.conference.mapper.NotificationMapper;
import com.scut.conference.mapper.PaperMapper;
import com.scut.conference.mapper.ScheduleItemMapper;
import com.scut.conference.mapper.TopicMapper;
import com.scut.conference.mapper.UserMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * 内容管理:专题/主旨报告/日程/通知/资料/会议信息 + 统计
 */
@Service
public class ContentService {

    private final TopicMapper topicMapper;
    private final KeynoteReportMapper keynoteReportMapper;
    private final ScheduleItemMapper scheduleItemMapper;
    private final NotificationMapper notificationMapper;
    private final MaterialMapper materialMapper;
    private final ConferenceMapper conferenceMapper;
    private final ExpertProfileMapper expertProfileMapper;
    private final PaperMapper paperMapper;
    private final UserMapper userMapper;
    private final FileStorageService fileStorage;
    private final JdbcTemplate jdbcTemplate;

    public ContentService(TopicMapper topicMapper, KeynoteReportMapper keynoteReportMapper,
                          ScheduleItemMapper scheduleItemMapper, NotificationMapper notificationMapper,
                          MaterialMapper materialMapper, ConferenceMapper conferenceMapper,
                          ExpertProfileMapper expertProfileMapper,
                          PaperMapper paperMapper, UserMapper userMapper,
                          FileStorageService fileStorage, JdbcTemplate jdbcTemplate) {
        this.topicMapper = topicMapper;
        this.keynoteReportMapper = keynoteReportMapper;
        this.scheduleItemMapper = scheduleItemMapper;
        this.notificationMapper = notificationMapper;
        this.materialMapper = materialMapper;
        this.conferenceMapper = conferenceMapper;
        this.expertProfileMapper = expertProfileMapper;
        this.paperMapper = paperMapper;
        this.userMapper = userMapper;
        this.fileStorage = fileStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------- 专家 ----------------

    /** 前台"专家介绍"页 */
    public List<Map<String, Object>> listPublicExperts() {
        return expertProfileMapper.listPublicExperts();
    }

    /** 评审分配/主旨报告选择用的精简专家列表 */
    public List<Map<String, Object>> listExpertsLite() {
        return expertProfileMapper.listExpertsLite();
    }

    // ---------------- 专题 ----------------

    public List<Topic> listTopics() {
        return topicMapper.findAll();
    }

    public Long createTopic(TopicRequest req) {
        Topic topic = buildTopic(null, req);
        topicMapper.insert(topic);
        return topic.getId();
    }

    public void updateTopic(Long id, TopicRequest req) {
        if (topicMapper.findById(id) == null) {
            throw new BizException("专题不存在");
        }
        topicMapper.update(buildTopic(id, req));
    }

    private Topic buildTopic(Long id, TopicRequest req) {
        if (req.reportLimit() != null && req.reportLimit() < 0) {
            throw new BizException("报告数量不能为负数");
        }
        if (req.keynoteExpertId() != null && expertProfileMapper.findByUserId(req.keynoteExpertId()) == null) {
            throw new BizException("所选报告专家不存在");
        }
        Topic topic = new Topic();
        topic.setId(id);
        topic.setName(req.name());
        topic.setDescription(req.description());
        topic.setSortNo(req.sortNo());
        topic.setReportLimit(req.reportLimit());
        topic.setKeynoteExpertId(req.keynoteExpertId());
        topic.setReportName(req.reportName());
        topic.setReportTimeNote(req.reportTimeNote());
        return topic;
    }

    public void uploadTopicPpt(Long id, MultipartFile file) {
        if (topicMapper.findById(id) == null) throw new BizException("专题不存在");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".ppt") && !name.endsWith(".pptx") && !name.endsWith(".pdf")) {
            throw new BizException("报告文件仅支持 PPT、PPTX 或 PDF");
        }
        String path = fileStorage.store(file, "topic-ppts", false);
        topicMapper.updatePpt(id, path);
    }

    public DownloadFile downloadTopicPpt(Long id) {
        Topic topic = topicMapper.findById(id);
        if (topic == null || topic.getPptPath() == null || topic.getPptPath().isBlank()) {
            throw new BizException("该专题暂无报告文件");
        }
        String path = topic.getPptPath();
        String ext = path.lastIndexOf('.') >= 0 ? path.substring(path.lastIndexOf('.')) : "";
        return new DownloadFile(fileStorage.loadAsResource(path), topic.getName() + "-报告" + ext);
    }

    public void deleteTopic(Long id) {
        if (topicMapper.findById(id) == null) {
            throw new BizException("专题不存在");
        }
        if (paperMapper.countByTopic(id) > 0) {
            throw new BizException("该专题下已有论文,无法删除");
        }
        topicMapper.deleteById(id);
    }

    // ---------------- 主旨报告 ----------------

    public List<Map<String, Object>> listKeynotes() {
        return keynoteReportMapper.listAll();
    }

    public void createKeynote(KeynoteRequest req) {
        keynoteReportMapper.insert(buildKeynote(null, req));
    }

    public void updateKeynote(Long id, KeynoteRequest req) {
        if (keynoteReportMapper.listAll().stream().noneMatch(k -> ((Number) k.get("id")).longValue() == id)) {
            throw new BizException("主旨报告不存在");
        }
        keynoteReportMapper.update(buildKeynote(id, req));
    }

    public void deleteKeynote(Long id) {
        keynoteReportMapper.deleteById(id);
    }

    private KeynoteReport buildKeynote(Long id, KeynoteRequest req) {
        KeynoteReport report = new KeynoteReport();
        report.setId(id);
        report.setExpertId(req.expertId());
        report.setTitle(req.title());
        report.setPaperAbstract(req.abstractText());
        report.setTimeNote(req.timeNote());
        report.setSortNo(req.sortNo());
        return report;
    }

    // ---------------- 日程 ----------------

    public List<ScheduleItem> listSchedule() {
        return scheduleItemMapper.findAll();
    }

    public void createSchedule(ScheduleRequest req) {
        scheduleItemMapper.insert(buildSchedule(null, req));
    }

    public void updateSchedule(Long id, ScheduleRequest req) {
        scheduleItemMapper.update(buildSchedule(id, req));
    }

    public void deleteSchedule(Long id) {
        scheduleItemMapper.deleteById(id);
    }

    private ScheduleItem buildSchedule(Long id, ScheduleRequest req) {
        ScheduleItem item = new ScheduleItem();
        item.setId(id);
        item.setDay(req.day());
        item.setStartTime(req.startTime());
        item.setEndTime(req.endTime());
        item.setTitle(req.title());
        item.setSpeaker(req.speaker());
        item.setLocation(req.location());
        item.setType(req.type());
        item.setSortNo(req.sortNo());
        return item;
    }

    // ---------------- 通知 ----------------

    public List<Notification> listAllNotifications() {
        return notificationMapper.findAll();
    }

    public List<Notification> listPublishedNotifications() {
        return notificationMapper.findPublished();
    }

    public Notification publishedDetail(Long id) {
        Notification notification = notificationMapper.findPublishedById(id);
        if (notification == null) {
            throw new BizException("通知不存在或未发布");
        }
        return notification;
    }

    public Long createNotification(NotificationRequest req) {
        Notification notification = new Notification();
        notification.setTitle(req.title());
        notification.setContent(req.content());
        notification.setExpiresAt(req.expiresAt());
        notification.setPublisher(req.publisher());
        notificationMapper.insert(notification);
        return notification.getId();
    }

    public void updateNotification(Long id, NotificationRequest req) {
        if (notificationMapper.findById(id) == null) {
            throw new BizException("通知不存在");
        }
        Notification notification = new Notification();
        notification.setId(id);
        notification.setTitle(req.title());
        notification.setContent(req.content());
        notification.setExpiresAt(req.expiresAt());
        notification.setPublisher(req.publisher());
        notificationMapper.update(notification);
    }

    public void publishNotification(Long id) {
        Notification notification = notificationMapper.findById(id);
        if (notification == null) throw new BizException("通知不存在");
        if (notification.getExpiresAt() != null && !notification.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BizException("通知有效期已过,请先修改有效时间");
        }
        notificationMapper.publish(id);
    }

    public void uploadNotificationPdf(Long id, MultipartFile file) {
        if (notificationMapper.findById(id) == null) throw new BizException("通知不存在");
        String path = fileStorage.store(file, "notifications", true);
        notificationMapper.updatePdf(id, path);
    }

    public DownloadFile downloadNotificationPdf(Long id) {
        Notification notice = publishedDetail(id);
        if (notice.getPdfPath() == null || notice.getPdfPath().isBlank()) {
            throw new BizException("该通知没有 PDF 附件");
        }
        return new DownloadFile(fileStorage.loadAsResource(notice.getPdfPath()), notice.getTitle() + ".pdf");
    }

    public void deleteNotification(Long id) {
        notificationMapper.deleteById(id);
    }

    // ---------------- 资料 ----------------

    public List<Material> listMaterials() {
        return materialMapper.findAll();
    }

    @Transactional
    public void uploadMaterial(MultipartFile file, MaterialMetaRequest req) {
        String name = (req.name() == null || req.name().isBlank())
                ? file.getOriginalFilename() : req.name();
        String path = fileStorage.store(file, "materials", false);
        Material material = new Material();
        material.setName(name);
        material.setDescription(req.description());
        material.setFilePath(path);
        material.setFileSize(file.getSize());
        materialMapper.insert(material);
    }

    public void updateMaterialMeta(Long id, MaterialMetaRequest req) {
        materialMapper.updateMeta(id, req.name(), req.description());
    }

    /** 下载计数 +1 后返回文件资源与文件名 */
    public DownloadFile downloadMaterial(Long id) {
        Material material = materialMapper.findById(id);
        if (material == null) {
            throw new BizException("资料不存在");
        }
        materialMapper.increaseDownloadCount(id);
        String path = material.getFilePath();
        String ext = path.lastIndexOf('.') >= 0 ? path.substring(path.lastIndexOf('.')) : "";
        String name = material.getName() == null ? "material" + ext : material.getName() + ext;
        return new DownloadFile(fileStorage.loadAsResource(path), name);
    }

    public void deleteMaterial(Long id) {
        materialMapper.deleteById(id);
    }

    // ---------------- 会议信息 ----------------

    public ConferenceInfo conferenceInfo() {
        return conferenceMapper.find();
    }

    public void updateConference(ConferenceRequest req) {
        ConferenceInfo info = new ConferenceInfo();
        info.setName(req.name());
        info.setShortName(req.shortName());
        info.setVenue(req.venue());
        info.setStartDate(req.startDate());
        info.setEndDate(req.endDate());
        info.setSubmitDeadline(req.submitDeadline());
        info.setNotifyDeadline(req.notifyDeadline());
        info.setFinalDeadline(req.finalDeadline());
        info.setIntro(req.intro());
        info.setCfpText(req.cfpText());
        info.setContactEmail(req.contactEmail());
        info.setOrganizerName(req.organizerName());
        info.setOrganizerUrl(req.organizerUrl());
        info.setOrganizerLogoUrl(req.organizerLogoUrl());
        info.setCoorganizerName(req.coorganizerName());
        info.setCoorganizerUrl(req.coorganizerUrl());
        info.setCoorganizerLogoUrl(req.coorganizerLogoUrl());
        conferenceMapper.update(info);
    }

    // ---------------- 统计 ----------------

    /** 后台仪表盘:论文按状态统计直接查视图 v_paper_stats */
    public Map<String, Object> adminStats() {
        Map<String, Object> data = new HashMap<>();
        data.put("paperCount", paperMapper.countAll());
        data.put("userCount", userMapper.countAll());
        data.put("byRole", jdbcTemplate.queryForList(
                "SELECT role, COUNT(*) AS cnt FROM sys_user GROUP BY role"));
        data.put("byStatus", jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS cnt FROM paper WHERE deleted_at IS NULL GROUP BY status"));
        // 使用视图 v_paper_stats:专题 x 状态
        data.put("topicStats", jdbcTemplate.queryForList(
                "SELECT topic_name AS \"topicName\", status, cnt FROM v_paper_stats ORDER BY topic_name"));
        // 高级 SQL 展示:CTE + 窗口函数 RANK() OVER —— 专题录用率排名(基于视图聚合)
        data.put("topicAcceptRank", jdbcTemplate.queryForList(
                "WITH t AS (" +
                "  SELECT COALESCE(topic_name, '未分专题') AS topic_name, " +
                "         SUM(cnt) AS total, " +
                "         SUM(CASE WHEN status IN ('ACCEPTED','FINAL_SUBMITTED') THEN cnt ELSE 0 END) AS accepted " +
                "  FROM v_paper_stats GROUP BY topic_name" +
                ") " +
                "SELECT topic_name AS \"topicName\", total, accepted, " +
                "       ROUND(100.0 * accepted / NULLIF(total, 0), 1) AS \"rate\", " +
                "       RANK() OVER (ORDER BY ROUND(100.0 * accepted / NULLIF(total, 0), 1) DESC) AS \"rank\" " +
                "FROM t ORDER BY \"rank\", topic_name"));
        return data;
    }

    /** 前台首页统计 */
    public Map<String, Object> publicStats() {
        Map<String, Object> data = new HashMap<>();
        data.put("paperCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM paper", Long.class));
        data.put("acceptedCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM paper WHERE status IN ('ACCEPTED','FINAL_SUBMITTED')", Long.class));
        data.put("expertCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE role = 'EXPERT'", Long.class));
        return data;
    }
}
