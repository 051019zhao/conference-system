package com.scut.conference.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色声明:标注在 Controller 方法或类上,拦截器据此做角色校验
 * 示例:@RequireRole({"ADMIN","EXPERT"})
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireRole {
    String[] value();
}
