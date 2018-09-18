package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.Set;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;

public interface MemberParser {

    MemberDTO parseMemberFromJson(final String jsonPayload);

    Set<MemberDTO> parseMembersFromJson(final String jsonPayload);

    Set<MemberDTO> parseMembersFromCsvInputStream(final ExtensionScheme extensionScheme,
                                                  final InputStream inputStream);

    Set<MemberDTO> parseMembersFromExcelInputStream(final ExtensionScheme extensionScheme,
                                                    final InputStream inputStream,
                                                    final String sheetName);

    Set<MemberDTO> parseMembersFromExcelWorkbook(final ExtensionScheme extensionScheme,
                                                 final Workbook workbook,
                                                 final String sheetName);
}
