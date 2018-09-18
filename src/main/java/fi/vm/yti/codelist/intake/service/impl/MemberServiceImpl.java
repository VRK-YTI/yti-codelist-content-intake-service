package fi.vm.yti.codelist.intake.service.impl;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.MemberDao;
import fi.vm.yti.codelist.intake.dao.ExtensionSchemeDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.parser.MemberParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.MemberService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class MemberServiceImpl implements MemberService {

    private final AuthorizationManager authorizationManager;
    private final MemberDao memberDao;
    private final MemberParser memberParser;
    private final ExtensionSchemeDao extensionSchemeDao;
    private final CodeSchemeDao codeSchemeDao;
    private final DtoMapperService dtoMapperService;

    @Inject
    public MemberServiceImpl(final AuthorizationManager authorizationManager,
                             final MemberDao memberDao,
                             final MemberParser memberParser,
                             final ExtensionSchemeDao extensionSchemeDao,
                             final CodeSchemeDao codeSchemeDao,
                             final DtoMapperService dtoMapperService) {
        this.authorizationManager = authorizationManager;
        this.memberDao = memberDao;
        this.memberParser = memberParser;
        this.extensionSchemeDao = extensionSchemeDao;
        this.codeSchemeDao = codeSchemeDao;
        this.dtoMapperService = dtoMapperService;
    }

    @Transactional
    public MemberDTO deleteMember(final UUID id) {
        final Member member = memberDao.findById(id);
        if (!authorizationManager.canExtensionBeDeleted(member)) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        final Set<Member> members = memberDao.findByExtensionSchemeId(member.getExtensionScheme().getId());
        members.forEach(extension1 -> {
            final Member broaderMember = member.getBroaderMember();
            if (broaderMember != null && broaderMember.getId() == id) {
                member.setBroaderMember(null);
                memberDao.save(member);
            }
        });
        final MemberDTO memberDto = dtoMapperService.mapMemberDto(member, false);
        memberDao.delete(member);
        return memberDto;
    }

    @Transactional
    public Set<MemberDTO> findAll() {
        return dtoMapperService.mapDeepMemberDtos(memberDao.findAll());
    }

    @Transactional
    public MemberDTO findById(final UUID id) {
        return dtoMapperService.mapDeepMemberDto(memberDao.findById(id));
    }

    @Transactional
    public Set<MemberDTO> findByExtensionSchemeId(final UUID id) {
        return dtoMapperService.mapDeepMemberDtos(memberDao.findByExtensionSchemeId(id));
    }

    @Transactional
    public Set<MemberDTO> parseAndPersistMemberFromJson(final String jsonPayload) {
        Set<Member> members;
        if (jsonPayload != null && !jsonPayload.isEmpty()) {
            final MemberDTO memberDto = memberParser.parseMemberFromJson(jsonPayload);
            if (memberDto.getExtensionScheme() != null) {
                final ExtensionScheme extensionScheme = extensionSchemeDao.findById(memberDto.getExtensionScheme().getId());
                if (!authorizationManager.canBeModifiedByUserInOrganization(extensionScheme.getParentCodeScheme().getCodeRegistry().getOrganizations())) {
                    throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
                }
                members = memberDao.updateMemberEntityFromDto(extensionScheme, memberDto);
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        if (members == null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return dtoMapperService.mapDeepMemberDtos(members);
    }

    @Transactional
    public Set<MemberDTO> parseAndPersistMembersFromSourceData(final String codeRegistryCodeValue,
                                                               final String codeSchemeCodeValue,
                                                               final String extensionSchemeCodeValue,
                                                               final String format,
                                                               final InputStream inputStream,
                                                               final String jsonPayload,
                                                               final String sheetName) {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            Set<Member> members;
            final ExtensionScheme extensionScheme = extensionSchemeDao.findByParentCodeSchemeIdAndCodeValue(codeScheme.getId(), extensionSchemeCodeValue);
            if (extensionScheme != null) {
                switch (format.toLowerCase()) {
                    case FORMAT_JSON:
                        if (jsonPayload != null && !jsonPayload.isEmpty()) {
                            members = memberDao.updateMemberEntitiesFromDtos(extensionScheme, memberParser.parseMembersFromJson(jsonPayload));
                        } else {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                        }
                        break;
                    case FORMAT_EXCEL:
                        members = memberDao.updateMemberEntitiesFromDtos(extensionScheme, memberParser.parseMembersFromExcelInputStream(extensionScheme, inputStream, sheetName));
                        break;
                    case FORMAT_CSV:
                        members = memberDao.updateMemberEntitiesFromDtos(extensionScheme, memberParser.parseMembersFromCsvInputStream(extensionScheme, inputStream));
                        break;
                    default:
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                }
                return dtoMapperService.mapDeepMemberDtos(members);
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    public Set<MemberDTO> parseAndPersistMembersFromExcelWorkbook(final ExtensionScheme extensionScheme,
                                                                  final Workbook workbook,
                                                                  final String sheetName) {
        if (!authorizationManager.canBeModifiedByUserInOrganization(extensionScheme.getParentCodeScheme().getCodeRegistry().getOrganizations())) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        Set<Member> members;
        final Set<MemberDTO> memberDtos = memberParser.parseMembersFromExcelWorkbook(extensionScheme, workbook, sheetName);
        members = memberDao.updateMemberEntitiesFromDtos(extensionScheme, memberDtos);
        return dtoMapperService.mapDeepMemberDtos(members);
    }
}
