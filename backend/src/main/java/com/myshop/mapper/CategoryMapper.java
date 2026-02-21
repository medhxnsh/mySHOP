package com.myshop.mapper;

import com.myshop.dto.request.CreateCategoryRequest;
import com.myshop.dto.response.CategoryResponse;
import com.myshop.model.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * CategoryMapper — Category entity ↔ DTO mapping.
 *
 * The Category entity has a `parent` field of type Category (self-referencing).
 * We flatten it: parent.id → parentId, parent.name → parentName.
 * If parent is null (root category), MapStruct sets parentId=null,
 * parentName=null.
 * 
 * @JsonInclude(NON_NULL) on CategoryResponse omits these null fields from JSON.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "parent.name", target = "parentName")
    CategoryResponse toResponse(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true) // set in service after parent lookup
    @Mapping(target = "children", ignore = true) // managed by JPA
    @Mapping(target = "createdAt", ignore = true)
    Category toEntity(CreateCategoryRequest request);
}
