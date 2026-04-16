package com.github.liyibo1110.hc.client5.http.entity.mime;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author liyibo
 * @date 2026-04-15 21:39
 */
public interface ContentBody extends ContentDescriptor {

    String getFilename();

    void writeTo(OutputStream out) throws IOException;
}
