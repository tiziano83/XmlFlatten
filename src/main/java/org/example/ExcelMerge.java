package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;

public class ExcelMerge {

  public static void main(String[] args) {
    // Percorso del file Excel
    String excelFilePath = "/temp/excelmerge/input.xlsx";

    try (FileInputStream fis = new FileInputStream(excelFilePath); Workbook workbook = new XSSFWorkbook(fis)) {

      // Ottieni i due sheet. Si assume che il primo foglio sia "Sheet1" e il secondo "Sheet2".
      Sheet sheet1 = workbook.getSheet("MATCH_SAP_SF");

      for (Row row1 : sheet1) {
        Cell cell1 = row1.getCell(5);

        String tier = "";
        try {
          tier = cell1.getStringCellValue();
        } catch (Exception e) {
          System.out.println("error");
        }
        int index = tier.indexOf("(Tier");
        if (index != -1) {
          tier = tier.substring(index+5,index+7).trim();
        }
        Cell targetCell = row1.getCell(6);
        if (targetCell == null) {
          targetCell = row1.createCell(6);
        }
        targetCell.setCellValue(tier);

      }//      Sheet sheet2 = workbook.getSheet("Customer_IFRS16");
//
//      // Supponiamo che:
//      // - le stringhe da cercare siano nella colonna A (indice 0) di entrambi i fogli;
//      // - le informazioni da prelevare dal secondo foglio siano nella colonna B (indice 1);
//      // - le informazioni da aggiungere nel primo foglio vadano nella colonna B (indice 1).
//
//      // Itera su ogni riga del primo foglio
//      for (Row row1 : sheet1) {
//        Cell cell1 = row1.getCell(1); // colonna A
//        if (cell1 != null && cell1.getCellType() == CellType.STRING) {
//          String searchString = cell1.getStringCellValue();
//
//          // Cerca nel secondo foglio
//          for (Row row2 : sheet2) {
//            Cell cell2 = row2.getCell(1); // colonna A
//            if (cell2 != null && cell2.getCellType() == CellType.STRING) {
//              if (cell2.getStringCellValue().equalsIgnoreCase(searchString)) {
////                String tier = row2.getCell(3).getStringCellValue();
//                String deployment = row2.getCell(7).getStringCellValue();
//////                String area = row2.getCell(6).getStringCellValue();
//                // Trovata la corrispondenza: preleva informazioni dalla colonna B del secondo foglio
//                setcell(row1, deployment);
//                // Esci dal ciclo interno una volta trovata la corrispondenza
//                break;
//              }
//            }
//          }
//        }
//      }

      // Salva il file aggiornato (crea un nuovo file)
      try (FileOutputStream fos = new FileOutputStream("/temp/excelmerge/updated_file"+System.currentTimeMillis()+".xlsx")) {
        workbook.write(fos);
      }
      System.out.println("File aggiornato con successo.");

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void setcell(Row row1, String str) {
    // Aggiungi o aggiorna la cella nella colonna B del primo foglio
    Cell targetCell = row1.getCell(4);
    if (targetCell == null) {
      targetCell = row1.createCell(4);
    }
    targetCell.setCellValue(str);
  }
  private static void setcell2(Row row1, String str) {
    // Aggiungi o aggiorna la cella nella colonna B del primo foglio
    Cell targetCell = row1.getCell(1);
    if (targetCell == null) {
      targetCell = row1.createCell(1);
    }
    targetCell.setCellValue(str);
  }
}
