package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.ValueType;

@Repository
@Transactional
public interface ValueTypeRepository extends CrudRepository<ValueType, String> {

    ValueType findById(final UUID id);

    ValueType findByLocalName(final String localName);

    Set<ValueType> findAll();
}
