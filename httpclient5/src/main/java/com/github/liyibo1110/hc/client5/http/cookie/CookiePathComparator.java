package com.github.liyibo1110.hc.client5.http.cookie;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

import java.io.Serializable;
import java.util.Comparator;

/**
 * 此Cookie比较器可确保满足共同条件的多个Cookie在Cookie头中按特定顺序排列，即Path属性更具体的Cookie优先于Path属性较不具体的Cookie。
 * 此比较器假设两个Cookie的Path属性与共同的请求URI匹配。否则，比较结果未定义。
 * @author liyibo
 * @date 2026-04-15 15:25
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class CookiePathComparator implements Serializable, Comparator<Cookie> {
    private static final long serialVersionUID = 7523645369616405818L;

    public static final CookiePathComparator INSTANCE = new CookiePathComparator();

    private String normalizePath(final Cookie cookie) {
        String path = cookie.getPath();
        if (path == null)
            path = "/";
        if (!path.endsWith("/"))
            path = path + '/';
        return path;
    }

    @Override
    public int compare(final Cookie c1, final Cookie c2) {
        final String path1 = normalizePath(c1);
        final String path2 = normalizePath(c2);
        if (path1.equals(path2))
            return 0;
        else if (path1.startsWith(path2))
            return -1;
        else if (path2.startsWith(path1))
            return 1;
        else
            return 0;
    }
}
