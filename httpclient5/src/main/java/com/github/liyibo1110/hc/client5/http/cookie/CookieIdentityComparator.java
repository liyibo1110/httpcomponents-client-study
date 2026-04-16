package com.github.liyibo1110.hc.client5.http.cookie;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

import java.io.Serializable;
import java.util.Comparator;

/**
 * 此Cookie比较工具可用于比较Cookie的身份。
 * 如果Cookie的名称相同且其域名属性（不区分大小写）匹配，则视为相同。
 * @author liyibo
 * @date 2026-04-15 15:23
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class CookieIdentityComparator implements Serializable, Comparator<Cookie> {
    private static final long serialVersionUID = 4466565437490631532L;

    public static final CookieIdentityComparator INSTANCE = new CookieIdentityComparator();

    @Override
    public int compare(final Cookie c1, final Cookie c2) {
        int res = c1.getName().compareTo(c2.getName());
        if (res == 0) {
            // do not differentiate empty and null domains
            String d1 = c1.getDomain();
            if (d1 == null)
                d1 = "";
            String d2 = c2.getDomain();
            if (d2 == null)
                d2 = "";
            res = d1.compareToIgnoreCase(d2);
        }
        if (res == 0) {
            String p1 = c1.getPath();
            if (p1 == null)
                p1 = "/";
            String p2 = c2.getPath();
            if (p2 == null)
                p2 = "/";
            res = p1.compareTo(p2);
        }
        return res;
    }
}
