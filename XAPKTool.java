package org.catrious.xapktool;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import java.text.*;
import java.util.*;

public class XAPKTool {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: xapktool -d <input.xapk> <output-dir>");
            System.out.println("or: xapktool -b <input-dir> <output.xapk>");
            System.out.println("or: xapktool -v <input.xapk>");
            return;
        }

        String command = args[0];
        if ("-d".equals(command)) {
            handleDecompile(args);
        } else if ("-b".equals(command)) {
            handleBuild(args);
        } else if ("-v".equals(command)) {
            handleView(args);
        } else {
            System.out.println("Unknown command.");
        }
    }

    private static void handleDecompile(String[] args) {
        String inputXAPK = args[1];
        String outputDir = args[2];

        // Change extension from .xapk to .zip
        if (!inputXAPK.endsWith(".xapk")) {
            System.out.println("Input file must have .xapk extension.");
            return;
        }
        String zipFile = inputXAPK.replace(".xapk", ".zip");
        File xapkFile = new File(inputXAPK);
        File zipDestination = new File(zipFile);

        try {
            // Copy .xapk to .zip
            Files.copy(xapkFile.toPath(), zipDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            // Unzip the .zip to the output directory
            unzipFile(zipFile, outputDir);
            // Delete the temporary .zip file
            Files.delete(zipDestination.toPath());

            // Create xapktool.xml in the output directory
            createXAPKToolXml(outputDir, inputXAPK);

            // Zip only the content of the output folder
            zipDirectoryContents(outputDir, inputXAPK.replace(".xapk", "_decompiled.zip"));

            System.out.println("Decompile complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleBuild(String[] args) {
        String inputDir = args[1];
        String outputXAPK = args[2];

        // Zip the output folder content
        try {
            zipDirectoryContents(inputDir, outputXAPK.replace(".xapk", ".zip"));
            File zipFile = new File(outputXAPK.replace(".xapk", ".zip"));
            Files.move(zipFile.toPath(), new File(outputXAPK).toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Delete xapktool.xml when building
            deleteXAPKToolXml(inputDir);

            System.out.println("Build complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleView(String[] args) {
        String inputXAPK = args[1];

        // Change extension from .xapk to .zip
        if (!inputXAPK.endsWith(".xapk")) {
            System.out.println("Input file must have .xapk extension.");
            return;
        }
        String zipFile = inputXAPK.replace(".xapk", ".zip");
        File xapkFile = new File(inputXAPK);
        File zipDestination = new File(zipFile);

        try {
            // Copy .xapk to .zip
            Files.copy(xapkFile.toPath(), zipDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            // Unzip the .zip to the output directory
            String outputDir = "output";
            unzipFile(zipFile, outputDir);
            // Open the manifest.json file with Notepad (for Windows)
            openManifestWithNotepad(outputDir);

            // Delete the temporary .zip file
            Files.delete(zipDestination.toPath());

            // Delete the output folder
            deleteDirectory(new File(outputDir));

            System.out.println("View complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void unzipFile(String zipFile, String outputDir) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows: Use PowerShell to unzip
            executeCommand("powershell -Command Expand-Archive -Path \"" + zipFile + "\" -DestinationPath \"" + outputDir + "\"");
        } else {
            // macOS/Linux: Use unzip
            executeCommand("unzip \"" + zipFile + "\" -d \"" + outputDir + "\"");
        }
    }

    private static void openManifestWithNotepad(String outputDir) {
        File manifestFile = new File(outputDir, "manifest.json");
        if (manifestFile.exists()) {
            executeCommand("notepad \"" + manifestFile.getAbsolutePath() + "\"");
        } else {
            System.out.println("manifest.json not found.");
        }
    }

    private static void executeCommand(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.inheritIO(); // Redirect output/error to console
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.out.println("Command failed with exit code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void zipDirectoryContents(String dirPath, String zipFile) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            File dir = new File(dirPath);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            ZipEntry zipEntry = new ZipEntry(file.getName());
                            zipOut.putNextEntry(zipEntry);
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = fis.read(buffer)) >= 0) {
                                zipOut.write(buffer, 0, length);
                            }
                            zipOut.closeEntry();
                        }
                    }
                }
            }
        }
    }

    private static void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        Files.delete(file.toPath());
                    }
                }
            }
            Files.delete(directory.toPath());
        }
    }

    private static void createXAPKToolXml(String outputDir, String inputXAPK) {
        File xapkToolXml = new File(outputDir, "xapktool.xml");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(xapkToolXml))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<xapkToolInfo>\n");
            writer.write("    <toolName>XAPKTool</toolName>\n");
            writer.write("    <version>1.0</version>\n");
            writer.write("    <description>This file was created by XAPKTool during the decompiling process</description>\n");
            writer.write("    <inputXAPK>" + inputXAPK + "</inputXAPK>\n");
            writer.write("    <outputDir>" + outputDir + "</outputDir>\n");

            // Get current timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            String timestamp = sdf.format(new Date());
            writer.write("    <timestamp>" + timestamp + "</timestamp>\n");

            writer.write("</xapkToolInfo>\n");

            System.out.println("xapktool.xml created.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteXAPKToolXml(String outputDir) {
        File xapkToolXml = new File(outputDir, "xapktool.xml");
        if (xapkToolXml.exists()) {
            try {
                Files.delete(xapkToolXml.toPath());
                System.out.println("xapktool.xml deleted.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
