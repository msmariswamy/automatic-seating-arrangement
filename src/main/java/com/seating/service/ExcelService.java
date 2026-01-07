package com.seating.service;

import com.seating.dto.RoomDTO;
import com.seating.dto.StudentDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Service for handling Excel operations
 */
@Service
@Slf4j
public class ExcelService {

    /**
     * Generate student template Excel file
     */
    public byte[] generateStudentTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {"Roll No", "Student Name", "Department", "Class",
                              "Subject1", "Subject2", "Subject3", "Subject4", "Subject5"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Add sample data
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("CS001");
            sampleRow.createCell(1).setCellValue("John Doe");
            sampleRow.createCell(2).setCellValue("CSE");
            sampleRow.createCell(3).setCellValue("BE-IV");
            sampleRow.createCell(4).setCellValue("Mathematics");
            sampleRow.createCell(5).setCellValue("Physics");
            sampleRow.createCell(6).setCellValue("");
            sampleRow.createCell(7).setCellValue("");
            sampleRow.createCell(8).setCellValue("");

            // Add instructions
            Sheet instructionSheet = workbook.createSheet("Instructions");
            Row instruction1 = instructionSheet.createRow(0);
            instruction1.createCell(0).setCellValue("Instructions for Student Data Upload:");

            Row instruction2 = instructionSheet.createRow(2);
            instruction2.createCell(0).setCellValue("1. Fill in student details in the 'Students' sheet");

            Row instruction3 = instructionSheet.createRow(3);
            instruction3.createCell(0).setCellValue("2. Roll No must be unique for each student");

            Row instruction4 = instructionSheet.createRow(4);
            instruction4.createCell(0).setCellValue("3. Enter subject names in Subject columns (leave empty if not applicable)");

            Row instruction5 = instructionSheet.createRow(5);
            instruction5.createCell(0).setCellValue("4. All filled subjects will be considered for seating arrangement");

            instructionSheet.setColumnWidth(0, 15000);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Generate room template Excel file
     */
    public byte[] generateRoomTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Rooms");

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {"Room No", "Total Benches", "Capacity", "R Count", "M Count", "L Count"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Add sample data
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("101");
            sampleRow.createCell(1).setCellValue(30);
            sampleRow.createCell(2).setCellValue(90);
            sampleRow.createCell(3).setCellValue(30);
            sampleRow.createCell(4).setCellValue(30);
            sampleRow.createCell(5).setCellValue(30);

            // Add instructions
            Sheet instructionSheet = workbook.createSheet("Instructions");
            Row instruction1 = instructionSheet.createRow(0);
            instruction1.createCell(0).setCellValue("Instructions for Room Data Upload:");

            Row instruction2 = instructionSheet.createRow(2);
            instruction2.createCell(0).setCellValue("1. Fill in room details in the 'Rooms' sheet");

            Row instruction3 = instructionSheet.createRow(3);
            instruction3.createCell(0).setCellValue("2. Room No must be unique");

            Row instruction4 = instructionSheet.createRow(4);
            instruction4.createCell(0).setCellValue("3. Capacity should equal (R Count + M Count + L Count)");

            Row instruction5 = instructionSheet.createRow(5);
            instruction5.createCell(0).setCellValue("4. R = Right seats, M = Middle seats, L = Left seats on each bench");

            Row instruction6 = instructionSheet.createRow(6);
            instruction6.createCell(0).setCellValue("5. Example: 30 benches with R=30, M=30, L=30 means 90 total seats");

            instructionSheet.setColumnWidth(0, 15000);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Parse student Excel file
     */
    public List<StudentDTO> parseStudentExcel(MultipartFile file) throws IOException {
        List<StudentDTO> students = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheet("Students");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    String rollNo = getCellValueAsString(row.getCell(0));
                    String name = getCellValueAsString(row.getCell(1));
                    String department = getCellValueAsString(row.getCell(2));
                    String className = getCellValueAsString(row.getCell(3));

                    if (rollNo.isEmpty() || name.isEmpty() || department.isEmpty() || className.isEmpty()) {
                        log.warn("Skipping row {} due to missing required fields", i + 1);
                        continue;
                    }

                    Set<String> subjects = new HashSet<>();
                    for (int j = 4; j < row.getLastCellNum(); j++) {
                        String subject = getCellValueAsString(row.getCell(j));
                        if (!subject.isEmpty()) {
                            subjects.add(subject.trim());
                        }
                    }

                    if (subjects.isEmpty()) {
                        log.warn("Skipping row {} - student {} has no subjects", i + 1, rollNo);
                        continue;
                    }

                    StudentDTO studentDTO = StudentDTO.builder()
                            .rollNo(rollNo.trim())
                            .name(name.trim())
                            .department(department.trim())
                            .className(className.trim())
                            .subjects(subjects)
                            .build();

                    students.add(studentDTO);
                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", i + 1, e.getMessage());
                }
            }
        }

        log.info("Parsed {} students from Excel file", students.size());
        return students;
    }

    /**
     * Parse room Excel file
     */
    public List<RoomDTO> parseRoomExcel(MultipartFile file) throws IOException {
        List<RoomDTO> rooms = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheet("Rooms");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    String roomNo = getCellValueAsString(row.getCell(0));
                    Integer totalBenches = getCellValueAsInteger(row.getCell(1));
                    Integer capacity = getCellValueAsInteger(row.getCell(2));
                    Integer rCount = getCellValueAsInteger(row.getCell(3));
                    Integer mCount = getCellValueAsInteger(row.getCell(4));
                    Integer lCount = getCellValueAsInteger(row.getCell(5));

                    if (roomNo.isEmpty() || totalBenches == null || capacity == null ||
                        rCount == null || mCount == null || lCount == null) {
                        log.warn("Skipping row {} due to missing required fields", i + 1);
                        continue;
                    }

                    if (capacity != (rCount + mCount + lCount)) {
                        log.warn("Row {}: Capacity mismatch. Expected {}, got {}",
                                i + 1, (rCount + mCount + lCount), capacity);
                    }

                    RoomDTO roomDTO = RoomDTO.builder()
                            .roomNo(roomNo.trim())
                            .totalBenches(totalBenches)
                            .capacity(capacity)
                            .rCount(rCount)
                            .mCount(mCount)
                            .lCount(lCount)
                            .build();

                    rooms.add(roomDTO);
                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", i + 1, e.getMessage());
                }
            }
        }

        log.info("Parsed {} rooms from Excel file", rooms.size());
        return rooms;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                return Integer.parseInt(cell.getStringCellValue().trim());
            }
        } catch (Exception e) {
            log.error("Error converting cell to integer: {}", e.getMessage());
        }
        return null;
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK &&
                !getCellValueAsString(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
