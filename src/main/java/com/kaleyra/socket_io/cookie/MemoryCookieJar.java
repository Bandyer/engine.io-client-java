package com.kaleyra.socket_io.cookie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Response;

public class MemoryCookieJar {

    private final HashMap<String, WrappedCookie> cache = new HashMap<>();

    synchronized public void saveFromResponse(Call call, Response response) {
        List<Cookie> list = Cookie.parseAll(call.request().url(), response.headers());
        for (Cookie cookie : list) cache.put(cookie.name(), WrappedCookie.wrap(cookie));
    }

    synchronized public Map<String, List<String>> loadForRequest(HttpUrl httpUrl) {
        List<String> validCookies = new ArrayList<>(cache.size());

        for (Map.Entry<String, WrappedCookie> cookie : cache.entrySet()) {
            if (cookie.getValue().isExpired()) cache.remove(cookie.getKey());
            else if (cookie.getValue().matches(httpUrl)) validCookies.add(cookie.getValue().unwrap().toString());
        }

        Map<String, List<String>> header = new HashMap<>(1);
        if (validCookies.isEmpty()) return header;
        header.put("Cookie", validCookies);
        return header;
    }

    synchronized void clear() {
        cache.clear();
    }
}
