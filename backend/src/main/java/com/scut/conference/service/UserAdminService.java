package com.scut.conference.service;

import com.scut.conference.common.BizException;
import com.scut.conference.dto.AdminDtos.CreateUserRequest;
import com.scut.conference.dto.AdminDtos.UpdateUserRequest;
import com.scut.conference.dto.AccountDtos.ExpertProfileRequest;
import com.scut.conference.entity.ExpertProfile;
import com.scut.conference.entity.SysUser;
import com.scut.conference.mapper.ExpertProfileMapper;
import com.scut.conference.mapper.UserMapper;
import com.scut.conference.security.UserContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理员用户管理:创建账号(系统生成用户名/密码)、编辑、禁用、重置密码
 */
@Service
public class UserAdminService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "EXPERT", "AUTHOR");

    private final UserMapper userMapper;
    private final ExpertProfileMapper expertProfileMapper;
    private final OperationLogService operationLogService;
    private final MailService mailService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserAdminService(UserMapper userMapper, ExpertProfileMapper expertProfileMapper,
                            OperationLogService operationLogService, MailService mailService) {
        this.userMapper = userMapper;
        this.expertProfileMapper = expertProfileMapper;
        this.operationLogService = operationLogService;
        this.mailService = mailService;
    }

    /** 分页用户列表,返回 list+total */
    public Map<String, Object> listUsers(String role, String keyword, int page, int size) {
        String r = role == null ? "" : role;
        String kw = keyword == null ? "" : keyword;
        Map<String, Object> data = new HashMap<>();
        data.put("list", userMapper.listUsers(r, kw, (page - 1) * size, size));
        data.put("total", userMapper.countUsers(r, kw));
        return data;
    }

    /**
     * 创建账号:用户名/密码由系统随机生成,仅此一次返回明文,由管理员转交本人。
     * 创建专家时同步创建专家档案。
     */
    @Transactional
    public Map<String, String> create(CreateUserRequest req) {
        if (!VALID_ROLES.contains(req.role())) {
            throw new BizException("角色不合法");
        }
        if (req.reviewCapacity() != null && req.reviewCapacity() < 0) {
            throw new BizException("评审容量不能为负数");
        }
        if (userMapper.findByEmail(req.email()) != null) {
            throw new BizException("该邮箱已被使用");
        }
        String prefix = switch (req.role()) {
            case "EXPERT" -> "expert";
            case "AUTHOR" -> "author";
            default -> "admin";
        };
        String username = prefix + randomCode(6);
        while (userMapper.findByUsername(username) != null) {
            username = prefix + randomCode(6);
        }
        String password = randomPassword();

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setRealName(req.realName());
        user.setEmail(req.email());
        user.setGender(req.gender());
        user.setPhone(req.phone());
        user.setPostalCode(req.postalCode());
        user.setWorkUnit(req.affiliation());
        user.setRole(req.role());
        user.setStatus(1);   // 管理员创建的账号直接可用
        userMapper.insert(user);

        if ("EXPERT".equals(req.role())) {
            ExpertProfile profile = new ExpertProfile();
            profile.setUserId(user.getId());
            profile.setTitle(req.title());
            profile.setAffiliation(req.affiliation());
            profile.setResearchKeywords(req.researchKeywords());
            profile.setAddress(req.address());
            profile.setWebsite(req.website());
            profile.setBio(req.bio());
            profile.setReviewTopics(req.reviewTopics());
            profile.setReviewCapacity(req.reviewCapacity());
            profile.setReviewEnabled(true);
            expertProfileMapper.insert(profile);
        }
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        operationLogService.log("USER_CREATE", "创建" + req.role() + "账号 " + username + "(" + req.realName() + ")");
        if ("EXPERT".equals(req.role()) && mailService.isEnabled()) {
            String email = req.email();
            String loginName = username;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    try {
                        mailService.send(email, "会议评审专家账号已创建",
                                "您的评审专家账号已创建，用户名：" + loginName
                                        + "。请在登录页使用邮箱验证码设置密码，首次登录后完善个人资料。");
                    } catch (Exception e) {
                        org.slf4j.LoggerFactory.getLogger(UserAdminService.class)
                                .warn("专家账号 {} 邮件发送失败: {}", loginName, e.getMessage());
                    }
                }
            });
        }
        return data;
    }

    /** UTF-8 CSV 批量导入专家；任一行失败则整批回滚。 */
    @Transactional
    public List<Map<String, String>> importExperts(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 1024 * 1024) {
            throw new BizException("请选择不超过 1MB 的 CSV 文件");
        }
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "");
        } catch (IOException e) {
            throw new BizException("CSV 文件读取失败");
        }
        String[] lines = content.split("\\R");
        if (lines.length < 2 || lines.length > 101) {
            throw new BizException("CSV 须包含表头和 1–100 条专家记录");
        }
        List<String> headers = parseCsvLine(lines[0]);
        if (!headers.contains("realName") || !headers.contains("email")) {
            throw new BizException("CSV 表头必须包含 realName 和 email");
        }
        List<Map<String, String>> created = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            List<String> cells = parseCsvLine(lines[i]);
            if (cells.size() != headers.size()) throw new BizException("CSV 第 " + (i + 1) + " 行列数不一致");
            Map<String, String> row = new HashMap<>();
            for (int j = 0; j < headers.size(); j++) row.put(headers.get(j).trim(), cells.get(j).trim());
            String name = row.getOrDefault("realName", "");
            String email = row.getOrDefault("email", "");
            if (name.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new BizException("CSV 第 " + (i + 1) + " 行姓名或邮箱不正确");
            }
            Integer capacity = null;
            if (!row.getOrDefault("reviewCapacity", "").isBlank()) {
                try {
                    capacity = Integer.parseInt(row.get("reviewCapacity"));
                } catch (NumberFormatException e) {
                    throw new BizException("CSV 第 " + (i + 1) + " 行评审容量不是整数");
                }
            }
            if (capacity != null && capacity < 0) throw new BizException("评审容量不能为负数");
            Map<String, String> result = create(new CreateUserRequest(name, email, "EXPERT",
                    row.get("title"), row.get("affiliation"), row.get("researchKeywords"),
                    row.get("gender"), row.get("phone"), row.get("postalCode"), row.get("address"),
                    row.get("website"), row.get("bio"), row.get("reviewTopics"), capacity));
            result.put("realName", name);
            result.put("email", email);
            created.add(result);
        }
        if (created.isEmpty()) throw new BizException("CSV 没有可导入的专家记录");
        return created;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"'); i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                values.add(cell.toString()); cell.setLength(0);
            } else cell.append(c);
        }
        if (quoted) throw new BizException("CSV 引号未闭合");
        values.add(cell.toString());
        return values;
    }

    /** 编辑基本信息;专家的档案信息(职称/单位/研究方向)一并更新 */
    @Transactional
    public void update(Long id, UpdateUserRequest req) {
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (req.email() != null && !req.email().equals(user.getEmail())) {
            SysUser exist = userMapper.findByEmail(req.email());
            if (exist != null) {
                throw new BizException("该邮箱已被其他账号使用");
            }
        }
        userMapper.updateBasic(id, req.realName(), req.email());
        if ("EXPERT".equals(user.getRole())) {
            userMapper.updateExpertContact(id, req.gender(), req.phone(), req.postalCode(), req.affiliation());
            ExpertProfile profile = expertProfileMapper.findByUserId(id);
            if (profile == null) {
                profile = new ExpertProfile();
                profile.setUserId(id);
                profile.setTitle(req.title());
                profile.setAffiliation(req.affiliation());
                profile.setResearchKeywords(req.researchKeywords());
                profile.setIsKeynote(req.isKeynote());
                profile.setAddress(req.address());
                profile.setWebsite(req.website());
                profile.setBio(req.bio());
                profile.setReviewTopics(req.reviewTopics());
                profile.setReviewCapacity(req.reviewCapacity());
                profile.setReviewEnabled(req.reviewEnabled());
                expertProfileMapper.insert(profile);
            } else {
                profile.setTitle(req.title());
                profile.setAffiliation(req.affiliation());
                profile.setResearchKeywords(req.researchKeywords());
                profile.setIsKeynote(req.isKeynote());
                profile.setAddress(req.address());
                profile.setWebsite(req.website());
                profile.setBio(req.bio());
                profile.setReviewTopics(req.reviewTopics());
                profile.setReviewCapacity(req.reviewCapacity());
                profile.setReviewEnabled(req.reviewEnabled());
                expertProfileMapper.updateByUserId(profile);
            }
        }
    }

    public ExpertProfile myExpertProfile() {
        Long userId = UserContext.requireUserId();
        SysUser user = userMapper.findById(userId);
        if (user == null || !"EXPERT".equals(user.getRole())) throw new BizException("仅专家可查看此资料");
        return expertProfileMapper.findByUserId(userId);
    }

    public void updateOwnExpertProfile(ExpertProfileRequest req) {
        ExpertProfile profile = myExpertProfile();
        if (profile == null) throw new BizException("专家档案不存在");
        if (req.reviewCapacity() != null && req.reviewCapacity() < 0) throw new BizException("评审容量不能为负数");
        profile.setTitle(req.title());
        profile.setAffiliation(req.affiliation());
        profile.setBio(req.bio());
        profile.setAddress(req.address());
        profile.setWebsite(req.website());
        profile.setResearchKeywords(req.researchKeywords());
        profile.setReviewTopics(req.reviewTopics());
        profile.setReviewCapacity(req.reviewCapacity());
        profile.setReviewEnabled(req.reviewEnabled());
        expertProfileMapper.updateByUserId(profile);
        operationLogService.log("EXPERT_PROFILE", "专家更新个人档案");
    }

    /** 启用/禁用账号 */
    public void toggleStatus(Long id, Integer status) {
        if (id.equals(UserContext.getUserId())) {
            throw new BizException("不能禁用当前登录的管理员账号");
        }
        if (userMapper.findById(id) == null) {
            throw new BizException("用户不存在");
        }
        userMapper.updateStatus(id, status);
        operationLogService.log("USER_STATUS", (status == 1 ? "启用" : "禁用") + "账号 #" + id);
    }

    /**
     * 注册审核:通过 -> status=1(可登录);拒绝 -> status=0(禁用,保留记录)。
     * 只有处于"待审核(2)"状态的账号可执行。
     */
    public void reviewRegister(Long id, boolean approve) {
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 2) {
            throw new BizException("该账号不在待审核状态");
        }
        userMapper.updateStatus(id, approve ? 1 : 0);
        operationLogService.log("REGISTER_REVIEW",
                (approve ? "通过" : "拒绝") + "注册审核:" + user.getUsername());
    }

    /** 重置密码:返回一次性新密码 */
    public String resetPassword(Long id) {
        if (userMapper.findById(id) == null) {
            throw new BizException("用户不存在");
        }
        String password = randomPassword();
        userMapper.updatePassword(id, encoder.encode(password));
        operationLogService.log("USER_RESET_PWD", "重置账号 #" + id + " 的密码");
        return password;
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /** 保证含大小写字母、数字和特殊字符,满足常见密码复杂度要求 */
    private String randomPassword() {
        return "Cs" + randomCode(6) + RANDOM.nextInt(10) + "!";
    }
}
