package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.NameValuePair;
import com.github.liyibo1110.hc.core5.util.Args;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 该类表示一种身份验证Challenge，由一个身份验证方案以及一个参数或一组名称/值对组成
 * @author liyibo
 * @date 2026-04-15 13:30
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public final class AuthChallenge {
    private final ChallengeType challengeType;
    private final String schemeName;
    private final String value;
    private final List<NameValuePair> params;

    public AuthChallenge(final ChallengeType challengeType, final String schemeName, final String value, final List<? extends NameValuePair> params) {
        super();
        this.challengeType = Args.notNull(challengeType, "Challenge type");
        this.schemeName = Args.notNull(schemeName, "schemeName");
        this.value = value;
        this.params = params != null ? Collections.unmodifiableList(new ArrayList<>(params)) : null;
    }

    public AuthChallenge(final ChallengeType challengeType, final String schemeName, final NameValuePair... params) {
        this(challengeType, schemeName, null, Arrays.asList(params));
    }

    public ChallengeType getChallengeType() {
        return challengeType;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public String getValue() {
        return value;
    }

    public List<NameValuePair> getParams() {
        return params;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(schemeName).append(" ");
        if (value != null)
            buffer.append(value);
        else if (params != null)
            buffer.append(params);
        return buffer.toString();
    }
}
