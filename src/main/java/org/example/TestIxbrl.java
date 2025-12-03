package org.example;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class TestIxbrl {

  public static void main(String[] args) {
    try {
      // Inizializza il documento DOM
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.newDocument();

      // Crea l'elemento <html> con namespace XHTML
      Element html = doc.createElementNS("http://www.w3.org/1999/xhtml", "html");
      html.setAttribute("xmlns:ix", "http://www.xbrl.org/2013/inlineXBRL");
      html.setAttribute("xmlns:xbrli", "http://www.xbrl.org/2003/instance");
      html.setAttribute("xmlns:iso4217", "http://www.xbrl.org/2003/iso4217");
      html.setAttribute("xmlns:ifrs", "http://xbrl.ifrs.org/taxonomy/2021-03-24/ifrs-full");
      doc.appendChild(html);

      // <head><title>Bilancio</title></head>
      Element head = doc.createElement("head");
      Element title = doc.createElement("title");
      title.setTextContent("Bilancio iXBRL");
      head.appendChild(title);
      html.appendChild(head);

      // <body>
      Element body = doc.createElement("body");

      // <h1>Bilancio 2024</h1>
      Element h1 = doc.createElement("h1");
      h1.setTextContent("Bilancio 2024");
      body.appendChild(h1);

      // <p>Ricavi: <ix:nonFraction ...>1000000</ix:nonFraction> EUR</p>
      Element p = doc.createElement("p");
      p.setTextContent("Ricavi: ");
      Element revenue = doc.createElementNS("http://www.xbrl.org/2013/inlineXBRL", "ix:nonFraction");
      revenue.setAttribute("contextRef", "current");
      revenue.setAttribute("name", "ifrs:Revenue");
      revenue.setAttribute("unitRef", "EUR");
      revenue.setAttribute("decimals", "0");
      revenue.setTextContent("1000000");
      p.appendChild(revenue);
      p.appendChild(doc.createTextNode(" EUR"));
      body.appendChild(p);

      html.appendChild(body);

      // Scrittura su file
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(new File("bilancio_ixbrl.xhtml"));
      transformer.transform(source, result);

      System.out.println("File iXBRL generato con successo: bilancio_ixbrl.xhtml");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
