package com.myshop.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * PagedResponse<T> — generic wrapper for paginated API responses.
 *
 * WHY NOT RETURN Spring's Page<T> DIRECTLY?
 * Spring's Page object has many internals (sort details, number of elements,
 * etc.)
 * that pollute the API contract. Clients don't need that complexity.
 * PagedResponse is a clean, stable contract. If we switch from JPA to Mongo
 * or Elasticsearch, the response format stays the same — clients aren't
 * affected.
 *
 * USAGE: return PagedResponse.of(productPage, productResponseList)
 *
 * @param <T> the type of content in the page (e.g. ProductResponse)
 */
@Data
@Builder
public class PagedResponse<T> {

    private List<T> content;

    /** Current page number (0-indexed to match Spring's convention) */
    private int page;

    private int size;
    private long totalElements;
    private int totalPages;

    /** true if this is the last page — useful for infinite scroll UIs */
    private boolean last;

    /**
     * Factory method to build from a Spring Data Page + pre-mapped content list.
     * We accept pre-mapped content because Spring's Page<Entity> content has
     * already been mapped to Page<DTO> content before calling this method.
     */
    public static <T> PagedResponse<T> of(org.springframework.data.domain.Page<?> page, List<T> content) {
        return PagedResponse.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
