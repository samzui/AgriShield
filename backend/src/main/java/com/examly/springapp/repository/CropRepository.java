package com.examly.springapp.repository;
import com.examly.springapp.entity.Crop;
import com.examly.springapp.enums.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CropRepository extends JpaRepository<Crop,Long> {
    List<Crop> findBySeason(Season season);
    java.util.Optional<Crop> findByCropNameIgnoreCaseAndSeason(String cropName,Season season);
}
