package org.example;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class XmlFlatten {

  public static void main(String[] args) {
    Map<String, NodeDTO> flatNodesMap = new LinkedHashMap<>();
    try {
      Path filePath = Path.of("/temp/Balance Sheet - Field ID.xml");

   //   byte[] bytes = Files.readString(filePath).getBytes(StandardCharsets.UTF_8);
      byte[] bytes = Files.readString(filePath,StandardCharsets.UTF_16LE).getBytes(StandardCharsets.UTF_16LE);
      // byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
      try (InputStream input = new ByteArrayInputStream(bytes)) {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
        Element root = document.getDocumentElement();
        flatNodesMap.put(root.getNodeName(), null);
        flattXml(root, flatNodesMap);
      }

      printResults(flatNodesMap);
      generateXmlStructure(flatNodesMap);
    } catch (IOException | ParserConfigurationException | SAXException e) {
      e.printStackTrace();
    }
  }

  private static void printResults(Map<String, NodeDTO> flatNodesMap) throws IOException {
    Path file = Paths.get("/temp/output.txt");
    try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      for (Entry<String, NodeDTO> elem : flatNodesMap.entrySet()) {
        if (elem.getValue() == null) {
          String s = "rootnode=" + elem.getKey();
          writer.write(s);
          writer.newLine();
          System.out.println(s);
        } else {
          String s = "node=" + elem.getKey() + " parent=" + elem.getValue().getParent();
          writer.write(s);
          writer.newLine();
          System.out.println(s);
          if (!elem.getValue().getAttribList().isEmpty()) {
            s = "attributes:";
            System.out.println(s);
            writer.write(s);
            writer.newLine();

            elem.getValue().getAttribList().forEach(s1 -> {
              try {
                System.out.println(s1);
                writer.write(s1);
                writer.newLine();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
          }
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private static void generateXmlStructure(Map<String, NodeDTO> flatNodesMap) throws IOException {
    Path file = Paths.get("/temp/xmlStructure.csv");
    Long prog = 1L;
    Map<String,String> itemCodeMap = new HashMap<>();
    try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      for (Entry<String, NodeDTO> elem : flatNodesMap.entrySet()) {
        if (elem.getValue() == null) {
          String s = createCsvRow(prog, elem.getKey(),"",itemCodeMap);
          prog++;
          writer.write(s);
          writer.newLine();
          System.out.println(s);
        } else {
          String s = createCsvRow(prog, elem.getKey(),elem.getValue().getParent(),itemCodeMap);
          prog++;
          writer.write(s);
          writer.newLine();
          System.out.println(s);
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private static String createCsvRow(Long prog, String tagName, String parentItem,
    Map<String, String> itemCodeMap) {
    String itemCode =  String.format("%06d", prog);
    itemCodeMap.put(tagName,itemCode);
    String parent = "";
    if(!parentItem.isEmpty())
      parent = itemCodeMap.get(parentItem);
    return String.format("%s;%s;%s;%s;",itemCode,cleanNodeName(tagName),cleanNodeName(tagName),parent);
  }

  private static String cleanNodeName(String tagName) {
    try {
      String[] n = tagName.split(":");
      return n[1];
    } catch (Exception e) {
      System.out.println("error "+e.getMessage());
      return tagName;
    }
  }

  private static void flattXml(Node currentNode, Map<String, NodeDTO> flatNodesMap) {
    NodeList childNodes = currentNode.getChildNodes();
    int length = childNodes.getLength();
    for (int i = 0; i < length; i++) {
      Node item = childNodes.item(i);
      if (item.getNodeType() == Node.ELEMENT_NODE) {
        NamedNodeMap n = currentNode.getAttributes();
        List<String> attrList = new ArrayList<>();
        for (int j = 0; j < n.getLength(); j++) {
          attrList.add(n.item(j).getNodeName());
        }
        NodeDTO ndto = new NodeDTO(currentNode.getNodeName(), attrList);
        flatNodesMap.put(item.getNodeName(), ndto);

        flattXml(item, flatNodesMap);
      } else {
        //  System.out.println("***item type>" + item.getNodeType());
      }
    }
/*    if (currentNode.getNodeType() == Node.TEXT_NODE &&
          !currentNode.getNodeValue().trim().isEmpty()) {
          flatNodesMap.put(currentNode.getNodeName(),currentPath);
      System.out.println(currentPath + "=" + currentNode.getNodeValue());
    } else {
      NodeList childNodes = currentNode.getChildNodes();
      int length = childNodes.getLength();
      String nextPath = currentPath.isEmpty()
                          ? currentNode.getNodeName()
                          : currentPath + "." + currentNode.getNodeName();
      for (int i = 0; i < length; i++) {
        Node item = childNodes.item(i);
        flattXml(nextPath, item,flatNodesMap);
      }
    }*/
  }

  public static class NodeDTO {

    private String parent;

    private List<String> attribList;

    public NodeDTO(String parent, List<String> attribList) {
      this.parent = parent;
      this.attribList = attribList;
    }

    public String getParent() {
      return parent;
    }

    public void setParent(String parent) {
      this.parent = parent;
    }

    public List<String> getAttribList() {
      return attribList;
    }

    public void setAttribList(List<String> attribList) {
      this.attribList = attribList;
    }
  }

}