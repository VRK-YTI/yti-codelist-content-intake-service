package fi.vm.yti.codelist.intake.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.constants.ApiConstants;
import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;

@Singleton
@Service
public class DomainImpl implements Domain {

    private static final Logger LOG = LoggerFactory.getLogger(DomainImpl.class);
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeRepository codeRepository;

    @Inject
    private DomainImpl(final CodeRegistryRepository codeRegistryRepository,
                       final CodeSchemeRepository codeSchemeRepository,
                       final CodeRepository codeRepository) {
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
    }

    public void persistCodeRegistries(final List<CodeRegistry> codeRegistries) {
        codeRegistryRepository.save(codeRegistries);
    }

    public void persistCodeSchemes(final List<CodeScheme> codeSchemes) {
        codeSchemeRepository.save(codeSchemes);
    }

    public void persistCodes(final List<Code> codes) {
        codeRepository.save(codes);
    }
}
