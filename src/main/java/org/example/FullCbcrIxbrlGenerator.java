package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FullCbcrIxbrlGenerator {

  static class Row {
    String countryCode, entityName, activities;
    long revenues, revRp, revTp, profit, taxAccrued, taxPaid, assets, earnings, employees;
    Row(String cc, String en, String act, long rev, long rp, long tp, long pr, long acc, long paid, long assets, long earn, long emp) {
      this.countryCode = cc; this.entityName = en; this.activities = act;
      this.revenues = rev; this.revRp = rp; this.revTp = tp; this.profit = pr;
      this.taxAccrued = acc; this.taxPaid = paid; this.assets = assets; this.earnings = earn; this.employees = emp;
    }
  }

  public static void main(String[] args) throws Exception {
    List<Row> rows = List.of(
      new Row("FR","Entity France","Manufacturing",5000000,1000000,4000000,800000,210000,200000,1200000,500000,120),
      new Row("IT","Entity Italy","Sales & Distribution",3000000,500000,2500000,500000,160000,150000,800000,300000,80)
    );

    Document doc = Jsoup.parse("", "", org.jsoup.parser.Parser.xmlParser());
    doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
    Element html = doc.appendElement("html")
                     .attr("xmlns","http://www.w3.org/1999/xhtml")
                     .attr("xmlns:ix","http://www.xbrl.org/2013/inlineXBRL")
                     .attr("xmlns:cbcr","http://publiccbcr.eu/taxonomy")
                     .attr("xmlns:xbrli","http://www.xbrl.org/2003/instance");

    html.appendElement("head")
      .appendElement("title").text("EU Public CbCR 2024 Report")
      .parent().appendElement("style").text(
        "body{font-family:Arial;font-size:12pt;}table{border-collapse:collapse;width:100%;}th,td{border:1px solid #666;padding:4px;}"
      );

    Element body = html.appendElement("body");
    body.appendElement("h1").text("Public Country-by-Country Report – Example Corp");

    Element ctx = body.appendElement("xbrli:context").attr("id","ctx2024");
    ctx.appendElement("xbrli:entity")
      .appendElement("xbrli:identifier")
      .attr("scheme","http://example.com")
      .text("ExampleCorpID");
    Element period = ctx.appendElement("xbrli:period");
    period.appendElement("xbrli:startDate").text("2024-01-01");
    period.appendElement("xbrli:endDate").text("2024-12-31");

    body.appendElement("xbrli:unit").attr("id","EUR")
      .appendElement("xbrli:measure").text("iso4217:EUR");

    Element table = body.appendElement("table");
    String[] headers = {"CountryCode","EntityName","Revenues","RevenueRelatedPartyTransactions","RevenuesThirdParty",
      "ProfitLossBeforeTax","IncomeTaxAccrued","IncomeTaxPaid","TangibleAssets","AccumulatedEarnings","Employees","NatureOfActivities"};
    Element trh = table.appendElement("tr");
    for (String h : headers) trh.appendElement("th").text(h);

    for (Row r : rows) {
      Element tr = table.appendElement("tr");
      tr.appendElement("td").appendElement("ix:nonNumeric")
        .attr("name","cbcr:CountryCode").attr("contextRef","ctx2024").text(r.countryCode);
      tr.appendElement("td").appendElement("ix:nonNumeric")
        .attr("name","cbcr:EntityName").attr("contextRef","ctx2024").text(r.entityName);
      // numeric fields
      for (long[] values : List.of(
        new long[]{r.revenues}, new long[]{r.revRp}, new long[]{r.revTp},
        new long[]{r.profit}, new long[]{r.taxAccrued}, new long[]{r.taxPaid},
        new long[]{r.assets}, new long[]{r.earnings}, new long[]{r.employees}
      )) {
        String name = headers[2 + table.children().size() - 2]; // simplified
      }
      // better to write each individually:
      tr.appendElement("td").appendElement("ix:nonFraction")
        .attr("name","cbcr:Revenues").attr("contextRef","ctx2024").attr("unitRef","EUR").attr("decimals","0").text(Long.toString(r.revenues));
      tr.appendElement("td").appendElement("ix:nonFraction")
        .attr("name","cbcr:RevenueRelatedPartyTransactions").attr("contextRef","ctx2024").attr("unitRef","EUR").attr("decimals","0").text(Long.toString(r.revRp));
      tr.appendElement("td").appendElement("ix:nonFraction")
        .attr("name","cbcr:RevenuesThirdParty").attr("contextRef","ctx2024").attr("unitRef","EUR").attr("decimals","0").text(Long.toString(r.revTp));
      tr.appendElement("td").appendElement("ix:nonFraction")
        .attr("name","cbcr:ProfitLossBeforeTax").attr("contextRef","ctx2024").attr("unitRef","EUR").attr("decimals","0").text(Long.toString(r.profit));
      tr.appendElement("td").appendElement("ix:nonFraction")
        .attr("name","cbcr:IncomeTaxAccrued").attr("contextRef","ctx2024").attr("unitRef","EUR").attr("decimals","0").text(Long.toString(r.taxAccrued));
      tr.appendElement("td").appendElement("ix:nonFraction")
        .attr("name","cbcr:IncomeTaxPaid").attr("contextRef","ctx2024").attr("unitRef","EUR").attr("decimals","0").text(Long.toString(r.taxPaid));
      tr.appendElement("td").appendElement("ix:nonFraction")
        .attr("name","cbcr:TangibleAssets").attr("contextRef","ctx2024").attr("unitRef","EUR").attr("decimals","0").text(Long.toString(r.assets));
      tr.appendElement("td").appendElement("ix:nonFraction")
        .attr("name","cbcr:AccumulatedEarnings").attr("contextRef","ctx2024").attr("unitRef","EUR").attr("decimals","0").text(Long.toString(r.earnings));
      tr.appendElement("td").appendElement("ix:nonFraction")
        .attr("name","cbcr:Employees").attr("contextRef","ctx2024").attr("unitRef","EUR").attr("decimals","0").text(Long.toString(r.employees));
      tr.appendElement("td").appendElement("ix:nonNumeric")
        .attr("name","cbcr:NatureOfActivities").attr("contextRef","ctx2024").text(r.activities);
    }

    try (FileOutputStream fos = new FileOutputStream("cbcr_full_report.xhtml")) {
      fos.write(doc.outerHtml().getBytes(StandardCharsets.UTF_8));
    }
    System.out.println("Generato cbcr_full_report.xhtml");
  }
}
