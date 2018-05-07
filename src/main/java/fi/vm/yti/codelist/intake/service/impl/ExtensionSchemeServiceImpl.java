package fi.vm.yti.codelist.intake.service.impl;

import java.util.Set;

import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.intake.dao.ExtensionSchemeDao;
import fi.vm.yti.codelist.intake.service.ExtensionSchemeService;

@Singleton
@Service
public class ExtensionSchemeServiceImpl extends BaseService implements ExtensionSchemeService {

    private final ExtensionSchemeDao extensionSchemeDao;

    public ExtensionSchemeServiceImpl(final ExtensionSchemeDao extensionSchemeDao) {
        this.extensionSchemeDao = extensionSchemeDao;
    }

    @Transactional
    public Set<ExtensionSchemeDTO> findAll() {
        return mapDeepExtensionSchemeDtos(extensionSchemeDao.findAll());
    }
}
