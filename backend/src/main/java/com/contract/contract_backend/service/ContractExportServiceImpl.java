package com.contract.contract_backend.service;

import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.repository.ContractRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractExportServiceImpl implements ContractExportService {

    private final ContractRepository contractRepository;

    @Override
    public byte[] exportWord(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在，contractId=" + contractId));

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);

            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setText(safe(contract.getTitle(), "合同文件"));

            document.createParagraph();

            XWPFParagraph infoParagraph = document.createParagraph();
            XWPFRun infoRun = infoParagraph.createRun();
            infoRun.setFontSize(12);
            infoRun.setText("合同编号：" + safe(contract.getContractNo()));
            infoRun.addBreak();
            infoRun.setText("合同类型：" + safe(contract.getContractType()));
            infoRun.addBreak();
            infoRun.setText("合同状态：" + safe(contract.getStatus()));
            infoRun.addBreak();
            infoRun.setText("创建时间：" + (contract.getCreatedAt() == null ? "" : contract.getCreatedAt().toString()));

            document.createParagraph();

            XWPFParagraph contentTitleParagraph = document.createParagraph();
            XWPFRun contentTitleRun = contentTitleParagraph.createRun();
            contentTitleRun.setBold(true);
            contentTitleRun.setFontSize(14);
            contentTitleRun.setText("合同正文");

            List<String> lines = splitLines(contract.getContent());
            for (String line : lines) {
                XWPFParagraph p = document.createParagraph();
                p.setFirstLineIndent(400);

                XWPFRun run = p.createRun();
                run.setFontSize(12);
                run.setText(line);
            }

            document.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("导出 Word 失败：" + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportPdf(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在，contractId=" + contractId));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font subTitleFont = new Font(baseFont, 14, Font.BOLD);
            Font bodyFont = new Font(baseFont, 12, Font.NORMAL);

            Paragraph title = new Paragraph(safe(contract.getTitle(), "合同文件"), titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph("合同编号：" + safe(contract.getContractNo()), bodyFont));
            document.add(new Paragraph("合同类型：" + safe(contract.getContractType()), bodyFont));
            document.add(new Paragraph("合同状态：" + safe(contract.getStatus()), bodyFont));
            document.add(new Paragraph("创建时间：" + (contract.getCreatedAt() == null ? "" : contract.getCreatedAt().toString()), bodyFont));

            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph("合同正文", subTitleFont));
            document.add(new Paragraph(" ", bodyFont));

            for (String line : splitLines(contract.getContent())) {
                document.add(new Paragraph(line, bodyFont));
            }

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("导出 PDF 失败：" + e.getMessage(), e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private List<String> splitLines(String content) {
        if (content == null || content.isBlank()) {
            return List.of("暂无合同内容");
        }
        return Arrays.asList(content.split("\\r?\\n"));
    }
}