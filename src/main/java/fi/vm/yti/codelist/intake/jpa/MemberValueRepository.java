package fi.vm.yti.codelist.intake.jpa;

import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.MemberValue;
import fi.vm.yti.codelist.intake.model.ValueType;

@Repository
@Transactional
public interface MemberValueRepository extends CrudRepository<MemberValue, String> {

    MemberValue findById(final UUID id);

    MemberValue findByMemberAndValueType(final Member member,
                                         final ValueType valueType);
}
