package com.seating.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.seating.dto.RoomReportDTO;
import com.seating.dto.SeatAllocationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for generating PDF reports
 */
@Service
@Slf4j
public class PdfService {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

    public byte[] generateRoomReportPdf(RoomReportDTO report, LocalDate date) throws DocumentException {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Paragraph title = new Paragraph("Seating Arrangement Report", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            Paragraph roomInfo = new Paragraph("Room No: " + report.getRoomNo(), HEADER_FONT);
            document.add(roomInfo);

            Paragraph dateInfo = new Paragraph("Date: " + date, NORMAL_FONT);
            document.add(dateInfo);

            Paragraph deptInfo = new Paragraph("Departments: " + String.join(", ", report.getDepartments()), NORMAL_FONT);
            document.add(deptInfo);

            Paragraph subjInfo = new Paragraph("Subjects: " + String.join(", ", report.getSubjects()), NORMAL_FONT);
            document.add(subjInfo);

            document.add(new Paragraph(" "));

            PdfPTable mainTable = new PdfPTable(3);
            mainTable.setWidthPercentage(100);

            PdfPCell rightHeader = createHeaderCell("Right Seats (R)");
            PdfPCell middleHeader = createHeaderCell("Middle Seats (M)");
            PdfPCell leftHeader = createHeaderCell("Left Seats (L)");

            mainTable.addCell(rightHeader);
            mainTable.addCell(middleHeader);
            mainTable.addCell(leftHeader);

            PdfPTable rightTable = createSeatTable();
            PdfPTable middleTable = createSeatTable();
            PdfPTable leftTable = createSeatTable();

            addSeatsToTable(rightTable, report.getRightSeats());
            addSeatsToTable(middleTable, report.getMiddleSeats());
            addSeatsToTable(leftTable, report.getLeftSeats());

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

    private PdfPCell createHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        return cell;
    }

    private PdfPTable createSeatTable() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        PdfPCell seatHeader = new PdfPCell(new Phrase("Seat", HEADER_FONT));
        seatHeader.setBackgroundColor(BaseColor.LIGHT_GRAY);
        seatHeader.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell rollHeader = new PdfPCell(new Phrase("Roll No", HEADER_FONT));
        rollHeader.setBackgroundColor(BaseColor.LIGHT_GRAY);
        rollHeader.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(seatHeader);
        table.addCell(rollHeader);

        return table;
    }

    private void addSeatsToTable(PdfPTable table, List<SeatAllocationDTO> seats) {
        for (SeatAllocationDTO seat : seats) {
            PdfPCell seatCell = new PdfPCell(new Phrase(seat.getSeatNo(), SMALL_FONT));
            seatCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell rollCell = new PdfPCell(new Phrase(seat.getRollNo(), SMALL_FONT));
            rollCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            table.addCell(seatCell);
            table.addCell(rollCell);
        }
    }
}
