package org.example;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringWriter;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XsdCombiner {
  private final Set<String> processedSchemas = new HashSet<>();
  private final String baseDirectory;

  public XsdCombiner(String baseDirectory) {
    this.baseDirectory = baseDirectory;
  }

  public String combineXsdSchemas(String mainSchemaPath) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();

    // Carica lo schema principale
    Document mainDoc = builder.parse(new File(baseDirectory, mainSchemaPath));
    processImports(mainDoc, builder);

    // Converti il documento in stringa
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(mainDoc), new StreamResult(writer));

    return writer.toString();
  }

  private void processImports(Document doc, DocumentBuilder builder) throws Exception {
    NodeList imports = doc.getElementsByTagName("xs:import");
    List<Element> importsToRemove = new ArrayList<>();

    for (int i = 0; i < imports.getLength(); i++) {
      Element importElement = (Element) imports.item(i);
      String schemaLocation = importElement.getAttribute("schemaLocation");

      if (!processedSchemas.contains(schemaLocation)) {
        processedSchemas.add(schemaLocation);

        // Carica lo schema importato
        File importedSchemaFile = new File(baseDirectory, schemaLocation);
        if (importedSchemaFile.exists()) {
          Document importedDoc = builder.parse(importedSchemaFile);

          // Processa ricorsivamente gli import dello schema importato
          processImports(importedDoc, builder);

          // Aggiungi gli elementi dello schema importato allo schema principale
          NodeList schemaChildren = importedDoc.getDocumentElement().getChildNodes();
          for (int j = 0; j < schemaChildren.getLength(); j++) {
            if (schemaChildren.item(j).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
              Element imported = (Element) doc.importNode(schemaChildren.item(j), true);
              doc.getDocumentElement().appendChild(imported);
            }
          }
        }

        // Marca questo import per la rimozione
        importsToRemove.add(importElement);
      }
    }

    // Rimuovi gli elementi import dopo aver processato tutto
    for (Element element : importsToRemove) {
      element.getParentNode().removeChild(element);
    }
  }

  // Metodo main di esempio
  public static void main(String[] args) {
    try {
      XsdCombiner combiner = new XsdCombiner("/temp/XMLValidator/Italy/");
      String combinedXsd = combiner.combineXsdSchemas("fornituraCbCR_v2.0.xsd");
      System.out.println(combinedXsd);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}