package org.example;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;

public class HtmlTableToExcel {
  public static void main(String[] args) {
    String htmlFilePath = "/Temp/HtmlToXls/Tabella.htm";
    String excelFilePath = "/Temp/HtmlToXls/output.xlsx";

    try {
      // Carica la pagina HTML
      Document doc = Jsoup.parse(new java.io.File(htmlFilePath), "UTF-8");

      // Trova la tabella (qui presuppone che ce ne sia solo una; altrimenti puoi selezionarla con un id o una classe specifica)
      Element table = doc.select("table").first();
      if (table == null) {
        System.out.println("Nessuna tabella trovata nella pagina HTML.");
        return;
      }

      // Crea il file Excel
      Workbook workbook = new XSSFWorkbook();
      Sheet sheet = workbook.createSheet("Dati");

      // Popola il foglio Excel
      int rowNum = 0;
      for (Element row : table.select("tr")) {
        Row excelRow = sheet.createRow(rowNum++);
        int colNum = 0;

        for (Element cell : row.select("th, td")) {
          Cell excelCell = excelRow.createCell(colNum++);
          Elements links = cell.select("a");

          if (!links.isEmpty()) {
            // Gestione hyperlink
            String linkText = links.text();
            String linkUrl = links.attr("href");

            CreationHelper createHelper = workbook.getCreationHelper();
            Hyperlink hyperlink = createHelper.createHyperlink(HyperlinkType.URL);
            hyperlink.setAddress(linkUrl);

            excelCell.setCellValue(linkText);
            excelCell.setHyperlink(hyperlink);
            CellStyle hlinkStyle = workbook.createCellStyle();
            Font hlinkFont = workbook.createFont();
            hlinkFont.setUnderline(Font.U_SINGLE);
            hlinkFont.setColor(IndexedColors.BLUE.getIndex());
            hlinkStyle.setFont(hlinkFont);
            excelCell.setCellStyle(hlinkStyle);
          } else {
            // Testo normale senza hyperlink
            excelCell.setCellValue(cell.text());
          }
        }
      }

      // Salva il file Excel
      try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
        workbook.write(fileOut);
      }
      workbook.close();
      System.out.println("File Excel creato con successo: " + excelFilePath);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
