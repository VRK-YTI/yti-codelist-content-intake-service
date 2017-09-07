package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.BusinessServiceSubRegion;
import fi.vm.yti.cls.common.model.Municipality;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.api.ApiUtils;
import fi.vm.yti.cls.intake.util.Utils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


/**
 * Class that handles parsing of businesservicesubregions from source data.
 */
@Service
public class BusinessServiceSubRegionParser {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessServiceSubRegionParser.class);

    private final ApiUtils m_apiUtils;

    private final ParserUtils m_parserUtils;


    @Inject
    public BusinessServiceSubRegionParser(final ApiUtils apiUtils,
                                          final ParserUtils parserUtils) {

        m_apiUtils = apiUtils;

        m_parserUtils = parserUtils;

    }


    /**
     * Parses the .xls Excel-file and returns the BusinessServiceSubRegions as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The BusinessServiceSubRegion -file.
     * @return List of BusinessServiceSubRegion objects.
     */
    public List<BusinessServiceSubRegion> parseBusinessServiceSubRegionsFromInputStream(final String source,
                                                                                        final InputStream inputStream) {

        final Map<String, BusinessServiceSubRegion> map = new HashMap<>();

        final Workbook workbook;

        try {
            workbook = new XSSFWorkbook(inputStream);
            final Sheet memberMunicipalities = workbook.getSheet("Sheet1");

            final Map<String, BusinessServiceSubRegion> existingBusinessServiceSubRegionsMap = m_parserUtils.getBusinessServiceSubRegionsMap();

            final Map<String, Municipality> existingMunicipalitiesMap = m_parserUtils.getMunicipalitiesMap();

            for (int i = 2; i < 290; i++) {

                final Row row = memberMunicipalities.getRow(i);

                final Cell codeCell = row.getCell(0);
                codeCell.setCellType(CellType.STRING);
                final String code = Utils.ensureBusinessServiceSubRegionIdPadding(codeCell.getStringCellValue());

                if (code != null && !code.isEmpty()) {

                    BusinessServiceSubRegion businessServiceSubRegion = null;

                    if (!map.containsKey(code)) {

                        final String finnishName = row.getCell(2).getStringCellValue().trim();
                        final String swedishName = row.getCell(4).getStringCellValue().trim();

                        businessServiceSubRegion = createOrUpdateBusinessServiceSubRegion(existingBusinessServiceSubRegionsMap, code, Status.VALID, source, finnishName, swedishName, null);

                        map.put(code, businessServiceSubRegion);
                    } else {
                        businessServiceSubRegion = map.get(code);
                    }

                    final Cell municipalityCodeCell = row.getCell(1);
                    municipalityCodeCell.setCellType(CellType.STRING);
                    final String municipalityCode = Utils.ensureMunicipalityIdPadding(municipalityCodeCell.getStringCellValue());
                    final Municipality municipality = existingMunicipalitiesMap.get(municipalityCode);

                    if (municipality != null) {

                        List<Municipality> municipalities = businessServiceSubRegion.getMunicipalities();

                        if (municipalities != null) {
                            boolean skip = false;
                            for (final Municipality munic : municipalities) {
                                if (municipality.getCodeValue().equalsIgnoreCase(munic.getCodeValue())) {
                                    skip = true;
                                }
                            }
                            if (!skip) {
                                municipalities.add(municipality);
                            }
                        } else {
                            municipalities = new ArrayList<>();
                            municipalities.add(municipality);
                            businessServiceSubRegion.setMunicipalities(municipalities);
                        }
                    } else {
                        LOG.error("BusinessServiceSubRegionParser municipality not found for code: " + municipalityCode);
                    }

                }

            }

        } catch (IOException e) {
            LOG.error("Parsing business service subregions failed. " + e.getMessage());
        }

        final List<BusinessServiceSubRegion> businessServiceSubRegions = new ArrayList<BusinessServiceSubRegion>(map.values());
        return businessServiceSubRegions;

    }


    private BusinessServiceSubRegion createOrUpdateBusinessServiceSubRegion(final Map<String, BusinessServiceSubRegion> businessServiceSubRegionsMap,
                                                                            final String code,
                                                                            final Status status,
                                                                            final String source,
                                                                            final String finnishName,
                                                                            final String swedishName,
                                                                            final String englishName) {

        final String url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_BUSINESSSERVICESUBREGIONS, code);
        final Date timeStamp = new Date(System.currentTimeMillis());

        BusinessServiceSubRegion businessServiceSubRegion = businessServiceSubRegionsMap.get(code);

        // Update
        if (businessServiceSubRegion != null) {
            boolean hasChanges = false;
            if (!Objects.equals(businessServiceSubRegion.getStatus(), status.toString())) {
                businessServiceSubRegion.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(businessServiceSubRegion.getUri(), url)) {
                businessServiceSubRegion.setUri(url);
                hasChanges = true;
            }
            if (!Objects.equals(businessServiceSubRegion.getSource(), source)) {
                businessServiceSubRegion.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(businessServiceSubRegion.getPrefLabelFi(), finnishName)) {
                businessServiceSubRegion.setPrefLabelFi(finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(businessServiceSubRegion.getPrefLabelSe(), swedishName)) {
                businessServiceSubRegion.setPrefLabelSe(swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(businessServiceSubRegion.getPrefLabelEn(), englishName)) {
                businessServiceSubRegion.setPrefLabelEn(englishName);
                hasChanges = true;
            }
            if (hasChanges) {
                businessServiceSubRegion.setModified(timeStamp);
            }

        // Create
        } else {
            businessServiceSubRegion = new BusinessServiceSubRegion();
            businessServiceSubRegion.setId(UUID.randomUUID().toString());
            businessServiceSubRegion.setUri(url);
            businessServiceSubRegion.setSource(source);
            businessServiceSubRegion.setStatus(status.toString());
            businessServiceSubRegion.setCreated(timeStamp);
            businessServiceSubRegion.setCodeValue(code);
            businessServiceSubRegion.setPrefLabelFi(finnishName);
            businessServiceSubRegion.setPrefLabelSe(swedishName);
            businessServiceSubRegion.setPrefLabelEn(englishName);
        }

        return businessServiceSubRegion;

    }

}
