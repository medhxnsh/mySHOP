package com.myshop.mapper;

import com.myshop.dto.request.CreateProductRequest;
import com.myshop.dto.response.ProductResponse;
import com.myshop.model.entity.Product;
import org.mapstruct.*;

/**
 * ProductMapper — handles Product ↔ DTO conversion.
 *
 * NESTED OBJECT MAPPING with @Mapping:
 * Product has a `category` field of type Category.
 * ProductResponse has `categoryId`, `categoryName`, `categorySlug` as flat
 * fields.
 * MapStruct can traverse nested objects using dot notation:
 * source = "category.id" → target = "categoryId"
 *
 * WHY FLATTEN?
 * REST API best practice: response DTOs should be flat (easy to parse in
 * frontend).
 * Nested objects require deeper traversal in JavaScript.
 * Flattening also avoids accidentally exposing internal structures.
 *
 * toEntity IGNORES category and version:
 * - category: must be fetched from DB using categoryId (done in service)
 * - version: managed by JPA/Hibernate, never set manually
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "category.slug", target = "categorySlug")
    @Mapping(source = "active", target = "isActive") // Map boolean Entity.active to DTO.isActive
    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true) // set in service after category lookup
    @Mapping(target = "version", ignore = true) // managed by JPA
    @Mapping(target = "avgRating", ignore = true) // updated by review aggregation (Phase 3)
    @Mapping(target = "reviewCount", ignore = true) // updated by review aggregation (Phase 3)
    @Mapping(target = "createdAt", ignore = true) // set by @PrePersist
    @Mapping(target = "updatedAt", ignore = true) // set by @PrePersist
    // Builder target: Lombok @Builder generates `active(boolean)` for the new
    // boolean active field
    @Mapping(target = "active", ignore = true) // defaults to true — set explicitly in service
    Product toEntity(CreateProductRequest request);

    /**
     * @BeanMapping + NullValuePropertyMappingStrategy.IGNORE:
     *              When updating, only non-null fields from the update DTO should
     *              overwrite the entity.
     *              Fields not present in UpdateProductRequest stay as-is on the
     *              entity.
     *              This enables true PATCH semantics (only update what you send).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "avgRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    // UpdateProductRequest.isActive (Boolean) → Product.setActive(boolean)
    @Mapping(source = "isActive", target = "active")
    void updateEntity(@MappingTarget Product product,
            com.myshop.dto.request.UpdateProductRequest request);
}
