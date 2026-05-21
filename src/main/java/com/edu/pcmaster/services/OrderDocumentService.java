package com.edu.pcmaster.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;

import com.edu.pcmaster.models.DeliveryType;
import com.edu.pcmaster.models.Order;
import com.edu.pcmaster.models.OrderItem;

@Service
public class OrderDocumentService {

	private static final NumberFormat VND_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
	private static final DateTimeFormatter DATE_FMT =
			DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

	/** Generate DOCX bytes for a confirmed order (phiếu xuất kho) */
	public byte[] generateExportDocument(Order order) throws IOException {
		try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			// ── Store header ───────────────────────────────────────────────────────
			addCenteredBold(doc, "CHUỖI CỬA HÀNG PC MASTER", 18);
			addCenteredNormal(doc, "Địa chỉ showroom: 123 Đường Láng, Đống Đa, Hà Nội", 11);
			addCenteredNormal(doc, "Hotline: 1800 1234  |  pcmaster.vn", 11);
			addSeparator(doc);

			// ── Document title ─────────────────────────────────────────────────────
			addCenteredBold(doc, "PHIẾU XUẤT KHO", 16);
			addCenteredNormal(doc, "Số phiếu: PX-" + String.format("%05d", order.getId()), 12);
			addCenteredNormal(doc, "Ngày xuất: " + DATE_FMT.format(order.getCreatedAt()), 11);
			addBlankLine(doc);

			// ── Customer / Delivery info ───────────────────────────────────────────
			addSectionTitle(doc, "THÔNG TIN KHÁCH HÀNG & GIAO HÀNG");

			String userName = order.getUser() != null ? order.getUser().getUsername() : "N/A";
			String userEmail = order.getUser() != null ? order.getUser().getEmail() : "N/A";
			addLabelValue(doc, "Tài khoản", userName + " (" + userEmail + ")");

			if (order.getDeliveryType() == DeliveryType.HOME_DELIVERY) {
				addLabelValue(doc, "Hình thức", "Giao hàng tận nhà");
				addLabelValue(doc, "Người nhận", order.getRecipientName() != null ? order.getRecipientName() : "—");
				addLabelValue(doc, "Điện thoại", order.getRecipientPhone() != null ? order.getRecipientPhone() : "—");
				addLabelValue(doc, "Địa chỉ", order.getShippingAddress() != null ? order.getShippingAddress() : "—");
			} else {
				addLabelValue(doc, "Hình thức", "Nhận tại showroom");
				addLabelValue(doc, "Showroom", "123 Đường Láng, Đống Đa, Hà Nội");
			}
			addBlankLine(doc);

			// ── Items table ────────────────────────────────────────────────────────
			addSectionTitle(doc, "DANH SÁCH HÀNG XUẤT");

			List<OrderItem> items = order.getItems();
			XWPFTable table = doc.createTable(items.size() + 1, 5);
			setTableWidth(table);

			// Header row
			String[] headers = {"STT", "Tên sản phẩm", "Số lượng", "Đơn giá (₫)", "Thành tiền (₫)"};
			XWPFTableRow headerRow = table.getRow(0);
			for (int i = 0; i < headers.length; i++) {
				XWPFRun run = headerRow.getCell(i).addParagraph().createRun();
				run.setText(headers[i]);
				run.setBold(true);
				run.setFontSize(11);
			}

			// Data rows
			BigDecimal grandTotal = BigDecimal.ZERO;
			for (int i = 0; i < items.size(); i++) {
				OrderItem item = items.get(i);
				BigDecimal lineTotal = item.getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
				grandTotal = grandTotal.add(lineTotal);

				XWPFTableRow row = table.getRow(i + 1);
				String productName = item.getProduct() != null ? item.getProduct().getName() : "—";

				setCellText(row, 0, String.valueOf(i + 1));
				setCellText(row, 1, productName);
				setCellText(row, 2, String.valueOf(item.getQuantity()));
				setCellText(row, 3, formatVnd(item.getSellingPrice()));
				setCellText(row, 4, formatVnd(lineTotal));
			}
			addBlankLine(doc);

			// ── Total ──────────────────────────────────────────────────────────────
			XWPFParagraph totalPara = doc.createParagraph();
			totalPara.setAlignment(ParagraphAlignment.RIGHT);
			XWPFRun totalRun = totalPara.createRun();
			totalRun.setBold(true);
			totalRun.setFontSize(13);
			totalRun.setText("TỔNG CỘNG: " + formatVnd(grandTotal) + " ₫");
			addBlankLine(doc);

			// ── Signatures ─────────────────────────────────────────────────────────
			addSeparator(doc);
			XWPFTable sigTable = doc.createTable(1, 3);
			setTableWidth(sigTable);
			setCellCenteredBold(sigTable.getRow(0), 0, "Người lập phiếu");
			setCellCenteredBold(sigTable.getRow(0), 1, "Thủ kho");
			setCellCenteredBold(sigTable.getRow(0), 2, "Khách hàng");

			addCenteredNormal(doc, "(Ký, ghi rõ họ tên)", 10);
			addBlankLine(doc);
			addCenteredNormal(doc, "— Phiếu này được tạo tự động bởi hệ thống PCMaster —", 9);

			doc.write(out);
			return out.toByteArray();
		}
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private void addCenteredBold(XWPFDocument doc, String text, int size) {
		XWPFParagraph p = doc.createParagraph();
		p.setAlignment(ParagraphAlignment.CENTER);
		XWPFRun r = p.createRun();
		r.setBold(true);
		r.setFontSize(size);
		r.setText(text);
	}

	private void addCenteredNormal(XWPFDocument doc, String text, int size) {
		XWPFParagraph p = doc.createParagraph();
		p.setAlignment(ParagraphAlignment.CENTER);
		XWPFRun r = p.createRun();
		r.setFontSize(size);
		r.setText(text);
	}

	private void addSectionTitle(XWPFDocument doc, String text) {
		XWPFParagraph p = doc.createParagraph();
		XWPFRun r = p.createRun();
		r.setBold(true);
		r.setFontSize(12);
		r.setText(text);
		r.addBreak();
	}

	private void addLabelValue(XWPFDocument doc, String label, String value) {
		XWPFParagraph p = doc.createParagraph();
		XWPFRun labelRun = p.createRun();
		labelRun.setBold(true);
		labelRun.setFontSize(11);
		labelRun.setText(label + ": ");
		XWPFRun valueRun = p.createRun();
		valueRun.setFontSize(11);
		valueRun.setText(value);
	}

	private void addSeparator(XWPFDocument doc) {
		XWPFParagraph p = doc.createParagraph();
		XWPFRun r = p.createRun();
		r.setText("─".repeat(80));
		r.setFontSize(9);
	}

	private void addBlankLine(XWPFDocument doc) {
		doc.createParagraph();
	}

	private void setCellText(XWPFTableRow row, int cellIndex, String text) {
		row.getCell(cellIndex).getParagraphs().get(0).createRun().setText(text);
	}

	private void setCellCenteredBold(XWPFTableRow row, int cellIndex, String text) {
		XWPFParagraph p = row.getCell(cellIndex).getParagraphs().get(0);
		p.setAlignment(ParagraphAlignment.CENTER);
		XWPFRun r = p.createRun();
		r.setBold(true);
		r.setText(text);
	}

	@SuppressWarnings("deprecation")
	private void setTableWidth(XWPFTable table) {
		CTTblPr tblPr = table.getCTTbl().getTblPr();
		if (tblPr == null) tblPr = table.getCTTbl().addNewTblPr();
		CTTblWidth tblW = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
		tblW.setW(java.math.BigInteger.valueOf(9360));
		tblW.setType(STTblWidth.DXA);
	}

	private String formatVnd(BigDecimal amount) {
		return VND_FORMAT.format(amount);
	}
}
