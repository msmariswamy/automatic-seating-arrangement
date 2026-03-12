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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportExcelService {

    private final ReportConfig reportConfig;

    // ===== Room Report =====

    public byte[] generateRoomReportExcel(RoomReportDTO report, LocalDate date, String time) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            RoomStyles rs = createRoomStyles(workbook);
            createRoomSheet(workbook, rs, report, date, time);
            return toBytes(workbook);
        }
    }

    public byte[] generateAllRoomsReportExcel(List<RoomReportDTO> reports, LocalDate date, String time) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            RoomStyles rs = createRoomStyles(workbook);
            for (RoomReportDTO report : reports) {
                createRoomSheet(workbook, rs, report, date, time);
            }
            return toBytes(workbook);
        }
    }

    private void createRoomSheet(Workbook workbook, RoomStyles rs, RoomReportDTO report, LocalDate date, String time) {
        String sheetName = getUniqueSheetName(workbook, "Room " + report.getRoomNo());
        Sheet sheet = workbook.createSheet(sheetName);

        List<SeatAllocationDTO> rSeats = report.getRightSeats() != null ? report.getRightSeats() : List.of();
        List<SeatAllocationDTO> mSeats = report.getMiddleSeats() != null ? report.getMiddleSeats() : List.of();
        List<SeatAllocationDTO> lSeats = report.getLeftSeats() != null ? report.getLeftSeats() : List.of();
        boolean hasMiddle = !mSeats.isEmpty();

        // Column layout: 2-section (5 cols) or 3-section (8 cols)
        int rSeatCol = 0, rRollCol = 1, gapCol1 = 2;
        int mSeatCol = -1, mRollCol = -1, gapCol2 = -1;
        int lSeatCol, lRollCol, totalCols;

        if (hasMiddle) {
            mSeatCol = 3; mRollCol = 4; gapCol2 = 5;
            lSeatCol = 6; lRollCol = 7;
            totalCols = 8;
        } else {
            lSeatCol = 3; lRollCol = 4;
            totalCols = 5;
        }

        // Set column widths
        for (int i = 0; i < totalCols; i++) {
            if (i == gapCol1 || i == gapCol2) {
                sheet.setColumnWidth(i, (int) (3.0 * 256));
            } else {
                sheet.setColumnWidth(i, (int) (13.0 * 256));
            }
        }

        int lastCol = totalCols - 1;
        int rowIdx = 0;

        // Row 1: College name
        Row row1 = sheet.createRow(rowIdx);
        createCell(row1, 0, reportConfig.getLine1() != null ? reportConfig.getLine1() : "", rs.bold13Center);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
        rowIdx++;

        // Row 2: Affiliation
        Row row2 = sheet.createRow(rowIdx);
        createCell(row2, 0, reportConfig.getLine2() != null ? reportConfig.getLine2() : "", rs.bold13Center);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
        rowIdx++;

        // Row 3: blank
        rowIdx++;

        // Row 4: Report title
        Row row4 = sheet.createRow(rowIdx);
        createCell(row4, 0, "Individual Room Report", rs.bold13Center);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
        rowIdx++;

        // Row 5: blank
        rowIdx++;

        // Row 6: Room No + Exam Time (3-section: exam time at F6)
        Row row6 = sheet.createRow(rowIdx);
        createCell(row6, 0, "Room No: " + report.getRoomNo(), rs.bold13);
        if (hasMiddle && time != null && !time.isEmpty()) {
            createCell(row6, gapCol2, "Exam Time :- " + time, rs.bold13);
        }
        rowIdx++;

        // Row 7: Departments + Exam Time (2-section: exam time at D7)
        Row row7 = sheet.createRow(rowIdx);
        createCell(row7, 0, "Departments: " + String.join(", ", report.getDepartments()), rs.normal13);
        if (!hasMiddle && time != null && !time.isEmpty()) {
            createCell(row7, lSeatCol, "Exam Time :- " + time, rs.bold13);
        }
        rowIdx++;

        // Row 8: blank
        rowIdx++;

        // Build section display rows (with inline department transition labels)
        List<SectionRow> rItems = buildSectionRows(rSeats);
        List<SectionRow> mItems = hasMiddle ? buildSectionRows(mSeats) : List.of();
        List<SectionRow> lItems = buildSectionRows(lSeats);

        // Row 9: Initial department labels per section
        Row deptLabelRow = sheet.createRow(rowIdx);
        String rDept = rSeats.isEmpty() ? "" : rSeats.get(0).getDepartment();
        createCell(deptLabelRow, rSeatCol, rDept, rs.bold13Center);
        createCell(deptLabelRow, rRollCol, "", rs.bold13Center);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, rSeatCol, rRollCol));

        if (hasMiddle) {
            String mDept = mSeats.isEmpty() ? "" : mSeats.get(0).getDepartment();
            createCell(deptLabelRow, mSeatCol, mDept, rs.bold13Center);
            createCell(deptLabelRow, mRollCol, "", rs.bold13Center);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, mSeatCol, mRollCol));
        }

        String lDept = lSeats.isEmpty() ? "" : lSeats.get(0).getDepartment();
        createCell(deptLabelRow, lSeatCol, lDept, rs.bold13Center);
        createCell(deptLabelRow, lRollCol, "", rs.bold13Center);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, lSeatCol, lRollCol));
        rowIdx++;

        // Row 10: Position headers
        Row posRow = sheet.createRow(rowIdx);
        createCell(posRow, rSeatCol, "Right Seats (R)", rs.sectionHeader);
        createCell(posRow, rRollCol, "", rs.sectionHeader);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, rSeatCol, rRollCol));

        if (hasMiddle) {
            createCell(posRow, mSeatCol, "Middle Seats (M)", rs.sectionHeader);
            createCell(posRow, mRollCol, "", rs.sectionHeader);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, mSeatCol, mRollCol));
        }

        createCell(posRow, lSeatCol, "Left Seats (L)", rs.sectionHeader);
        createCell(posRow, lRollCol, "", rs.sectionHeader);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, lSeatCol, lRollCol));
        rowIdx++;

        // Row 11: Column headers (Seat / Roll No)
        Row colHdrRow = sheet.createRow(rowIdx);
        createCell(colHdrRow, rSeatCol, "Seat", rs.colHeader);
        createCell(colHdrRow, rRollCol, "Roll No", rs.colHeader);
        if (hasMiddle) {
            createCell(colHdrRow, mSeatCol, "Seat", rs.colHeader);
            createCell(colHdrRow, mRollCol, "Roll No", rs.colHeader);
        }
        createCell(colHdrRow, lSeatCol, "Seat", rs.colHeader);
        createCell(colHdrRow, lRollCol, "Roll No", rs.colHeader);
        rowIdx++;

        // Data rows
        int maxItems = Math.max(rItems.size(), Math.max(mItems.size(), lItems.size()));

        for (int i = 0; i < maxItems; i++) {
            Row dataRow = sheet.createRow(rowIdx);
            writeSectionCell(sheet, dataRow, rowIdx, rSeatCol, rRollCol, i, rItems, rs);
            if (hasMiddle) {
                writeSectionCell(sheet, dataRow, rowIdx, mSeatCol, mRollCol, i, mItems, rs);
            }
            writeSectionCell(sheet, dataRow, rowIdx, lSeatCol, lRollCol, i, lItems, rs);
            rowIdx++;
        }
    }

    private void writeSectionCell(Sheet sheet, Row row, int rowIdx, int seatCol, int rollCol,
                                  int itemIdx, List<SectionRow> items, RoomStyles rs) {
        if (itemIdx < items.size()) {
            SectionRow sr = items.get(itemIdx);
            if (sr.isLabel()) {
                createCell(row, seatCol, sr.deptLabel(), rs.deptLabel);
                createCell(row, rollCol, "", rs.deptLabel);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, seatCol, rollCol));
            } else {
                createCell(row, seatCol, sr.seatNo(), rs.data);
                createCell(row, rollCol, sr.rollNo(), rs.data);
            }
        } else {
            createCell(row, seatCol, "", rs.data);
            createCell(row, rollCol, "", rs.data);
        }
    }

    private record SectionRow(String seatNo, String rollNo, String deptLabel) {
        boolean isLabel() { return deptLabel != null; }
        static SectionRow data(String seatNo, String rollNo) { return new SectionRow(seatNo, rollNo, null); }
        static SectionRow label(String dept) { return new SectionRow(null, null, dept); }
    }

    private List<SectionRow> buildSectionRows(List<SeatAllocationDTO> seats) {
        List<SectionRow> items = new ArrayList<>();
        if (seats.isEmpty()) return items;
        String currentDept = seats.get(0).getDepartment();
        for (SeatAllocationDTO seat : seats) {
            if (!seat.getDepartment().equals(currentDept)) {
                items.add(SectionRow.label(seat.getDepartment()));
                currentDept = seat.getDepartment();
            }
            items.add(SectionRow.data(seat.getSeatNo(), seat.getRollNo()));
        }
        return items;
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
            String time, String sem, String examType) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            JsStyles jsStyles = createJsStyles(workbook);
            createJuniorSupervisorSheet(workbook, jsStyles, report, date, time, sem, examType);
            return toBytes(workbook);
        }
    }

    public byte[] generateAllJuniorSupervisorReportExcel(List<JuniorSupervisorReportDTO> reports, LocalDate date,
            String time, String sem, String examType) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            JsStyles jsStyles = createJsStyles(workbook);
            // Merge reports with the same roomNo+department into a single sheet
            List<JuniorSupervisorReportDTO> merged = mergeJuniorSupervisorReports(reports);
            for (JuniorSupervisorReportDTO report : merged) {
                createJuniorSupervisorSheet(workbook, jsStyles, report, date, time, sem, examType);
            }
            return toBytes(workbook);
        }
    }

    /**
     * Merges junior supervisor reports that share the same roomNo + department.
     * Combines student lists and sorts by roll number within each merged group.
     */
    private List<JuniorSupervisorReportDTO> mergeJuniorSupervisorReports(List<JuniorSupervisorReportDTO> reports) {
        // Group by roomNo + department
        Map<String, List<JuniorSupervisorReportDTO>> grouped = reports.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getRoomNo() + "|||" + r.getDepartment(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<JuniorSupervisorReportDTO> merged = new ArrayList<>();
        for (List<JuniorSupervisorReportDTO> group : grouped.values()) {
            JuniorSupervisorReportDTO first = group.get(0);
            if (group.size() == 1) {
                merged.add(first);
                continue;
            }
            // Merge all student lists and sort by roll number
            List<JuniorSupervisorReportDTO.StudentEntry> allStudents = new ArrayList<>();
            for (JuniorSupervisorReportDTO r : group) {
                allStudents.addAll(r.getStudents());
            }
            allStudents.sort(Comparator.comparing(JuniorSupervisorReportDTO.StudentEntry::getRollNo));
            // Renumber sr. no.
            for (int i = 0; i < allStudents.size(); i++) {
                allStudents.get(i).setSrNo(i + 1);
            }
            merged.add(JuniorSupervisorReportDTO.builder()
                    .roomId(first.getRoomId())
                    .roomNo(first.getRoomNo())
                    .department(first.getDepartment())
                    .className(first.getClassName())
                    .subject(first.getSubject())
                    .totalStudents(allStudents.size())
                    .students(allStudents)
                    .build());
        }
        return merged;
    }

    private void createJuniorSupervisorSheet(Workbook workbook, JsStyles js, JuniorSupervisorReportDTO report,
            LocalDate date, String time, String sem, String examType) {
        String sheetName = getUniqueSheetName(workbook, report.getRoomNo() + "-" + report.getDepartment());
        Sheet sheet = workbook.createSheet(sheetName);

        // Set column widths (units: 1/256 of a character width)
        sheet.setColumnWidth(0, (int)(4.63 * 256));   // A - Sr. No.
        sheet.setColumnWidth(1, (int)(21.13 * 256));   // B - Seat No.
        sheet.setColumnWidth(2, (int)(15.5 * 256));    // C - Signature
        sheet.setColumnWidth(3, (int)(1.75 * 256));    // D - Separator
        sheet.setColumnWidth(4, (int)(4.63 * 256));    // E - Sr. No.
        sheet.setColumnWidth(5, (int)(20.25 * 256));   // F - Seat No.
        sheet.setColumnWidth(6, (int)(16.0 * 256));    // G - Signature

        int rowIdx = 0;

        // Row 1: College name
        Row row1 = sheet.createRow(rowIdx);
        row1.setHeightInPoints(29.25f);
        createCell(row1, 0, "SHRI SIDH THAKURNATH COLLEGE OF ARTS & COMMERCE", js.collegeName);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 6));
        rowIdx++;

        // Row 2: Affiliation
        Row row2 = sheet.createRow(rowIdx);
        row2.setHeightInPoints(21.75f);
        createCell(row2, 0, "(Affiliated to University of Mumbai, Mumbai)", js.affiliation);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 6));
        rowIdx++;

        // Row 3: Address
        Row row3 = sheet.createRow(rowIdx);
        row3.setHeightInPoints(18.75f);
        createCell(row3, 0, "ULHASNAGAR - 421 004. Dist. Thane", js.address);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 6));
        rowIdx++;

        // Row 4: Report title
        Row row4 = sheet.createRow(rowIdx);
        row4.setHeightInPoints(21.75f);
        createCell(row4, 0, "Junior Supervisor's Report", js.reportTitle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 6));
        rowIdx++;

        // Row 5: Empty spacer
        Row row5 = sheet.createRow(rowIdx);
        row5.setHeightInPoints(9.75f);
        rowIdx++;

        // Row 6: Block No., Room No., Date, Time
        Row row6 = sheet.createRow(rowIdx);
        row6.setHeightInPoints(33.0f);
        createCell(row6, 0, "Block No.:  ", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
        createCell(row6, 2, "Room No.:  " + report.getRoomNo(), js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 2, 4));
        String dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        createCell(row6, 5, "Date: " + dateStr, js.fieldLabel);
        createCell(row6, 6, "Time: " + time, js.fieldLabel);
        rowIdx++;

        // Row 7: Empty spacer
        Row row7 = sheet.createRow(rowIdx);
        row7.setHeightInPoints(3.75f);
        rowIdx++;

        // Row 8: CLASS, COURSE, SEM, Exam
        Row row8 = sheet.createRow(rowIdx);
        row8.setHeightInPoints(33.0f);
        createCell(row8, 0, "CLASS: " + report.getDepartment(), js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
        createCell(row8, 2, "COURSE: " + report.getSubject(), js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 2, 4));
        createCell(row8, 5, "SEM: " + sem, js.fieldLabel);
        createCell(row8, 6, "Exam: " + examType, js.fieldLabel);
        rowIdx++;

        // Row 9: Empty spacer
        Row row9 = sheet.createRow(rowIdx);
        row9.setHeightInPoints(1.5f);
        rowIdx++;

        // Row 10: Total No. in Block, Seat No.
        Row row10 = sheet.createRow(rowIdx);
        row10.setHeightInPoints(25.5f);
        createCell(row10, 0, "Total No. in the Block: ", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 2));
        createCell(row10, 4, " Seat No.: ", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 4, 6));
        rowIdx++;

        // Row 11: Empty spacer
        Row row11 = sheet.createRow(rowIdx);
        row11.setHeightInPoints(6.0f);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 6));
        rowIdx++;

        // Row 12: Present/Absent
        Row row12 = sheet.createRow(rowIdx);
        row12.setHeightInPoints(22.5f);
        createCell(row12, 0, "Total No. of Candidates Present: ", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 2));
        createCell(row12, 4, "Total No. of Candidates Absent: ", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 4, 6));
        rowIdx++;

        // Row 13: Empty spacer
        Row row13 = sheet.createRow(rowIdx);
        row13.setHeightInPoints(9.75f);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 6));
        rowIdx++;

        // Row 14: Table headers (two-column layout)
        Row headerRow = sheet.createRow(rowIdx);
        headerRow.setHeightInPoints(49.5f);
        createCell(headerRow, 0, "Sr. No.", js.tableHeader);
        createCell(headerRow, 1, "Examination Seat No.", js.tableHeader);
        createCell(headerRow, 2, "Signature of the Candidate", js.tableHeader);
        createCell(headerRow, 3, "", js.tableHeader);
        createCell(headerRow, 4, "Sr. No.", js.tableHeader);
        createCell(headerRow, 5, "Examination Seat No.", js.tableHeader);
        createCell(headerRow, 6, "Signature of the Candidate", js.tableHeader);
        rowIdx++;

        // Data rows: 24 rows, students split left (1-24) and right (25-48)
        int studentsPerSide = 24;
        List<JuniorSupervisorReportDTO.StudentEntry> students = report.getStudents();

        if (students.size() > studentsPerSide * 2) {
            log.warn("Junior Supervisor Report for Room {}, Dept {} has {} students but only {} fit in template. Extra students truncated.",
                    report.getRoomNo(), report.getDepartment(), students.size(), studentsPerSide * 2);
        }

        for (int i = 0; i < studentsPerSide; i++) {
            Row dataRow = sheet.createRow(rowIdx);
            dataRow.setHeightInPoints(i == studentsPerSide - 1 ? 24.75f : 24.0f);

            // Left side (students 0 to 23)
            createNumericCell(dataRow, 0, i + 1, js.srNo);
            if (i < students.size()) {
                createCell(dataRow, 1, students.get(i).getRollNo(), js.seatNo);
            } else {
                createCell(dataRow, 1, "", js.seatNo);
            }
            createCell(dataRow, 2, "", js.bordered);

            // Right side (students 24 to 47)
            int rightIdx = i + studentsPerSide;
            createNumericCell(dataRow, 4, rightIdx + 1, js.srNo);
            if (rightIdx < students.size()) {
                createCell(dataRow, 5, students.get(rightIdx).getRollNo(), js.seatNo);
            } else {
                createCell(dataRow, 5, "", js.seatNo);
            }
            createCell(dataRow, 6, "", js.bordered);

            rowIdx++;
        }

        // Footer: Total Supplements
        Row suppRow = sheet.createRow(rowIdx);
        suppRow.setHeightInPoints(24.75f);
        createCell(suppRow, 0, "Total No. of Suppliments Used :", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 6));
        rowIdx++;

        // Empty row
        Row emptyRow40 = sheet.createRow(rowIdx);
        emptyRow40.setHeightInPoints(11.25f);
        rowIdx++;

        // Jr. Supervisor
        Row jrRow = sheet.createRow(rowIdx);
        jrRow.setHeightInPoints(24.75f);
        createCell(jrRow, 0, "Name of Jr. Supervisor:", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 4));
        createCell(jrRow, 5, "Signature:", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 5, 6));
        rowIdx++;

        // Empty row
        Row emptyRow42 = sheet.createRow(rowIdx);
        emptyRow42.setHeightInPoints(7.5f);
        rowIdx++;

        // Checked by
        Row checkedRow = sheet.createRow(rowIdx);
        checkedRow.setHeightInPoints(24.75f);
        createCell(checkedRow, 0, "Checked by another Jr. Supervisor:", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 4));
        createCell(checkedRow, 5, "Signature:", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 5, 6));
        rowIdx++;

        // Empty row
        Row emptyRow44 = sheet.createRow(rowIdx);
        emptyRow44.setHeightInPoints(6.75f);
        rowIdx++;

        // Sr. Supervisor
        Row srRow = sheet.createRow(rowIdx);
        srRow.setHeightInPoints(24.75f);
        createCell(srRow, 0, "Name of Sr. Supervisor:", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 4));
        createCell(srRow, 5, "Signature:", js.fieldLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 5, 6));
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

    private static final String FONT_NAME = "Times New Roman";

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
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
        font.setFontName(FONT_NAME);
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createUnderlineTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setBold(true);
        font.setUnderline(Font.U_SINGLE);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createNormalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createCenteredStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCenteredNoBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createCenteredWrapStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
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

    // ===== Room Report Styles =====

    private record RoomStyles(CellStyle bold13, CellStyle bold13Center, CellStyle normal13,
                               CellStyle sectionHeader, CellStyle colHeader,
                               CellStyle data, CellStyle deptLabel) {}

    private RoomStyles createRoomStyles(Workbook workbook) {
        Font bold13Font = workbook.createFont();
        bold13Font.setFontName(FONT_NAME);
        bold13Font.setBold(true);
        bold13Font.setFontHeightInPoints((short) 13);

        Font normal13Font = workbook.createFont();
        normal13Font.setFontName(FONT_NAME);
        normal13Font.setFontHeightInPoints((short) 13);

        CellStyle bold13 = workbook.createCellStyle();
        bold13.setFont(bold13Font);

        CellStyle bold13Center = workbook.createCellStyle();
        bold13Center.setFont(bold13Font);
        bold13Center.setAlignment(HorizontalAlignment.CENTER);

        CellStyle normal13 = workbook.createCellStyle();
        normal13.setFont(normal13Font);

        CellStyle sectionHeader = workbook.createCellStyle();
        sectionHeader.setFont(bold13Font);
        sectionHeader.setAlignment(HorizontalAlignment.CENTER);
        sectionHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(sectionHeader);

        CellStyle colHeader = workbook.createCellStyle();
        colHeader.setFont(bold13Font);
        colHeader.setAlignment(HorizontalAlignment.CENTER);
        colHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(colHeader);

        CellStyle data = workbook.createCellStyle();
        data.setFont(normal13Font);
        data.setAlignment(HorizontalAlignment.CENTER);
        addThinBorders(data);

        CellStyle deptLabel = workbook.createCellStyle();
        deptLabel.setFont(bold13Font);
        deptLabel.setAlignment(HorizontalAlignment.CENTER);
        addThinBorders(deptLabel);

        return new RoomStyles(bold13, bold13Center, normal13, sectionHeader, colHeader, data, deptLabel);
    }

    private void addThinBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    // ===== Junior Supervisor Styles =====

    private record JsStyles(CellStyle collegeName, CellStyle affiliation, CellStyle address,
                             CellStyle reportTitle, CellStyle fieldLabel, CellStyle tableHeader,
                             CellStyle srNo, CellStyle seatNo, CellStyle bordered) {}

    private JsStyles createJsStyles(Workbook workbook) {
        String fontName = "Times New Roman";

        // College name: size 18, center
        CellStyle collegeName = workbook.createCellStyle();
        Font collegeFont = workbook.createFont();
        collegeFont.setFontName(fontName);
        collegeFont.setFontHeightInPoints((short) 18);
        collegeName.setFont(collegeFont);
        collegeName.setAlignment(HorizontalAlignment.CENTER);
        collegeName.setVerticalAlignment(VerticalAlignment.CENTER);

        // Affiliation: size 14, center
        CellStyle affiliation = workbook.createCellStyle();
        Font affFont = workbook.createFont();
        affFont.setFontName(fontName);
        affFont.setFontHeightInPoints((short) 14);
        affiliation.setFont(affFont);
        affiliation.setAlignment(HorizontalAlignment.CENTER);
        affiliation.setVerticalAlignment(VerticalAlignment.CENTER);

        // Address: size 12, bold, center
        CellStyle address = workbook.createCellStyle();
        Font addrFont = workbook.createFont();
        addrFont.setFontName(fontName);
        addrFont.setBold(true);
        addrFont.setFontHeightInPoints((short) 12);
        address.setFont(addrFont);
        address.setAlignment(HorizontalAlignment.CENTER);
        address.setVerticalAlignment(VerticalAlignment.CENTER);

        // Report title: size 14, bold, center
        CellStyle reportTitle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setFontName(fontName);
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        reportTitle.setFont(titleFont);
        reportTitle.setAlignment(HorizontalAlignment.CENTER);
        reportTitle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Field label: size 12, bold, left, vcenter
        CellStyle fieldLabel = workbook.createCellStyle();
        Font fieldFont = workbook.createFont();
        fieldFont.setFontName(fontName);
        fieldFont.setBold(true);
        fieldFont.setFontHeightInPoints((short) 12);
        fieldLabel.setFont(fieldFont);
        fieldLabel.setAlignment(HorizontalAlignment.LEFT);
        fieldLabel.setVerticalAlignment(VerticalAlignment.CENTER);

        // Table header: size 9, bold, center, vcenter, wrap, thin borders
        CellStyle tableHeader = workbook.createCellStyle();
        Font thFont = workbook.createFont();
        thFont.setFontName(fontName);
        thFont.setBold(true);
        thFont.setFontHeightInPoints((short) 9);
        tableHeader.setFont(thFont);
        tableHeader.setAlignment(HorizontalAlignment.CENTER);
        tableHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        tableHeader.setWrapText(true);
        tableHeader.setBorderTop(BorderStyle.THIN);
        tableHeader.setBorderBottom(BorderStyle.THIN);
        tableHeader.setBorderLeft(BorderStyle.THIN);
        tableHeader.setBorderRight(BorderStyle.THIN);

        // Sr. No.: size 10, bold, thin borders, center
        CellStyle srNo = workbook.createCellStyle();
        Font srFont = workbook.createFont();
        srFont.setFontName(fontName);
        srFont.setBold(true);
        srFont.setFontHeightInPoints((short) 10);
        srNo.setFont(srFont);
        srNo.setVerticalAlignment(VerticalAlignment.CENTER);
        srNo.setBorderTop(BorderStyle.THIN);
        srNo.setBorderBottom(BorderStyle.THIN);
        srNo.setBorderLeft(BorderStyle.THIN);
        srNo.setBorderRight(BorderStyle.THIN);

        // Seat No.: size 13, thin borders, center
        CellStyle seatNo = workbook.createCellStyle();
        Font seatFont = workbook.createFont();
        seatFont.setFontName(fontName);
        seatFont.setFontHeightInPoints((short) 13);
        seatNo.setFont(seatFont);
        seatNo.setVerticalAlignment(VerticalAlignment.CENTER);
        seatNo.setBorderTop(BorderStyle.THIN);
        seatNo.setBorderBottom(BorderStyle.THIN);
        seatNo.setBorderLeft(BorderStyle.THIN);
        seatNo.setBorderRight(BorderStyle.THIN);

        // Bordered: thin borders, center (for empty signature cells)
        CellStyle bordered = workbook.createCellStyle();
        Font borderedFont = workbook.createFont();
        borderedFont.setFontName(fontName);
        bordered.setFont(borderedFont);
        bordered.setVerticalAlignment(VerticalAlignment.CENTER);
        bordered.setBorderTop(BorderStyle.THIN);
        bordered.setBorderBottom(BorderStyle.THIN);
        bordered.setBorderLeft(BorderStyle.THIN);
        bordered.setBorderRight(BorderStyle.THIN);

        return new JsStyles(collegeName, affiliation, address, reportTitle, fieldLabel,
                tableHeader, srNo, seatNo, bordered);
    }
}
