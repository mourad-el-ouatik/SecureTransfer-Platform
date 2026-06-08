package com.securetransfer.platform.document.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.securetransfer.platform.transaction.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PdfReceiptService {

    @Value("${receipt.storage-dir}")
    private String storageDir;

    public Path generateReceipt(Transaction tx) {
        try {
            Path dir = Paths.get(storageDir);
            Files.createDirectories(dir);
            Path filePath = dir.resolve("receipt_" + tx.getId() + ".pdf");

            Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, new FileOutputStream(filePath.toFile()));
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.DARK_GRAY);
            Paragraph title = new Paragraph("SecureTransfer — Recu de Transaction", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            doc.add(title);

            PdfPTable separator = new PdfPTable(1);
            separator.setWidthPercentage(100);
            separator.setSpacingAfter(15);
            PdfPCell sepCell = new PdfPCell(new Phrase(" "));
            sepCell.setBackgroundColor(new Color(0, 102, 204));
            sepCell.setFixedHeight(3f);
            sepCell.setBorder(Rectangle.NO_BORDER);
            separator.addCell(sepCell);
            doc.add(separator);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(20);

            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

            addRow(table, "N Transaction", String.valueOf(tx.getId()), labelFont, valueFont);
            addRow(table, "Type", tx.getType().name(), labelFont, valueFont);
            addRow(table, "Montant", tx.getAmount() + " MAD", labelFont, valueFont);
            addRow(table, "Frais", tx.getFee() + " MAD", labelFont, valueFont);
            addRow(table, "Total", tx.getAmount().add(tx.getFee()) + " MAD", labelFont, valueFont);
            addRow(table, "Statut", tx.getStatus().name(), labelFont, valueFont);
            addRow(table, "Date", tx.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), labelFont, valueFont);

            if (tx.getWithdrawalCode() != null) {
                addRow(table, "Code retrait", tx.getWithdrawalCode(), labelFont, valueFont);
            }

            doc.add(table);

            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);
            Paragraph footer = new Paragraph("Document genere automatiquement — SecureTransfer Platform", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            log.info("PDF genere : {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("Erreur generation PDF pour transaction {}", tx.getId(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font lf, Font vf) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, lf));
        labelCell.setBackgroundColor(new Color(240, 240, 240));
        labelCell.setPadding(8);
        labelCell.setBorderColor(Color.LIGHT_GRAY);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, vf));
        valueCell.setPadding(8);
        valueCell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    public Path getReceiptPath(Long transactionId) {
        return Paths.get(storageDir).resolve("receipt_" + transactionId + ".pdf");
    }
}
