package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.intake.model.ValueType;

@Repository
@Transactional
public interface ValueTypeRepository extends CrudRepository<ValueType, String> {

    ValueType findById(final UUID id);

    ValueType findByLocalName(final String localName);

    Set<ValueType> findAll();
}
