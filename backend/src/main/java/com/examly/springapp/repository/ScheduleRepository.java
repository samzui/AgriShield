package com.examly.springapp.repository;
import com.examly.springapp.entity.Schedule;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ScheduleRepository extends JpaRepository<Schedule,Long> {
}
