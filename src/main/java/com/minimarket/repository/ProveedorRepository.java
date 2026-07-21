package com.minimarket.repository;

import com.minimarket.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findFirstByActivoTrueOrderByIdAsc();
}
