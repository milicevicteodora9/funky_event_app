package com.example.funkyeventapp.services;

import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.Receipt;
import com.example.funkyeventapp.models.ReceiptProcessingStatus;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative parser for common Serbian and English fiscal receipt layouts. */
final class ReceiptTextParser {
    private static final Pattern MONEY = Pattern.compile(
            "(?<!\\d)([0-9O][0-9O .,'’]*[.,][0-9O]{2})(?!\\d)");
    private static final Pattern DATE_DMY = Pattern.compile(
            "(?<!\\d)([0-3]?\\d)[./-]([01]?\\d)[./-](\\d{2}|\\d{4})(?!\\d)");
    private static final Pattern DATE_YMD = Pattern.compile(
            "(?<!\\d)(20\\d{2})[-/.]([01]?\\d)[-/.]([0-3]?\\d)(?!\\d)");
    private static final Pattern NAMED_MERCHANT = Pattern.compile(
            "(?iu)^(?:NAZIV(?:\\s+OBVEZNIKA)?|PRODAVAC|MERCHANT|VENDOR|НАЗИВ(?:\\s+ОБВЕЗНИКА)?|ПРОДАВАЦ)\\s*[:.-]?\\s*(.+)$");

    private ReceiptTextParser() { }

    static Receipt parse(String recognizedText) {
        String text = recognizedText == null ? "" : recognizedText.trim();
        List<String> lines = lines(text);
        Receipt receipt = new Receipt();
        receipt.setRecognizedText(text);
        receipt.setProcessingStatus(text.isEmpty()
                ? ReceiptProcessingStatus.ERROR : ReceiptProcessingStatus.PROCESSED);
        receipt.setSeller(findMerchant(lines));
        receipt.setTotalAmount(findTotal(lines));
        receipt.setIssueDate(findDate(text));
        receipt.setCurrency(findCurrency(text));
        return receipt;
    }

    private static List<String> lines(String text) {
        List<String> result = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.trim().replaceAll("\\s+", " ");
            if (!line.isEmpty()) result.add(line);
        }
        return result;
    }

    private static String findMerchant(List<String> lines) {
        for (int index = 0; index < Math.min(lines.size(), 12); index++) {
            String header = foldCyrillic(lines.get(index).toUpperCase(Locale.ROOT));
            if (!header.matches(".*(?:FISKALNI|FICKALNI|RACUN|PACUN|PA4YH).*")) continue;
            for (int candidate = index + 1; candidate < Math.min(lines.size(), index + 6); candidate++) {
                String value = lines.get(candidate).trim();
                if (value.matches("[0-9:/ -]+")) continue;
                if (plausibleMerchant(value)) return value;
            }
        }
        for (int index = 0; index < Math.min(lines.size(), 15); index++) {
            Matcher named = NAMED_MERCHANT.matcher(lines.get(index));
            if (named.matches() && plausibleMerchant(named.group(1))) return named.group(1).trim();
            if (named.matches() && index + 1 < lines.size() && plausibleMerchant(lines.get(index + 1))) {
                return lines.get(index + 1).trim();
            }
        }

        String best = "";
        int bestScore = Integer.MIN_VALUE;
        for (int index = 0; index < Math.min(lines.size(), 12); index++) {
            String line = lines.get(index);
            if (!plausibleMerchant(line)) continue;
            String upper = line.toUpperCase(Locale.ROOT);
            int score = 40 - index * 2;
            if (upper.matches(".*(?:\\bD\\.?O\\.?O\\.?\\b|\\bDOO\\b|\\bPR\\b|\\bA\\.?D\\.?\\b|\\bLLC\\b|\\bLTD\\b|\\bINC\\b|\\bДОО\\b|\\bПР\\b).*")) score += 90;
            if (upper.matches(".*(?:MARKET|RESTORAN|PEKARA|APOTEKA|HOTEL|CAFE|KAFANA|SHOP|STORE|МАРКЕТ|РЕСТОРАН|ПЕКАРА|АПОТЕКА).*")) score += 35;
            if (upper.matches("[\\p{L}]+(?:[-&][\\p{L}]+)+")) score += 35;
            if (upper.contains("-") && upper.matches(".*[\\p{L}].*")) score += 25;
            int letters = 0;
            int upperLetters = 0;
            for (int i = 0; i < line.length(); i++) {
                char value = line.charAt(i);
                if (Character.isLetter(value)) {
                    letters++;
                    if (Character.isUpperCase(value)) upperLetters++;
                }
            }
            if (letters > 4 && upperLetters * 2 >= letters) score += 12;
            if (score > bestScore) {
                bestScore = score;
                best = line;
            }
        }
        return best;
    }

    private static boolean plausibleMerchant(String line) {
        if (line == null || line.length() < 2 || line.length() > 80 || !line.matches(".*[\\p{L}].*")) return false;
        String upper = line.toUpperCase(Locale.ROOT);
        if (upper.matches(".*(?:FISKALNI\\s+RAČUN|FISKALNI\\s+RACUN|ФИСКАЛНИ\\s+РАЧУН|RECEIPT|UKUPNO|УКУПНО|TOTAL|UPLATU|УПЛАТУ|DATUM|ДАТУМ|VREME|ВРЕМЕ|PIB|ПИБ|BROJ\\s+RAČUNA|БРОЈ\\s+РАЧУНА|PDV|ПДВ).*")) return false;
        int digits = 0;
        for (int index = 0; index < line.length(); index++) if (Character.isDigit(line.charAt(index))) digits++;
        return digits * 3 <= line.length();
    }

    private static BigDecimal findTotal(List<String> lines) {
        BigDecimal bestAmount = null;
        int bestScore = Integer.MIN_VALUE;
        for (int index = 0; index < lines.size(); index++) {
            String upper = lines.get(index).toUpperCase(Locale.ROOT);
            int labelScore = totalLabelScore(upper);
            if (labelScore <= 0 || isRejectedTotalLine(upper)) continue;
            for (int offset = -1; offset <= 2; offset++) {
                int candidateIndex = index + offset;
                if (candidateIndex < 0 || candidateIndex >= lines.size()) continue;
                List<BigDecimal> amounts = amounts(lines.get(candidateIndex));
                for (BigDecimal amount : amounts) {
                    if (amount.signum() <= 0) continue;
                    int score = labelScore - Math.abs(offset) * 8;
                    score += Math.min(15, index * 15 / Math.max(1, lines.size()));
                    String candidateUpper = lines.get(candidateIndex).toUpperCase(Locale.ROOT);
                    if (candidateUpper.matches(".*(?:RSD|DIN|EUR|USD|AED|€|\\$).*")) score += 8;
                    if (score >= bestScore) {
                        bestScore = score;
                        bestAmount = amount;
                    }
                }
            }
        }
        return bestAmount == null ? repeatedOrLargestPaymentAmount(lines) : bestAmount;
    }

    private static int totalLabelScore(String upper) {
        String folded = foldCyrillic(upper);
        if (folded.matches(".*(?:ZA|3A)\\s+(?:UPLATU|YPLATY|YNNATY|YNNARY).*")) return 140;
        if (folded.matches(".*(?:UKUPAN|YKYPAN|UKYPAH|YKYPAH)\\s+(?:IZNOS|I3NOS|IZHOS|I3HOC).*")) return 125;
        if (folded.matches(".*(?:UKUPNO|GRAND\\s+TOTAL).*")) return 120;
        if (folded.matches(".*(?:TOTAL|SVEGA).*")) return 110;
        if (folded.matches(".*IZNOS\\s+ZA\\s+PLA[CĆ]ANJE.*")) return 125;
        return 0;
    }

    private static String foldCyrillic(String value) {
        return value.replace('З', 'Z').replace('А', 'A').replace('У', 'U')
                .replace('П', 'P').replace('Л', 'L').replace('Т', 'T')
                .replace('И', 'I').replace('К', 'K').replace('Н', 'N')
                .replace('О', 'O').replace('С', 'S').replace('В', 'V')
                .replace('Е', 'E').replace('Г', 'G').replace('М', 'M')
                .replace('Р', 'R').replace('Б', 'B').replace('Ј', 'J')
                .replace('Ћ', 'C').replace('Ч', 'C').replace('Ш', 'S')
                .replace('Ж', 'Z').replace('Д', 'D');
    }

    private static boolean isRejectedTotalLine(String upper) {
        String folded = foldCyrillic(upper);
        return upper.matches(".*(?:SUBTOTAL|MEĐUZBIR|MEDJUZBIR|ПОДЗБИР|PDV|ПДВ|POREZ|ПОРЕЗ|TAX\\s+TOTAL).*")
                || folded.matches(".*(?:P[O0]RE[Z2]|NORE[Z2]|PDV|TAX).*")
                || (folded.contains("UKUPNO") && folded.matches(".*(?:NAZIV|CENA|KOL\\.?|QTY).*") )
                || folded.matches(".*(?:POVRACAJ|KUSUR|CHANGE).*");
    }

    private static BigDecimal repeatedOrLargestPaymentAmount(List<String> lines) {
        BigDecimal paid = labeledAmount(lines, "(?:GOTOVINA|G0TOVINA|CASH|KARTICA|CARD)");
        BigDecimal change = labeledAmount(lines, "(?:POVRA[CĆ]AJ|KUSUR|CHANGE)");
        if (paid != null && change != null && paid.compareTo(change) >= 0) {
            return paid.subtract(change).setScale(2);
        }

        Map<BigDecimal, Integer> counts = new HashMap<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String folded = foldCyrillic(line.toUpperCase(Locale.ROOT));
            if (isRejectedTotalLine(line.toUpperCase(Locale.ROOT))
                    || isTaxContext(lines, index)
                    || folded.matches(".*(?:POVRACAJ|KUSUR|CHANGE).*")) continue;
            for (BigDecimal amount : amounts(line)) {
                BigDecimal normalized = amount.stripTrailingZeros();
                counts.put(normalized, counts.containsKey(normalized) ? counts.get(normalized) + 1 : 1);
            }
        }
        BigDecimal largestRepeated = null;
        for (Map.Entry<BigDecimal, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= 2 && (largestRepeated == null
                    || entry.getKey().compareTo(largestRepeated) > 0)) {
                largestRepeated = entry.getKey();
            }
        }
        if (largestRepeated != null) return largestRepeated.setScale(2);
        return paid;
    }

    private static BigDecimal labeledAmount(List<String> lines, String labelPattern) {
        for (int index = 0; index < lines.size(); index++) {
            String folded = foldCyrillic(lines.get(index).toUpperCase(Locale.ROOT));
            if (!folded.matches(".*" + labelPattern + ".*")) continue;
            for (int offset = 0; offset <= 1; offset++) {
                int candidateIndex = index + offset;
                if (candidateIndex >= lines.size()) break;
                List<BigDecimal> found = amounts(lines.get(candidateIndex));
                if (!found.isEmpty()) return found.get(found.size() - 1);
            }
        }
        return null;
    }

    private static boolean isTaxContext(List<String> lines, int index) {
        for (int candidate = Math.max(0, index - 1);
             candidate <= Math.min(lines.size() - 1, index + 1); candidate++) {
            String folded = foldCyrillic(lines.get(candidate).toUpperCase(Locale.ROOT));
            if (folded.matches(".*(?:P[O0]RE[Z2]|NORE[Z2]|PDV|TAX).*")) return true;
        }
        return false;
    }

    private static List<BigDecimal> amounts(String line) {
        List<BigDecimal> result = new ArrayList<>();
        Matcher matcher = MONEY.matcher(line.replace('o', '0').replace('O', '0'));
        while (matcher.find()) {
            BigDecimal value = parseMoney(matcher.group(1));
            if (value != null) result.add(value);
        }
        return result;
    }

    private static BigDecimal parseMoney(String raw) {
        String value = raw.replace(" ", "").replace("'", "").replace("’", "");
        int comma = value.lastIndexOf(',');
        int dot = value.lastIndexOf('.');
        char decimal = comma > dot ? ',' : '.';
        if (comma >= 0 && dot >= 0) value = value.replace(decimal == ',' ? "." : ",", "");
        if (decimal == ',') value = value.replace(',', '.');
        try { return new BigDecimal(value); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static LocalDate findDate(String text) {
        Matcher dmy = DATE_DMY.matcher(text);
        while (dmy.find()) {
            int year = Integer.parseInt(dmy.group(3));
            if (year < 100) year += 2000;
            LocalDate date = safeDate(year, Integer.parseInt(dmy.group(2)), Integer.parseInt(dmy.group(1)));
            if (date != null) return date;
        }
        Matcher ymd = DATE_YMD.matcher(text);
        while (ymd.find()) {
            LocalDate date = safeDate(Integer.parseInt(ymd.group(1)), Integer.parseInt(ymd.group(2)), Integer.parseInt(ymd.group(3)));
            if (date != null) return date;
        }
        return null;
    }

    private static LocalDate safeDate(int year, int month, int day) {
        if (year < 2000 || year > LocalDate.now().getYear() + 1) return null;
        try { return LocalDate.of(year, month, day); }
        catch (DateTimeException ignored) { return null; }
    }

    private static Currency findCurrency(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.contains("RSD") || upper.contains("DIN")) return Currency.RSD;
        if (upper.contains("USD") || upper.contains("$")) return Currency.USD;
        if (upper.contains("AED")) return Currency.AED;
        String folded = foldCyrillic(upper);
        if (folded.contains("FISKALNI RACUN") || folded.contains("ZA UPLATU")
                || folded.matches("(?s).*(?:3A)\\s+(?:YPLATY|YNNATY|YNNARY).*")
                || upper.contains("ПДВ")) return Currency.RSD;
        return Currency.EUR;
    }
}
