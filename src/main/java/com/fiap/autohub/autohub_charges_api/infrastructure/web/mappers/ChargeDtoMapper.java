package com.fiap.autohub.autohub_charges_api.infrastructure.web.mappers;

import com.fiap.autohub.autohub_charges_api.domain.entities.Charge;
import com.fiap.autohub.autohub_charges_api.infrastructure.web.dtos.ChargeResponseDto;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Mapper entre a entidade de domínio Charge e o DTO de resposta ChargeResponseDto.
 */
@Mapper(componentModel = "spring")
public interface ChargeDtoMapper {

    ChargeResponseDto toResponseDto(Charge charge);

    List<ChargeResponseDto> toResponseDtoList(List<Charge> charges);
}