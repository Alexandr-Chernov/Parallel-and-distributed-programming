package InheritanceIndex_2;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InheritanceIndex {
    private static final Pattern CLASS_PATTERN = Pattern.compile(".*class\\s+(\\w+)\\s*(<.+>)?\\s*(extends\\s+(\\w+))?\\s+(implements\\s+([\\w,\\s]+)(<.+>)?)?");
    private static final Pattern INTERFACE_PATTERN = Pattern.compile(".*interface\\s+(\\w+)\\s*(<.+>)?\\s*(extends\\s+([\\w,\\s]+))?");

    public static void main(String[] args) {
        String projectDirectory = "src/InheritanceIndex_2/classes";
        List<File> javaFiles = getJavaFiles(projectDirectory);
        Map<String, List<String>> inheritanceIndex = buildInheritanceIndex(javaFiles);

        inheritanceIndex.forEach((className, subclasses) -> {
            System.out.println(className);
            System.out.println("\tSubclasses: " + String.join(", ", subclasses));
        });
    }

    private static List<File> getJavaFiles(String directory) {
        List<File> javaFiles = new ArrayList<>();
        File folder = new File(directory);

        if (folder.exists() && folder.isDirectory()) {
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile() && file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }

        return javaFiles;
    }

    private static Map<String, List<String>> buildInheritanceIndex(List<File> javaFiles) {
        Map<String, List<String>> inheritanceIndex = new HashMap<>();

        for (File file : javaFiles) {
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();

                    Matcher classMatcher = CLASS_PATTERN.matcher(line);
                    if (classMatcher.find()) {
                        String className = classMatcher.group(1);
                        String classParent = classMatcher.group(4);
                        String interfaceParent = classMatcher.group(6);
                        if (classParent != null) {
                            inheritanceIndex.computeIfAbsent(className, k -> new ArrayList<>()).add(classParent);
                        }
                        if (interfaceParent != null) {
                            inheritanceIndex.computeIfAbsent(className, k -> new ArrayList<>()).add(interfaceParent);
                        }
                    }

                    Matcher interfaceMatcher = INTERFACE_PATTERN.matcher(line);
                    if (interfaceMatcher.find()) {
                        String interfaceName = interfaceMatcher.group(1);
                        String interfaceParent = interfaceMatcher.group(4);
                        if (interfaceParent != null) {
                            inheritanceIndex.computeIfAbsent(interfaceName, k -> new ArrayList<>()).add(interfaceParent);
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return inheritanceIndex;
    }
}