package com.scut.conference.service;

import com.scut.conference.common.BizException;
import com.scut.conference.dto.AuthDtos.LoginRequest;
import com.scut.conference.dto.AuthDtos.RegisterRequest;
import com.scut.conference.dto.AccountDtos.UpdateProfileRequest;
import com.scut.conference.entity.SysUser;
import com.scut.conference.entity.UserAvatar;
import com.scut.conference.mapper.UserAvatarMapper;
import com.scut.conference.mapper.UserMapper;
import com.scut.conference.security.JwtUtil;
import com.scut.conference.security.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 注册(邮箱验证码审核)/登录/当前用户
 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final UserAvatarMapper userAvatarMapper;
    private final JdbcTemplate jdbcTemplate;
    private final MailService mailService;
    private final JwtUtil jwtUtil;
    private final OperationLogService operationLogService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, UserAvatarMapper userAvatarMapper,
                       JdbcTemplate jdbcTemplate,
                       MailService mailService, JwtUtil jwtUtil,
                       OperationLogService operationLogService) {
        this.userMapper = userMapper;
        this.userAvatarMapper = userAvatarMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.mailService = mailService;
        this.jwtUtil = jwtUtil;
        this.operationLogService = operationLogService;
    }

    /** 发送 6 位验证码,5 分钟有效。返回值:邮件未启用时把验证码返回给前端方便联调 */
    public String sendCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        jdbcTemplate.update(
                "INSERT INTO verify_code(email, code, expire_at, used) VALUES(?, ?, now() + interval '5 minute', false)",
                email, code);
        mailService.send(email, "会议系统注册验证码", "您正在注册会议管理系统账号,验证码为:" + code + ",5 分钟内有效。");
        return mailService.isEnabled() ? null : code;
    }

    /**
     * 邮箱验证码校验 + 注册作者账号。
     * 注册后进入"待审核"状态(status=2),由管理员在后台通过/拒绝后方可登录使用。
     */
    public void register(RegisterRequest req) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM verify_code WHERE email = ? AND code = ? AND used = false AND expire_at > now() " +
                        "ORDER BY id DESC LIMIT 1", req.email(), req.code());
        if (rows.isEmpty()) {
            throw new BizException("验证码错误或已过期");
        }
        if (userMapper.findByUsername(req.username()) != null) {
            throw new BizException("用户名已存在");
        }
        if (userMapper.findByEmail(req.email()) != null) {
            throw new BizException("该邮箱已被注册");
        }
        // 验证码标记已用 + 创建待审核账号(同一事务,注册失败可整体回滚)
        jdbcTemplate.update("UPDATE verify_code SET used = true WHERE id = ?", rows.get(0).get("id"));
        SysUser user = new SysUser();
        user.setUsername(req.username());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRealName(req.realName());
        user.setEmail(req.email());
        user.setRole("AUTHOR");
        user.setStatus(2);   // 待管理员审核
        userMapper.insert(user);
        operationLogService.log("REGISTER", "注册作者账号(待审核)" + user.getUsername(),
                user.getId(), user.getUsername(), "AUTHOR");
    }

    /** 用户名密码登录,签发 JWT(区分:待审核 / 禁用) */
    public Map<String, Object> login(LoginRequest req) {
        SysUser user = userMapper.findByUsername(req.username());
        if (user == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 2) {
            throw new BizException("账号正在等待管理员审核,审核通过后即可登录");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已被禁用,请联系管理员");
        }
        // 登录时 UserContext 尚未建立,显式指定用户信息
        operationLogService.log("LOGIN", "用户登录", user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtil.generate(user.getId(), user.getRole()));
        data.put("user", user);   // passwordHash 已 @JsonIgnore,不会序列化
        return data;
    }

    // ---------------- 头像(BYTEA 大对象存储) ----------------

    /** 上传/更换头像:仅 png/jpeg,≤2MB;先删后插保持一个用户一张头像 */
    @Transactional
    public void saveAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择头像图片");
        }
        if (file.getSize() > 2L * 1024 * 1024) {
            throw new BizException("头像图片不能超过 2MB");
        }
        String type = file.getContentType() == null ? "" : file.getContentType();
        if (!type.equals("image/png") && !type.equals("image/jpeg") && !type.equals("image/jpg")) {
            throw new BizException("头像仅支持 PNG/JPEG 格式");
        }
        UserAvatar avatar = new UserAvatar();
        avatar.setUserId(userId);
        avatar.setContentType(type.equals("image/jpg") ? "image/jpeg" : type);
        try {
            avatar.setContent(file.getBytes());
        } catch (IOException e) {
            throw new BizException("读取图片失败");
        }
        userAvatarMapper.deleteByUserId(userId);
        userAvatarMapper.insert(avatar);
        operationLogService.log("AVATAR_UPDATE", "更新头像");
    }

    /** 读取头像(前台匿名可读,<img> 标签无法携带 Token) */
    public UserAvatar getAvatar(Long userId) {
        return userAvatarMapper.findByUserId(userId);
    }

    public SysUser currentUser() {
        return userMapper.findById(UserContext.requireUserId());
    }

    /** 个人中心:修改姓名与邮箱(邮箱变更需保证唯一) */
    public void updateProfile(Long userId, UpdateProfileRequest req) {
        SysUser user = userMapper.findById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!req.email().equals(user.getEmail())) {
            SysUser exist = userMapper.findByEmail(req.email());
            if (exist != null) {
                throw new BizException("该邮箱已被其他账号使用");
            }
        }
        userMapper.updateProfile(userId, req.realName(), req.email(), req.gender(),
                req.education(), req.jobTitle(), req.phone(), req.workUnit(), req.postalCode());
        operationLogService.log("PROFILE_UPDATE", "修改个人资料");
    }

    /** 个人中心:验证原密码后修改密码 */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.findById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!encoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BizException("原密码不正确");
        }
        userMapper.updatePassword(userId, encoder.encode(newPassword));
        operationLogService.log("PASSWORD_CHANGE", "修改登录密码");
    }

    /** 忘记密码:邮箱验证码校验通过后重置密码 */
    public void resetPassword(String email, String code, String newPassword) {
        SysUser user = userMapper.findByEmail(email);
        if (user == null) {
            throw new BizException("该邮箱未注册");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM verify_code WHERE email = ? AND code = ? AND used = false AND expire_at > now() " +
                        "ORDER BY id DESC LIMIT 1", email, code);
        if (rows.isEmpty()) {
            throw new BizException("验证码错误或已过期");
        }
        jdbcTemplate.update("UPDATE verify_code SET used = true WHERE id = ?", rows.get(0).get("id"));
        userMapper.updatePassword(user.getId(), encoder.encode(newPassword));
    }
}
