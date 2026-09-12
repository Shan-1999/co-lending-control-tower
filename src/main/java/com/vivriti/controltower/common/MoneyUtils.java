package com.vivriti.controltower.common;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public final class MoneyUtils {

    private MoneyUtils() {
        // Prevent instantiation
    }

    public static long addPaise(long a, long b) {
        return Math.addExact(a, b);
    }

    public static long subtractPaise(long a, long b) {
        return Math.subtractExact(a, b);
    }

    public static String formatPaiseAsInr(long paise) {
        double rupees = paise / 100.0;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return formatter.format(rupees);
    }

    public static long parseInrToPaise(String inr) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        try {
            Number num = formatter.parse(inr);
            return Math.round(num.doubleValue() * 100.0);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid INR format: " + inr, e);
        }
    }

    public static boolean isValidPaise(long paise) {
        return paise >= 0;
    }
}
