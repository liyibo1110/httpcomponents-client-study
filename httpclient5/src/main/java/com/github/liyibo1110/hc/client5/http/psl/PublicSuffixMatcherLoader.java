package com.github.liyibo1110.hc.client5.http.psl;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * PublicSuffixMatcher对应的loader组件。
 * @author liyibo
 * @date 2026-04-16 14:27
 */
@Contract(threading = ThreadingBehavior.SAFE)
public class PublicSuffixMatcherLoader {
    private static final Logger LOG = LoggerFactory.getLogger(PublicSuffixMatcherLoader.class);

    private static PublicSuffixMatcher load(final InputStream in) throws IOException {
        final List<PublicSuffixList> lists = PublicSuffixListParser.INSTANCE.parseByType(new InputStreamReader(in, StandardCharsets.UTF_8));
        return new PublicSuffixMatcher(lists);
    }

    public static PublicSuffixMatcher load(final URL url) throws IOException {
        Args.notNull(url, "URL");
        try (InputStream in = url.openStream()) {
            return load(in);
        }
    }

    public static PublicSuffixMatcher load(final File file) throws IOException {
        Args.notNull(file, "File");
        try (InputStream in = new FileInputStream(file)) {
            return load(in);
        }
    }

    /** 懒汉式单例 */
    private static volatile PublicSuffixMatcher DEFAULT_INSTANCE;

    public static PublicSuffixMatcher getDefault() {
        if (DEFAULT_INSTANCE == null) {
            synchronized (PublicSuffixMatcherLoader.class) {
                if (DEFAULT_INSTANCE == null) {
                    // 注意这个txt文件是通过pom.xml配置的maven插件来实现的动态下载功能
                    final URL url = PublicSuffixMatcherLoader.class.getResource("/mozilla/public-suffix-list.txt");
                    if (url != null) {
                        try {
                            DEFAULT_INSTANCE = load(url);
                        } catch (final IOException ex) {
                            // Should never happen
                            LOG.warn("Failure loading public suffix list from default resource", ex);
                        }
                    } else {
                        DEFAULT_INSTANCE = new PublicSuffixMatcher(DomainType.ICANN, Collections.singletonList("com"), null);
                    }
                }
            }
        }
        return DEFAULT_INSTANCE;
    }
}
