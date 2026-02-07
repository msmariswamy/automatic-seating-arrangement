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

    public byte[] generateRoomReportExcel(RoomReportDTO report, LocalDate date, boolean showSubject) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            createRoomSheet(workbook, styles, report, date, showSubject);
            return toBytes(workbook);
        }
    }

    public byte[] generateAllRoomsReportExcel(List<RoomReportDTO> reports, LocalDate date, boolean showSubject) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            for (RoomReportDTO report : reports) {
                createRoomSheet(workbook, styles, report, date, showSubject);
            }
            return toBytes(workbook);
        }
    }

    private void createRoomSheet(Workbook workbook, Styles styles, RoomReportDTO report, LocalDate date, boolean showSubject) {
        String sheetName = getUniqueSheetName(workbook, "Room " + report.getRoomNo());
        Sheet sheet = workbook.createSheet(sheetName);

        // Column layout depends on showSubject
        // Without subject: Seat,Roll | gap | Seat,Roll | gap | Seat,Roll  (cols per section=2, gap=1)
        // With subject:    Seat,Roll,Subject | gap | Seat,Roll,Subject | gap | Seat,Roll,Subject
        int colsPerSection = showSubject ? 3 : 2;
        int sectionGap = 1;
        int rOffset = 0;
        int mOffset = colsPerSection + sectionGap;
        int lOffset = 2 * (colsPerSection + sectionGap);
        int totalCols = lOffset + colsPerSection;

        int rowIdx = 0;

        // College header
        rowIdx = addCollegeHeader(sheet, rowIdx, styles, totalCols);

        // Title
        Row titleRow = sheet.createRow(rowIdx);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Individual Room Report");
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, totalCols - 1));
        rowIdx += 2;

        // Room info
        Row roomRow = sheet.createRow(rowIdx++);
        createCell(roomRow, 0, "Room No: " + report.getRoomNo(), styles.bold);
        createCell(roomRow, mOffset, "Date: " + date, styles.bold);

        Row deptRow = sheet.createRow(rowIdx++);
        createCell(deptRow, 0, "Departments: " + String.join(", ", report.getDepartments()), styles.normal);

        Row subjRow = sheet.createRow(rowIdx++);
        createCell(subjRow, 0, "Subjects: " + String.join(", ", report.getSubjects()), styles.normal);

        rowIdx++; // blank row

        // Section headers
        Row sectionRow = sheet.createRow(rowIdx);
        for (int c = rOffset; c < rOffset + colsPerSection; c++)
            createCell(sectionRow, c, "", styles.header);
        sectionRow.getCell(rOffset).setCellValue("Right Seats (R)");
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, rOffset, rOffset + colsPerSection - 1));

        for (int c = mOffset; c < mOffset + colsPerSection; c++)
            createCell(sectionRow, c, "", styles.header);
        sectionRow.getCell(mOffset).setCellValue("Middle Seats (M)");
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, mOffset, mOffset + colsPerSection - 1));

        for (int c = lOffset; c < lOffset + colsPerSection; c++)
            createCell(sectionRow, c, "", styles.header);
        sectionRow.getCell(lOffset).setCellValue("Left Seats (L)");
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, lOffset, lOffset + colsPerSection - 1));
        rowIdx++;

        // Sub-headers
        Row subHeaderRow = sheet.createRow(rowIdx++);
        int[] offsets = {rOffset, mOffset, lOffset};
        for (int offset : offsets) {
            createCell(subHeaderRow, offset, "Seat", styles.header);
            createCell(subHeaderRow, offset + 1, "Roll No", styles.header);
            if (showSubject) {
                createCell(subHeaderRow, offset + 2, "Subject", styles.header);
            }
        }

        // Data rows
        int rightSize = report.getRightSeats() != null ? report.getRightSeats().size() : 0;
        int middleSize = report.getMiddleSeats() != null ? report.getMiddleSeats().size() : 0;
        int leftSize = report.getLeftSeats() != null ? report.getLeftSeats().size() : 0;
        int maxRows = Math.max(Math.max(rightSize, middleSize), leftSize);

        for (int i = 0; i < maxRows; i++) {
            Row dataRow = sheet.createRow(rowIdx + i);

            // Right seats
            Cell rSeat = createCell(dataRow, rOffset, "", styles.centered);
            Cell rRoll = createCell(dataRow, rOffset + 1, "", styles.centered);
            Cell rSubj = showSubject ? createCell(dataRow, rOffset + 2, "", styles.centered) : null;
            if (i < rightSize) {
                SeatAllocationDTO seat = report.getRightSeats().get(i);
                rSeat.setCellValue(seat.getSeatNo());
                rRoll.setCellValue(seat.getRollNo());
                if (rSubj != null && seat.getSubject() != null) rSubj.setCellValue(seat.getSubject());
            }

            // Middle seats
            Cell mSeat = createCell(dataRow, mOffset, "", styles.centered);
            Cell mRoll = createCell(dataRow, mOffset + 1, "", styles.centered);
            Cell mSubj = showSubject ? createCell(dataRow, mOffset + 2, "", styles.centered) : null;
            if (i < middleSize) {
                SeatAllocationDTO seat = report.getMiddleSeats().get(i);
                mSeat.setCellValue(seat.getSeatNo());
                mRoll.setCellValue(seat.getRollNo());
                if (mSubj != null && seat.getSubject() != null) mSubj.setCellValue(seat.getSubject());
            }

            // Left seats
            Cell lSeat = createCell(dataRow, lOffset, "", styles.centered);
            Cell lRoll = createCell(dataRow, lOffset + 1, "", styles.centered);
            Cell lSubj = showSubject ? createCell(dataRow, lOffset + 2, "", styles.centered) : null;
            if (i < leftSize) {
                SeatAllocationDTO seat = report.getLeftSeats().get(i);
                lSeat.setCellValue(seat.getSeatNo());
                lRoll.setCellValue(seat.getRollNo());
                if (lSubj != null && seat.getSubject() != null) lSubj.setCellValue(seat.getSubject());
            }
        }

        autoSizeColumns(sheet, totalCols);
    }

    // ===== Consolidated Report =====

    public byte[] generateConsolidatedReportExcel(List<ConsolidatedReportDTO> report, LocalDate date,
            boolean showAllRollNumbers) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            Sheet sheet = workbook.createSheet("Consolidated Report");

            int totalCols = showAllRollNumbers ? 5 : 6;

            int rowIdx = 0;
            rowIdx = addCollegeHeader(sheet, rowIdx, styles, totalCols);

            Row titleRow = sheet.createRow(rowIdx);
            createCell(titleRow, 0, "Consolidated Report", styles.title);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, totalCols - 1));
            rowIdx += 2;

            Row dateRow = sheet.createRow(rowIdx++);
            createCell(dateRow, 0, "Date: " + date, styles.bold);
            rowIdx++;

            // Table headers
            Row headerRow = sheet.createRow(rowIdx++);
            if (showAllRollNumbers) {
                String[] headers = {"Sr No", "Room No", "Department", "Roll Numbers", "Total Count"};
                for (int i = 0; i < headers.length; i++) {
                    createCell(headerRow, i, headers[i], styles.header);
                }
            } else {
                String[] headers = {"Sr No", "Room No", "Department", "Roll No From", "Roll No To", "Total Count"};
                for (int i = 0; i < headers.length; i++) {
                    createCell(headerRow, i, headers[i], styles.header);
                }
            }

            // Data
            int srNo = 1;
            for (ConsolidatedReportDTO row : report) {
                Row dataRow = sheet.createRow(rowIdx++);
                if (showAllRollNumbers) {
                    createNumericCell(dataRow, 0, srNo++, styles.centered);
                    createCell(dataRow, 1, row.getRoomNo(), styles.centered);
                    createCell(dataRow, 2, row.getDepartment(), styles.centered);
                    String allRolls = row.getAllRollNumbers() != null
                            ? String.join(", ", row.getAllRollNumbers()) : "";
                    createCell(dataRow, 3, allRolls, styles.centeredWrap);
                    createNumericCell(dataRow, 4, row.getTotalCount(), styles.centered);
                } else {
                    createNumericCell(dataRow, 0, srNo++, styles.centered);
                    createCell(dataRow, 1, row.getRoomNo(), styles.centered);
                    createCell(dataRow, 2, row.getDepartment(), styles.centered);
                    createCell(dataRow, 3, row.getRollNoFrom(), styles.centered);
                    createCell(dataRow, 4, row.getRollNoTo(), styles.centered);
                    createNumericCell(dataRow, 5, row.getTotalCount(), styles.centered);
                }
            }

            autoSizeColumns(sheet, totalCols);
            return toBytes(workbook);
        }
    }

    // ===== Department Consolidated Report =====

    public byte[] generateDepartmentConsolidatedReportExcel(List<DepartmentConsolidatedReportDTO> report, LocalDate date,
            boolean showAllRollNumbers) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);
            Sheet sheet = workbook.createSheet("Dept Consolidated Report");

            int totalCols = showAllRollNumbers ? 5 : 6;

            int rowIdx = 0;
            rowIdx = addCollegeHeader(sheet, rowIdx, styles, totalCols);

            Row titleRow = sheet.createRow(rowIdx);
            createCell(titleRow, 0, "Department Consolidated Report", styles.title);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, totalCols - 1));
            rowIdx += 2;

            Row dateRow = sheet.createRow(rowIdx++);
            createCell(dateRow, 0, "Date: " + date, styles.bold);
            rowIdx++;

            Row headerRow = sheet.createRow(rowIdx++);
            if (showAllRollNumbers) {
                String[] headers = {"Department", "Class", "Room No", "Roll Numbers", "Total Count"};
                for (int i = 0; i < headers.length; i++) {
                    createCell(headerRow, i, headers[i], styles.header);
                }
            } else {
                String[] headers = {"Department", "Class", "Room No", "From Seat No", "To Seat No", "Total Count"};
                for (int i = 0; i < headers.length; i++) {
                    createCell(headerRow, i, headers[i], styles.header);
                }
            }

            for (DepartmentConsolidatedReportDTO row : report) {
                Row dataRow = sheet.createRow(rowIdx++);
                if (showAllRollNumbers) {
                    createCell(dataRow, 0, row.getDepartment(), styles.centered);
                    createCell(dataRow, 1, row.getClassName(), styles.centered);
                    createCell(dataRow, 2, row.getRoomNo(), styles.centered);
                    String allRolls = row.getAllRollNumbers() != null
                            ? String.join(", ", row.getAllRollNumbers()) : "";
                    createCell(dataRow, 3, allRolls, styles.centeredWrap);
                    createNumericCell(dataRow, 4, row.getTotalCount(), styles.centered);
                } else {
                    createCell(dataRow, 0, row.getDepartment(), styles.centered);
                    createCell(dataRow, 1, row.getClassName(), styles.centered);
                    createCell(dataRow, 2, row.getRoomNo(), styles.centered);
                    createCell(dataRow, 3, row.getFromSeat() != null ? row.getFromSeat() : "", styles.centered);
                    createCell(dataRow, 4, row.getToSeat() != null ? row.getToSeat() : "", styles.centered);
                    createNumericCell(dataRow, 5, row.getTotalCount(), styles.centered);
                }
            }

            autoSizeColumns(sheet, totalCols);
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
        String sheetName = getUniqueSheetName(workbook, report.getRoomNo() + "-" + report.getDepartment() + "-" + report.getClassName() + "-" + report.getSubject());
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
        String sheetName = getUniqueSheetName(workbook, report.getDepartment() + " - " + report.getClassName());
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

        for (int i = 2; i <= 99; i++) {
            String suffix = " (" + i + ")";
            int maxBaseLen = 31 - suffix.length();
            String base = name.length() > maxBaseLen ? name.substring(0, maxBaseLen) : name;
            String candidate = base + suffix;
            if (workbook.getSheet(candidate) == null) return candidate;
        }
        return name.substring(0, 26) + " (xx)";
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
                createCenteredNoBorderStyle(workbook),
                createCenteredWrapStyle(workbook)
        );
    }

    private record Styles(CellStyle header, CellStyle title, CellStyle underlineTitle,
                           CellStyle bold, CellStyle normal, CellStyle centered,
                           CellStyle centeredNoBorder, CellStyle centeredWrap) {}

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

    private CellStyle createCenteredWrapStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
}
