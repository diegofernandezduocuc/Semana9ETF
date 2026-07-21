package com.minimarket.repository;

import com.minimarket.entity.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
    boolean existsBySucursalIdAndProductoIdAndEstado(Long sucursalId, Long productoId, String estado);
    List<OrdenCompra> findByEstado(String estado);
}
