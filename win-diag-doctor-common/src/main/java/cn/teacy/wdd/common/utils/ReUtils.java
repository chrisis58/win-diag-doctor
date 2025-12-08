package cn.teacy.wdd.common.utils;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReUtils {

    /**
     * Only allows letters, digits, and underscores
     */
    private static final Pattern PLAIN_TEXT = Pattern.compile("^[a-zA-Z0-9_]+$");

    public static boolean isPlainText(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return PLAIN_TEXT.matcher(input).matches();
    }

}
