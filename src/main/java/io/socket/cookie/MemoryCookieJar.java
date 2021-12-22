package io.socket.cookie;

import okhttp3.*;

import java.util.*;

public class MemoryCookieJar {

    private final Set<WrappedCookie> cache = new HashSet<>();

    synchronized public void saveFromResponse(Call call, Response response) {
        List<Cookie> list = Cookie.parseAll(call.request().url(), response.headers());
        List<WrappedCookie> cookiesToAdd = new ArrayList<>(list.size());
        for (Cookie cookie : list) {
            cookiesToAdd.add(WrappedCookie.wrap(cookie));
        }
        cache.removeAll(cookiesToAdd);
        cache.addAll(cookiesToAdd);
    }

    synchronized public Map<String, List<String>> loadForRequest(HttpUrl httpUrl) {
        Set<WrappedCookie> cookiesToRemove = new HashSet<>(cache.size());
        List<String> validCookies = new ArrayList<>(cache.size());

        for (WrappedCookie cookie : cache) {
            if (cookie.isExpired()) cookiesToRemove.add(cookie);
            else if (cookie.matches(httpUrl)) validCookies.add(cookie.unwrap().toString());
        }

        cache.removeAll(cookiesToRemove);
        Map<String, List<String>> header = new HashMap<>(1);
        if (validCookies.isEmpty()) return header;
        header.put("Cookie", validCookies);
        return header;
    }

    synchronized void clear() {
        cache.clear();
    }
}
