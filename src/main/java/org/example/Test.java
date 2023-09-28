package org.example;

import java.math.BigDecimal;

public class Test {
  private String campo1;
  private String campo2;
  private BigDecimal valoreNumerico;

  public Test(String campo1, String campo2, BigDecimal valoreNumerico) {
    this.campo1 = campo1;
    this.campo2 = campo2;
    this.valoreNumerico = valoreNumerico;
  }

  public String getCampo1() {
    return campo1;
  }

  public void setCampo1(String campo1) {
    this.campo1 = campo1;
  }

  public String getCampo2() {
    return campo2;
  }

  public void setCampo2(String campo2) {
    this.campo2 = campo2;
  }

  public BigDecimal getValoreNumerico() {
    return valoreNumerico;
  }

  public void setValoreNumerico(BigDecimal valoreNumerico) {
    this.valoreNumerico = valoreNumerico;
  }
}
