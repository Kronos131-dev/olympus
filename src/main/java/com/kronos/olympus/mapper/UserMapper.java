package com.kronos.olympus.mapper;

import com.kronos.olympus.dto.request.RegisterRequest;
import com.kronos.olympus.dto.response.UserResponse;
import com.kronos.olympus.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    // Mapper pour convertir l'entité en réponse (sans le mot de passe)
    UserResponse toResponse(User user);

    // Mapper pour convertir la requête d'inscription en entité
    // Le password et le role seront gérés par le service d'authentification
    @Mapping(target = "currentWeightKg", source = "weightKg")
    User toEntity(RegisterRequest request);
}
