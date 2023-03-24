package org.example;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

public class XmlValidation {

  public static void main(String[] args) {

    System.out.println("TCBCRV2_SAMPLE_00101.xml validates against CbcXML_v2.0.xsd? "+validateXMLSchema("/temp/XMLValidator/fornituraCbCR_v2.0.xsd", "/temp/XMLValidator/EXPORT.xml"));


  }

  public static boolean validateXMLSchema(String xsdPath, String xmlPath){

    try {
      SchemaFactory factory =
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = factory.newSchema(new File(xsdPath));
      Validator validator = schema.newValidator();
      validator.validate(new StreamSource(new File(xmlPath)));
    } catch (IOException | SAXException e) {
      System.out.println("Exception: "+e.getMessage());
      return false;
    }
    return true;
  }
}