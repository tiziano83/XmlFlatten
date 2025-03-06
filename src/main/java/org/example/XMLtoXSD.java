package org.example;

import java.io.FileInputStream;
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
    String xsdPath = "/temp/XMLValidator/Italy/fornituraCbCR_v2.0.xsd";
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
    xmlSchemaCollection.setBaseUri("/temp/XMLValidator/Italy/");
    XmlSchema schema = xmlSchemaCollection.read(new StringReader(xsdString));

    // Analizza lo schema principale
    analyzeSchema(schema, null);

    // Analizza anche gli eventuali schemi importati
    for (XmlSchema s : xmlSchemaCollection.getXmlSchemas()) {
      if (s != schema) {
        analyzeSchema(s, null);
      }
    }

    // Al termine dell'analisi, stampiamo la lista delle relazioni
    for (XmlRelationship rel : relationships) {
      System.out.println(rel);
    }
  }

  private static void analyzeSchema(XmlSchema schema, XmlSchemaObject parent) {
    for (Object obj : schema.getItems()) {
      if (obj instanceof XmlSchemaElement) {
        analyzeElement((XmlSchemaElement) obj, parent);
      } else if (obj instanceof XmlSchemaType) {
        analyzeType((XmlSchemaType) obj, parent);
      }
    }
  }

  private static void analyzeElement(XmlSchemaElement element, XmlSchemaObject parent) {
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

    // Se l'elemento ha un padre, aggiungo la relazione
    if (parent != null) {
      String parentCodeStr = String.format("%04d", elementCodes.get(parent));
      String childCodeStr = String.format("%04d", code);
      relationships.add(new XmlRelationship(
        parentCodeStr,
        childCodeStr,
        false,
        isMandatory,
        minOccurs,
        maxOccurs,
        "V",
        getElementName(element),
        new HashMap<>()
      ));
    }

    System.out.println("Analyzing element: " + getElementName(element)
                         + " (code " + String.format("%04d", code) + "), parent: " + (parent != null ? getElementName(parent) : "none")
                         + ", minOccurs: " + minOccurs + ", maxOccurs: " + maxOccurs);

    // Se l'elemento ha un tipo complesso, analizzo i suoi figli
    XmlSchemaType schemaType = element.getSchemaType();
    if (schemaType instanceof XmlSchemaComplexType) {
      XmlSchemaComplexType complexType = (XmlSchemaComplexType) schemaType;
      analyzeComplexType(complexType, element);
    } else if (schemaType instanceof XmlSchemaSimpleType) {
      XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType) schemaType;
      analyzeSimpleType(simpleType, element);
    }
  }

  private static void analyzeComplexType(XmlSchemaComplexType complexType, XmlSchemaObject parent) {
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

        if (parent != null) {
          String parentCodeStr = String.format("%04d", elementCodes.get(parent));
          String childCodeStr = String.format("%04d", attrCode);
          relationships.add(new XmlRelationship(
            parentCodeStr,
            childCodeStr,
            true,
            isMandatory,
            minOccurs,
            maxOccurs,
            "A",
            getElementName(attribute),
            new HashMap<>()
          ));
        }

        System.out.println("Analyzing attribute: " + getElementName(attribute)
                             + " (code " + String.format("%04d", attrCode) + "), parent: " + (parent != null ? getElementName(parent) : "none")
                             + ", isMandatory: " + isMandatory);
      }
    }
  }

  private static void analyzeSimpleType(XmlSchemaSimpleType simpleType, XmlSchemaObject parent) {
    System.out.println("Analyzing simple type for element: " + getElementName(parent));
  }

  private static void analyzeType(XmlSchemaType type, XmlSchemaObject parent) {
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
}
