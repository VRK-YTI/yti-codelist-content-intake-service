package fi.vm.yti.codelist.intake.service.impl;

import java.util.Set;

import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.intake.dao.ExtensionDao;
import fi.vm.yti.codelist.intake.service.ExtensionService;

@Singleton
@Service
public class ExtensionServiceImpl extends BaseService implements ExtensionService {

    private final ExtensionDao extensionDao;

    public ExtensionServiceImpl(final ExtensionDao extensionDao) {
        this.extensionDao = extensionDao;
    }

    @Transactional
    public Set<ExtensionDTO> findAll() {
        return mapDeepExtensionDtos(extensionDao.findAll());
    }
}
