package fi.vm.yti.cls.intake.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import fi.vm.yti.cls.common.model.BusinessServiceSubRegion;
import fi.vm.yti.cls.common.model.ElectoralDistrict;
import fi.vm.yti.cls.common.model.HealthCareDistrict;
import fi.vm.yti.cls.common.model.Magistrate;
import fi.vm.yti.cls.common.model.MagistrateServiceUnit;
import fi.vm.yti.cls.common.model.Municipality;
import fi.vm.yti.cls.common.model.PostManagementDistrict;
import fi.vm.yti.cls.common.model.PostalCode;
import fi.vm.yti.cls.common.model.Region;
import fi.vm.yti.cls.common.model.Register;
import fi.vm.yti.cls.common.model.RegisterItem;
import fi.vm.yti.cls.common.model.StreetAddress;
import fi.vm.yti.cls.intake.jpa.BusinessServiceSubRegionRepository;
import fi.vm.yti.cls.intake.jpa.ElectoralDistrictRepository;
import fi.vm.yti.cls.intake.jpa.HealthCareDistrictRepository;
import fi.vm.yti.cls.intake.jpa.MagistrateRepository;
import fi.vm.yti.cls.intake.jpa.MagistrateServiceUnitRepository;
import fi.vm.yti.cls.intake.jpa.MunicipalityRepository;
import fi.vm.yti.cls.intake.jpa.PostManagementDistrictRepository;
import fi.vm.yti.cls.intake.jpa.PostalCodeRepository;
import fi.vm.yti.cls.intake.jpa.RegionRepository;
import fi.vm.yti.cls.intake.jpa.RegisterItemRepository;
import fi.vm.yti.cls.intake.jpa.RegisterRepository;
import fi.vm.yti.cls.intake.jpa.StreetAddressRepository;


/**
 * Utility class for code parsers that contains helper methods to minimize repetition.
 */
@Component
public class ParserUtils {

    private final RegionRepository m_regionRepository;

    private final MagistrateRepository m_magistrateRepository;

    private final MunicipalityRepository m_municipalityRepository;

    private final PostalCodeRepository m_postalCodeRepository;

    private final PostManagementDistrictRepository m_postManagementDistrictRepository;

    private final MagistrateServiceUnitRepository m_magistrateServiceUnitRepository;

    private final HealthCareDistrictRepository m_healthCareDistrictRepository;

    private final ElectoralDistrictRepository m_electoralDistrictRepository;

    private final BusinessServiceSubRegionRepository m_businessServiceSubRegionRepository;

    private final StreetAddressRepository m_streetAddressRepository;

    private final RegisterRepository m_registerRepository;

    private final RegisterItemRepository m_registerItemRepository;


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
                       final RegisterRepository registerRepository,
                       final RegisterItemRepository registerItemRepository) {

        m_regionRepository = regionRepository;
        m_magistrateRepository = magistrateRepository;
        m_municipalityRepository = municipalityRepository;
        m_postalCodeRepository = postalCodeRepository;
        m_postManagementDistrictRepository = postManagementDistrictRepository;
        m_magistrateServiceUnitRepository = magistrateServiceUnitRepository;
        m_healthCareDistrictRepository = healthCareDistrictRepository;
        m_electoralDistrictRepository = electoralDistrictRepository;
        m_businessServiceSubRegionRepository = businessServiceSubRegionRepository;
        m_streetAddressRepository = streetAddressRepository;
        m_registerRepository = registerRepository;
        m_registerItemRepository = registerItemRepository;

    }


    public Map<String, Region> getRegionsMap() {

        final List<Region> regions = m_regionRepository.findAll();

        final Map<String, Region> regionsMap = new HashMap<>();

        for (final Region region : regions) {
            regionsMap.put(region.getCode(), region);
        }

        return regionsMap;
    }


    public Map<String, Magistrate> getMagistratesMap() {

        final List<Magistrate> magistrates = m_magistrateRepository.findAll();

        final Map<String, Magistrate> magistratesMap = new HashMap<>();

        for (final Magistrate magistrate : magistrates) {
            magistratesMap.put(magistrate.getCode(), magistrate);
        }

        return magistratesMap;

    }


    public Map<String, Municipality> getMunicipalitiesMap() {

        final List<Municipality> municipalities = m_municipalityRepository.findAll();

        final Map<String, Municipality> municipalityMap = new HashMap<>();

        for (final Municipality municipality : municipalities) {
            municipalityMap.put(municipality.getCode(), municipality);
        }

        return municipalityMap;

    }


    public Map<String, PostalCode> getPostalCodesMap() {

        final List<PostalCode> postalCodes = m_postalCodeRepository.findAll();

        final Map<String, PostalCode> postalCodeMap = new HashMap<>();

        for (final PostalCode postalCode : postalCodes) {
            postalCodeMap.put(postalCode.getCode(), postalCode);
        }

        return postalCodeMap;

    }


    public Map<String, MagistrateServiceUnit> getMagistrateServiceUnitsMap() {

        final List<MagistrateServiceUnit> magistrateServiceUnits = m_magistrateServiceUnitRepository.findAll();

        final Map<String, MagistrateServiceUnit> magistrateServiceUnitMap = new HashMap<>();

        for (final MagistrateServiceUnit magistrateServiceUnit : magistrateServiceUnits) {
            magistrateServiceUnitMap.put(magistrateServiceUnit.getCode(), magistrateServiceUnit);
        }

        return magistrateServiceUnitMap;

    }


    public Map<String, HealthCareDistrict> getHealthCareDistrictsMap() {

        final List<HealthCareDistrict> healthCareDistricts = m_healthCareDistrictRepository.findAll();

        final Map<String, HealthCareDistrict> healthCareDistrictsMap = new HashMap<>();

        for (final HealthCareDistrict healthCareDistrict : healthCareDistricts) {
            healthCareDistrictsMap.put(healthCareDistrict.getCode(), healthCareDistrict);
        }

        return healthCareDistrictsMap;

    }


    public Map<String, ElectoralDistrict> getElectoralDistrictsMap() {

        final List<ElectoralDistrict> electoralDistricts = m_electoralDistrictRepository.findAll();

        final Map<String, ElectoralDistrict> electoralDistrictsMap = new HashMap<>();

        for (final ElectoralDistrict electoralDistrict : electoralDistricts) {
            electoralDistrictsMap.put(electoralDistrict.getCode(), electoralDistrict);
        }

        return electoralDistrictsMap;

    }


    public Map<String, BusinessServiceSubRegion> getBusinessServiceSubRegionsMap() {

        final List<BusinessServiceSubRegion> businessServiceSubregions = m_businessServiceSubRegionRepository.findAll();

        final Map<String, BusinessServiceSubRegion> businessServiceSubRegionsMap = new HashMap<>();

        for (final BusinessServiceSubRegion businessServiceSubRegion : businessServiceSubregions) {
            businessServiceSubRegionsMap.put(businessServiceSubRegion.getCode(), businessServiceSubRegion);
        }

        return businessServiceSubRegionsMap;

    }


    public Map<String, PostManagementDistrict> getPostManagementDistrictsMap() {

        final List<PostManagementDistrict> postManagementDistricts = m_postManagementDistrictRepository.findAll();

        final Map<String, PostManagementDistrict> postManagementDistrictsMap = new HashMap<>();

        for (final PostManagementDistrict postManagementDistrict : postManagementDistricts) {
            postManagementDistrictsMap.put(postManagementDistrict.getCode(), postManagementDistrict);
        }

        return postManagementDistrictsMap;

    }


    public Map<String, StreetAddress> getStreetAddressesMap() {

        final Set<StreetAddress> streetAddresses = m_streetAddressRepository.findAll();

        final Map<String, StreetAddress> streetAddressesMap = new HashMap<>();

        for (final StreetAddress streetAddress : streetAddresses) {
            streetAddressesMap.put(streetAddress.getMunicipality().getCode() + streetAddress.getNameFinnish(), streetAddress);
        }

        return streetAddressesMap;

    }


    public Map<String, Register> getRegistersMap() {

        final List<Register> registers = m_registerRepository.findAll();

        final Map<String, Register> registersMap = new HashMap<>();

        for (final Register register : registers) {
            registersMap.put(register.getCode(), register);
        }

        return registersMap;

    }


    public Map<String, RegisterItem> getRegisterItemsMap(final String register) {

        final List<RegisterItem> registerItems = m_registerItemRepository.findByRegister(register);

        final Map<String, RegisterItem> registerItemsMap = new HashMap<>();

        for (final RegisterItem registerItem : registerItems) {
            registerItemsMap.put(registerItem.getCode(), registerItem);
        }

        return registerItemsMap;

    }

}
