package org.example;

import java.io.FileWriter;
import java.io.IOException;
import org.apache.ws.commons.schema.*;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import org.apache.ws.commons.schema.utils.XmlSchemaObjectBase;

public class XMLtoXSD {

  private static int codeCounter = 1;
  private static final Map<XmlSchemaObjectBase, Integer> elementCodes = new HashMap<>();
  private static final Map<XmlSchemaObjectBase, String> elementValues = new HashMap<>();
  private static final Map<XmlSchemaObjectBase, XmlSchemaObjectBase> parentMap = new HashMap<>();
  private static final Map<XmlSchemaObjectBase, String> elementTypes = new HashMap<>();

  public static void main(String[] args) throws Exception {
    XmlSchemaCollection xmlSchemaCollection;
    String xsdPath = "/temp/XmlPillar2/GIR/GLOBEXML_v1.0.xsd";


    // Carica lo schema XSD principale
    xmlSchemaCollection = new XmlSchemaCollection();
    xmlSchemaCollection.setBaseUri("/temp/XmlPillar2/GIR/");
    InputStream is = new FileInputStream(xsdPath);
    XmlSchema schema = xmlSchemaCollection.read(new StreamSource(is));

    // Analizza lo schema principale
    analyzeSchema(schema, null);

    // Analizza tutti gli schemi caricati (compresi quelli importati)
    for (XmlSchema s : xmlSchemaCollection.getXmlSchemas()) {
      if (s != schema) {
        analyzeSchema(s, null);
      }
    }

    // Scrivi la struttura tabellare in un file CSV
    writeResultsToCSV("/temp/XmlPillar2/GIR/output.csv");
  }

  private static void analyzeSchema(XmlSchema schema, XmlSchemaObjectBase parent) {
    for (XmlSchemaObject item : schema.getItems()) {
      if (item instanceof XmlSchemaElement) {
        analyzeElement((XmlSchemaElement) item, parent);
      } else if (item instanceof XmlSchemaType) {
        analyzeType((XmlSchemaType) item, parent);
      }
    }
  }

  private static void analyzeElement(XmlSchemaElement element, XmlSchemaObjectBase parent) {
    int code = codeCounter++;
    elementCodes.put(element, code);
    elementValues.put(element, "");
    parentMap.put(element, parent);
    elementTypes.put(element, "V");

    XmlSchemaType schemaType = element.getSchemaType();
    if (schemaType instanceof XmlSchemaComplexType) {
      XmlSchemaComplexType complexType = (XmlSchemaComplexType) schemaType;
      analyzeComplexType(complexType, element);
    } else if (schemaType instanceof XmlSchemaSimpleType) {
      XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType) schemaType;
      analyzeSimpleType(simpleType, element);
    }
  }

  private static void analyzeComplexType(XmlSchemaComplexType complexType, XmlSchemaObjectBase parent) {
    XmlSchemaParticle particle = complexType.getParticle();

    if (particle instanceof XmlSchemaSequence) {
      XmlSchemaSequence sequence = (XmlSchemaSequence) particle;
      for (XmlSchemaSequenceMember member : sequence.getItems()) {
        if (member instanceof XmlSchemaElement) {
          analyzeElement((XmlSchemaElement) member, parent);
        }
      }
    }

    // Analizza anche gli attributi del tipo complesso
    for (XmlSchemaAttributeOrGroupRef attributeOrGroupRef : complexType.getAttributes()) {
      if (attributeOrGroupRef instanceof XmlSchemaAttribute) {
        XmlSchemaAttribute attribute = (XmlSchemaAttribute) attributeOrGroupRef;
        int attrCode = codeCounter++;
        elementCodes.put(attribute, attrCode);
        elementValues.put(attribute, attribute.getDefaultValue() != null ? attribute.getDefaultValue() : "");
        parentMap.put(attribute, parent);
        elementTypes.put(attribute, "A");
      }
      // Gestisci eventuali gruppi di attributi se necessario
      // else if (attributeOrGroupRef instanceof XmlSchemaAttributeGroupRef) {
      //    // Handle attribute group references if needed
      // }
    }
  }

  private static void analyzeSimpleType(XmlSchemaSimpleType simpleType, XmlSchemaObjectBase parent) {
    // Puoi aggiungere la logica per gestire i SimpleType se necessario
    // Per esempio, estrarre i valori o le restrizioni
  }

  private static void analyzeType(XmlSchemaType type, XmlSchemaObjectBase parent) {
    if (type instanceof XmlSchemaComplexType) {
      analyzeComplexType((XmlSchemaComplexType) type, parent);
    } else if (type instanceof XmlSchemaSimpleType) {
      analyzeSimpleType((XmlSchemaSimpleType) type, parent);
    }
  }

  private static String getElementName(XmlSchemaObjectBase element) {
    if (element instanceof XmlSchemaElement) {
      QName qname = ((XmlSchemaElement) element).getQName();
      return qname != null ? qname.getLocalPart() : "Unnamed Element";
    } else if (element instanceof XmlSchemaAttribute) {
      return ((XmlSchemaAttribute) element).getName();
    }
    return "Unknown Type";
  }

  private static void writeResultsToCSV(String filePath) {
    List<String[]> dataLines = new ArrayList<>();
    dataLines.add(new String[]{"Code", "Element/Attribute", "Type", "Parent", "Value"});

    List<Map.Entry<XmlSchemaObjectBase, Integer>> entries = new ArrayList<>(elementCodes.entrySet());
    entries.sort(Map.Entry.comparingByValue());

    for (Map.Entry<XmlSchemaObjectBase, Integer> entry : entries) {
      XmlSchemaObjectBase element = entry.getKey();
      Integer code = entry.getValue();
      Integer parentCode = null;
      String value = elementValues.getOrDefault(element, "");
      String type = elementTypes.get(element);

      XmlSchemaObjectBase parent = parentMap.get(element);
      if (parent != null) {
        parentCode = elementCodes.get(parent);
      }

      dataLines.add(new String[]{String.valueOf(code), getElementName(element), type, String.valueOf(parentCode), value});
    }

    try (FileWriter csvWriter = new FileWriter(filePath)) {
      for (String[] line : dataLines) {
        csvWriter.append(String.join(",", line));
        csvWriter.append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
