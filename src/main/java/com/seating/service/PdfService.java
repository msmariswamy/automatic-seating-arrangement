package com.seating.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.seating.config.ReportConfig;
import com.seating.dto.ConsolidatedReportDTO;
import com.seating.dto.JuniorSupervisorReportDTO;
import com.seating.dto.RoomReportDTO;
import com.seating.dto.SeatAllocationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for generating PDF reports
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PdfService {

    private final ReportConfig reportConfig;

    public byte[] generateRoomReportPdf(RoomReportDTO report, LocalDate date, String fontSize) throws DocumentException {
        FontSizes fonts = getFontSizes(fontSize);
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Add college header
            addCollegeHeader(document, fonts);

            Paragraph title = new Paragraph("Individual Room Report", fonts.titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            Paragraph roomInfo = new Paragraph("Room No: " + report.getRoomNo(), fonts.headerFont);
            document.add(roomInfo);

            Paragraph dateInfo = new Paragraph("Date: " + date, fonts.normalFont);
            document.add(dateInfo);

            Paragraph deptInfo = new Paragraph("Departments: " + String.join(", ", report.getDepartments()), fonts.normalFont);
            document.add(deptInfo);

            Paragraph subjInfo = new Paragraph("Subjects: " + String.join(", ", report.getSubjects()), fonts.normalFont);
            document.add(subjInfo);

            document.add(new Paragraph(" "));

            PdfPTable mainTable = new PdfPTable(3);
            mainTable.setWidthPercentage(100);

            PdfPCell rightHeader = createHeaderCell("Right Seats (R)", fonts);
            PdfPCell middleHeader = createHeaderCell("Middle Seats (M)", fonts);
            PdfPCell leftHeader = createHeaderCell("Left Seats (L)", fonts);

            mainTable.addCell(rightHeader);
            mainTable.addCell(middleHeader);
            mainTable.addCell(leftHeader);

            PdfPTable rightTable = createSeatTable(fonts);
            PdfPTable middleTable = createSeatTable(fonts);
            PdfPTable leftTable = createSeatTable(fonts);

            addSeatsToTable(rightTable, report.getRightSeats(), fonts);
            addSeatsToTable(middleTable, report.getMiddleSeats(), fonts);
            addSeatsToTable(leftTable, report.getLeftSeats(), fonts);

            PdfPCell rightCell = new PdfPCell(rightTable);
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setPadding(5);

            PdfPCell middleCell = new PdfPCell(middleTable);
            middleCell.setBorder(Rectangle.NO_BORDER);
            middleCell.setPadding(5);

            PdfPCell leftCell = new PdfPCell(leftTable);
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPadding(5);

            mainTable.addCell(rightCell);
            mainTable.addCell(middleCell);
            mainTable.addCell(leftCell);

            document.add(mainTable);

            document.close();
            log.info("Generated PDF report for room {}", report.getRoomNo());

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            throw new DocumentException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private PdfPCell createHeaderCell(String text, FontSizes fonts) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fonts.headerFont));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        return cell;
    }

    private PdfPTable createSeatTable(FontSizes fonts) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        PdfPCell seatHeader = new PdfPCell(new Phrase("Seat", fonts.headerFont));
        seatHeader.setBackgroundColor(BaseColor.LIGHT_GRAY);
        seatHeader.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell rollHeader = new PdfPCell(new Phrase("Roll No", fonts.headerFont));
        rollHeader.setBackgroundColor(BaseColor.LIGHT_GRAY);
        rollHeader.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(seatHeader);
        table.addCell(rollHeader);

        return table;
    }

    private void addSeatsToTable(PdfPTable table, List<SeatAllocationDTO> seats, FontSizes fonts) {
        for (SeatAllocationDTO seat : seats) {
            PdfPCell seatCell = new PdfPCell(new Phrase(seat.getSeatNo(), fonts.smallFont));
            seatCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell rollCell = new PdfPCell(new Phrase(seat.getRollNo(), fonts.smallFont));
            rollCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            table.addCell(seatCell);
            table.addCell(rollCell);
        }
    }

    public byte[] generateConsolidatedReportPdf(List<ConsolidatedReportDTO> report, LocalDate date, String fontSize) throws DocumentException {
        FontSizes fonts = getFontSizes(fontSize);
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Add college header
            addCollegeHeader(document, fonts);

            Paragraph title = new Paragraph("Consolidated Report", fonts.titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            Paragraph dateInfo = new Paragraph("Date: " + date, fonts.normalFont);
            document.add(dateInfo);

            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 3, 2, 2, 2});

            // Add headers
            table.addCell(createHeaderCell("Room No", fonts));
            table.addCell(createHeaderCell("Department", fonts));
            table.addCell(createHeaderCell("Roll No From", fonts));
            table.addCell(createHeaderCell("Roll No To", fonts));
            table.addCell(createHeaderCell("Total Count", fonts));

            // Add data rows
            for (ConsolidatedReportDTO row : report) {
                table.addCell(createDataCell(row.getRoomNo(), fonts));
                table.addCell(createDataCell(row.getDepartment(), fonts));
                table.addCell(createDataCell(row.getRollNoFrom(), fonts));
                table.addCell(createDataCell(row.getRollNoTo(), fonts));
                table.addCell(createDataCell(String.valueOf(row.getTotalCount()), fonts));
            }

            document.add(table);

            document.close();
            log.info("Generated consolidated PDF report for date {}", date);

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error generating consolidated PDF: {}", e.getMessage(), e);
            throw new DocumentException("Failed to generate consolidated PDF: " + e.getMessage());
        }
    }

    public byte[] generateMergedRoomReportsPdf(List<RoomReportDTO> reports, LocalDate date, String fontSize) throws DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4);
            PdfCopy copy = new PdfCopy(document, outputStream);
            document.open();

            // Generate PDF for each room and add to merged document
            for (RoomReportDTO report : reports) {
                byte[] roomPdf = generateRoomReportPdf(report, date, fontSize);
                PdfReader reader = new PdfReader(roomPdf);

                // Add all pages from this room's PDF
                for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                    copy.addPage(copy.getImportedPage(reader, i));
                }

                reader.close();
            }

            document.close();
            log.info("Generated merged PDF report for {} rooms on date {}", reports.size(), date);

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error generating merged PDF: {}", e.getMessage(), e);
            throw new DocumentException("Failed to generate merged PDF: " + e.getMessage());
        }
    }

    private PdfPCell createDataCell(String text, FontSizes fonts) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fonts.normalFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        return cell;
    }

    public byte[] generateJuniorSupervisorReportPdf(JuniorSupervisorReportDTO report, LocalDate date,
            String fontSize, boolean showAnswerSheetCol, boolean showSupplementsCol) throws DocumentException {
        FontSizes fonts = getFontSizes(fontSize);
        // Reduced margins to fit content on one page
        Document document = new Document(PageSize.A4, 25, 25, 20, 20);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Header - College Name (reduced font sizes)
            Font collegeBold = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            Paragraph line1 = new Paragraph("SHRI SIDH THAKURNATH COLLEGE OF ARTS & COMMERCE", collegeBold);
            line1.setAlignment(Element.ALIGN_CENTER);
            line1.setSpacingAfter(0);
            document.add(line1);

            Font collegeNormal = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
            Paragraph line2 = new Paragraph("(Affiliated to University of Mumbai, Mumbai)", collegeNormal);
            line2.setAlignment(Element.ALIGN_CENTER);
            line2.setSpacingAfter(0);
            document.add(line2);

            Paragraph line3 = new Paragraph("ULHASNAGAR - 421 004. Dist. Thane", collegeNormal);
            line3.setAlignment(Element.ALIGN_CENTER);
            line3.setSpacingAfter(3);
            document.add(line3);

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD | Font.UNDERLINE);
            Paragraph title = new Paragraph("Junior Supervisor's Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            // Exam details section (reduced font sizes)
            Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

            // Department and Room No row
            PdfPTable deptRoomTable = new PdfPTable(2);
            deptRoomTable.setWidthPercentage(100);
            deptRoomTable.setSpacingAfter(3);

            PdfPCell deptCell = new PdfPCell();
            deptCell.setBorder(Rectangle.NO_BORDER);
            deptCell.addElement(createLabelValuePhrase("Department: ", report.getDepartment(), labelFont, valueFont));
            deptRoomTable.addCell(deptCell);

            PdfPCell roomCell = new PdfPCell();
            roomCell.setBorder(Rectangle.NO_BORDER);
            roomCell.addElement(createLabelValuePhrase("Room No: ", report.getRoomNo(), labelFont, valueFont));
            deptRoomTable.addCell(roomCell);

            document.add(deptRoomTable);

            // Class, Subject, Date row
            PdfPTable detailsTable = new PdfPTable(3);
            detailsTable.setWidthPercentage(100);
            detailsTable.setSpacingAfter(3);

            PdfPCell classCell = new PdfPCell();
            classCell.setBorder(Rectangle.NO_BORDER);
            classCell.addElement(createLabelValuePhrase("Class: ", report.getClassName(), labelFont, valueFont));
            detailsTable.addCell(classCell);

            PdfPCell subjectCell = new PdfPCell();
            subjectCell.setBorder(Rectangle.NO_BORDER);
            subjectCell.addElement(createLabelValuePhrase("Subject: ", report.getSubject(), labelFont, valueFont));
            detailsTable.addCell(subjectCell);

            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.addElement(createLabelValuePhrase("Date: ", "____________", labelFont, valueFont));
            detailsTable.addCell(dateCell);

            document.add(detailsTable);

            // Calculate seat number range
            String seatNoRange = "_______";
            if (!report.getStudents().isEmpty()) {
                String firstSeatNo = report.getStudents().get(0).getRollNo();
                String lastSeatNo = report.getStudents().get(report.getStudents().size() - 1).getRollNo();
                seatNoRange = firstSeatNo + " to " + lastSeatNo;
            }

            // SEM, Total, Seat No row
            PdfPTable semTable = new PdfPTable(3);
            semTable.setWidthPercentage(100);
            semTable.setSpacingAfter(3);

            PdfPCell semCell = new PdfPCell();
            semCell.setBorder(Rectangle.NO_BORDER);
            semCell.addElement(createLabelValuePhrase("SEM: ", "_______", labelFont, valueFont));
            semTable.addCell(semCell);

            PdfPCell totalBlockCell = new PdfPCell();
            totalBlockCell.setBorder(Rectangle.NO_BORDER);
            totalBlockCell.addElement(createLabelValuePhrase("Total No. in the Block: ", String.valueOf(report.getTotalStudents()), labelFont, valueFont));
            semTable.addCell(totalBlockCell);

            PdfPCell seatNoCell = new PdfPCell();
            seatNoCell.setBorder(Rectangle.NO_BORDER);
            seatNoCell.addElement(createLabelValuePhrase("Seat No: ", seatNoRange, labelFont, valueFont));
            semTable.addCell(seatNoCell);

            document.add(semTable);

            // Total Present/Absent row
            PdfPTable presentAbsentTable = new PdfPTable(2);
            presentAbsentTable.setWidthPercentage(100);
            presentAbsentTable.setSpacingAfter(5);

            PdfPCell presentCell = new PdfPCell();
            presentCell.setBorder(Rectangle.NO_BORDER);
            presentCell.addElement(createLabelValuePhrase("Total No. of Candidates Present: ", "_______", labelFont, valueFont));
            presentAbsentTable.addCell(presentCell);

            PdfPCell absentCell = new PdfPCell();
            absentCell.setBorder(Rectangle.NO_BORDER);
            absentCell.addElement(createLabelValuePhrase("Total No. of Candidates Absent: ", "_______", labelFont, valueFont));
            presentAbsentTable.addCell(absentCell);

            document.add(presentAbsentTable);

            // Create the main table with two side-by-side tables (1-20 and 21-40)
            int totalStudents = report.getStudents().size();

            // Main container table with 2 columns (left table | right table)
            PdfPTable mainTable = new PdfPTable(2);
            mainTable.setWidthPercentage(100);
            mainTable.setSpacingBefore(3);

            // Left table (1-20)
            PdfPTable leftTable = createStudentTable(fonts, showAnswerSheetCol, showSupplementsCol);
            addStudentRows(leftTable, report.getStudents(), 0, Math.min(20, totalStudents), fonts, showAnswerSheetCol, showSupplementsCol);
            // Fill remaining rows if less than 20
            for (int i = totalStudents; i < 20; i++) {
                addEmptyRow(leftTable, i + 1, showAnswerSheetCol, showSupplementsCol, fonts);
            }

            PdfPCell leftCell = new PdfPCell(leftTable);
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPaddingRight(3);
            mainTable.addCell(leftCell);

            // Right table (21-40)
            PdfPTable rightTable = createStudentTable(fonts, showAnswerSheetCol, showSupplementsCol);
            if (totalStudents > 20) {
                addStudentRows(rightTable, report.getStudents(), 20, Math.min(40, totalStudents), fonts, showAnswerSheetCol, showSupplementsCol);
            }
            // Fill remaining rows up to 40
            for (int i = Math.max(20, totalStudents); i < 40; i++) {
                addEmptyRow(rightTable, i + 1, showAnswerSheetCol, showSupplementsCol, fonts);
            }

            PdfPCell rightCell = new PdfPCell(rightTable);
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setPaddingLeft(3);
            mainTable.addCell(rightCell);

            document.add(mainTable);

            // Footer section (reduced spacing)
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

            Paragraph answerSheets = new Paragraph();
            answerSheets.add(new Chunk("Total No. of Main Answer Sheets Used: ", labelFont));
            answerSheets.add(new Chunk("_____________", footerFont));
            answerSheets.setSpacingBefore(5);
            document.add(answerSheets);

            // Jr Supervisor row
            PdfPTable jrSupTable = new PdfPTable(2);
            jrSupTable.setWidthPercentage(100);
            jrSupTable.setSpacingBefore(5);

            PdfPCell jrNameCell = new PdfPCell();
            jrNameCell.setBorder(Rectangle.NO_BORDER);
            jrNameCell.addElement(createLabelValuePhrase("Name of Jr. Supervisor: ", "________________", labelFont, footerFont));
            jrSupTable.addCell(jrNameCell);

            PdfPCell jrSignCell = new PdfPCell();
            jrSignCell.setBorder(Rectangle.NO_BORDER);
            jrSignCell.addElement(createLabelValuePhrase("Signature: ", "________________", labelFont, footerFont));
            jrSupTable.addCell(jrSignCell);

            document.add(jrSupTable);

            // Checked by row
            PdfPTable checkedTable = new PdfPTable(2);
            checkedTable.setWidthPercentage(100);
            checkedTable.setSpacingBefore(5);

            PdfPCell checkedNameCell = new PdfPCell();
            checkedNameCell.setBorder(Rectangle.NO_BORDER);
            checkedNameCell.addElement(createLabelValuePhrase("Checked by another Jr. Supervisor: ", "________________", labelFont, footerFont));
            checkedTable.addCell(checkedNameCell);

            PdfPCell checkedSignCell = new PdfPCell();
            checkedSignCell.setBorder(Rectangle.NO_BORDER);
            checkedSignCell.addElement(createLabelValuePhrase("Signature: ", "________________", labelFont, footerFont));
            checkedTable.addCell(checkedSignCell);

            document.add(checkedTable);

            // Sr Supervisor row
            PdfPTable srSupTable = new PdfPTable(2);
            srSupTable.setWidthPercentage(100);
            srSupTable.setSpacingBefore(5);

            PdfPCell srNameCell = new PdfPCell();
            srNameCell.setBorder(Rectangle.NO_BORDER);
            srNameCell.addElement(createLabelValuePhrase("Name of Sr. Supervisor: ", "________________", labelFont, footerFont));
            srSupTable.addCell(srNameCell);

            PdfPCell srSignCell = new PdfPCell();
            srSignCell.setBorder(Rectangle.NO_BORDER);
            srSignCell.addElement(createLabelValuePhrase("Signature: ", "________________", labelFont, footerFont));
            srSupTable.addCell(srSignCell);

            document.add(srSupTable);

            document.close();
            log.info("Generated Junior Supervisor Report PDF for room {} subject {}", report.getRoomNo(), report.getSubject());

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error generating Junior Supervisor Report PDF: {}", e.getMessage(), e);
            throw new DocumentException("Failed to generate Junior Supervisor Report PDF: " + e.getMessage());
        }
    }

    private Phrase createLabelValuePhrase(String label, String value, Font labelFont, Font valueFont) {
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(label, labelFont));
        phrase.add(new Chunk(value, valueFont));
        return phrase;
    }

    private PdfPTable createStudentTable(FontSizes fonts, boolean showAnswerSheetCol, boolean showSupplementsCol) throws DocumentException {
        int columnCount = 3; // Sr No, Seat No, Signature
        if (showAnswerSheetCol) columnCount++;
        if (showSupplementsCol) columnCount++;

        PdfPTable table = new PdfPTable(columnCount);
        table.setWidthPercentage(100);

        // Set column widths
        float[] widths;
        if (showAnswerSheetCol && showSupplementsCol) {
            widths = new float[]{1f, 2f, 2f, 2.5f, 2.5f};
        } else if (showAnswerSheetCol || showSupplementsCol) {
            widths = new float[]{1f, 2f, 3f, 3f};
        } else {
            widths = new float[]{1f, 2.5f, 3.5f};
        }
        table.setWidths(widths);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

        // Headers
        table.addCell(createTableHeaderCell("Sr No", headerFont));
        table.addCell(createTableHeaderCell("Seat No", headerFont));
        if (showAnswerSheetCol) {
            table.addCell(createTableHeaderCell("Main Answer Sheet No.", headerFont));
        }
        if (showSupplementsCol) {
            table.addCell(createTableHeaderCell("No. of Suppl. & Stationery", headerFont));
        }
        table.addCell(createTableHeaderCell("Signature", headerFont));

        return table;
    }

    private PdfPCell createTableHeaderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2);
        return cell;
    }

    private void addStudentRows(PdfPTable table, List<JuniorSupervisorReportDTO.StudentEntry> students,
            int start, int end, FontSizes fonts, boolean showAnswerSheetCol, boolean showSupplementsCol) {
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

        for (int i = start; i < end; i++) {
            JuniorSupervisorReportDTO.StudentEntry student = students.get(i);

            table.addCell(createTableDataCell(String.valueOf(student.getSrNo()), cellFont));
            table.addCell(createTableDataCell(student.getRollNo(), cellFont));
            if (showAnswerSheetCol) {
                table.addCell(createTableDataCell("", cellFont)); // Blank for answer sheet
            }
            if (showSupplementsCol) {
                table.addCell(createTableDataCell("", cellFont)); // Blank for supplements
            }
            table.addCell(createTableDataCell("", cellFont)); // Blank for signature
        }
    }

    private void addEmptyRow(PdfPTable table, int srNo, boolean showAnswerSheetCol, boolean showSupplementsCol, FontSizes fonts) {
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

        table.addCell(createTableDataCell(String.valueOf(srNo), cellFont));
        table.addCell(createTableDataCell("", cellFont));
        if (showAnswerSheetCol) {
            table.addCell(createTableDataCell("", cellFont));
        }
        if (showSupplementsCol) {
            table.addCell(createTableDataCell("", cellFont));
        }
        table.addCell(createTableDataCell("", cellFont));
    }

    private PdfPCell createTableDataCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        cell.setMinimumHeight(21);
        return cell;
    }

    public byte[] generateAllJuniorSupervisorReportsPdf(List<JuniorSupervisorReportDTO> reports, LocalDate date,
            String fontSize, boolean showAnswerSheetCol, boolean showSupplementsCol) throws DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4);
            PdfCopy copy = new PdfCopy(document, outputStream);
            document.open();

            for (JuniorSupervisorReportDTO report : reports) {
                byte[] reportPdf = generateJuniorSupervisorReportPdf(report, date, fontSize, showAnswerSheetCol, showSupplementsCol);
                PdfReader reader = new PdfReader(reportPdf);

                for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                    copy.addPage(copy.getImportedPage(reader, i));
                }

                reader.close();
            }

            document.close();
            log.info("Generated merged Junior Supervisor Reports PDF for {} reports on date {}", reports.size(), date);

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error generating merged Junior Supervisor Reports PDF: {}", e.getMessage(), e);
            throw new DocumentException("Failed to generate merged Junior Supervisor Reports PDF: " + e.getMessage());
        }
    }

    /**
     * Adds the college header to the PDF document
     */
    private void addCollegeHeader(Document document, FontSizes fonts) throws DocumentException {
        if (reportConfig.getLine1() != null && !reportConfig.getLine1().isEmpty()) {
            Paragraph headerLine1 = new Paragraph(reportConfig.getLine1(), fonts.collegeBoldFont);
            headerLine1.setAlignment(Element.ALIGN_CENTER);
            document.add(headerLine1);
        }

        if (reportConfig.getLine2() != null && !reportConfig.getLine2().isEmpty()) {
            Paragraph headerLine2 = new Paragraph(reportConfig.getLine2(), fonts.collegeBoldFont);
            headerLine2.setAlignment(Element.ALIGN_CENTER);
            document.add(headerLine2);
        }

        document.add(new Paragraph(" "));
    }

    private FontSizes getFontSizes(String fontSize) {
        // Try to parse as integer (numeric font size)
        try {
            int baseSize = Integer.parseInt(fontSize);
            // Calculate proportional sizes based on base size
            // Title is ~1.8x base, Header is ~1.2x base, Normal is base, Small is ~0.8x base
            int titleSize = (int) (baseSize * 1.8);
            int headerSize = (int) (baseSize * 1.2);
            int normalSize = baseSize;
            int smallSize = Math.max(6, (int) (baseSize * 0.8)); // Minimum 6pt

            return new FontSizes(titleSize, headerSize, normalSize, smallSize);
        } catch (NumberFormatException e) {
            // Fallback to text-based sizes for backward compatibility
            return switch (fontSize.toLowerCase()) {
                case "small" -> new FontSizes(14, 10, 8, 7);
                case "large" -> new FontSizes(22, 14, 12, 10);
                default -> new FontSizes(18, 12, 10, 8); // medium (10pt base)
            };
        }
    }

    private static class FontSizes {
        final Font titleFont;
        final Font headerFont;
        final Font normalFont;
        final Font smallFont;
        final Font collegeBoldFont;

        FontSizes(int titleSize, int headerSize, int normalSize, int smallSize) {
            this.titleFont = new Font(Font.FontFamily.HELVETICA, titleSize, Font.BOLD);
            this.headerFont = new Font(Font.FontFamily.HELVETICA, headerSize, Font.BOLD);
            this.normalFont = new Font(Font.FontFamily.HELVETICA, normalSize, Font.NORMAL);
            this.smallFont = new Font(Font.FontFamily.HELVETICA, smallSize, Font.NORMAL);
            // College header uses a slightly larger bold font
            this.collegeBoldFont = new Font(Font.FontFamily.HELVETICA, headerSize, Font.BOLD);
        }
    }
}
