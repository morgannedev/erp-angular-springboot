package com.algedro.exception;

/**
 * HTTP 422 — La categoría intenta referenciarse a sí misma como padre.
 * Refleja el constraint de BD: chk_categorias_no_self_ref (padre_id <> id),
 * pero se valida en el Service antes de llegar a la BD.
 */
public class AutoReferenciaException extends RuntimeException {
    public AutoReferenciaException(Long id) {
        super("La categoría con id=" + id + " no puede ser su propio padre.");
    }
}