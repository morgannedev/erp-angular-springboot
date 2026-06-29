package com.algedro.categoria.repository;

import com.algedro.categoria.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Categorías raíz activas con sus subcategorías — para GET /categorias (árbol) */
    @Query("""
            SELECT c FROM Categoria c
            WHERE c.padre IS NULL
              AND (:soloActivas = false OR c.activo = true)
            ORDER BY c.nombre
            """)
    List<Categoria> findRaices(@Param("soloActivas") boolean soloActivas);

    /** Subcategorías de una categoría raíz dada */
    List<Categoria> findByPadreId(Long padreId);
}