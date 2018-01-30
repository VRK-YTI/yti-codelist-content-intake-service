package fi.vm.yti.codelist.intake.parser;

import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

public abstract class AbstractBaseParser {

    public static final String EU_REGISTRY = "eu";
    public static final String YTI_DATACLASSIFICATION_CODESCHEME = "dcat";

    public String resolveLanguageFromHeader(final String prefix,
                                            final String header) {
        return header.substring(header.indexOf(prefix) + prefix.length()).toLowerCase();
    }

    public UUID parseUUIDFromString(final String uuidString) {
        final UUID uuid;
        if (uuidString == null || uuidString.isEmpty()) {
            uuid = null;
        } else {
            uuid = UUID.fromString(uuidString);
        }
        return uuid;
    }

    public static boolean isRowEmpty(final Row row) {
        for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
            final Cell cell = row.getCell(cellIndex);
            if (cell != null && cell.getCellTypeEnum() != CellType.BLANK)
                return false;
        }
        return true;
    }

}
