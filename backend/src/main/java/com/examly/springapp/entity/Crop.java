package com.examly.springapp.entity;
import com.examly.springapp.enums.Season;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="crops",uniqueConstraints=@UniqueConstraint(columnNames= {
    "crop_name","season"
}))
public class Crop extends BaseEntity {
    @Column(name="crop_name",nullable=false,length=120) private String cropName;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Season season;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal standardYieldKgHa;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal mspRate;
    private boolean active=true;
}
