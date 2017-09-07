package fi.vm.yti.cls.intake.domain;

import fi.vm.yti.cls.common.model.BusinessId;
import fi.vm.yti.cls.common.model.BusinessServiceSubRegion;
import fi.vm.yti.cls.common.model.CodeRegistry;
import fi.vm.yti.cls.common.model.ElectoralDistrict;
import fi.vm.yti.cls.common.model.HealthCareDistrict;
import fi.vm.yti.cls.common.model.Magistrate;
import fi.vm.yti.cls.common.model.MagistrateServiceUnit;
import fi.vm.yti.cls.common.model.Municipality;
import fi.vm.yti.cls.common.model.PostManagementDistrict;
import fi.vm.yti.cls.common.model.PostalCode;
import fi.vm.yti.cls.common.model.Region;
import fi.vm.yti.cls.common.model.CodeScheme;
import fi.vm.yti.cls.common.model.Code;
import fi.vm.yti.cls.common.model.StreetAddress;
import fi.vm.yti.cls.common.model.StreetNumber;

import java.util.List;

public interface Domain {

    /**
     * Methods for persisting data to PostgreSQL.
     */

    void persistCodeRegistries(final List<CodeRegistry> codeRegistries);

    void persistCodeSchemes(final List<CodeScheme> codeSchemes);

    void persistCodes(final List<Code> codes);

    void persistMunicipalities(final List<Municipality> municipalities);

    void persistMagistrates(final List<Magistrate> magistrates);

    void persistStreetAddresses(final List<StreetAddress> streetAddresses);

    void persistStreetNumbers(final List<StreetNumber> streetNumbers);

    void persistRegions(final List<Region> regions);

    void persistPostalCodes(final List<PostalCode> postalCodes);

    void persistHealthCareDistricts(final List<HealthCareDistrict> healthCareDistricts);

    void persistElectoralDistricts(final List<ElectoralDistrict> electoralDistricts);

    void persistMagistrateServiceUnits(final List<MagistrateServiceUnit> magistrateServiceUnits);

    void persistPostManagementDistricts(final List<PostManagementDistrict> postManagementDistricts);

    void persistBusinessServiceSubRegions(final List<BusinessServiceSubRegion> businessServiceSubRegions);

    void persistBusinessIds(final List<BusinessId> businessIds);


    /**
     * Methods for indexing data to ElasticSearch.
     */

    void createIndex(final String indexName);

    void deleteIndex(final String indexName);

    void deleteTypeFromIndex(final String indexName, final String type);

    void ensureNestedPrefLabelsMapping(final String indexName, final String indexType);

    void refreshIndex(final String indexName);

    void indexMunicipalities();

    void indexMagistrates();

    void indexStreetAddresses();

    void indexRegions();

    void indexPostalCodes();

    void indexHealthCareDistricts();

    void indexElectoralDistricts();

    void indexMagistrateServiceUnits();

    void indexPostManagementDistricts();

    void indexBusinessServiceSubRegions();

    void indexBusinessIds();

    void indexCodeSchemes();

    void reIndexCodeRegistries();

    void reIndexCodeSchemes();

    void reIndexCodes(final String codeRegistryCodeValue, final String codeSchemeCodeValue);

    void reIndexEverything();

    void reIndexYti();

}
