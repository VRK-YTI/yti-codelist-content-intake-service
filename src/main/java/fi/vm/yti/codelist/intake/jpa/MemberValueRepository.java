package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.MemberValue;

@Repository
@Transactional
public interface MemberValueRepository extends CrudRepository<MemberValue, String> {

    MemberValue findById(final UUID id);

    Set<MemberValue> findByMemberId(final UUID id);
}
