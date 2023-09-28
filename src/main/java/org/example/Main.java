package org.example;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

  public static void main(String[] args) {

        Test t = new Test("A", "X", BigDecimal.valueOf(10));
        Test t1 = new Test("A", "Y",BigDecimal.valueOf(20));
        Test t3 = new Test("A", "Y",BigDecimal.valueOf(2));

        List<Test> records = Arrays.asList(
          t,t1,t3
          // Altri record
        );

    Map<List<String>, BigDecimal> result = records.stream()
                                             .collect(Collectors.groupingBy(
                                               test -> Arrays.asList(test.getCampo1(), test.getCampo2()),
                                               Collectors.reducing(
                                                 BigDecimal.ZERO,
                                                 Test::getValoreNumerico,
                                                 BigDecimal::add
                                               )
                                             ));

        // Stampa il risultato
        result.forEach((campi, somma) -> {
          System.out.println("Campi: " + campi + ", Somma: " + somma);
        });
      }



}