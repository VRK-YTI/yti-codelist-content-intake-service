package fi.vm.yti.codelist.intake.parser;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;

import fi.vm.yti.codelist.intake.dao.impl.CodeDaoImpl;
import fi.vm.yti.codelist.intake.model.Code;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doCallRealMethod;

@RunWith(SpringRunner.class)
public class CodeDaoTest {

    @MockBean
    private CodeDaoImpl codeDao;

    @Before
    public void setup() {
        doCallRealMethod().when(codeDao).evaluateAndSetHierarchyLevels(any(Set.class), any(Set.class));
    }

    @Test
    public void evaluateAndSetHierarchyLevelsTest() {
        final Set<Code> codes = createCodes();
        codeDao.evaluateAndSetHierarchyLevels(codes, codes);
        int i = 0;
        for (final Code code : codes) {
            assertEquals(++i, (int) code.getHierarchyLevel());
        }
        assertEquals(15, codes.size());
    }

    private Set<Code> createCodes() {
        final Set<Code> codes = new LinkedHashSet<>();
        Code earlierCode = null;
        for (int i = 0; i < 15; i++) {
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
