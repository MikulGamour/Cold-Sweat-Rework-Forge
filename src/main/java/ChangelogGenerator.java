import java.io.*;
import java.util.*;
import java.util.regex.*;

public class ChangelogGenerator {
    private static final String COMMON_STYLE = """
            font-family: 'JetBrains Mono',monospace; 
            background-color: #212121; 
            color: #aaafb6;""";

    private static final String HEADER_STYLE = """
            color: #ffffff; 
            font-size: 1.5em; 
            font-weight: bold;""";

    private static final String SECTION_STYLE = """
            color: #ffffff; 
            font-size: 1.17em; 
            font-weight: bold;""";

    private static final String WARNING_STYLE = "color: orange;";
    static class Section {
        String title;
        List<String> items = new ArrayList<>();
        Map<String, List<String>> subItems = new HashMap<>();
        Map<String, List<String>> warnings = new HashMap<>();

        Section(String title) {
            this.title = title;
        }
    }

    public static void main(String[] args) {
        String inputFilePath = "changelog.txt";
        String outputFilePath = "formatted_changelog.txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            List<String> allLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }

            // Find the last line of dashes
            int lastDashIndex = -1;
            for (int i = allLines.size() - 1; i >= 0; i--) {
                if (allLines.get(i).trim().matches("^-+$")) {
                    lastDashIndex = i;
                    break;
                }
            }

            // Get only the lines after the last dash line
            List<String> lines = new ArrayList<>(allLines.subList(lastDashIndex + 1, allLines.size()));

            // Find version number (assumed to be the first non-empty line)
            String version = "";
            for (String l : lines) {
                if (!l.trim().isEmpty()) {
                    version = l.trim();
                    break;
                }
            }

            // Parse sections
            List<Section> sections = parseSections(lines);

            // Generate HTML
            generateHTML(writer, version, sections);

        } catch (IOException e) {
            System.err.println("Error processing the files: " + e.getMessage());
        }
    }

    private static List<Section> parseSections(List<String> lines) {
        List<Section> sections = new ArrayList<>();
        Section currentSection = new Section(""); // Default section for items before first section header
        sections.add(currentSection);
        String currentItem = null;

        for (String line : lines) {
            // Get original indentation level
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }

            String trimmed = line.trim();

            // Skip empty lines and version number
            if (trimmed.isEmpty() || sections.size() == 1 && currentSection.items.isEmpty() && !trimmed.endsWith(":")) {
                continue;
            }

            // Check if line is a list item first
            boolean isListItem = trimmed.startsWith("*") || trimmed.startsWith("-") || trimmed.startsWith("!");

            // New section (ends with : and not indented and not a list item)
            if (trimmed.endsWith(":") && indent == 0 && !isListItem) {
                currentSection = new Section(trimmed);
                sections.add(currentSection);
                currentItem = null;
                continue;
            }

            // Main item (starts with * or - without indentation)
            if ((trimmed.startsWith("*") || trimmed.startsWith("-")) && indent < 4) {
                String prefix = trimmed.substring(0, 1);
                String content = trimmed.substring(1).trim();
                currentItem = prefix + "\n" + content;  // Store prefix and content separately
                currentSection.items.add(currentItem);
                currentSection.subItems.put(currentItem, new ArrayList<>());
            }
            // Warning (starts with !)
            else if (trimmed.startsWith("!")) {
                currentItem = "!\n" + trimmed.substring(1).trim();  // Store with warning prefix
                currentSection.items.add(currentItem);
                currentSection.subItems.put(currentItem, new ArrayList<>());
            }
            // Sub-item (indented with spaces or starts with dash)
            else if (indent >= 4 || (trimmed.startsWith("-") && indent >= 2)) {
                if (currentItem != null) {
                    String subItemContent = trimmed.startsWith("-") ? trimmed.substring(1).trim() : trimmed;
                    if (trimmed.startsWith("*")) {
                        subItemContent = trimmed.substring(1).trim();
                    }
                    currentSection.subItems.get(currentItem).add(
                            (trimmed.startsWith("*") ? "*\n" : "") + subItemContent
                    );
                }
            }
            // Regular item
            else if (!trimmed.isEmpty()) {
                currentItem = "\n" + trimmed;  // No prefix for regular items
                currentSection.items.add(currentItem);
                currentSection.subItems.put(currentItem, new ArrayList<>());
            }
        }

        // Remove empty default section if it wasn't used
        if (sections.get(0).items.isEmpty()) {
            sections.remove(0);
        }

        return sections;
    }

    private static void generateHTML(BufferedWriter writer, String version, List<Section> sections)
    throws IOException {
        // Start document
        writer.write("<div style=\"" + COMMON_STYLE + "\">");

        // Version header
        writer.write(String.format("<span style=\"%s\">%s</span>",
                                   HEADER_STYLE, version));
        writer.write("<br><br>\n");

        // Write each section
        for (Section section : sections) {
            // Section header
            writer.write(String.format("<span style=\"%s\">%s</span>\n",
                                       SECTION_STYLE, section.title));

            writer.write("<ul style=\"font-size: 0.83em;\">\n");

            // Write items
            for (String item : section.items) {
                // Split prefix and content
                String[] parts = item.split("\n", 2);
                String prefix = parts[0];
                String content = parts[1];

                if (prefix.equals("*")) {
                    writer.write(String.format("<li><span style=\"color: #ffffff; font-weight: bold;\">%s</span>\n", content));
                } else if (prefix.equals("!")) {
                    writer.write(String.format("<li style=\"%s\">! %s\n", WARNING_STYLE, content));
                } else {
                    writer.write(String.format("<li>%s\n", content));
                }

                // Start sub-items list if we have any sub-items or warnings
                List<String> subItems = section.subItems.get(item);
                List<String> warnings = section.warnings.get(item);
                if (!subItems.isEmpty() || (warnings != null && !warnings.isEmpty())) {
                    writer.write("<ul style=\"margin-left: 40px;\">\n");

                    // Write sub-items
                    for (String subItem : subItems) {
                        // Check if sub-item is important
                        String[] subParts = subItem.split("\n", 2);
                        boolean subImportant = subParts[0].equals("*");
                        String subContent = subParts.length > 1 ? subParts[1] : subParts[0];

                        if (subImportant) {
                            writer.write(String.format("<li><span style=\"color: #ffffff; font-weight: bold;\">%s</span></li>\n", subContent));
                        } else {
                            writer.write(String.format("<li>%s</li>\n", subContent));
                        }
                    }

                    // Write warnings
                    if (warnings != null) {
                        for (String warning : warnings) {
                            writer.write(String.format("<li style=\"%s\">! %s</li>\n", WARNING_STYLE, warning));
                        }
                    }

                    writer.write("</ul>\n");
                }

                writer.write("</li>\n");
            }

            writer.write("</ul>\n<br>\n");
        }

        // Close document
        writer.write("</div>");
    }
}