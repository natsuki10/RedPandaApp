package com.example.redpandaapp.service;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.redpandaapp.model.RedPanda;

/*
 * レッサーパンダ個体一覧読み込みサービスクラス
 */

@Service
public class ExcelImportService {

	//例外時に読み込むバックアップファイル
    private static final String BACKUP_FILE = "redpandas_backup.xlsx"; 

    // URLから読み込み
    public List<RedPanda> loadRedPandas(String url) {
        try (InputStream is = new URL(url).openStream()) {
            return readExcel(is);
        } catch (Exception e) {
            System.err.println("URLからの読み込みに失敗しました。: " + e.getMessage());
            return loadFromBackup();
        }
    }

    // ローカルのバックアップExcelを読む
    private List<RedPanda> loadFromBackup() {
        try (InputStream is = new ClassPathResource(BACKUP_FILE).getInputStream()) {
            return readExcel(is);
        } catch (Exception e) {
            System.err.println("ローカルファイルの読み込みにも失敗しました: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // 共通処理。Excelの内容を RedPanda のリストに変換
    private List<RedPanda> readExcel(InputStream inputStream) throws Exception {
        List<RedPanda> list = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            RedPanda panda = new RedPanda();
            panda.setName(getCellValue(row.getCell(0)));
            panda.setGender(getCellValue(row.getCell(1)));
            panda.setBirthDate(getCellValue(row.getCell(2)));
            panda.setDeathDate(getCellValue(row.getCell(3)));
            panda.setAge(getCellValue(row.getCell(4)));
            panda.setMovedOutDate(getCellValue(row.getCell(5)));
            panda.setMovedOutZoo(getCellValue(row.getCell(6)));
            panda.setArrivalDate(getCellValue(row.getCell(7)));
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
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> cell.toString();
        };
    }
}
