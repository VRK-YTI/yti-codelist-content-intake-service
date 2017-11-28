package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.common.model.Organization;

@Repository
public interface OrganizationRepository extends CrudRepository<Organization, String> {

    Set<Organization> findAll();
}
