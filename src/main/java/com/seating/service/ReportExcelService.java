package com.seating.service;

import com.seating.config.ReportConfig;
import com.seating.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportExcelService {

    private final ReportConfig reportConfig;

    // ===== Room Report =====

    public byte[] generateRoomReportExcel(RoomReportDTO report, LocalDate date) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            createRoomSheet(workbook, styles, report, date);
            return toBytes(workbook);
        }
    }

    public byte[] generateAllRoomsReportExcel(List<RoomReportDTO> reports, LocalDate date) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            for (RoomReportDTO report : reports) {
                createRoomSheet(workbook, styles, report, date);
            }
            return toBytes(workbook);
        }
    }

    private void createRoomSheet(Workbook workbook, Styles styles, RoomReportDTO report, LocalDate date) {
        String sheetName = getUniqueSheetName(workbook, "Room " + report.getRoomNo());
        Sheet sheet = workbook.createSheet(sheetName);

        int rowIdx = 0;

        // College header
        rowIdx = addCollegeHeader(sheet, rowIdx, styles, 8);

        // Title
        Row titleRow = sheet.createRow(rowIdx);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Individual Room Report");
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 7));
        rowIdx += 2;

        // Room info
        Row roomRow = sheet.createRow(rowIdx++);
        createCell(roomRow, 0, "Room No: " + report.getRoomNo(), styles.bold);
        createCell(roomRow, 4, "Date: " + date, styles.bold);

        Row deptRow = sheet.createRow(rowIdx++);
        createCell(deptRow, 0, "Departments: " + String.join(", ", report.getDepartments()), styles.normal);

        Row subjRow = sheet.createRow(rowIdx++);
        createCell(subjRow, 0, "Subjects: " + String.join(", ", report.getSubjects()), styles.normal);

        rowIdx++; // blank row

        // Section headers: Right (cols 0-1), Middle (cols 3-4), Left (cols 6-7)
        Row sectionRow = sheet.createRow(rowIdx);
        createCell(sectionRow, 0, "Right Seats (R)", styles.header);
        createCell(sectionRow, 1, "", styles.header);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
        createCell(sectionRow, 3, "Middle Seats (M)", styles.header);
        createCell(sectionRow, 4, "", styles.header);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 3, 4));
        createCell(sectionRow, 6, "Left Seats (L)", styles.header);
        createCell(sectionRow, 7, "", styles.header);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 6, 7));
        rowIdx++;

        // Sub-headers
        Row subHeaderRow = sheet.createRow(rowIdx++);
        int[] colOffsets = {0, 3, 6};
        for (int offset : colOffsets) {
            createCell(subHeaderRow, offset, "Seat", styles.header);
            createCell(subHeaderRow, offset + 1, "Roll No", styles.header);
        }

        // Data rows
        int rightSize = report.getRightSeats() != null ? report.getRightSeats().size() : 0;
        int middleSize = report.getMiddleSeats() != null ? report.getMiddleSeats().size() : 0;
        int leftSize = report.getLeftSeats() != null ? report.getLeftSeats().size() : 0;
        int maxRows = Math.max(Math.max(rightSize, middleSize), leftSize);

        for (int i = 0; i < maxRows; i++) {
            Row dataRow = sheet.createRow(rowIdx + i);

            // Right seats - always create cells for border grid
            Cell rSeat = createCell(dataRow, 0, "", styles.centered);
            Cell rRoll = createCell(dataRow, 1, "", styles.centered);
            if (i < rightSize) {
                SeatAllocationDTO seat = report.getRightSeats().get(i);
                rSeat.setCellValue(seat.getSeatNo());
                rRoll.setCellValue(seat.getRollNo());
            }

            // Middle seats
            Cell mSeat = createCell(dataRow, 3, "", styles.centered);
            Cell mRoll = createCell(dataRow, 4, "", styles.centered);
            if (i < middleSize) {
                SeatAllocationDTO seat = report.getMiddleSeats().get(i);
                mSeat.setCellValue(seat.getSeatNo());
                mRoll.setCellValue(seat.getRollNo());
            }

            // Left seats
            Cell lSeat = createCell(dataRow, 6, "", styles.centered);
            Cell lRoll = createCell(dataRow, 7, "", styles.centered);
            if (i < leftSize) {
                SeatAllocationDTO seat = report.getLeftSeats().get(i);
                lSeat.setCellValue(seat.getSeatNo());
                lRoll.setCellValue(seat.getRollNo());
            }
        }

        autoSizeColumns(sheet, 8);
    }

    // ===== Consolidated Report =====

    public byte[] generateConsolidatedReportExcel(List<ConsolidatedReportDTO> report, LocalDate date) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            Sheet sheet = workbook.createSheet("Consolidated Report");

            int rowIdx = 0;
            rowIdx = addCollegeHeader(sheet, rowIdx, styles, 5);

            Row titleRow = sheet.createRow(rowIdx);
            createCell(titleRow, 0, "Consolidated Report", styles.title);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 4));
            rowIdx += 2;

            Row dateRow = sheet.createRow(rowIdx++);
            createCell(dateRow, 0, "Date: " + date, styles.bold);
            rowIdx++;

            // Table headers
            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {"Room No", "Department", "Roll No From", "Roll No To", "Total Count"};
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], styles.header);
            }

            // Data
            for (ConsolidatedReportDTO row : report) {
                Row dataRow = sheet.createRow(rowIdx++);
                createCell(dataRow, 0, row.getRoomNo(), styles.centered);
                createCell(dataRow, 1, row.getDepartment(), styles.centered);
                createCell(dataRow, 2, row.getRollNoFrom(), styles.centered);
                createCell(dataRow, 3, row.getRollNoTo(), styles.centered);
                createNumericCell(dataRow, 4, row.getTotalCount(), styles.centered);
            }

            autoSizeColumns(sheet, 5);
            return toBytes(workbook);
        }
    }

    // ===== Department Consolidated Report =====

    public byte[] generateDepartmentConsolidatedReportExcel(List<DepartmentConsolidatedReportDTO> report, LocalDate date) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            Sheet sheet = workbook.createSheet("Dept Consolidated Report");

            int rowIdx = 0;
            rowIdx = addCollegeHeader(sheet, rowIdx, styles, 6);

            Row titleRow = sheet.createRow(rowIdx);
            createCell(titleRow, 0, "Department Consolidated Report", styles.title);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 5));
            rowIdx += 2;

            Row dateRow = sheet.createRow(rowIdx++);
            createCell(dateRow, 0, "Date: " + date, styles.bold);
            rowIdx++;

            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {"Department", "Class", "Room No", "From Seat No", "To Seat No", "Total Count"};
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], styles.header);
            }

            for (DepartmentConsolidatedReportDTO row : report) {
                Row dataRow = sheet.createRow(rowIdx++);
                createCell(dataRow, 0, row.getDepartment(), styles.centered);
                createCell(dataRow, 1, row.getClassName(), styles.centered);
                createCell(dataRow, 2, row.getRoomNo(), styles.centered);
                createCell(dataRow, 3, row.getFromSeat() != null ? row.getFromSeat() : "", styles.centered);
                createCell(dataRow, 4, row.getToSeat() != null ? row.getToSeat() : "", styles.centered);
                createNumericCell(dataRow, 5, row.getTotalCount(), styles.centered);
            }

            autoSizeColumns(sheet, 6);
            return toBytes(workbook);
        }
    }

    // ===== Junior Supervisor Report =====

    public byte[] generateJuniorSupervisorReportExcel(JuniorSupervisorReportDTO report, LocalDate date,
            boolean showAnswerSheetCol, boolean showSupplementsCol) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            createJuniorSupervisorSheet(workbook, styles, report, date, showAnswerSheetCol, showSupplementsCol);
            return toBytes(workbook);
        }
    }

    public byte[] generateAllJuniorSupervisorReportExcel(List<JuniorSupervisorReportDTO> reports, LocalDate date,
            boolean showAnswerSheetCol, boolean showSupplementsCol) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            for (JuniorSupervisorReportDTO report : reports) {
                createJuniorSupervisorSheet(workbook, styles, report, date, showAnswerSheetCol, showSupplementsCol);
            }
            return toBytes(workbook);
        }
    }

    private void createJuniorSupervisorSheet(Workbook workbook, Styles styles, JuniorSupervisorReportDTO report,
            LocalDate date, boolean showAnswerSheetCol, boolean showSupplementsCol) {
        String sheetName = getUniqueSheetName(workbook, "Room " + report.getRoomNo() + " - " + report.getSubject());
        Sheet sheet = workbook.createSheet(sheetName);

        int totalCols = 3; // Sr No, Seat No, Signature
        if (showAnswerSheetCol) totalCols++;
        if (showSupplementsCol) totalCols++;
        int lastCol = totalCols - 1;

        int rowIdx = 0;

        // College header (hardcoded to match PDF)
        Row line1Row = sheet.createRow(rowIdx);
        createCell(line1Row, 0, "SHRI SIDH THAKURNATH COLLEGE OF ARTS & COMMERCE", styles.title);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
        rowIdx++;

        Row line2Row = sheet.createRow(rowIdx);
        createCell(line2Row, 0, "(Affiliated to University of Mumbai, Mumbai)", styles.centeredNoBorder);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
        rowIdx++;

        Row line3Row = sheet.createRow(rowIdx);
        createCell(line3Row, 0, "ULHASNAGAR - 421 004. Dist. Thane", styles.centeredNoBorder);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
        rowIdx++;

        Row titleRow = sheet.createRow(rowIdx);
        createCell(titleRow, 0, "Junior Supervisor's Report", styles.underlineTitle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
        rowIdx += 2;

        // Exam details
        Row deptRoomRow = sheet.createRow(rowIdx++);
        createCell(deptRoomRow, 0, "Department: " + report.getDepartment(), styles.bold);
        createCell(deptRoomRow, lastCol, "Room No: " + report.getRoomNo(), styles.bold);

        Row classRow = sheet.createRow(rowIdx++);
        createCell(classRow, 0, "Class: " + report.getClassName(), styles.bold);
        int midCol = totalCols / 2;
        createCell(classRow, midCol, "Subject: _______", styles.bold);
        createCell(classRow, lastCol, "Date: ____________", styles.bold);

        // Seat number range
        String seatNoRange = "_______";
        if (!report.getStudents().isEmpty()) {
            String first = report.getStudents().get(0).getRollNo();
            String last = report.getStudents().get(report.getStudents().size() - 1).getRollNo();
            seatNoRange = first + " to " + last;
        }

        Row semRow = sheet.createRow(rowIdx++);
        createCell(semRow, 0, "SEM: _______", styles.bold);
        createCell(semRow, midCol, "Total No. in the Block: " + report.getTotalStudents(), styles.bold);
        createCell(semRow, lastCol, "Seat No: " + seatNoRange, styles.bold);

        Row presentRow = sheet.createRow(rowIdx++);
        createCell(presentRow, 0, "Total No. of Candidates Present: _______", styles.bold);
        createCell(presentRow, lastCol, "Total No. of Candidates Absent: _______", styles.bold);

        rowIdx++; // blank row

        // Table headers
        Row tableHeaderRow = sheet.createRow(rowIdx++);
        int colIdx = 0;
        createCell(tableHeaderRow, colIdx++, "Sr No", styles.header);
        createCell(tableHeaderRow, colIdx++, "Seat No", styles.header);
        if (showAnswerSheetCol) {
            createCell(tableHeaderRow, colIdx++, "Main Answer Sheet No.", styles.header);
        }
        if (showSupplementsCol) {
            createCell(tableHeaderRow, colIdx++, "No. of Suppl. & Stationery", styles.header);
        }
        createCell(tableHeaderRow, colIdx, "Signature", styles.header);

        // Student data (40 rows)
        for (int i = 0; i < 40; i++) {
            Row dataRow = sheet.createRow(rowIdx++);
            colIdx = 0;
            createNumericCell(dataRow, colIdx++, i + 1, styles.centered);

            if (i < report.getStudents().size()) {
                createCell(dataRow, colIdx++, report.getStudents().get(i).getRollNo(), styles.centered);
            } else {
                createCell(dataRow, colIdx++, "", styles.centered);
            }

            if (showAnswerSheetCol) {
                createCell(dataRow, colIdx++, "", styles.centered);
            }
            if (showSupplementsCol) {
                createCell(dataRow, colIdx++, "", styles.centered);
            }
            createCell(dataRow, colIdx, "", styles.centered);
        }

        rowIdx++; // blank row

        // Footer
        Row answerRow = sheet.createRow(rowIdx++);
        createCell(answerRow, 0, "Total No. of Main Answer Sheets Used: _____________", styles.bold);
        rowIdx++;

        Row jrSupRow = sheet.createRow(rowIdx++);
        createCell(jrSupRow, 0, "Name of Jr. Supervisor: ________________", styles.bold);
        createCell(jrSupRow, lastCol, "Signature: ________________", styles.bold);

        Row checkedRow = sheet.createRow(rowIdx++);
        createCell(checkedRow, 0, "Checked by another Jr. Supervisor: ________________", styles.bold);
        createCell(checkedRow, lastCol, "Signature: ________________", styles.bold);

        Row srSupRow = sheet.createRow(rowIdx++);
        createCell(srSupRow, 0, "Name of Sr. Supervisor: ________________", styles.bold);
        createCell(srSupRow, lastCol, "Signature: ________________", styles.bold);

        autoSizeColumns(sheet, totalCols);
    }

    // ===== Marksheet Report =====

    public byte[] generateMarksheetReportExcel(MarksheetReportDTO report, LocalDate date, int blankColumns) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            createMarksheetSheet(workbook, styles, report, date, blankColumns);
            return toBytes(workbook);
        }
    }

    public byte[] generateAllMarksheetReportExcel(List<MarksheetReportDTO> reports, LocalDate date, int blankColumns) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            for (MarksheetReportDTO report : reports) {
                createMarksheetSheet(workbook, styles, report, date, blankColumns);
            }
            return toBytes(workbook);
        }
    }

    private void createMarksheetSheet(Workbook workbook, Styles styles, MarksheetReportDTO report,
            LocalDate date, int blankColumns) {
        String sheetName = getUniqueSheetName(workbook, report.getSubject());
        Sheet sheet = workbook.createSheet(sheetName);

        int totalCols = 2 + blankColumns + 2; // Sr No, Seat Number, blanks..., Total, In Words
        int lastCol = totalCols - 1;

        int rowIdx = 0;

        // College header (hardcoded to match PDF)
        Row line1Row = sheet.createRow(rowIdx);
        createCell(line1Row, 0, "SHRI SIDH THAKURNATH COLLEGE OF ARTS & COMMERCE", styles.title);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
        rowIdx++;

        Row line2Row = sheet.createRow(rowIdx);
        createCell(line2Row, 0, "(Affiliated to University of Mumbai, Mumbai)", styles.centeredNoBorder);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
        rowIdx++;

        Row line3Row = sheet.createRow(rowIdx);
        createCell(line3Row, 0, "ULHASNAGAR - 421 004. Dist. Thane", styles.centeredNoBorder);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
        rowIdx++;

        Row titleRow = sheet.createRow(rowIdx);
        createCell(titleRow, 0, "Marklist", styles.underlineTitle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
        rowIdx += 2;

        // Exam details
        Row deptRow = sheet.createRow(rowIdx++);
        createCell(deptRow, 0, "Department: " + report.getDepartment(), styles.bold);

        int midCol = totalCols / 2;
        Row classRow = sheet.createRow(rowIdx++);
        createCell(classRow, 0, "Class: " + report.getClassName(), styles.bold);
        createCell(classRow, midCol, "Subject: " + report.getSubject(), styles.bold);

        Row semRow = sheet.createRow(rowIdx++);
        createCell(semRow, 0, "Date: _______", styles.bold);
        createCell(semRow, midCol, "SEM: _______", styles.bold);

        String seatNoRange = "_______";
        if (!report.getStudents().isEmpty()) {
            String first = report.getStudents().get(0).getSeatNo();
            String last = report.getStudents().get(report.getStudents().size() - 1).getSeatNo();
            seatNoRange = first + " to " + last;
        }

        Row totalRow = sheet.createRow(rowIdx++);
        createCell(totalRow, 0, "Total No. in Block: " + report.getTotalStudents(), styles.bold);
        createCell(totalRow, midCol, "Seat No.: " + seatNoRange, styles.bold);

        Row presentRow = sheet.createRow(rowIdx++);
        createCell(presentRow, 0, "Total No. of Candidates Present: _______", styles.bold);
        createCell(presentRow, midCol, "Total No. of Candidates Absent: _______", styles.bold);

        rowIdx++; // blank row

        // Table headers
        Row tableHeaderRow = sheet.createRow(rowIdx++);
        int colIdx = 0;
        createCell(tableHeaderRow, colIdx++, "Sr No", styles.header);
        createCell(tableHeaderRow, colIdx++, "Seat Number", styles.header);
        for (int i = 0; i < blankColumns; i++) {
            createCell(tableHeaderRow, colIdx++, "", styles.header);
        }
        createCell(tableHeaderRow, colIdx++, "Total", styles.header);
        createCell(tableHeaderRow, colIdx, "In Words", styles.header);

        // Student data
        int totalStudents = report.getStudents().size();
        int rowCount = Math.max(40, totalStudents);

        for (int i = 0; i < rowCount; i++) {
            Row dataRow = sheet.createRow(rowIdx++);
            colIdx = 0;
            createNumericCell(dataRow, colIdx++, i + 1, styles.centered);

            if (i < totalStudents) {
                createCell(dataRow, colIdx++, report.getStudents().get(i).getSeatNo(), styles.centered);
            } else {
                createCell(dataRow, colIdx++, "", styles.centered);
            }

            for (int j = 0; j < blankColumns; j++) {
                createCell(dataRow, colIdx++, "", styles.centered);
            }
            createCell(dataRow, colIdx++, "", styles.centered); // Total
            createCell(dataRow, colIdx, "", styles.centered);   // In Words
        }

        rowIdx++; // blank row

        // Footer
        Row examinerRow = sheet.createRow(rowIdx++);
        createCell(examinerRow, 0, "Name of Examiner: __________", styles.bold);
        createCell(examinerRow, lastCol, "Signature with date: ___________", styles.bold);

        Row checkedRow = sheet.createRow(rowIdx++);
        createCell(checkedRow, 0, "Checked by: _________", styles.bold);
        createCell(checkedRow, lastCol, "Signature with date: ___________", styles.bold);

        autoSizeColumns(sheet, totalCols);
    }

    // ===== Helper Methods =====

    private int addCollegeHeader(Sheet sheet, int rowIdx, Styles styles, int mergeWidth) {
        if (reportConfig.getLine1() != null && !reportConfig.getLine1().isEmpty()) {
            Row row = sheet.createRow(rowIdx);
            createCell(row, 0, reportConfig.getLine1(), styles.title);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, mergeWidth - 1));
            rowIdx++;
        }
        if (reportConfig.getLine2() != null && !reportConfig.getLine2().isEmpty()) {
            Row row = sheet.createRow(rowIdx);
            createCell(row, 0, reportConfig.getLine2(), styles.bold);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, mergeWidth - 1));
            rowIdx++;
        }
        rowIdx++; // blank row after header
        return rowIdx;
    }

    private Cell createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        return cell;
    }

    private Cell createNumericCell(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        return cell;
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String getUniqueSheetName(Workbook workbook, String baseName) {
        String name = sanitizeSheetName(baseName);
        if (workbook.getSheet(name) == null) return name;

        String base = name.length() > 28 ? name.substring(0, 28) : name;
        for (int i = 2; i <= 99; i++) {
            String candidate = base + " (" + i + ")";
            if (workbook.getSheet(candidate) == null) return candidate;
        }
        return base + " (x)";
    }

    private String sanitizeSheetName(String name) {
        String sanitized = name.replaceAll("[\\\\/?*\\[\\]:]", "_");
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31);
        }
        return sanitized;
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return outputStream.toByteArray();
    }

    // ===== Styles =====

    private Styles createStyles(Workbook workbook) {
        return new Styles(
                createHeaderStyle(workbook),
                createTitleStyle(workbook),
                createUnderlineTitleStyle(workbook),
                createBoldStyle(workbook),
                createNormalStyle(workbook),
                createCenteredStyle(workbook),
                createCenteredNoBorderStyle(workbook)
        );
    }

    private record Styles(CellStyle header, CellStyle title, CellStyle underlineTitle,
                           CellStyle bold, CellStyle normal, CellStyle centered, CellStyle centeredNoBorder) {}

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createUnderlineTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setUnderline(Font.U_SINGLE);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private CellStyle createNormalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private CellStyle createCenteredStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCenteredNoBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}
