package com.github.liyibo1110.hc.client5.http.utils;

import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.MessageHeaders;
import com.github.liyibo1110.hc.core5.util.Args;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 一个用于解析和格式化HTTP日期（如Cookie和其他标头中所用）的辅助类。
 * 该类支持RFC 2616第3.3.1节定义的日期格式，以及其他一些常见的非标准格式。
 * @author liyibo
 * @date 2026-04-14 10:50
 */
public final class DateUtils {

    private DateUtils() {}

    /** 用于解析RFC 1123格式HTTP日期头部的日期格式模式 */
    public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    public static final DateTimeFormatter FORMATTER_RFC1123 = new DateTimeFormatterBuilder()
            .parseLenient()
            .parseCaseInsensitive()
            .appendPattern(PATTERN_RFC1123)
            .toFormatter(Locale.ENGLISH);

    /** 用于解析RFC 1036格式HTTP日期头部的日期格式模式 */
    public static final String PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";

    public static final DateTimeFormatter FORMATTER_RFC1036 = new DateTimeFormatterBuilder()
            .parseLenient()
            .parseCaseInsensitive()
            .appendPattern(PATTERN_RFC1036)
            .toFormatter(Locale.ENGLISH);

    /** 用于解析ANSI C格式HTTP日期头部的日期格式模式 */
    public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";

    public static final DateTimeFormatter FORMATTER_ASCTIME = new DateTimeFormatterBuilder()
            .parseLenient()
            .parseCaseInsensitive()
            .appendPattern(PATTERN_ASCTIME)
            .toFormatter(Locale.ENGLISH);

    public static final DateTimeFormatter[] STANDARD_PATTERNS = new DateTimeFormatter[] {
            FORMATTER_RFC1123,
            FORMATTER_RFC1036,
            FORMATTER_ASCTIME
    };

    static final ZoneId GMT_ID = ZoneId.of("GMT");

    public static Date toDate(final Instant instant) {
        return instant != null ? new Date(instant.toEpochMilli()) : null;
    }

    public static Instant toInstant(final Date date) {
        return date != null ? Instant.ofEpochMilli(date.getTime()) : null;
    }

    public static LocalDateTime toUTC(final Instant instant) {
        return instant != null ? instant.atZone(ZoneOffset.UTC).toLocalDateTime() : null;
    }

    public static LocalDateTime toUTC(final Date date) {
        return toUTC(toInstant(date));
    }

    public static Instant parseDate(final String dateValue, final DateTimeFormatter... dateFormatters) {
        Args.notNull(dateValue, "Date value");
        String v = dateValue;
        // trim single quotes around date if present
        // see issue #5279
        if (v.length() > 1 && v.startsWith("'") && v.endsWith("'"))
            v = v.substring (1, v.length() - 1);

        for (final DateTimeFormatter dateFormatter : dateFormatters) {
            try {
                return Instant.from(dateFormatter.parse(v));
            } catch (final DateTimeParseException ignore) {

            }
        }
        return null;
    }

    public static Instant parseStandardDate(final String dateValue) {
        return parseDate(dateValue, STANDARD_PATTERNS);
    }

    public static Instant parseStandardDate(final MessageHeaders headers, final String headerName) {
        if (headers == null)
            return null;
        final Header header = headers.getFirstHeader(headerName);
        if (header == null)
            return null;
        return parseStandardDate(header.getValue());
    }

    public static String formatStandardDate(final Instant instant) {
        return formatDate(instant, FORMATTER_RFC1123);
    }

    public static String formatDate(final Instant instant, final DateTimeFormatter dateTimeFormatter) {
        Args.notNull(instant, "Instant");
        Args.notNull(dateTimeFormatter, "DateTimeFormatter");
        return dateTimeFormatter.format(instant.atZone(GMT_ID));
    }

    @Deprecated
    public static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    @Deprecated
    public static Date parseDate(final String dateValue) {
        return parseDate(dateValue, null, null);
    }

    @Deprecated
    public static Date parseDate(final MessageHeaders headers, final String headerName) {
        return toDate(parseStandardDate(headers, headerName));
    }

    /**
     * 使用给定的消息头进行比较，判断第一条消息是否晚于第二条消息。
     */
    @Deprecated
    public static boolean isAfter(final MessageHeaders message1, final MessageHeaders message2, final String headerName) {
        if (message1 != null && message2 != null) {
            final Header dateHeader1 = message1.getFirstHeader(headerName);
            if (dateHeader1 != null) {
                final Header dateHeader2 = message2.getFirstHeader(headerName);
                if (dateHeader2 != null) {
                    final Date date1 = parseDate(dateHeader1.getValue());
                    if (date1 != null) {
                        final Date date2 = parseDate(dateHeader2.getValue());
                        if (date2 != null)
                            return date1.after(date2);
                    }
                }
            }
        }
        return false;
    }

    @Deprecated
    public static boolean isBefore(final MessageHeaders message1, final MessageHeaders message2, final String headerName) {
        if (message1 != null && message2 != null) {
            final Header dateHeader1 = message1.getFirstHeader(headerName);
            if (dateHeader1 != null) {
                final Header dateHeader2 = message2.getFirstHeader(headerName);
                if (dateHeader2 != null) {
                    final Date date1 = parseDate(dateHeader1.getValue());
                    if (date1 != null) {
                        final Date date2 = parseDate(dateHeader2.getValue());
                        if (date2 != null)
                            return date1.before(date2);
                    }
                }
            }
        }
        return false;
    }

    @Deprecated
    public static Date parseDate(final String dateValue, final String[] dateFormats) {
        return parseDate(dateValue, dateFormats, null);
    }

    @Deprecated
    public static Date parseDate(final String dateValue, final String[] dateFormats, final Date startDate) {
        final DateTimeFormatter[] dateTimeFormatters;
        if (dateFormats != null) {
            dateTimeFormatters = new DateTimeFormatter[dateFormats.length];
            for (int i = 0; i < dateFormats.length; i++) {
                dateTimeFormatters[i] = new DateTimeFormatterBuilder()
                        .parseLenient()
                        .parseCaseInsensitive()
                        .appendPattern(dateFormats[i])
                        .toFormatter();
            }
        } else {
            dateTimeFormatters = STANDARD_PATTERNS;
        }
        return toDate(parseDate(dateValue, dateTimeFormatters));
    }

    @Deprecated
    public static String formatDate(final Date date) {
        return formatStandardDate(toInstant(date));
    }

    @Deprecated
    public static String formatDate(final Date date, final String pattern) {
        Args.notNull(date, "Date");
        Args.notNull(pattern, "Pattern");
        return DateTimeFormatter.ofPattern(pattern).format(toInstant(date).atZone(GMT_ID));
    }

    @Deprecated
    public static void clearThreadLocal() {}
}
