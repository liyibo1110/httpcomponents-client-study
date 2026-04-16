package com.github.liyibo1110.hc.client5.http.cookie;

/**
 * CookieAttributeHandler的扩展旨在处理一个特定的常见属性，该属性的名称可通过getAttributeName()方法获取。
 * @author liyibo
 * @date 2026-04-15 15:22
 */
public interface CommonCookieAttributeHandler extends CookieAttributeHandler {

    String getAttributeName();
}
