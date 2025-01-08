package InheritanceIndex_6;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InheritanceIndex {

    public static void main(String[] args) throws Exception {
        int numberOfThreads = 5;
        int taskSize = 10;

        BlockingQueue<File> taskQueue = new LinkedBlockingQueue<>(taskSize);
        BlockingQueue<HashMap<String, HashSet<String>>> resultQueue = new LinkedBlockingQueue<>(10);

        Thread[] threads = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            threads[i] = new Thread(new BuilderInheritanceIndexProcessor(taskQueue, resultQueue));
            threads[i].start();
        }

        String projectDirectory = "src/spring-framework";
        List<File> javaFiles = getJavaFiles(projectDirectory);

        HashMap<String, HashSet<String>> inheritanceIndex = new HashMap<>();

        Thread thread = new Thread(() -> {
            int index = 0;
            while (true) {
                try {
                    if (index == javaFiles.size() - 1) {
                        break;
                    }
                    inheritanceIndex.putAll(resultQueue.take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                index++;
            }
        });
        thread.start();

        for (File javaFile : javaFiles) {
            taskQueue.put(javaFile);
        }

        for (int i = 0; i < numberOfThreads; i++) {
            taskQueue.put(new File(""));
        }

        thread.join();

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
}

class BuilderInheritanceIndexProcessor implements Runnable {
    private static final Pattern CLASS_PATTERN = Pattern.compile(".*class\\s+(\\w+)\\s*(<.+>)?\\s*(extends\\s+(\\w+))?\\s+(implements\\s+([\\w,\\s]+)(<.+>)?)?");
    private static final Pattern INTERFACE_PATTERN = Pattern.compile(".*interface\\s+(\\w+)\\s*(<.+>)?\\s*(extends\\s+([\\w,\\s]+))?");
    private final BlockingQueue<File> taskQueue;
    private final BlockingQueue<HashMap<String, HashSet<String>>> resultQueue;

    public BuilderInheritanceIndexProcessor(BlockingQueue<File> taskQueue, BlockingQueue<HashMap<String, HashSet<String>>> resultQueue) {
        this.taskQueue = taskQueue;
        this.resultQueue = resultQueue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                File file = taskQueue.take();

                if (!file.exists()) {
                    System.out.println("Closed");
                    break;
                }

                HashMap<String, HashSet<String>> result = buildInheritanceIndex(file);
                resultQueue.put(result);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private HashMap<String, HashSet<String>> buildInheritanceIndex(File file) {
        HashMap<String, HashSet<String>> inheritanceIndex = new HashMap<>();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Matcher classMatcher = CLASS_PATTERN.matcher(line);
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

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return inheritanceIndex;
    }
}