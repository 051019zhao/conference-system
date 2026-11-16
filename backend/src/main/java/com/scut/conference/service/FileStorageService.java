package com.scut.conference.service;

import com.scut.conference.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地磁盘文件存储:按子目录(papers/finals/materials)隔离,文件名用 UUID 防冲突。
 * 数据库只保存路径,下载时再从磁盘读取。
 * 类型安全:要求 PDF 的文件做"扩展名 + 文件头魔数(%PDF-)"双重校验,
 * 仅凭扩展名判断会被伪造(改后缀),魔数校验是真正的内容级校验。
 */
@Service
public class FileStorageService {

    /** 附件大小上限 20MB(题目要求) */
    private static final long MAX_SIZE = 20L * 1024 * 1024;
    private static final long MAX_SUPPLEMENT_SIZE = 10L * 1024 * 1024;

    /** PDF 文件头魔数 */
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

    /** 压缩包/图片魔数:zip(PK)、7z、rar */
    private static final byte[] ZIP_MAGIC = {'P', 'K', 3, 4};
    private static final byte[] SEVENZ_MAGIC = {'7', 'z', (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};
    private static final byte[] RAR_MAGIC = {'R', 'a', 'r', '!', 0x1A, 0x07};
    private static final byte[] DOC_MAGIC = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    @Value("${app.upload-dir}")
    private String baseDir;

    public String store(MultipartFile file, String subdir, boolean requirePdf) {
        return store(file, subdir, requirePdf ? FileKind.PDF_ONLY : FileKind.ANY);
    }

    /** 附属资料:压缩包、Word、PDF 和常用图片，均校验文件头。 */
    public String storeSupplement(MultipartFile file) {
        return store(file, "supplements", FileKind.SUPPLEMENT);
    }

    private enum FileKind { PDF_ONLY, SUPPLEMENT, ANY }

    private String store(MultipartFile file, String subdir, FileKind kind) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        long sizeLimit = kind == FileKind.SUPPLEMENT ? MAX_SUPPLEMENT_SIZE : MAX_SIZE;
        if (file.getSize() > sizeLimit) {
            throw new BizException(kind == FileKind.SUPPLEMENT ? "证明材料不能超过 10MB" : "附件不能超过 20MB");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.lastIndexOf('.') >= 0) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        switch (kind) {
            case PDF_ONLY -> {
                if (!".pdf".equals(ext)) {
                    throw new BizException("仅支持上传 PDF 文件");
                }
                if (!matchesMagic(file, PDF_MAGIC)) {
                    throw new BizException("文件内容不是有效的 PDF(校验文件头失败)");
                }
            }
            case SUPPLEMENT -> {
                boolean extOk = ".zip".equals(ext) || ".rar".equals(ext) || ".7z".equals(ext)
                        || ".doc".equals(ext) || ".docx".equals(ext)
                        || ".pdf".equals(ext) || ".png".equals(ext) || ".jpg".equals(ext) || ".jpeg".equals(ext);
                if (!extOk) {
                    throw new BizException("证明材料仅支持 ZIP/RAR/7z、Word、PDF 或 PNG/JPG 图片");
                }
                boolean magicOk = switch (ext) {
                    case ".pdf" -> matchesMagic(file, PDF_MAGIC);
                    case ".zip" -> matchesMagic(file, ZIP_MAGIC);
                    case ".7z" -> matchesMagic(file, SEVENZ_MAGIC);
                    case ".rar" -> matchesMagic(file, RAR_MAGIC);
                    case ".doc" -> matchesMagic(file, DOC_MAGIC);
                    case ".docx" -> matchesMagic(file, ZIP_MAGIC);
                    case ".png" -> matchesMagic(file, PNG_MAGIC);
                    case ".jpg", ".jpeg" -> matchesMagic(file, JPEG_MAGIC);
                    default -> false;
                };
                if (!magicOk) {
                    throw new BizException("附属资料内容与扩展名不符(校验文件头失败)");
                }
            }
            default -> { }
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Paths.get(baseDir, subdir).toAbsolutePath();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target);
            return target.toString();
        } catch (IOException e) {
            throw new BizException("文件保存失败:" + e.getMessage());
        }
    }

    /** 读取文件头若干字节比对魔数,只读流不落盘 */
    private boolean matchesMagic(MultipartFile file, byte[] magic) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = new byte[magic.length];
            int read = in.read(head);
            if (read < magic.length) {
                return false;
            }
            for (int i = 0; i < magic.length; i++) {
                if (head[i] != magic[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** 把磁盘文件包装为可下载资源,不存在时报业务异常 */
    public Resource loadAsResource(String path) {
        FileSystemResource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new BizException("文件不存在或已被删除");
        }
        return resource;
    }
}
