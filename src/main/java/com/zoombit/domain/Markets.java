package com.zoombit.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "markets")
@Getter
@Setter
public class Markets {

    @Id
    private String market;

    private String koreanName;

    private String englishName;
}
