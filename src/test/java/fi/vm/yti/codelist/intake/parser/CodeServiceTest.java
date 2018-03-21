package fi.vm.yti.codelist.intake.parser;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.service.CodeService;
import static org.junit.Assert.assertEquals;

public class CodeServiceTest {

    @InjectMocks
    private CodeService codeService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void evaluateAndSetHierarchyLevelsTest() {
        final Set<Code> codes = createCodes();
        codeService.evaluateAndSetHierarchyLevels(codes);
        Integer i = 0;
        for (final Code code : codes) {
            assertEquals(++i, code.getHierarchyLevel());
        }
        assertEquals(10, codes.size());
    }

    private Set<Code> createCodes() {
        final Set<Code> codes = new LinkedHashSet<>();
        UUID earlierCodeId = null;
        for (int i = 0; i < 10; i++) {
            final Code code = new Code();
            code.setId(UUID.randomUUID());
            code.setCodeValue("codevalue" + i);
            codes.add(code);
            if (earlierCodeId != null) {
                code.setBroaderCodeId(earlierCodeId);
            }
            earlierCodeId = code.getId();
        }
        return codes;
    }
}