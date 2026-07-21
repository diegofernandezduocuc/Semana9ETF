package com.minimarket.repository;

import com.minimarket.entity.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {
    List<Promocion> findByProductoIdAndActivaTrue(Long productoId);
    List<Promocion> findByActivaTrue();
}
