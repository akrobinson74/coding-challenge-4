package com.finreach.challenge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class NumeralConverter {
    private static final Logger LOGGER;

    static {
        LOGGER = Logger.getLogger("NumeralConvertor");
        LOGGER.setLevel(Level.WARNING);
    }

    private static final Map<String, Integer> ROMAN_NUMERAL_VALUES =
        new HashMap<String, Integer>(){{
            put("I", 1);
            put("IV", 4);
            put("V", 5);
            put("IX", 9);
            put("X", 10);
            put("XL", 40);
            put("L", 50);
            put("XC", 90);
            put("C", 100);
            put("CD", 400);
            put("D", 500);
            put("CM", 900);
            put("M", 1000);
        }};
    private static final Set<String> SUBTRACTION_INDICATING_NUMERALS =
        new HashSet<>(Arrays.asList("C", "X", "I"));

    private static final Set<String> METALS =
        new HashSet<>(Arrays.asList("Gold", "Iron", "Silver"));

    private static final Map<String, String> GALACTIC_TO_ROMAN_UNITS =
        new HashMap<String, String>();
    private static final Map<String, Integer> METAL_VALUES =
        new HashMap<String, Integer>() {{
            put("Iron", 195);  // technically 195.5 but have run out of time :(
            put("Gold", 14450);
            put("Silver", 17);
        }};

    private static final Pattern KEY_PATTERN =
        Pattern.compile("(.*?) is ([CDILMVX]+|[0-9]+ Credits)");
    private static final Pattern OUTPUT_REQUIRING_PATTERN =
        Pattern.compile("^.+(many Credits|much) is (.*?) \\?$");
    private static final String ROMAN_NUMERAL_PATTERN = "[CDILMXV]+";

    private static final List<String> TO_BE_CALCULATED_LATER =
        new ArrayList<>();

    private static String previousNumeral = "";

    /**
     * Input is either a conversion key or a query to be answered.
     *
     * Conversion are of the form:
     *  unit_token is ROMAN_NUMERAL
     *      OR
     *  unit_token * N Metal is decimal value Credits
     *
     * Questions are of the form:
     *  is unit_token * N ?                 ->  ROMAN_NUMERAL response
     *  Credits is unit_token * N Metal ?   ->  decimal response
     */

    public static void main(String[] args) throws IOException {
        Files.lines(new File(args[0]).toPath()).forEach( line -> {
            System.out.println(parseInput(line));
        });
    }

    private static String parseInput(String line) {
        Matcher matcher = OUTPUT_REQUIRING_PATTERN.matcher(line);
        String result = "";

        LOGGER.log(Level.FINE,"Parsing line: " + line);

        // Output cases
        if (matcher.matches()) {
            String galacticValue = matcher.group(2);
            String[] tokens = galacticValue.split(" ");

            // decimal conversion
            if (matcher.group(1).equals("many Credits")) {
                Integer multiplier = METAL_VALUES.get(tokens[tokens.length - 1]);

                result = String.format("%s is %d Credits.",
                    galacticValue,
                    mapGalacticToDecimal(
                        Arrays.copyOf(tokens, tokens.length - 1),
                        multiplier
                    )
                );
            }

            // roman numeral conversion
            else {
                result = String.format("%s is %d",
                    galacticValue,
                    mapGalacticToDecimal(tokens, 1)
                );
            }
        }
        else {
            matcher = KEY_PATTERN.matcher(line);
            if (!matcher.matches()) {
                // Garbage - doesn't match either input or output patterns
                // log it
                result = "What'chu talkin' 'bout, Willis?";
            }
            else if (matcher.group(2).matches(ROMAN_NUMERAL_PATTERN)) {

                storeGalacticToRomanConversion(
                    matcher.group(1).split(" "),
                    matcher.group(2).split("")
                );
            }
            else {
                String[] valueTokens = matcher.group(2).split(" ");
                Integer decimalValue = Integer.parseInt(valueTokens[0]);

                String[] conversionTokens = matcher.group(1).split(" ");
                String metal = conversionTokens[conversionTokens.length - 1];

                Integer divisor =
                    METAL_VALUES.containsKey(metal) ? METAL_VALUES.get(metal) : 0;
                if (divisor != 0) {
                    storeGalacticToRomanConversion(
                        conversionTokens,
                        convertDecimalToRoman(decimalValue / divisor).split(""));
                }
                // Something bad has happened Log the edge case
                else {
                }
            }
        }

        return result;
    }

    private static Integer mapGalacticToDecimal(
        final String[] galacticTokens,
        final Integer factor) {
        return convertRomanToDecimal(mapGalaticToRoman(galacticTokens)) * factor;
    }

    private static String mapGalaticToRoman(final String[] galacticTokens) {
        StringBuffer sb = new StringBuffer();
        Arrays.stream(galacticTokens)
            .map(token -> GALACTIC_TO_ROMAN_UNITS.get(token))
            .forEach(romanDigit -> {sb.append(romanDigit);});
        return sb.toString();
    }

    private static String convertDecimalToRoman(final Integer decimalValue) {
        String romanValue = "";
        int val = decimalValue;

        while (val >= 1000) {
            romanValue += "M";
            val -= 1000;
        }
        while (val >= 900) {
            romanValue += "CM";
            val -= 900;
        }
        while (val >= 500) {
            romanValue += "D";
            val -= 500;
        }
        while (val >= 100) {
            romanValue += "C";
            val -= 100;
        }
        while (val >= 90) {
            romanValue += "XC";
            val -= 90;
        }
        while (val >= 50) {
            romanValue += "L";
            val -= 50;
        }
        while (val >= 40) {
            romanValue += "XL";
            val -= 40;
        }
        while (val >= 10) {
            romanValue += "X";
            val -= 10;
        }
        while (val >= 9) {
            romanValue += "IX";
            val -= 9;
        }
        while (val >= 5) {
            romanValue += "V";
            val -= 5;
        }
        while (val >= 4) {
            romanValue += "IV";
            val -= 4;
        }
        while (val >= 1) {
            romanValue += "I";
            val -= 1;
        }

        return romanValue;
    }

    private static Integer convertRomanToDecimal(final String romanNumber) {
        List<String> reversedRomanNumerals = Arrays.asList(romanNumber.split(""));
        // now is actually reversed
        Collections.reverse(reversedRomanNumerals);
        return reversedRomanNumerals.stream()
            .mapToInt(numeral -> lookupIntegerValForNumeral(numeral)).sum();
    }

    private static int lookupIntegerValForNumeral(final String numeral) {
        boolean flipSign = false;
        if (!(previousNumeral.equals("")) &&
            !(numeral.equals(previousNumeral)) &&
            SUBTRACTION_INDICATING_NUMERALS.contains(numeral)) {
            flipSign = true;
        }

        previousNumeral = numeral;

        return ROMAN_NUMERAL_VALUES.getOrDefault(numeral, 0)
            * (flipSign ? -1 : 1);
    }

    private static void storeGalacticToRomanConversion(
        final String[] glacticTokens,
        final String[] romanDigits) {

        for (int i = 0; i < romanDigits.length; i++) {
            LOGGER.log(Level.FINE,"Converting: " + glacticTokens[i]);
            if (GALACTIC_TO_ROMAN_UNITS.size() == 0 ||
                ! GALACTIC_TO_ROMAN_UNITS.containsKey(glacticTokens[i]))
                GALACTIC_TO_ROMAN_UNITS.put(glacticTokens[i], romanDigits[i]);

        }
    }
}
