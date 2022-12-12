package com.kaleyra.socket_io.cookie;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import java.util.Objects;

public class WrappedCookie {

    private final Cookie cookie;

    WrappedCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    public Cookie unwrap() {
        return cookie;
    }

    public boolean isExpired() {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    public boolean matches(HttpUrl url) {
        return cookie.matches(url);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedCookie that = (WrappedCookie) o;
        return cookie.equals(that.cookie);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cookie);
    }

    public static WrappedCookie wrap(Cookie cookie) {
        return new WrappedCookie(cookie);
    }
}