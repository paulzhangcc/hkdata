package com.meimeitech.hkdata.util;

import com.alibaba.fastjson.JSON;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author paul
 * @description
 * @date 2019/6/25
 */
public class ExcelUtil {
    public static String timeFormat(String data) throws Exception {
        Date result = null;

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            result = simpleDateFormat.parse(data);
        } catch (Exception e) {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                result = simpleDateFormat.parse(data);
            } catch (Exception e1) {

            }
        }
        if (result == null) {
            return null;
        } else {
            return new SimpleDateFormat("HH:mm:ss").format(result);
        }
    }

    public static String dataFormat(String data) throws Exception {
        Date result = null;

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            result = simpleDateFormat.parse(data);
        } catch (Exception e) {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
                result = simpleDateFormat.parse(data);
            } catch (Exception e1) {

            }
        }
        if (result == null) {
            return null;
        } else {
            return new SimpleDateFormat("yyyy-MM-dd").format(result);
        }
    }


    public static void main(String[] args) throws Exception {


        // 打开指定位置的Excel文件
        String file = "C:\\Users\\paul\\Desktop\\111\\1.xlsx";
        Workbook workbook = null;
        try {
            workbook = new HSSFWorkbook(new FileInputStream(new File(file)));
        } catch (Exception e) {
            workbook = new XSSFWorkbook(new FileInputStream(new File(file)));
        }
        Sheet sheet = workbook.getSheetAt(0);
        int i = 0;
        for (Row row : sheet) {
            if (i == 0 || i == 1) {
                i++;
                continue;
            }
            List<String> rowList = new ArrayList<>();
            for (Cell cell : row) {
                switch (cell.getCellType()) {
                    case STRING:
                        rowList.add(cell.getStringCellValue());
                        break;
                    case NUMERIC:
                        rowList.add(String.valueOf(cell.getNumericCellValue()));
                        break;
                    case BOOLEAN:
                        rowList.add(String.valueOf(cell.getBooleanCellValue()));
                        break;
                    case BLANK:
                        rowList.add(null);
                        break;
                }
            }
            i++;

            if (rowList.size() > 0 && rowList.get(0) != null) {
                System.out.println(JSON.toJSONString(rowList));
            }
        }
    }
}
