package com.example.backend.core.banking;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class IbanGenerator {

    private static final SecureRandom RND = new SecureRandom();

    public String generateItalianIban() {
        String abi = randomDigits(5);
        String cab = randomDigits(5);
        String account = randomDigits(12);

        String bbanWithoutCin = abi + cab + account;
        char cin = computeItalianCin(bbanWithoutCin);

        String bban = cin + bbanWithoutCin; // CIN + ABI + CAB + CONTO

        String country = "IT";
        String checkDigits = computeIbanCheckDigits(country, bban);

        return country + checkDigits + bban;
    }

    private static String randomDigits(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(RND.nextInt(10));
        return sb.toString();
    }

    // Italian CIN algorithm (ABI+CAB+CONTO => CIN letter)
    private static char computeItalianCin(String bbanWithoutCin) {
        // positions are 1-based from left
        int sum = 0;
        for (int i = 0; i < bbanWithoutCin.length(); i++) {
            char ch = bbanWithoutCin.charAt(i);
            int pos = i + 1;
            if (pos % 2 == 1) {
                sum += oddPositionValue(ch);
            } else {
                sum += evenPositionValue(ch);
            }
        }
        int mod = sum % 26;
        return (char) ('A' + mod);
    }

    private static int evenPositionValue(char ch) {
        // even: digits as-is; letters A=0..Z=25
        if (ch >= '0' && ch <= '9') return ch - '0';
        return (Character.toUpperCase(ch) - 'A');
    }

    private static int oddPositionValue(char ch) {
        // odd mapping table (official Italian CIN mapping)
        return switch (Character.toUpperCase(ch)) {
            case '0' -> 1;
            case '1' -> 0;
            case '2' -> 5;
            case '3' -> 7;
            case '4' -> 9;
            case '5' -> 13;
            case '6' -> 15;
            case '7' -> 17;
            case '8' -> 19;
            case '9' -> 21;
            case 'A' -> 1;
            case 'B' -> 0;
            case 'C' -> 5;
            case 'D' -> 7;
            case 'E' -> 9;
            case 'F' -> 13;
            case 'G' -> 15;
            case 'H' -> 17;
            case 'I' -> 19;
            case 'J' -> 21;
            case 'K' -> 2;
            case 'L' -> 4;
            case 'M' -> 18;
            case 'N' -> 20;
            case 'O' -> 11;
            case 'P' -> 3;
            case 'Q' -> 6;
            case 'R' -> 8;
            case 'S' -> 12;
            case 'T' -> 14;
            case 'U' -> 16;
            case 'V' -> 10;
            case 'W' -> 22;
            case 'X' -> 25;
            case 'Y' -> 24;
            case 'Z' -> 23;
            default -> throw new IllegalArgumentException("Invalid BBAN char: " + ch);
        };
    }

    // ISO 13616 check digits (mod 97)
    private static String computeIbanCheckDigits(String country, String bban) {
        String rearranged = bban + country + "00";
        String numeric = toNumericIban(rearranged);
        int mod = mod97(numeric);
        int check = 98 - mod;
        return (check < 10 ? "0" : "") + check;
    }

    private static String toNumericIban(String s) {
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (char ch : s.toCharArray()) {
            if (Character.isDigit(ch)) {
                sb.append(ch);
            } else if (Character.isLetter(ch)) {
                int val = Character.toUpperCase(ch) - 'A' + 10;
                sb.append(val);
            } else {
                throw new IllegalArgumentException("Invalid IBAN char: " + ch);
            }
        }
        return sb.toString();
    }

    private static int mod97(String numericString) {
        int remainder = 0;
        for (int i = 0; i < numericString.length(); i++) {
            int digit = numericString.charAt(i) - '0';
            remainder = (remainder * 10 + digit) % 97;
        }
        return remainder;
    }
}
