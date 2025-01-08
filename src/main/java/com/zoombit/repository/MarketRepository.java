package com.zoombit.repository;

import com.zoombit.domain.Markets;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRepository extends JpaRepository<Markets, String> {

}
