package com.github.liyibo1110.hc.client5.http.entity.mime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MIME entity的header部分
 * @author liyibo
 * @date 2026-04-15 22:30
 */
public class Header implements Iterable<MimeField> {
    private final List<MimeField> fields;
    private final Map<String, List<MimeField>> fieldMap;

    public Header() {
        super();
        this.fields = new LinkedList<>();
        this.fieldMap = new HashMap<>();
    }

    public void addField(final MimeField field) {
        if (field == null)
            return;
        final String key = field.getName().toLowerCase(Locale.ROOT);
        final List<MimeField> values = this.fieldMap.computeIfAbsent(key, k -> new LinkedList<>());
        values.add(field);
        this.fields.add(field);
    }

    public List<MimeField> getFields() {
        return new ArrayList<>(this.fields);
    }

    public MimeField getField(final String name) {
        if (name == null)
            return null;
        final String key = name.toLowerCase(Locale.ROOT);
        final List<MimeField> list = this.fieldMap.get(key);
        if (list != null && !list.isEmpty())
            return list.get(0);
        return null;
    }

    public List<MimeField> getFields(final String name) {
        if (name == null)
            return null;
        final String key = name.toLowerCase(Locale.ROOT);
        final List<MimeField> list = this.fieldMap.get(key);
        if (list == null || list.isEmpty())
            return Collections.emptyList();
        return new ArrayList<>(list);
    }

    public int removeFields(final String name) {
        if (name == null)
            return 0;
        final String key = name.toLowerCase(Locale.ROOT);
        final List<MimeField> removed = fieldMap.remove(key);
        if (removed == null || removed.isEmpty())
            return 0;
        this.fields.removeAll(removed);
        return removed.size();
    }

    public void setField(final MimeField field) {
        if (field == null)
            return;

        final String key = field.getName().toLowerCase(Locale.ROOT);
        final List<MimeField> list = fieldMap.get(key);
        if (list == null || list.isEmpty()) {
            addField(field);
            return;
        }
        list.clear();
        list.add(field);
        int firstOccurrence = -1;
        int index = 0;
        for (final Iterator<MimeField> it = this.fields.iterator(); it.hasNext(); index++) {
            final MimeField f = it.next();
            if (f.getName().equalsIgnoreCase(field.getName())) {
                it.remove();
                if (firstOccurrence == -1)
                    firstOccurrence = index;
            }
        }
        this.fields.add(firstOccurrence, field);
    }

    @Override
    public Iterator<MimeField> iterator() {
        return Collections.unmodifiableList(fields).iterator();
    }

    @Override
    public String toString() {
        return this.fields.toString();
    }
}
