package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.http.ContentType;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 由File支持的二进制body部分。
 * @author liyibo
 * @date 2026-04-15 22:14
 */
public class FileBody extends AbstractContentBody {
    private final File file;
    private final String filename;

    public FileBody(final File file) {
        this(file, ContentType.DEFAULT_BINARY, file != null ? file.getName() : null);
    }

    public FileBody(final File file, final ContentType contentType, final String filename) {
        super(contentType);
        Args.notNull(file, "File");
        this.file = file;
        this.filename = filename == null ? file.getName() : filename;
    }

    public FileBody(final File file, final ContentType contentType) {
        this(file, contentType, file != null ? file.getName() : null);
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(this.file);
    }

    @Override
    public void writeTo(final OutputStream out) throws IOException {
        Args.notNull(out, "Output stream");
        try (InputStream in = new FileInputStream(this.file)) {
            final byte[] tmp = new byte[4096];
            int l;
            while ((l = in.read(tmp)) != -1)
                out.write(tmp, 0, l);
            out.flush();
        }
    }

    @Override
    public long getContentLength() {
        return this.file.length();
    }

    @Override
    public String getFilename() {
        return filename;
    }

    public File getFile() {
        return this.file;
    }
}
