package tw.nekomimi.nekogram.translator;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Bin
 */
class Utils {

    static String signWeb(String text, long key1, long key2) {
        List<Integer> c = new ArrayList<>();
        for (int F = 0; F < text.length(); F++) {
            int p = text.charAt(F);
            if (128 > p) {
                c.add(p);
            } else {
                if (2048 > p) {
                    c.add(p >> 6 | 192);
                } else {
                    if ((55296 == (64512 & p) && F + 1 < text.length() && 56320 == (64512 & text.charAt(F + 1)))) {
                        p = 65536 + ((1023 & p) << 10) + (1023 & text.charAt(++F));
                        c.add(p >> 18 | 240);
                        c.add(p >> 12 & 63 | 128);
                    } else {
                        c.add(p >> 12 | 224);
                    }
                    c.add(p >> 6 & 63 | 128);
                }
                c.add(63 & p | 128);
            }
        }
        String formula1 = "+-a^+6";
        String formula2 = "+-3^+b+-f";
        long v = key1;
        for (Integer i : c) {
            v += i;
            v = n(v, formula1);
        }
        v = n(v, formula2);
        v ^= key2;
        if (0 > v)
            v = (0x7fffffff & v) + 0x80000000L;
        v %= 1e6;
        return v + "." + (v ^ key1);
    }

    private static long n(long r, String o) {
        for (int t = 0; t < o.length() - 2; t += 3) {
            long e = o.charAt(t + 2);
            e = e >= 'a' ? e - 87 : e - '0';
            e = '+' == o.charAt(t + 1) ? r >>> e : r << e;
            r = '+' == o.charAt(t) ? r + e & 0xffffffffL : r ^ e;
        }
        return r;
    }

    static String encodeURIComponent(String str) {
        if (str == null) return null;

        byte[] bytes = str.getBytes(Charset.defaultCharset());
        StringBuilder builder = new StringBuilder(bytes.length);

        for (byte c : bytes) {
            String HEX = "0123456789ABCDEF";
            if (c >= 'a' ? c <= 'z' || c == '~' :
                    c >= 'A' ? c <= 'Z' || c == '_' :
                            c >= '0' ? c <= '9' : c == '-' || c == '.')
                builder.append((char) c);
            else
                builder.append('%')
                        .append(HEX.charAt(c >> 4 & 0xf))
                        .append(HEX.charAt(c & 0xf));
        }

        return builder.toString();
    }

}
