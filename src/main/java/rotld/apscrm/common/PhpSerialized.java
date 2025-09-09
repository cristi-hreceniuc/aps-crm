package rotld.apscrm.common;


import java.util.*;
import java.util.regex.*;

/** Parser minimalist pentru array-uri PHP serializate (doar chei/valori string). */
public final class PhpSerialized {
    private static final Pattern PAIR = Pattern.compile("s:\\d+:\"([^\"]*)\";s:\\d+:\"([^\"]*)\";");

    private PhpSerialized(){}

    public static Map<String,String> parseAssoc(String s){
        Map<String,String> map = new LinkedHashMap<>();
        if (s == null || s.isBlank()) return map;
        Matcher m = PAIR.matcher(s);
        while (m.find()){
            map.put(m.group(1), m.group(2));
        }
        return map;
    }
}