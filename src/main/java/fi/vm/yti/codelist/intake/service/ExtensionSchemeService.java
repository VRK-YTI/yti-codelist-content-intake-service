package fi.vm.yti.codelist.intake.service;

import java.util.Set;

import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;

public interface ExtensionSchemeService {

    Set<ExtensionSchemeDTO> findAll();
}
