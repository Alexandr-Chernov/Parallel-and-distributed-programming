package InheritanceIndex_3;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InheritanceIndex {
    private static final Pattern CLASS_PATTERN = Pattern.compile(".*class\\s+(\\w+)\\s*(<.+>)?\\s*(extends\\s+(\\w+))?\\s+(implements\\s+([\\w,\\s]+)(<.+>)?)?");
    private static final Pattern INTERFACE_PATTERN = Pattern.compile(".*interface\\s+(\\w+)\\s*(<.+>)?\\s*(extends\\s+([\\w,\\s]+))?");

    public static void main(String[] args) throws InterruptedException {
        String projectDirectory = "src/spring-framework";
        List<File> javaFiles = getJavaFiles(projectDirectory);
        HashMap<String, HashSet<String>> inheritanceIndex = new HashMap<>();
        
        CountDownLatch countDownLatch = new CountDownLatch(javaFiles.size());

        buildInheritanceIndex(inheritanceIndex, javaFiles, countDownLatch);

        countDownLatch.await();

        inheritanceIndex.forEach((className, subclasses) -> {
            System.out.println(className);
            System.out.println("\tSubclasses: " + String.join(", ", subclasses) + "\n");
        });

    }

    private static List<File> getJavaFiles(String directory) {
        List<File> javaFiles = new ArrayList<>();
        File folder = new File(directory);
        if (folder.exists() && folder.isDirectory()) {
            findJavaFiles(folder, javaFiles);
        }
        return javaFiles;
    }

    private static void findJavaFiles(File folder, List<File> javaFiles) {
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".java")) {
                javaFiles.add(file);
            } else if (file.isDirectory()) {
                findJavaFiles(file, javaFiles);
            }
        }
    }

    private static void buildInheritanceIndex(HashMap<String, HashSet<String>> inheritanceIndex, List<File> javaFiles, CountDownLatch countDownLatch) {
        ReentrantLock locker = new ReentrantLock();

        for (File file : javaFiles) {
            new Thread(() -> {
                try (Scanner scanner = new Scanner(file)) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        Matcher classMatcher = CLASS_PATTERN.matcher(line);
                        locker.lock();
                        try {
                            if (classMatcher.find()) {
                                String className = classMatcher.group(1);
                                String classParent = classMatcher.group(4);
                                String interfaceParent = classMatcher.group(6);
                                if (classParent != null) {
                                    inheritanceIndex.computeIfAbsent(className, k -> new HashSet<>()).add(classParent);
                                }
                                if (interfaceParent != null) {
                                    inheritanceIndex.computeIfAbsent(className, k -> new HashSet<>()).add(interfaceParent);
                                }
                            }
                            Matcher interfaceMatcher = INTERFACE_PATTERN.matcher(line);
                            if (interfaceMatcher.find()) {
                                String interfaceName = interfaceMatcher.group(1);
                                String interfaceParent = interfaceMatcher.group(4);
                                if (interfaceParent != null) {
                                    inheritanceIndex.computeIfAbsent(interfaceName, k -> new HashSet<>()).add(interfaceParent);
                                }
                            }
                        } finally {
                            locker.unlock();
                        }
                    }
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
        }
    }
}
