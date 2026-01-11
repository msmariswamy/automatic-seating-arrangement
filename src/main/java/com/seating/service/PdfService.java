package com.seating.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.seating.dto.ConsolidatedReportDTO;
import com.seating.dto.RoomReportDTO;
import com.seating.dto.SeatAllocationDTO;
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
public class PdfService {

    public byte[] generateRoomReportPdf(RoomReportDTO report, LocalDate date, String fontSize) throws DocumentException {
        FontSizes fonts = getFontSizes(fontSize);
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Paragraph title = new Paragraph("Seating Arrangement Report", fonts.titleFont);
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

            Paragraph title = new Paragraph("Consolidated Seating Arrangement Report", fonts.titleFont);
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
            table.addCell(createHeaderCell("Seat From", fonts));
            table.addCell(createHeaderCell("Seat To", fonts));
            table.addCell(createHeaderCell("Total Count", fonts));

            // Add data rows
            for (ConsolidatedReportDTO row : report) {
                table.addCell(createDataCell(row.getRoomNo(), fonts));
                table.addCell(createDataCell(row.getDepartment(), fonts));
                table.addCell(createDataCell(row.getSeatFrom(), fonts));
                table.addCell(createDataCell(row.getSeatTo(), fonts));
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

        FontSizes(int titleSize, int headerSize, int normalSize, int smallSize) {
            this.titleFont = new Font(Font.FontFamily.HELVETICA, titleSize, Font.BOLD);
            this.headerFont = new Font(Font.FontFamily.HELVETICA, headerSize, Font.BOLD);
            this.normalFont = new Font(Font.FontFamily.HELVETICA, normalSize, Font.NORMAL);
            this.smallFont = new Font(Font.FontFamily.HELVETICA, smallSize, Font.NORMAL);
        }
    }
}
