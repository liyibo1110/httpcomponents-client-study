package com.github.liyibo1110.hc.client5.http.protocol;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 该类表示一组用作重定向目标的URI。
 * @author liyibo
 * @date 2026-04-15 16:18
 */
public final class RedirectLocations {
    private final Set<URI> unique;
    private final List<URI> all;

    public RedirectLocations() {
        super();
        this.unique = new HashSet<>();
        this.all = new ArrayList<>();
    }

    public boolean contains(final URI uri) {
        return this.unique.contains(uri);
    }

    public void add(final URI uri) {
        this.unique.add(uri);
        this.all.add(uri);
    }

    public List<URI> getAll() {
        return new ArrayList<>(this.all);
    }

    public URI get(final int index) {
        return this.all.get(index);
    }

    public int size() {
        return this.all.size();
    }

    public void clear() {
        unique.clear();
        all.clear();
    }
}
