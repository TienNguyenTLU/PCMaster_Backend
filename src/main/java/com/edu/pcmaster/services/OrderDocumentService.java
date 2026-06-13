package com.edu.pcmaster.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.edu.pcmaster.models.DeliveryType;
import com.edu.pcmaster.models.Order;
import com.edu.pcmaster.models.OrderItem;

@Service
public class OrderDocumentService {

	private static final NumberFormat VND_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
	private static final DateTimeFormatter DATE_FMT =
			DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

	/** Generate XLSX bytes for a confirmed order (phiếu xuất kho) */
	public byte[] generateExportDocument(Order order) throws IOException {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			Sheet sheet = wb.createSheet("PHIẾU XUẤT KHO");
			sheet.setDefaultColumnWidth(14);

			// ── Styles ────────────────────────────────────────────────────────────
			Font titleFont = wb.createFont();
			titleFont.setBold(true);
			titleFont.setFontHeightInPoints((short) 18);
			titleFont.setFontName("Times New Roman");

			Font subtitleFont = wb.createFont();
			subtitleFont.setFontHeightInPoints((short) 11);
			subtitleFont.setFontName("Times New Roman");

			Font headerFont = wb.createFont();
			headerFont.setBold(true);
			headerFont.setFontHeightInPoints((short) 11);
			headerFont.setFontName("Times New Roman");
			headerFont.setColor(IndexedColors.WHITE.getIndex());

			Font boldFont = wb.createFont();
			boldFont.setBold(true);
			boldFont.setFontHeightInPoints((short) 11);
			boldFont.setFontName("Times New Roman");

			Font normalFont = wb.createFont();
			normalFont.setFontHeightInPoints((short) 11);
			normalFont.setFontName("Times New Roman");

			Font signatureFont = wb.createFont();
			signatureFont.setBold(true);
			signatureFont.setFontHeightInPoints((short) 11);
			signatureFont.setFontName("Times New Roman");

			// Title style (centered, bold, large)
			CellStyle titleStyle = wb.createCellStyle();
			titleStyle.setFont(titleFont);
			titleStyle.setAlignment(HorizontalAlignment.CENTER);
			titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			// Subtitle style (centered, normal)
			CellStyle subtitleStyle = wb.createCellStyle();
			subtitleStyle.setFont(subtitleFont);
			subtitleStyle.setAlignment(HorizontalAlignment.CENTER);

			// Label style (bold, left)
			CellStyle labelStyle = wb.createCellStyle();
			labelStyle.setFont(boldFont);

			// Normal style
			CellStyle normalStyle = wb.createCellStyle();
			normalStyle.setFont(normalFont);

			// Header style (dark blue background, white text, bordered)
			CellStyle tableHeaderStyle = wb.createCellStyle();
			tableHeaderStyle.setFont(headerFont);
			tableHeaderStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
			tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			tableHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
			tableHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			tableHeaderStyle.setBorderTop(BorderStyle.THIN);
			tableHeaderStyle.setBorderBottom(BorderStyle.THIN);
			tableHeaderStyle.setBorderLeft(BorderStyle.THIN);
			tableHeaderStyle.setBorderRight(BorderStyle.THIN);

			// Data cell style (bordered, left)
			CellStyle dataCellStyle = wb.createCellStyle();
			dataCellStyle.setFont(normalFont);
			dataCellStyle.setBorderTop(BorderStyle.THIN);
			dataCellStyle.setBorderBottom(BorderStyle.THIN);
			dataCellStyle.setBorderLeft(BorderStyle.THIN);
			dataCellStyle.setBorderRight(BorderStyle.THIN);
			dataCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			// Data cell center
			CellStyle dataCellCenterStyle = wb.createCellStyle();
			dataCellCenterStyle.cloneStyleFrom(dataCellStyle);
			dataCellCenterStyle.setAlignment(HorizontalAlignment.CENTER);

			// Data cell number (bordered, right, number format)
			CellStyle dataCellNumberStyle = wb.createCellStyle();
			dataCellNumberStyle.setFont(normalFont);
			dataCellNumberStyle.setBorderTop(BorderStyle.THIN);
			dataCellNumberStyle.setBorderBottom(BorderStyle.THIN);
			dataCellNumberStyle.setBorderLeft(BorderStyle.THIN);
			dataCellNumberStyle.setBorderRight(BorderStyle.THIN);
			dataCellNumberStyle.setAlignment(HorizontalAlignment.RIGHT);
			dataCellNumberStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			dataCellNumberStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

			// Total style (bold, right, bordered)
			CellStyle totalLabelStyle = wb.createCellStyle();
			totalLabelStyle.setFont(boldFont);
			totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
			totalLabelStyle.setBorderTop(BorderStyle.THIN);
			totalLabelStyle.setBorderBottom(BorderStyle.DOUBLE);
			totalLabelStyle.setBorderLeft(BorderStyle.THIN);
			totalLabelStyle.setBorderRight(BorderStyle.THIN);

			CellStyle totalValueStyle = wb.createCellStyle();
			totalValueStyle.setFont(boldFont);
			totalValueStyle.setAlignment(HorizontalAlignment.RIGHT);
			totalValueStyle.setBorderTop(BorderStyle.THIN);
			totalValueStyle.setBorderBottom(BorderStyle.DOUBLE);
			totalValueStyle.setBorderLeft(BorderStyle.THIN);
			totalValueStyle.setBorderRight(BorderStyle.THIN);
			totalValueStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

			// Signature header style (bold, center)
			CellStyle sigStyle = wb.createCellStyle();
			sigStyle.setFont(signatureFont);
			sigStyle.setAlignment(HorizontalAlignment.CENTER);
			sigStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			CellStyle sigSubStyle = wb.createCellStyle();
			sigSubStyle.setFont(normalFont);
			sigSubStyle.setAlignment(HorizontalAlignment.CENTER);

			int rowIdx = 0;

			// ── Row 0: Company name ──────────────────────────────────────────────
			Row r0 = sheet.createRow(rowIdx++);
			r0.setHeightInPoints(28);
			Cell c0 = r0.createCell(0);
			c0.setCellValue("CHUỖI CỬA HÀNG PC MASTER");
			c0.setCellStyle(titleStyle);
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

			// ── Row 1: Address ───────────────────────────────────────────────────
			Row r1 = sheet.createRow(rowIdx++);
			Cell c1 = r1.createCell(0);
			c1.setCellValue("Địa chỉ showroom: 123 Đường Láng, Đống Đa, Hà Nội  |  Hotline: 1800 1234  |  pcmaster.vn");
			c1.setCellStyle(subtitleStyle);
			sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

			// ── Row 2: blank ─────────────────────────────────────────────────────
			sheet.createRow(rowIdx++);

			// ── Row 3: Title "PHIẾU XUẤT KHO" ───────────────────────────────────
			Row r3 = sheet.createRow(rowIdx++);
			r3.setHeightInPoints(30);
			Cell c3 = r3.createCell(0);
			c3.setCellValue("PHIẾU XUẤT KHO");
			c3.setCellStyle(titleStyle);
			sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 5));

			// ── Row 4: Số phiếu + Ngày xuất ─────────────────────────────────────
			Row r4 = sheet.createRow(rowIdx++);
			Cell c4a = r4.createCell(0);
			c4a.setCellValue("Số phiếu: PX-" + String.format("%05d", order.getId()));
			c4a.setCellStyle(subtitleStyle);
			sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 2));

			Cell c4b = r4.createCell(3);
			c4b.setCellValue("Ngày xuất: " + DATE_FMT.format(order.getCreatedAt()));
			c4b.setCellStyle(subtitleStyle);
			sheet.addMergedRegion(new CellRangeAddress(4, 4, 3, 5));

			// ── Row 5: blank ─────────────────────────────────────────────────────
			sheet.createRow(rowIdx++);

			// ── Customer info ────────────────────────────────────────────────────
			String userName = order.getUser() != null ? order.getUser().getUsername() : "N/A";
			String userEmail = order.getUser() != null ? order.getUser().getEmail() : "N/A";

			Row r6 = sheet.createRow(rowIdx++);
			createLabelValueCells(r6, 0, "Tài khoản:", userName + " (" + userEmail + ")", labelStyle, normalStyle);

			if (order.getDeliveryType() == DeliveryType.HOME_DELIVERY) {
				Row r7 = sheet.createRow(rowIdx++);
				createLabelValueCells(r7, 0, "Hình thức:", "Giao hàng tận nhà", labelStyle, normalStyle);

				Row r8 = sheet.createRow(rowIdx++);
				createLabelValueCells(r8, 0, "Người nhận:", order.getRecipientName() != null ? order.getRecipientName() : "—", labelStyle, normalStyle);

				Row r9 = sheet.createRow(rowIdx++);
				createLabelValueCells(r9, 0, "Điện thoại:", order.getRecipientPhone() != null ? order.getRecipientPhone() : "—", labelStyle, normalStyle);

				Row r10 = sheet.createRow(rowIdx++);
				createLabelValueCells(r10, 0, "Địa chỉ:", order.getShippingAddress() != null ? order.getShippingAddress() : "—", labelStyle, normalStyle);
			} else {
				Row r7 = sheet.createRow(rowIdx++);
				createLabelValueCells(r7, 0, "Hình thức:", "Nhận tại showroom", labelStyle, normalStyle);

				Row r8 = sheet.createRow(rowIdx++);
				createLabelValueCells(r8, 0, "Showroom:", "123 Đường Láng, Đống Đa, Hà Nội", labelStyle, normalStyle);
			}

			// ── Blank row ────────────────────────────────────────────────────────
			sheet.createRow(rowIdx++);

			// ── Table header ─────────────────────────────────────────────────────
			Row headerRow = sheet.createRow(rowIdx++);
			headerRow.setHeightInPoints(22);
			String[] headers = {"STT", "Tên sản phẩm", "ĐVT", "Số lượng", "Đơn giá (₫)", "Thành tiền (₫)"};
			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(tableHeaderStyle);
			}

			// ── Data rows ────────────────────────────────────────────────────────
			List<OrderItem> items = order.getItems();
			BigDecimal grandTotal = BigDecimal.ZERO;

			for (int i = 0; i < items.size(); i++) {
				OrderItem item = items.get(i);
				BigDecimal lineTotal = item.getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
				grandTotal = grandTotal.add(lineTotal);

				Row dataRow = sheet.createRow(rowIdx++);
				String productName = item.getProduct() != null ? item.getProduct().getName() : "—";

				Cell sttCell = dataRow.createCell(0);
				sttCell.setCellValue(i + 1);
				sttCell.setCellStyle(dataCellCenterStyle);

				Cell nameCell = dataRow.createCell(1);
				nameCell.setCellValue(productName);
				nameCell.setCellStyle(dataCellStyle);

				Cell dvtCell = dataRow.createCell(2);
				dvtCell.setCellValue("Cái");
				dvtCell.setCellStyle(dataCellCenterStyle);

				Cell qtyCell = dataRow.createCell(3);
				qtyCell.setCellValue(item.getQuantity());
				qtyCell.setCellStyle(dataCellCenterStyle);

				Cell priceCell = dataRow.createCell(4);
				priceCell.setCellValue(item.getSellingPrice().doubleValue());
				priceCell.setCellStyle(dataCellNumberStyle);

				Cell totalCell = dataRow.createCell(5);
				totalCell.setCellValue(lineTotal.doubleValue());
				totalCell.setCellStyle(dataCellNumberStyle);
			}

			// ── Total row ────────────────────────────────────────────────────────
			Row totalRow = sheet.createRow(rowIdx++);
			Cell totalLabelCell = totalRow.createCell(0);
			totalLabelCell.setCellValue("TỔNG CỘNG");
			totalLabelCell.setCellStyle(totalLabelStyle);
			// Merge label across columns 0-4
			sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 4));
			// Apply border to merged cells
			for (int c = 1; c <= 4; c++) {
				Cell mergedCell = totalRow.createCell(c);
				mergedCell.setCellStyle(totalLabelStyle);
			}
			Cell grandTotalCell = totalRow.createCell(5);
			grandTotalCell.setCellValue(grandTotal.doubleValue());
			grandTotalCell.setCellStyle(totalValueStyle);

			// ── Blank rows before signatures ─────────────────────────────────────
			sheet.createRow(rowIdx++);
			sheet.createRow(rowIdx++);

			// ── Signature section ────────────────────────────────────────────────
			Row sigRow = sheet.createRow(rowIdx++);
			Cell sig1 = sigRow.createCell(0);
			sig1.setCellValue("Người lập phiếu");
			sig1.setCellStyle(sigStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 1));
			Cell sig1b = sigRow.createCell(1);
			sig1b.setCellStyle(sigStyle);

			Cell sig2 = sigRow.createCell(2);
			sig2.setCellValue("Thủ kho");
			sig2.setCellStyle(sigStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 2, 3));
			Cell sig2b = sigRow.createCell(3);
			sig2b.setCellStyle(sigStyle);

			Cell sig3 = sigRow.createCell(4);
			sig3.setCellValue("Khách hàng");
			sig3.setCellStyle(sigStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 4, 5));
			Cell sig3b = sigRow.createCell(5);
			sig3b.setCellStyle(sigStyle);

			// ── "(Ký, ghi rõ họ tên)" row ────────────────────────────────────────
			Row sigSubRow = sheet.createRow(rowIdx++);
			for (int col = 0; col <= 5; col += 2) {
				Cell subCell = sigSubRow.createCell(col);
				subCell.setCellValue("(Ký, ghi rõ họ tên)");
				subCell.setCellStyle(sigSubStyle);
				sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, col, col + 1));
				Cell subCellB = sigSubRow.createCell(col + 1);
				subCellB.setCellStyle(sigSubStyle);
			}

			// ── Footer ───────────────────────────────────────────────────────────
			sheet.createRow(rowIdx++);
			sheet.createRow(rowIdx++);
			sheet.createRow(rowIdx++);
			sheet.createRow(rowIdx++);
			Row footerRow = sheet.createRow(rowIdx);
			Cell footerCell = footerRow.createCell(0);
			footerCell.setCellValue("— Phiếu này được tạo tự động bởi hệ thống PCMaster —");
			footerCell.setCellStyle(subtitleStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 5));

			// ── Auto-size columns ────────────────────────────────────────────────
			sheet.setColumnWidth(0, 2000);   // STT
			sheet.setColumnWidth(1, 12000);  // Tên sản phẩm
			sheet.setColumnWidth(2, 2500);   // ĐVT
			sheet.setColumnWidth(3, 3000);   // Số lượng
			sheet.setColumnWidth(4, 5000);   // Đơn giá
			sheet.setColumnWidth(5, 5500);   // Thành tiền

			// ── Print setup ──────────────────────────────────────────────────────
			sheet.getPrintSetup().setLandscape(false);
			sheet.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
			sheet.setFitToPage(true);

			wb.write(out);
			return out.toByteArray();
		}
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private void createLabelValueCells(Row row, int startCol, String label, String value,
									   CellStyle labelStyle, CellStyle valueStyle) {
		Cell labelCell = row.createCell(startCol);
		labelCell.setCellValue(label);
		labelCell.setCellStyle(labelStyle);

		Cell valueCell = row.createCell(startCol + 1);
		valueCell.setCellValue(value);
		valueCell.setCellStyle(valueStyle);
	}

	private String formatVnd(BigDecimal amount) {
		return VND_FORMAT.format(amount);
	}
}
