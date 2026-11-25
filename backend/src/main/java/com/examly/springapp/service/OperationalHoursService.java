package com.examly.springapp.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.*;
@Service public class OperationalHoursService {
    private final boolean enabled;
    private final LocalTime start;
    private final LocalTime end;
    public OperationalHoursService(@Value("${app.operational-hours.enabled:false}")boolean enabled,@Value("${app.operational-hours.start:08:00}")String start,@Value("${app.operational-hours.end:20:00}")String end) {
        this.enabled=enabled;
        this.start=LocalTime.parse(start);
        this.end=LocalTime.parse(end);
    }
    public void assertOpen() {
        if(!enabled)return;
        LocalTime now=LocalTime.now();
        if(now.isBefore(start)||now.isAfter(end))throw new IllegalStateException("This operational function is available only during configured operational hours");
    }
}
