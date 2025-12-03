package org.example;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

public class XMLtoXSD {

  // Contatore per generare i codici in formato 4 cifre
  private static int codeCounter = 1;
  // Utilizziamo XmlSchemaObject come chiave nelle mappe
  private static final Map<XmlSchemaObject, Integer> elementCodes = new HashMap<>();
  // Mappa per tenere traccia della relazione padre–figlio
  private static final Map<XmlSchemaObject, XmlSchemaObject> parentMap = new HashMap<>();
  // Mappa per memorizzare il tipo ("V" per elemento, "A" per attributo)
  private static final Map<XmlSchemaObject, String> elementTypes = new HashMap<>();

  // Lista di XmlRelationship che costituirà l'output finale
  private static final List<XmlRelationship> relationships = new ArrayList<>();

  public static void main(String[] args) throws Exception {
    XmlSchemaCollection xmlSchemaCollection;
    String xsdPath = "/temp/XMLValidator/GIRBelgio/qdmtt_declaration v0.8.1.xsd";
    String xsdString;

    // Lettura dello schema XSD da file
    try (InputStreamReader reader = new InputStreamReader(new FileInputStream(xsdPath))) {
      StringBuilder sb = new StringBuilder();
      int ch;
      while ((ch = reader.read()) != -1) {
        sb.append((char) ch);
      }
      xsdString = sb.toString();
    }

    // Rimuovo i percorsi (path) dagli import
    xsdString = removePathFromImport(xsdString);

    // Carica lo schema XSD principale
    xmlSchemaCollection = new XmlSchemaCollection();
    xmlSchemaCollection.setBaseUri("/temp/XMLValidator/GIRBelgio/");
    XmlSchema schema = xmlSchemaCollection.read(new StringReader(xsdString));

    // Analizza lo schema principale
    analyzeSchema(schema, null);

    // Analizza anche gli eventuali schemi importati
    for (XmlSchema s : xmlSchemaCollection.getXmlSchemas()) {
      if (s != schema) {
        analyzeSchema(s, null);
      }
    }
    try (BufferedWriter writer = new BufferedWriter(new FileWriter("/temp/outputXsdXml.txt"))) {
      // Al termine dell'analisi, stampiamo la lista delle relazioni
      for (XmlRelationship rel : relationships) {
        if (rel == null) {
          continue;
        }
        String line = rel.toString();
        System.out.println(line);
        writer.write(line);
        writer.newLine();
      }
    }
  }

  private static void analyzeSchema(XmlSchema schema, XmlSchemaObject parent) throws IOException {
    for (Object obj : schema.getItems()) {
      if (obj instanceof XmlSchemaElement) {
        analyzeElement((XmlSchemaElement) obj, parent);
      } else if (obj instanceof XmlSchemaType) {
        analyzeType((XmlSchemaType) obj, parent);
      }
    }
  }

  private static void analyzeElement(XmlSchemaElement element, XmlSchemaObject parent) throws IOException {
    // Assegna un codice formattato a 4 cifre
    int code = codeCounter++;
    elementCodes.put(element, code);
    // Registra il parent
    parentMap.put(element, parent);
    // Tipologia: "V" per elemento
    elementTypes.put(element, "V");

    // Recupera minOccurs e maxOccurs (default "1" se non specificato)
    String minOccurs = String.valueOf(element.getMinOccurs());
    String maxOccurs = String.valueOf(element.getMaxOccurs());
    // L'elemento è obbligatorio se minOccurs != 0
    boolean isMandatory = !minOccurs.equals("0");
    String documentation = getDocumentation(element);
    if (documentation != null) {
      System.out.println("Documentation: " + documentation);
    }
    XmlRelationship xmlR = null;
    if (code == 1) {
      String parentCodeStr = String.format("%04d", elementCodes.get(parent));
      String childCodeStr = String.format("%04d", code);
      xmlR =
        new XmlRelationship(parentCodeStr, childCodeStr, false, isMandatory, minOccurs, maxOccurs, "V", getElementName(element),
          new HashMap<>(), documentation, null, null, null);
      relationships.add(xmlR);
    }
    // Se l'elemento ha un padre, aggiungo la relazione
    if (parent != null) {
      String parentCodeStr = String.format("%04d", elementCodes.get(parent));
      String childCodeStr = String.format("%04d", code);
      xmlR =
        new XmlRelationship(parentCodeStr, childCodeStr, false, isMandatory, minOccurs, maxOccurs, "V", getElementName(element),
          new HashMap<>(), documentation, null, null, null);
    }

    System.out.println("Analyzing element: " + getElementName(element) + " (code " + String.format("%04d", code) + "), parent: " +
                         (parent != null ? getElementName(parent) : "none") + ", minOccurs: " + minOccurs + ", maxOccurs: " +
                         maxOccurs + " documentation: " + documentation);

    // Se l'elemento ha un tipo complesso, analizzo i suoi figli
    XmlSchemaType schemaType = element.getSchemaType();
    if (schemaType instanceof XmlSchemaComplexType) {
      XmlSchemaComplexType complexType = (XmlSchemaComplexType) schemaType;
      relationships.add(analyzeComplexType(complexType, element));
    } else if (schemaType instanceof XmlSchemaSimpleType) {
      XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType) schemaType;
      Map<String, String> valuesForSimpleType = analyzeSimpleType(simpleType, element);
      if (xmlR != null) {
        xmlR.setMarkup(valuesForSimpleType.get("markup"));
        xmlR.setBaseTipe(valuesForSimpleType.get("basetype"));
        xmlR.setEnumValue(valuesForSimpleType.get("enumvalue"));
      }
    }
    relationships.add(xmlR);
  }

  private static XmlRelationship analyzeComplexType(XmlSchemaComplexType complexType, XmlSchemaObject parent) throws IOException {
    if (complexType.getParticle() instanceof XmlSchemaSequence) {
      XmlSchemaSequence sequence = (XmlSchemaSequence) complexType.getParticle();
      for (XmlSchemaSequenceMember member : sequence.getItems()) {
        if (member instanceof XmlSchemaElement) {
          analyzeElement((XmlSchemaElement) member, parent);
        }
      }
    }

    // Analizza gli attributi
    for (Object attrObj : complexType.getAttributes()) {
      if (attrObj instanceof XmlSchemaAttribute) {
        XmlSchemaAttribute attribute = (XmlSchemaAttribute) attrObj;
        int attrCode = codeCounter++;
        elementCodes.put(attribute, attrCode);
        elementTypes.put(attribute, "A");
        parentMap.put(attribute, parent);

        String minOccurs = "1";
        String maxOccurs = "1";
        boolean isMandatory = (attribute.getUse() != null && attribute.getUse().toString().equals("required"));

        String documentation = getDocumentation(attribute);
        if (documentation != null) {
          System.out.println("Documentation: " + documentation);
        }

        if (parent != null) {
          String parentCodeStr = String.format("%04d", elementCodes.get(parent));
          String childCodeStr = String.format("%04d", attrCode);
          return new XmlRelationship(parentCodeStr, childCodeStr, true, isMandatory, minOccurs, maxOccurs, "A",
            getElementName(attribute), new HashMap<>(), documentation, null, null, null);
        }

        System.out.println(
          "Analyzing attribute: " + getElementName(attribute) + " (code " + String.format("%04d", attrCode) + "), parent: " +
            (parent != null ? getElementName(parent) : "none") + ", isMandatory: " + isMandatory + " documentation: " +
            documentation);
      }
    }
    return null;
  }

  //  private static void analyzeSimpleType(XmlSchemaSimpleType simpleType, XmlSchemaObject parent) {
  //    String markup = ((XmlSchemaDocumentation) simpleType.getAnnotation().getItems().get(0)).getMarkup().item(0).toString();
  //    String test = (XmlSchemaSimpleTypeRestriction)((XmlSchemaSimpleType)simpleType).getContent();
  //    simpleType.getContent().toString();
  //    System.out.println("Analyzing simple type for element: " + getElementName(parent));
  //  }

  private static Map<String, String> analyzeSimpleType(XmlSchemaSimpleType simpleType, XmlSchemaObject parent) throws IOException {

    Map<String, String> retVal = new HashMap<>();
    String typeName = simpleType.getName() != null ? simpleType.getName() : "UnnamedSimpleType";
    String info = "Analyzing simple type: " + typeName;
    System.out.println(info);
    //    writer.write(info);
    //    writer.newLine();

    String markup = null;
    try {
      markup = ((XmlSchemaDocumentation) simpleType.getAnnotation().getItems().get(0)).getMarkup().item(0).toString();
    } catch (Exception e) {
      markup = "not found";
    }
    System.out.println("markup " + markup);
    retVal.put("markup", markup);

    // Documentazione
    String documentation = getDocumentation(simpleType);
    if (documentation != null) {
      String docLine = "Documentation: " + documentation;
      retVal.put("documentation", documentation);
      System.out.println(docLine);
      //      writer.write(docLine);
      //      writer.newLine();
    }

    // Restriction e enumerazioni
    if (simpleType.getContent() instanceof org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction) {
      org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction restriction =
        (org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction) simpleType.getContent();

      String baseType = restriction.getBaseTypeName() != null ? restriction.getBaseTypeName().toString() : "UnknownBase";
      String baseLine = "Base type: " + baseType;
      retVal.put("basetype", baseType);
      System.out.println(baseLine);
      //      writer.write(baseLine);
      //      writer.newLine();

      // Enumerazioni
      for (Object facetObj : restriction.getFacets()) {
        if (facetObj instanceof org.apache.ws.commons.schema.XmlSchemaEnumerationFacet) {
          org.apache.ws.commons.schema.XmlSchemaEnumerationFacet enumFacet =
            (org.apache.ws.commons.schema.XmlSchemaEnumerationFacet) facetObj;
          String enumValue = enumFacet.getValue().toString();
          String enumLine = "Enumeration value: " + enumValue;
          retVal.put("enumvalue", enumValue);
          System.out.println(enumLine);
          //          writer.write(enumLine);
          //          writer.newLine();
        }
      }
    }
    return retVal;
  }

  private static void analyzeType(XmlSchemaType type, XmlSchemaObject parent) throws IOException {
    if (type instanceof XmlSchemaComplexType) {
      analyzeComplexType((XmlSchemaComplexType) type, parent);
    } else if (type instanceof XmlSchemaSimpleType) {
      analyzeSimpleType((XmlSchemaSimpleType) type, parent);
    }
  }

  private static String getElementName(XmlSchemaObject element) {
    if (element instanceof XmlSchemaElement) {
      QName qname = ((XmlSchemaElement) element).getQName();
      return qname != null ? qname.getLocalPart() : "Unnamed Element";
    } else if (element instanceof XmlSchemaAttribute) {
      return ((XmlSchemaAttribute) element).getName();
    }
    return "Unknown Type";
  }

  public static String removePathFromImport(String xsdContent) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    DOMImplementationLS domImplLS = (DOMImplementationLS) builder.getDOMImplementation().getFeature("LS", "3.0");

    LSParser parser = domImplLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
    LSInput lsInput = domImplLS.createLSInput();
    lsInput.setCharacterStream(new StringReader(xsdContent));
    Document doc = parser.parse(lsInput);

    NodeList imports = doc.getElementsByTagName("xs:import");
    for (int i = 0; i < imports.getLength(); i++) {
      Element importElement = (Element) imports.item(i);
      String schemaLocation = importElement.getAttribute("schemaLocation");
      if (!schemaLocation.isEmpty()) {
        String fileName = schemaLocation.substring(schemaLocation.lastIndexOf('/') + 1);
        importElement.setAttribute("schemaLocation", fileName);
      }
    }

    LSSerializer serializer = domImplLS.createLSSerializer();
    return serializer.writeToString(doc);
  }

  private static String getDocumentation(XmlSchemaObject obj) {
    if (obj instanceof XmlSchemaElement) {
      XmlSchemaElement element = (XmlSchemaElement) obj;
      if (element.getAnnotation() != null) {
        for (Object item : element.getAnnotation().getItems()) {
          if (item instanceof org.apache.ws.commons.schema.XmlSchemaDocumentation) {
            org.apache.ws.commons.schema.XmlSchemaDocumentation doc = (org.apache.ws.commons.schema.XmlSchemaDocumentation) item;
            if (doc.getMarkup() != null && doc.getMarkup().getLength() > 0) {
              return doc.getMarkup().item(0).getTextContent();
            }
          }
        }
      }
    } else if (obj instanceof XmlSchemaAttribute) {
      XmlSchemaAttribute attribute = (XmlSchemaAttribute) obj;
      if (attribute.getAnnotation() != null) {
        for (Object item : attribute.getAnnotation().getItems()) {
          if (item instanceof org.apache.ws.commons.schema.XmlSchemaDocumentation) {
            org.apache.ws.commons.schema.XmlSchemaDocumentation doc = (org.apache.ws.commons.schema.XmlSchemaDocumentation) item;
            if (doc.getMarkup() != null && doc.getMarkup().getLength() > 0) {
              return doc.getMarkup().item(0).getTextContent();
            }
          }
        }
      }
    }
    return null;
  }

}
