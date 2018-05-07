package fi.vm.yti.codelist.intake.service;

import java.util.Set;

import fi.vm.yti.codelist.common.dto.ExtensionDTO;

public interface ExtensionService {

    Set<ExtensionDTO> findAll();
}
