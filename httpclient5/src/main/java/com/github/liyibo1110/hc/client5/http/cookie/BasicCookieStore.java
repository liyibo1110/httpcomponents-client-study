package com.github.liyibo1110.hc.client5.http.cookie;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * CookieStore的默认实现类。
 * @author liyibo
 * @date 2026-04-15 15:30
 */
@Contract(threading = ThreadingBehavior.SAFE)
public class BasicCookieStore implements CookieStore, Serializable {
    private static final long serialVersionUID = -7581093305228232025L;

    /** 底层存储结构 */
    private final TreeSet<Cookie> cookies;
    private transient ReadWriteLock lock;

    public BasicCookieStore() {
        super();
        this.cookies = new TreeSet<>(CookieIdentityComparator.INSTANCE);
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void addCookie(final Cookie cookie) {
        if(cookie != null) {
            lock.writeLock().lock();
            try {
                // 先尝试删除旧的cookie，并且新的cookie没有过期才会写入store
                cookies.remove(cookie);
                if (!cookie.isExpired(Instant.now()))
                    cookies.add(cookie);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public void addCookies(final Cookie[] cookies) {
        if (cookies != null) {
            for (final Cookie cookie : cookies)
                this.addCookie(cookie);
        }
    }

    @Override
    public List<Cookie> getCookies() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(cookies);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean clearExpired(final Date date) {
        if (date == null)
            return false;
        lock.writeLock().lock();
        try {
            // 只要有一个被remove了，最终结果就是true
            boolean removed = false;
            for(final Iterator<Cookie> iter = cookies.iterator(); iter.hasNext(); ) {
                if(iter.next().isExpired(date)) {
                    iter.remove();
                    removed = true;
                }
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean clearExpired(final Instant instant) {
        if (instant == null)
            return false;
        lock.writeLock().lock();
        try {
            boolean removed = false;
            for (final Iterator<Cookie> it = cookies.iterator(); it.hasNext(); ) {
                if (it.next().isExpired(instant)) {
                    it.remove();
                    removed = true;
                }
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            cookies.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return cookies.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        /* Reinstantiate transient fields. */
        this.lock = new ReentrantReadWriteLock();
    }
}
