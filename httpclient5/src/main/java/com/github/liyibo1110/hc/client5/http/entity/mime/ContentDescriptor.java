package com.github.liyibo1110.hc.client5.http.entity.mime;

/**
 * 表示常见的content properties
 * @author liyibo
 * @date 2026-04-15 21:33
 */
public interface ContentDescriptor {

    /**
     * 返回正文描述符的MIME类型。
     * @return 从content-type定义中解析出的MIME类型。该值不能为空，但若未指定content-type，则必须为text/plain。
     */
    String getMimeType();

    /**
     * 获取此内容的默认MIME媒体类型。例如TEXT、IMAGE、MULTIPART。
     * @return 当指定了content-type时，使用MIME媒体类型，否则使用正确的默认值TEXT。
     */
    String getMediaType();

    /**
     * 获取此内容的默认MIME子类型
     * @return 当指定了content-type时，使用MIME媒体类型，否则使用正确的默认值PLAIN。
     */
    String getSubType();

    /**
     * 正文描述符字符集，其默认值根据MIME类型进行适当设置。
     * 对于TEXT类型，该值将默认为us-ascii。对于其他类型，如果缺少charset参数，则该属性将为null。
     * @return 从内容类型定义中解析出的字符集。对于TEXT类型，该字段不能为空；若未设置，则默认设为us-ascii。对于其他类型，若未设置，则返回null。
     */
    String getCharset();

    /**
     * 返回正文描述符的content-length字段。
     * @return 已知的内容长度，或-1（表示不存在Content-Length头部）。
     */
    long getContentLength();
}
