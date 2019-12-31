package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.intake.model.Extension;

public interface MemberParser {

    MemberDTO parseMemberFromJson(final String jsonPayload);

    Set<MemberDTO> parseMembersFromJson(final String jsonPayload);

    Set<MemberDTO> parseMembersFromCsvInputStream(final Extension extension,
                                                  final InputStream inputStream);

    Set<MemberDTO> parseMembersFromExcelInputStream(final Extension extension,
                                                    final InputStream inputStream,
                                                    final String sheetName,
                                                    final Map<String, LinkedHashSet<MemberDTO>> memberDTOsToBeDeletedPerExtension,
                                                    final String codeSchemeStatus);

    Set<MemberDTO> parseMembersFromExcelWorkbook(final Extension extension,
                                                 final Workbook workbook,
                                                 final String sheetName,
                                                 final Map<String, LinkedHashSet<MemberDTO>> memberDTOsToBeDeletedPerExtension,
                                                 final String codeSchemeStatus);
}
