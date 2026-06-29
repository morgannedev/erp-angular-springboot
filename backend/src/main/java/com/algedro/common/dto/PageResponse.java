// src/main/java/com/algedro/common/dto/PageResponse.java
package com.algedro.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;
import java.util.List;

public record PageResponse<T>(
        @JsonProperty("contenido")      List<T>  contenido,
        @JsonProperty("pagina")         int      pagina,
        @JsonProperty("tamano")         int      tamano,
        @JsonProperty("totalPaginas")   int      totalPaginas,
        @JsonProperty("totalElementos") long     totalElementos
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }
}
