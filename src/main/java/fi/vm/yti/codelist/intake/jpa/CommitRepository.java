package fi.vm.yti.codelist.intake.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.intake.model.Commit;

@Repository
@Transactional
public interface CommitRepository extends CrudRepository<Commit, String> {

    Commit findByTraceId(@Param("traceId") final String traceId);

    @Query(value = "SELECT * FROM commit as c WHERE c.id IN (SELECT e.commit_id FROM editedentity as e WHERE e.coderegistry_id = :codeRegistryId) ORDER BY modified DESC limit 1", nativeQuery = true)
    Commit findLatestCommitByCodeRegistryId(@Param("codeRegistryId") final UUID codeRegistryId);

    @Query(value = "SELECT * FROM commit as c WHERE c.id IN (SELECT e.commit_id FROM editedentity as e WHERE e.codescheme_id = :codeSchemeId) ORDER BY modified DESC limit 1", nativeQuery = true)
    Commit findLatestCommitByCodeSchemeId(@Param("codeSchemeId") final UUID codeSchemeId);

    @Query(value = "SELECT * FROM commit as c WHERE c.id IN (SELECT e.commit_id FROM editedentity as e WHERE e.code_id = :codeId) ORDER BY modified DESC limit 1", nativeQuery = true)
    Commit findLatestCommitByCodeId(@Param("codeId") final UUID codeId);

    @Query(value = "SELECT * FROM commit as c WHERE c.id IN (SELECT e.commit_id FROM editedentity as e WHERE e.extension_id = :extensionId) ORDER BY modified DESC limit 1", nativeQuery = true)
    Commit findLatestCommitByExtensionId(@Param("extensionId") final UUID extensionId);

    @Query(value = "SELECT * FROM commit as c WHERE c.id IN (SELECT e.commit_id FROM editedentity as e WHERE e.member_id = :memberId) ORDER BY modified DESC limit 1", nativeQuery = true)
    Commit findLatestCommitByMemberId(@Param("memberId") final UUID memberId);
}
