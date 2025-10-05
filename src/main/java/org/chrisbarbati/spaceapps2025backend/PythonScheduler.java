package org.chrisbarbati.spaceapps2025backend;

import jakarta.annotation.PostConstruct;
import org.slf4j.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;

@Component
public class PythonScheduler {

    //Logging
    private static final Logger logger = LoggerFactory.getLogger(PythonScheduler.class);

    private final Path pythonScriptPath;

    public PythonScheduler() throws IOException {
        // Create a temporary working directory to hold the Python script + .env
        Path tempDir = Files.createTempDirectory("python_runner_");

        // Copy Python script
        ClassPathResource scriptResource = new ClassPathResource("scripts/downloader.py");
        this.pythonScriptPath = tempDir.resolve("downloader.py");
        try (InputStream in = scriptResource.getInputStream()) {
            Files.copy(in, pythonScriptPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Copy .env file (if it exists)
        ClassPathResource envResource = new ClassPathResource("scripts/.env");
        if (envResource.exists()) {
            Path envPath = tempDir.resolve(".env");
            try (InputStream in = envResource.getInputStream()) {
                Files.copy(in, envPath, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info(".env file extracted to {}", envPath);
        } else {
            logger.warn("No .env file found in resources/scripts/");
        }

        // Make sure the script is executable (Linux/macOS)
        pythonScriptPath.toFile().setExecutable(true);

        logger.info("Python script extracted to {}", pythonScriptPath);
        logger.info("Working directory for Python process: {}", tempDir);
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void runPythonScript() {
        try {
            String pythonExe = detectPythonExecutable();
            logger.info("Executing Python script using {}", pythonExe);
            logger.info("Python script path: {}", pythonScriptPath);

            ProcessBuilder pb = new ProcessBuilder(pythonExe, pythonScriptPath.getFileName().toString());
            pb.directory(pythonScriptPath.getParent().toFile()); // set working dir

            Process process = pb.start();

            // Start threads to handle stdout and stderr separately
            Thread stdoutReader = new Thread(() ->
                    readStream(process.getInputStream(), line -> logger.info("[Python] {}", line)));
            Thread stderrReader = new Thread(() ->
                    readStream(process.getErrorStream(), line -> logger.error("[Python-ERR] {}", line)));

            stdoutReader.start();
            stderrReader.start();

            int exitCode = process.waitFor();

            stdoutReader.join();
            stderrReader.join();

            if (exitCode == 0) {
                logger.info("Python script completed successfully (exit code = 0)");
            } else {
                logger.warn("Python script exited with non-zero code: {}", exitCode);
            }

        } catch (Exception e) {
            logger.error("Error while executing Python script", e);
        }
    }

    private void readStream(InputStream stream, Consumer<String> logLine) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logLine.accept(line);
            }
        } catch (IOException e) {
            logger.error("Error reading Python process output", e);
        }
    }

    private String detectPythonExecutable() {
        List<String> candidates = List.of("python3", "python");

        for (String candidate : candidates) {
            try {
                Process check = new ProcessBuilder(candidate, "--version").start();
                if (check.waitFor() == 0) {
                    return candidate;
                }
            } catch (Exception ignored) {}
        }

        throw new IllegalStateException("Python interpreter not found on system PATH.");
    }

    public Path getPythonScriptPath() {
        return pythonScriptPath;
    }
}
