package com.scut.conference.common;

/**
 * 业务异常:Service 层校验失败时抛出,由全局异常处理器转成统一响应
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
