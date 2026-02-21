package com.myshop.mapper;

import com.myshop.dto.response.UserResponse;
import com.myshop.model.entity.User;
import org.mapstruct.Mapper;

/**
 * UserMapper — compile-time generated implementation.
 *
 * MapStruct reads this interface and generates a real class UserMapperImpl.java
 * at compile time. You can view it at:
 * target/generated-sources/annotations/com/myshop/mapper/UserMapperImpl.java
 *
 * WHY IS THIS BETTER THAN BeanUtils.copyProperties() or ModelMapper?
 * - MapStruct: COMPILE-TIME generation. Errors caught before you run the app.
 * Zero reflection at runtime = extremely fast.
 * - ModelMapper: RUNTIME reflection. Mapping errors only caught when the
 * mapping code runs. Slower due to reflection.
 * - BeanUtils: No type safety. "description" on both objects? MapStruct
 * verifies
 * types match. BeanUtils silently fails if types differ.
 *
 * @Mapping: When field names differ between source and target, use @Mapping.
 *           Here fullName in User maps to fullName in UserResponse (same name →
 *           no annotation needed).
 *           passwordHash is NOT in UserResponse → MapStruct ignores it
 *           automatically.
 *
 *           componentModel = "spring": MapStruct generates @Component on the
 *           implementation.
 *           This makes UserMapper an injectable Spring bean (@Autowired
 *           / @RequiredArgsConstructor).
 */
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
