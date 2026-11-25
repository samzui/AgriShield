package com.examly.springapp.security;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Service public class SessionActivityService {
    private static final Duration TIMEOUT=Duration.ofMinutes(30);
    private final Map<String,Instant> activity=new ConcurrentHashMap<>();
    public boolean active(String user) {
        Instant last=activity.get(user);
        return last==null||last.plus(TIMEOUT).isAfter(Instant.now());
    }
    public void touch(String user) {
        activity.put(user,Instant.now());
    }
    public void invalidate(String user) {
        activity.remove(user);
    }
}
