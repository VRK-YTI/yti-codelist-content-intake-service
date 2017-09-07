package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.BusinessServiceSubRegion;
import fi.vm.yti.cls.common.model.Code;
import fi.vm.yti.cls.common.model.CodeRegistry;
import fi.vm.yti.cls.common.model.CodeScheme;
import fi.vm.yti.cls.common.model.ElectoralDistrict;
import fi.vm.yti.cls.common.model.HealthCareDistrict;
import fi.vm.yti.cls.common.model.Magistrate;
import fi.vm.yti.cls.common.model.MagistrateServiceUnit;
import fi.vm.yti.cls.common.model.Municipality;
import fi.vm.yti.cls.common.model.PostManagementDistrict;
import fi.vm.yti.cls.common.model.PostalCode;
import fi.vm.yti.cls.common.model.Region;
import fi.vm.yti.cls.common.model.StreetAddress;
import fi.vm.yti.cls.intake.jpa.BusinessServiceSubRegionRepository;
import fi.vm.yti.cls.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.cls.intake.jpa.CodeRepository;
import fi.vm.yti.cls.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.cls.intake.jpa.ElectoralDistrictRepository;
import fi.vm.yti.cls.intake.jpa.HealthCareDistrictRepository;
import fi.vm.yti.cls.intake.jpa.MagistrateRepository;
import fi.vm.yti.cls.intake.jpa.MagistrateServiceUnitRepository;
import fi.vm.yti.cls.intake.jpa.MunicipalityRepository;
import fi.vm.yti.cls.intake.jpa.PostManagementDistrictRepository;
import fi.vm.yti.cls.intake.jpa.PostalCodeRepository;
import fi.vm.yti.cls.intake.jpa.RegionRepository;
import fi.vm.yti.cls.intake.jpa.StreetAddressRepository;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for code parsers that contains helper methods to minimize repetition.
 */
@Component
public class ParserUtils {

    private final RegionRepository regionRepository;
    private final MagistrateRepository magistrateRepository;
    private final MunicipalityRepository municipalityRepository;
    private final PostalCodeRepository postalCodeRepository;
    private final PostManagementDistrictRepository postManagementDistrictRepository;
    private final MagistrateServiceUnitRepository magistrateServiceUnitRepository;
    private final HealthCareDistrictRepository healthCareDistrictRepository;
    private final ElectoralDistrictRepository electoralDistrictRepository;
    private final BusinessServiceSubRegionRepository businessServiceSubRegionRepository;
    private final StreetAddressRepository streetAddressRepository;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;

    @Inject
    public ParserUtils(final RegionRepository regionRepository,
                       final MagistrateRepository magistrateRepository,
                       final MunicipalityRepository municipalityRepository,
                       final PostalCodeRepository postalCodeRepository,
                       final PostManagementDistrictRepository postManagementDistrictRepository,
                       final MagistrateServiceUnitRepository magistrateServiceUnitRepository,
                       final HealthCareDistrictRepository healthCareDistrictRepository,
                       final ElectoralDistrictRepository electoralDistrictRepository,
                       final BusinessServiceSubRegionRepository businessServiceSubRegionRepository,
                       final StreetAddressRepository streetAddressRepository,
                       final CodeRegistryRepository codeRegistryRepository,
                       final CodeSchemeRepository codeSchemeRepository,
                       final CodeRepository codeRepository) {
        this.regionRepository = regionRepository;
        this.magistrateRepository = magistrateRepository;
        this.municipalityRepository = municipalityRepository;
        this.postalCodeRepository = postalCodeRepository;
        this.postManagementDistrictRepository = postManagementDistrictRepository;
        this.magistrateServiceUnitRepository = magistrateServiceUnitRepository;
        this.healthCareDistrictRepository = healthCareDistrictRepository;
        this.electoralDistrictRepository = electoralDistrictRepository;
        this.businessServiceSubRegionRepository = businessServiceSubRegionRepository;
        this.streetAddressRepository = streetAddressRepository;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
    }

    public Map<String, Region> getRegionsMap() {
        final List<Region> regions = regionRepository.findAll();
        final Map<String, Region> regionsMap = new HashMap<>();
        for (final Region region : regions) {
            regionsMap.put(region.getCodeValue(), region);
        }
        return regionsMap;
    }

    public Map<String, Magistrate> getMagistratesMap() {
        final List<Magistrate> magistrates = magistrateRepository.findAll();
        final Map<String, Magistrate> magistratesMap = new HashMap<>();
        for (final Magistrate magistrate : magistrates) {
            magistratesMap.put(magistrate.getCodeValue(), magistrate);
        }
        return magistratesMap;
    }

    public Map<String, Municipality> getMunicipalitiesMap() {
        final List<Municipality> municipalities = municipalityRepository.findAll();
        final Map<String, Municipality> municipalityMap = new HashMap<>();
        for (final Municipality municipality : municipalities) {
            municipalityMap.put(municipality.getCodeValue(), municipality);
        }
        return municipalityMap;
    }

    public Map<String, PostalCode> getPostalCodesMap() {
        final List<PostalCode> postalCodes = postalCodeRepository.findAll();
        final Map<String, PostalCode> postalCodeMap = new HashMap<>();
        for (final PostalCode postalCode : postalCodes) {
            postalCodeMap.put(postalCode.getCodeValue(), postalCode);
        }
        return postalCodeMap;
    }

    public Map<String, MagistrateServiceUnit> getMagistrateServiceUnitsMap() {
        final List<MagistrateServiceUnit> magistrateServiceUnits = magistrateServiceUnitRepository.findAll();
        final Map<String, MagistrateServiceUnit> magistrateServiceUnitMap = new HashMap<>();
        for (final MagistrateServiceUnit magistrateServiceUnit : magistrateServiceUnits) {
            magistrateServiceUnitMap.put(magistrateServiceUnit.getCodeValue(), magistrateServiceUnit);
        }
        return magistrateServiceUnitMap;
    }

    public Map<String, HealthCareDistrict> getHealthCareDistrictsMap() {
        final List<HealthCareDistrict> healthCareDistricts = healthCareDistrictRepository.findAll();
        final Map<String, HealthCareDistrict> healthCareDistrictsMap = new HashMap<>();
        for (final HealthCareDistrict healthCareDistrict : healthCareDistricts) {
            healthCareDistrictsMap.put(healthCareDistrict.getCodeValue(), healthCareDistrict);
        }
        return healthCareDistrictsMap;
    }

    public Map<String, ElectoralDistrict> getElectoralDistrictsMap() {
        final List<ElectoralDistrict> electoralDistricts = electoralDistrictRepository.findAll();
        final Map<String, ElectoralDistrict> electoralDistrictsMap = new HashMap<>();
        for (final ElectoralDistrict electoralDistrict : electoralDistricts) {
            electoralDistrictsMap.put(electoralDistrict.getCodeValue(), electoralDistrict);
        }
        return electoralDistrictsMap;
    }

    public Map<String, BusinessServiceSubRegion> getBusinessServiceSubRegionsMap() {
        final List<BusinessServiceSubRegion> businessServiceSubregions = businessServiceSubRegionRepository.findAll();
        final Map<String, BusinessServiceSubRegion> businessServiceSubRegionsMap = new HashMap<>();
        for (final BusinessServiceSubRegion businessServiceSubRegion : businessServiceSubregions) {
            businessServiceSubRegionsMap.put(businessServiceSubRegion.getCodeValue(), businessServiceSubRegion);
        }
        return businessServiceSubRegionsMap;
    }


    public Map<String, PostManagementDistrict> getPostManagementDistrictsMap() {
        final List<PostManagementDistrict> postManagementDistricts = postManagementDistrictRepository.findAll();
        final Map<String, PostManagementDistrict> postManagementDistrictsMap = new HashMap<>();
        for (final PostManagementDistrict postManagementDistrict : postManagementDistricts) {
            postManagementDistrictsMap.put(postManagementDistrict.getCodeValue(), postManagementDistrict);
        }
        return postManagementDistrictsMap;
    }

    public Map<String, StreetAddress> getStreetAddressesMap() {
        final Set<StreetAddress> streetAddresses = streetAddressRepository.findAll();
        final Map<String, StreetAddress> streetAddressesMap = new HashMap<>();
        for (final StreetAddress streetAddress : streetAddresses) {
            streetAddressesMap.put(streetAddress.getMunicipality().getCodeValue() + streetAddress.getPrefLabelFi(), streetAddress);
        }
        return streetAddressesMap;
    }

    public Map<String, CodeRegistry> getCodeRegistriesMap() {
        final List<CodeRegistry> codeRegistries = codeRegistryRepository.findAll();
        final Map<String, CodeRegistry> codeRegistriesMap = new HashMap<>();
        for (final CodeRegistry codeRegistry : codeRegistries) {
            codeRegistriesMap.put(codeRegistry.getCodeValue(), codeRegistry);
        }
        return codeRegistriesMap;
    }

    public Map<String, CodeScheme> getCodeSchemesMap() {
        final List<CodeScheme> codeSchemes = codeSchemeRepository.findAll();
        final Map<String, CodeScheme> codeSchemesMap = new HashMap<>();
        for (final CodeScheme codeScheme : codeSchemes) {
            codeSchemesMap.put(codeScheme.getCodeValue(), codeScheme);
        }
        return codeSchemesMap;
    }

    public Map<String, Code> getCodesMap(final CodeScheme codeScheme) {
        final List<Code> codes = codeRepository.findByCodeScheme(codeScheme);
        final Map<String, Code> codesMap = new HashMap<>();
        for (final Code code : codes) {
            codesMap.put(code.getCodeValue(), code);
        }
        return codesMap;
    }

}
