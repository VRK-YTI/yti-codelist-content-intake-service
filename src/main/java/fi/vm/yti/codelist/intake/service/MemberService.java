package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;

public interface MemberService {

    MemberDTO deleteMember(final UUID id);

    Set<MemberDTO> findAll();

    MemberDTO findById(final UUID id);

    Set<MemberDTO> findByExtensionSchemeId(final UUID id);

    Set<MemberDTO> parseAndPersistMemberFromJson(final String jsonPayload);

    Set<MemberDTO> parseAndPersistMembersFromSourceData(final String codeRegistryCodeValue,
                                                        final String codeSchemeCodeValue,
                                                        final String extensionSchemeCodeValue,
                                                        final String format,
                                                        final InputStream inputStream,
                                                        final String jsonPayload,
                                                        final String sheetname);

    Set<MemberDTO> parseAndPersistMembersFromExcelWorkbook(final ExtensionScheme extensionScheme,
                                                           final Workbook workbook,
                                                           final String sheetName);
}
