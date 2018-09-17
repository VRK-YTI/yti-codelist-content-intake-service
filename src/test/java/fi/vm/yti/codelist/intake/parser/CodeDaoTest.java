package fi.vm.yti.codelist.intake.parser;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import fi.vm.yti.codelist.intake.dao.impl.CodeDaoImpl;
import fi.vm.yti.codelist.intake.model.Code;
import static org.junit.Assert.assertEquals;

@ActiveProfiles({"automatedtest"})
public class CodeDaoTest {

    @InjectMocks
    private CodeDaoImpl codeDao;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void evaluateAndSetHierarchyLevelsTest() {
        final Set<Code> codes = createCodes();
        codeDao.evaluateAndSetHierarchyLevels(codes);
        Integer i = 0;
        for (final Code code : codes) {
            assertEquals(++i, code.getHierarchyLevel());
        }
        assertEquals(8, codes.size());
    }

    private Set<Code> createCodes() {
        final Set<Code> codes = new LinkedHashSet<>();
        Code earlierCode = null;
        for (int i = 0; i < 8; i++) {
            final Code code = new Code();
            code.setId(UUID.randomUUID());
            code.setCodeValue("codevalue" + i);
            codes.add(code);
            if (earlierCode != null) {
                code.setBroaderCode(earlierCode);
            }
            earlierCode = code;
        }
        return codes;
    }
}