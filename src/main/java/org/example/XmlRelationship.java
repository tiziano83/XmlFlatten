package org.example;

import java.util.HashMap;
import java.util.Map;

public class XmlRelationship {

  private String parentCode;
  private String childCode;
  private boolean isAttribute;
  private boolean isMandatory;
  private String minOccurs;
  private String maxOccurs;
  private String type;
  private Map<String, String> additionalAttributes;

  // Constructor
  public XmlRelationship(String parentCode, String childCode, boolean isAttribute, boolean isMandatory,
    String minOccurs, String maxOccurs, String type, String tagName,
    Map<String, String> additionalAttributes) {
    this.parentCode = parentCode;
    this.childCode = childCode;
    this.isAttribute = isAttribute;
    this.isMandatory = isMandatory;
    this.minOccurs = minOccurs;
    this.maxOccurs = maxOccurs;
    this.type = type;
    this.additionalAttributes = additionalAttributes != null ? additionalAttributes : new HashMap<>();
    // Se ti serve salvare anche il nome del tag, puoi aggiungerlo agli additionalAttributes
    this.additionalAttributes.put("tagName", tagName);
  }

  // Getters
  public String getParentCode() {
    return parentCode;
  }

  public String getChildCode() {
    return childCode;
  }

  public boolean isAttribute() {
    return isAttribute;
  }

  public boolean isMandatory() {
    return isMandatory;
  }

  public String getMinOccurs() {
    return minOccurs;
  }

  public String getMaxOccurs() {
    return maxOccurs;
  }

  public String getType() {
    return type;
  }

  public Map<String, String> getAdditionalAttributes() {
    return additionalAttributes;
  }

  public void setAdditionalAttributes(Map<String, String> additionalAttributes) {
    this.additionalAttributes = additionalAttributes;
  }

  @Override
  public String toString() {
    return String.format("Parent: %s, Child: %s, IsAttribute: %b, IsMandatory: %b, MinOccurs: %s, MaxOccurs: %s, Type: %s, Additional: %s",
      parentCode, childCode, isAttribute, isMandatory, minOccurs, maxOccurs, type, additionalAttributes);
  }
}
