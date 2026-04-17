package com.github.liyibo1110.hc.client5.http.psl;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析来自publicsuffix.org的列表，并配置一个PublicSuffixFilter。
 * @author liyibo
 * @date 2026-04-16 14:14
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class PublicSuffixListParser {
    public static final PublicSuffixListParser INSTANCE = new PublicSuffixListParser();

    public PublicSuffixListParser() {}

    /**
     * 解析成公共后缀列表的格式（没有DomainType）。
     * 从文件创建Reader时，请确保使用正确的编码（原始列表采用UTF-8编码）。
     */
    public PublicSuffixList parse(final Reader reader) throws IOException {
        final List<String> rules = new ArrayList<>();
        final List<String> exceptions = new ArrayList<>();
        final BufferedReader r = new BufferedReader(reader);

        String line;
        while ((line = r.readLine()) != null) {
            if (line.isEmpty())
                continue;
            if (line.startsWith("//"))
                continue; //entire lines can also be commented using //
            if (line.startsWith("."))
                line = line.substring(1); // A leading dot is optional
            // An exclamation mark (!) at the start of a rule marks an exception to a previous wildcard rule
            final boolean isException = line.startsWith("!");
            if (isException)
                line = line.substring(1);

            if (isException)
                exceptions.add(line);
            else
                rules.add(line);
        }
        return new PublicSuffixList(DomainType.UNKNOWN, rules, exceptions);
    }

    /**
     * 根据域名类型解析公共后缀列表格式（目前支持ICANN和PRIVATE）。
     * 从文件创建Reader时，请确保使用正确的编码（原始列表采用UTF-8编码）。
     */
    public List<PublicSuffixList> parseByType(final Reader reader) throws IOException {
        final List<PublicSuffixList> result = new ArrayList<>(2);

        final BufferedReader r = new BufferedReader(reader);

        DomainType domainType = null;
        List<String> rules = null;
        List<String> exceptions = null;
        String line;
        while ((line = r.readLine()) != null) {
            if (line.isEmpty())
                continue;

            if (line.startsWith("//")) {
                if (domainType == null) {
                    if (line.contains("===BEGIN ICANN DOMAINS==="))
                        domainType = DomainType.ICANN;
                    else if (line.contains("===BEGIN PRIVATE DOMAINS==="))
                        domainType = DomainType.PRIVATE;
                } else {
                    if (line.contains("===END ICANN DOMAINS===") || line.contains("===END PRIVATE DOMAINS===")) {
                        if (rules != null)
                            result.add(new PublicSuffixList(domainType, rules, exceptions));
                        domainType = null;
                        rules = null;
                        exceptions = null;
                    }
                }

                continue; //entire lines can also be commented using //
            }
            if (domainType == null)
                continue;

            if (line.startsWith("."))
                line = line.substring(1); // A leading dot is optional

            // An exclamation mark (!) at the start of a rule marks an exception to a previous wildcard rule
            final boolean isException = line.startsWith("!");
            if (isException)
                line = line.substring(1);

            if (isException) {
                if (exceptions == null)
                    exceptions = new ArrayList<>();
                exceptions.add(line);
            } else {
                if (rules == null)
                    rules = new ArrayList<>();
                rules.add(line);
            }
        }
        return result;
    }
}
