package com.scut.conference.common;

import org.springframework.core.io.Resource;

/** 下载文件:资源 + 建议的下载文件名 */
public record DownloadFile(Resource resource, String fileName) {}
