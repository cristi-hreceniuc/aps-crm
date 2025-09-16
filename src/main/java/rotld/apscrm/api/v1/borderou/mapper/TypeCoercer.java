package rotld.apscrm.api.v1.borderou.mapper;


public final class TypeCoercer {
    private TypeCoercer(){}

    public static String coerce(String type, String raw) {
        if (raw == null) return null;
        type = type == null ? "string" : type.toLowerCase();

        try {
            return switch (type) {
                case "integer", "int", "number" -> String.valueOf(Integer.parseInt(raw.trim()));
                case "boolean", "bool" -> String.valueOf(
                        raw.trim().equalsIgnoreCase("true") ||
                                raw.trim().equals("1") ||
                                raw.trim().equalsIgnoreCase("yes") ||
                                raw.trim().equalsIgnoreCase("da")
                );
                case "date" -> raw.trim(); // lasă formatul așa cum vine din UI
                default -> raw; // string
            };
        } catch (Exception ex) {
            // dacă nu trece validarea, nu coercionăm – lăsăm valoarea veche să nu stricăm setarea
            throw new IllegalArgumentException("Valoare invalidă pentru tipul: " + type);
        }
    }
}