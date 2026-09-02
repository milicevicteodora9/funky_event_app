package com.example.funkyeventapp.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.Receipt;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReceiptTextParserTest {
    @Test public void parsesSerbianLatinReceiptWithAmountOnNextLine() {
        Receipt receipt = ReceiptTextParser.parse("FISKALNI RAČUN\nFUNKY MARKET DOO\nPIB 123456789\n"
                + "Datum 02.09.2026\nUKUPAN IZNOS\n1.234,56 RSD\nPDV 205,76");

        assertEquals("FUNKY MARKET DOO", receipt.getSeller());
        assertEquals(new BigDecimal("1234.56"), receipt.getTotalAmount());
        assertEquals(LocalDate.of(2026, 9, 2), receipt.getIssueDate());
        assertEquals(Currency.RSD, receipt.getCurrency());
    }

    @Test public void parsesEnglishReceiptAndRejectsSubtotal() {
        Receipt receipt = ReceiptTextParser.parse("RECEIPT\nACME STORE LLC\nSUBTOTAL 900.00\n"
                + "TAX TOTAL 90.00\nGRAND TOTAL $990.00\n2026-08-31");

        assertEquals("ACME STORE LLC", receipt.getSeller());
        assertEquals(new BigDecimal("990.00"), receipt.getTotalAmount());
        assertEquals(LocalDate.of(2026, 8, 31), receipt.getIssueDate());
        assertEquals(Currency.USD, receipt.getCurrency());
    }

    @Test public void parsesCyrillicReceipt() {
        Receipt receipt = ReceiptTextParser.parse("ФИСКАЛНИ РАЧУН\nПЕКАРА СУНЦЕ ДОО\n"
                + "ДАТУМ 1.9.26\nЗА УПЛАТУ 850,00 RSD");

        assertEquals("ПЕКАРА СУНЦЕ ДОО", receipt.getSeller());
        assertEquals(new BigDecimal("850.00"), receipt.getTotalAmount());
        assertEquals(LocalDate.of(2026, 9, 1), receipt.getIssueDate());
    }

    @Test public void leavesAmountEmptyWithoutReliableTotalLabel() {
        Receipt receipt = ReceiptTextParser.parse("CAFE TEST PR\nEspresso 220,00\nVoda 180,00");
        assertNull(receipt.getTotalAmount());
    }

    @Test public void parsesProvidedSerbianFiscalLayoutAndDegradedCyrillicLabel() {
        Receipt receipt = ReceiptTextParser.parse("FISKALNI RACUN\n101654096\nBILANS-MICON\n"
                + "1020024-lokal\nSTRANILOVSKA 33\nNOVI SAD\nNaziv Cena Kol. Ukupno\n"
                + "Kolor stampa A4 30.00 120 3600.00\nPapir A4 6.00 120 720.00\n"
                + "3a ynnary: 4320.00\nGotovina 4320.00\nUkupan iznos poreza: 720.00\n"
                + "28.07.2026 19:08:54");

        assertEquals("BILANS-MICON", receipt.getSeller());
        assertEquals(new BigDecimal("4320.00"), receipt.getTotalAmount());
        assertEquals(LocalDate.of(2026, 7, 28), receipt.getIssueDate());
        assertEquals(Currency.RSD, receipt.getCurrency());
    }

    @Test public void fallsBackToCashAndRepeatedLargestAmountWhenPaymentLabelIsUnreadable() {
        Receipt receipt = ReceiptTextParser.parse("QИCKANH PA4YH\n101654096\nBILANS-MICON\n"
                + "Naziv Cena Kol. Ukupno\nKolor stampa 30.00 120 3600.00\n"
                + "Papir 6.00 120 720.00\n3a yплaтy 4320.00\nGotovina 4320.00\n"
                + "Ukupan iznos poreza 720.00\n28.07.2026");

        assertEquals("BILANS-MICON", receipt.getSeller());
        assertEquals(new BigDecimal("4320.00"), receipt.getTotalAmount());
        assertEquals(Currency.RSD, receipt.getCurrency());
    }

    @Test public void ignoresRepeatedTaxAndUsesCashMinusChangeWhenTotalLabelIsUnreadable() {
        Receipt receipt = ReceiptTextParser.parse("ФИСКАЛНИ РАЧУН\nSTATOVAC-KOMERC D.O.O.\n"
                + "НЕЧИТЉИВА ОЗНАКА\n7.701,00\nГотовина:\n8.000,00\nПовраћај:\n299,00\n"
                + "Ознака Име Стопа Порез\nЂ О-ПДВ 20,00% 1.283,50\n"
                + "Укупан износ пореза:\n1.283,50\nПФР време: 28.07.2026 19:37:06");

        assertEquals("STATOVAC-KOMERC D.O.O.", receipt.getSeller());
        assertEquals(new BigDecimal("7701.00"), receipt.getTotalAmount());
        assertEquals(LocalDate.of(2026, 7, 28), receipt.getIssueDate());
    }

    @Test public void prioritizesReceiptTotalOverTaxTotal() {
        Receipt receipt = ReceiptTextParser.parse("STATOVAC-KOMERC D.O.O.\n"
                + "Укупан износ: 7.701,00\nГотовина: 8.000,00\nПовраћај: 299,00\n"
                + "Порез 1.283,50\nУкупан износ пореза: 1.283,50");

        assertEquals(new BigDecimal("7701.00"), receipt.getTotalAmount());
    }
}
