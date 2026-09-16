package com.agrisense.farm;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FarmRepository extends JpaRepository<Farm, Long> {
    List<Farm> findByUserIdOrderByCreatedAtDesc(Long userId);
}
