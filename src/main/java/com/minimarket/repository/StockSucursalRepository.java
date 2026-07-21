package com.minimarket.repository;

import com.minimarket.entity.StockSucursal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockSucursalRepository extends JpaRepository<StockSucursal, Long> {
    List<StockSucursal> findBySucursalId(Long sucursalId);
    Optional<StockSucursal> findBySucursalIdAndProductoId(Long sucursalId, Long productoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockSucursal s where s.sucursal.id = :sucursalId and s.producto.id = :productoId")
    Optional<StockSucursal> findForUpdate(@Param("sucursalId") Long sucursalId, @Param("productoId") Long productoId);
}
