package com.examly.springapp.service;
import com.examly.springapp.dto.request.MobileRequests.SyncRequest;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
@Service public class MobileSyncService {
    public Map<String,Object> sync(SyncRequest r) {
        List<Map<String,Object>>accepted=new ArrayList<>(),conflicts=new ArrayList<>();
        for(var c:r.changes()) {
            if(!List.of("CREATE","UPDATE","DELETE").contains(c.operation().toUpperCase()))conflicts.add(Map.of("clientId",c.clientId(),"reason","Unsupported operation"));
            else accepted.add(Map.of("clientId",c.clientId(),"status","ACCEPTED"));
        }return Map.of("deviceId",r.deviceId(),"serverTime",Instant.now(),"lastClientSync",r.lastSyncAt(),"accepted",accepted,"conflicts",conflicts,"offlineModeSupported",true);
    }
}
