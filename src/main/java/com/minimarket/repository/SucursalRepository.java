package com.minimarket.repository;

import com.minimarket.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {
    Optional<Sucursal> findByNombre(String nombre);
}
