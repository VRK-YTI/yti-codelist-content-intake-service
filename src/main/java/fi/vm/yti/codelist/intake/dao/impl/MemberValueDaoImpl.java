package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.MemberValueDTO;
import fi.vm.yti.codelist.intake.dao.MemberValueDao;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.MemberValueRepository;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.MemberValue;
import fi.vm.yti.codelist.intake.model.PropertyType;
import fi.vm.yti.codelist.intake.model.ValueType;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;
import static fi.vm.yti.codelist.intake.util.ValidationUtils.validateStringAgainstRegexp;

@Component
public class MemberValueDaoImpl implements MemberValueDao {

    private final MemberValueRepository memberValueRepository;

    @Inject
    public MemberValueDaoImpl(final MemberValueRepository memberValueRepository) {
        this.memberValueRepository = memberValueRepository;
    }

    @Transactional
    public MemberValue findById(final UUID id) {
        return memberValueRepository.findById(id);
    }

    @Transactional
    public Set<MemberValue> findByMemberId(final UUID id) {
        return memberValueRepository.findByMemberId(id);
    }

    @Transactional
    public void save(final Set<MemberValue> memberValues) {
        memberValueRepository.saveAll(memberValues);
    }

    @Transactional
    public void save(final MemberValue memberValue) {
        memberValueRepository.save(memberValue);
    }

    @Transactional
    public Set<MemberValue> updateMemberValueEntitiesFromDtos(final Member member,
                                                              final Set<MemberValueDTO> memberValueDtos,
                                                              final Map<String, ValueType> valueTypeMap) {
        final Set<MemberValue> memberValues = new HashSet<>();
        final Set<MemberValue> existingMemberValues = findByMemberId(member.getId());
        if (memberValueDtos != null && !memberValueDtos.isEmpty()) {
            memberValueDtos.forEach(memberValueDto -> {
                final MemberValue memberValue = createOrUpdateMemberValue(existingMemberValues, member, memberValueDto, valueTypeMap);
                if (memberValue != null) {
                    existingMemberValues.add(memberValue);
                    memberValues.add(memberValue);
                }
            });
        }
        return memberValues;
    }

    private MemberValue createOrUpdateMemberValue(final Set<MemberValue> existingMemberValues,
                                                  final Member member,
                                                  final MemberValueDTO fromMemberValue,
                                                  final Map<String, ValueType> valueTypeMap) {
        validateMemberValue(fromMemberValue, member.getExtension().getPropertyType());
        MemberValue existingMemberValue = null;
        if (fromMemberValue.getValueType() != null) {
            final ValueType valueType = valueTypeMap.get(fromMemberValue.getValueType().getLocalName());
            if (valueType == null) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBERVALUE_VALUETYPE_NOT_SET));
            }
            for (final MemberValue memberValue : existingMemberValues) {
                if (memberValue.getValueType().getId().equals(valueType.getId())) {
                    existingMemberValue = memberValue;
                    break;
                }
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBERVALUE_VALUETYPE_NOT_SET));
        }
        final MemberValue memberValue;
        if (existingMemberValue != null) {
            validateMemberIdsToMatch(member, existingMemberValue.getMember());
            memberValue = updateMemberValue(member, existingMemberValue, fromMemberValue);
        } else {
            memberValue = createMemberValue(member, fromMemberValue, valueTypeMap);
        }
        return memberValue;
    }

    private MemberValue updateMemberValue(final Member member,
                                          final MemberValue existingMemberValue,
                                          final MemberValueDTO fromMemberValue) {
        existingMemberValue.setValue(fromMemberValue.getValue());
        final Date timeStamp = new Date(System.currentTimeMillis());
        existingMemberValue.setModified(timeStamp);
        existingMemberValue.setMember(member);
        return existingMemberValue;
    }

    private MemberValue createMemberValue(final Member member,
                                          final MemberValueDTO fromMemberValue,
                                          final Map<String, ValueType> valueTypeMap) {
        final MemberValue memberValue = new MemberValue();
        memberValue.setId(UUID.randomUUID());
        final ValueType valueType = valueTypeMap.get(fromMemberValue.getValueType().getLocalName());
        if (valueType != null) {
            memberValue.setValueType(valueType);
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_VALUETYPE_NOT_FOUND));
        }
        memberValue.setValue(fromMemberValue.getValue());
        final Date timeStamp = new Date(System.currentTimeMillis());
        memberValue.setCreated(timeStamp);
        memberValue.setModified(timeStamp);
        memberValue.setMember(member);
        return memberValue;
    }

    private void validateMemberIdsToMatch(final Member member,
                                          final Member existingMember) {
        if (!existingMember.getId().equals(member.getId())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_ID_MISMATCH));
        }
    }

    private void validateMemberValue(final MemberValueDTO memberValue,
                                     final PropertyType propertyType) {
        final ValueType valueType = propertyType.getValueTypeWithLocalName(memberValue.getValueType().getLocalName());
        if (valueType != null) {
            final String memberValueString = memberValue.getValue();
            if (!valueType.getRequired() && (memberValueString == null || memberValueString.isEmpty())) {
                return;
            }
            final String regexp = valueType.getRegexp();
            if (regexp != null && !regexp.isEmpty() && !validateStringAgainstRegexp(memberValueString, regexp)) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBERVALUE_VALIDATION_FAILED));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_VALUETYPE_NOT_FOUND));
        }
    }
}
