package com.myshop.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** CategoryResponse — read-only view of a Category for API responses. */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {
    private UUID id;
    private String name;
    private String slug;
    /** null if this is a root category */
    private UUID parentId;
    private String parentName;
}
