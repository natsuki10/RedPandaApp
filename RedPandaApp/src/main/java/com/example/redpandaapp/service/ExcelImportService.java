package com.example.redpandaapp.service;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.redpandaapp.model.RedPanda;

@Service
public class ExcelImportService {

    public List<RedPanda> loadRedPandas(String urlStr) {
        List<RedPanda> pandaList = new ArrayList<>();

        try (InputStream is = new URL(urlStr).openStream()) {
            pandaList = parseExcel(is);
        } catch (Exception e) {
            // ネットワーク失敗時は resources 配下のバックアップExcelを読み込む
            try (InputStream fallback = getClass().getResourceAsStream("/redpandas_backup.xlsx")) {
                pandaList = parseExcel(fallback);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return pandaList;
    }

    private List<RedPanda> parseExcel(InputStream inputStream) throws Exception {
        List<RedPanda> list = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        for (int i = 2; i <= sheet.getLastRowNum()-1; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            RedPanda panda = new RedPanda();

            panda.setName(getCellValue(row.getCell(0)));
            panda.setGender(getCellValue(row.getCell(1)));
            panda.setBirthDate(formatDateCell(row.getCell(2)));
            panda.setDeathDate(formatDateCell(row.getCell(3)));
            panda.setAge(calculateAge(panda.getBirthDate(), panda.getDeathDate()));
            panda.setMovedOutDate(formatDateCell(row.getCell(5)));
            panda.setMovedOutZoo(getCellValue(row.getCell(6)));
            panda.setArrivalDate(formatDateCell(row.getCell(7)));
            panda.setOriginZoo(getCellValue(row.getCell(8)));
            panda.setFather(getCellValue(row.getCell(9)));
            panda.setMother(getCellValue(row.getCell(10)));
            panda.setPair1(getCellValue(row.getCell(11)));
            panda.setPair2(getCellValue(row.getCell(12)));
            panda.setPair3(getCellValue(row.getCell(13)));
            panda.setPersonality(getCellValue(row.getCell(14)));
            panda.setFeature(getCellValue(row.getCell(15)));

            list.add(panda);
        }

        workbook.close();
        return list;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield new SimpleDateFormat("yyyy/MM/dd").format(cell.getDateCellValue());
                } else {
                    // Excelのシリアル値も日付として変換を試みる
                    yield new SimpleDateFormat("yyyy/MM/dd").format(DateUtil.getJavaDate(cell.getNumericCellValue()));
                }
            }
            case FORMULA -> {
                FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                CellValue value = evaluator.evaluate(cell);
                if (value.getCellType() == CellType.NUMERIC) {
                    yield String.valueOf(value.getNumberValue());
                } else {
                    yield value.formatAsString();
                }
            }
            default -> "";
        };
    }

    private String formatDateCell(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                double numericValue = cell.getNumericCellValue();

                // 0 は "1899/12/31" になってしまうため排除
                if (numericValue == 0.0) return "";

                return new SimpleDateFormat("yyyy/MM/dd").format(DateUtil.getJavaDate(numericValue));
            }

            // 日付以外の文字列も考慮して fallback
            return getCellValue(cell);
        } catch (Exception e) {
            return "";
        }
    }


    private String calculateAge(String birthDateStr, String deathDateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            LocalDate birthDate = LocalDate.parse(birthDateStr, formatter);
            LocalDate endDate = (deathDateStr == null || deathDateStr.isBlank())
                    ? LocalDate.now()
                    : LocalDate.parse(deathDateStr, formatter);

            long years = ChronoUnit.YEARS.between(birthDate, endDate);
            return years + "歳";
        } catch (Exception e) {
            return "";
        }
    }
}
