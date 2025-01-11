package com.zoombit.repository;

import com.zoombit.domain.Markets;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MarketRepository extends JpaRepository<Markets, String> {

    @Query("SELECT m.market FROM Markets m")
    List<String> findAllMarketIds();
}
