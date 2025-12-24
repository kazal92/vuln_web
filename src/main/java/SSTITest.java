
public class SSTITest {
    public static void main(String[] args) {
        testPayload("${T(java.lang.Runtime).getRuntime().exec('calc')}");
        testPayload("${new java.lang.String(new byte[]{65})}");
        testPayload("${'hello } world'}");

        // Complex payload often used in SSTI
        testPayload(
                "${T(org.apache.commons.io.IOUtils).toString(T(java.lang.Runtime).getRuntime().exec('whoami').getInputStream())}");
    }

    public static void testPayload(String template) {
        System.out.println("Testing template: " + template);

        if (template.contains("${")) {
            // The regex from ToolController.java
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^\\}]+)\\}");
            java.util.regex.Matcher matcher = pattern.matcher(template);

            while (matcher.find()) {
                String expression = matcher.group(1).trim();
                System.out.println("  [Regex Match] Extracted expression: [" + expression + "]");

                // Simulate checking if it would likely fail parsing if truncated
                if (!template.contains(expression + "}")) {
                    // This check is a bit naive, but basically if the extracted expression
                    // plus a closing brace isn't what the user intended (i.e. if it was cut short)
                    // logic implies it's cut short.
                }

                int openBraces = 0;
                for (char c : expression.toCharArray()) {
                    if (c == '{')
                        openBraces++;
                    if (c == '}')
                        openBraces--;
                }

                if (openBraces != 0) {
                    System.out.println(
                            "  [Analysis] Partial expression detected (Unbalanced braces). Parsing would fail.");
                } else if (expression.endsWith("new byte[]{65")) { // Specific check for the byte array case
                    System.out.println("  [Analysis] Truncated at '}'. Parsing would fail.");
                } else {
                    System.out.println("  [Analysis] Expression looks structurally complete (regarding braces).");
                }
            }
        } else {
            System.out.println("  No ${ found.");
        }
        System.out.println("--------------------------------------------------");
    }
}
