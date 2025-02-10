package dev.raphaeldelio.redisapollo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static java.lang.Math.abs;

@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileService() {
    }

    /**
     * Reads a file containing a JSON array of arrays of strings, then processes
     * each inner array into an object of type T using reflection.
     */
    public <T> void readAndProcessFile(String filePath, Class<T> clazz, Consumer<List<T>> process) {
        try {
            // Read file content
            String content = Files.readString(Path.of(filePath));

            // Parse into a list of list of strings
            // e.g. [[val1, val2, ...], [val1, val2, ...], ...]
            List<List<String>> data = objectMapper.readValue(content, new TypeReference<>() {
            });

            // Convert each sub-list into an object via reflection
            List<T> results = new ArrayList<>();
            for (List<String> row : data) {
                T obj = convertRowToObject(row, clazz);
                if (obj != null) {
                    results.add(obj);
                }
            }

            // Pass the list of objects to the provided lambda
            process.accept(results);

        } catch (IOException e) {
            throw new RuntimeException("Error reading or processing file: " + filePath, e);
        }
    }

    /**
     * Attempts to construct an instance of T by matching the row size
     * to a constructor parameter count. If row.size() is smaller than the
     * constructor's param count, missing params get default values.
     */
    private <T> T convertRowToObject(List<String> row, Class<T> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            int paramCount = paramTypes.length;
            // We'll try to use a constructor with paramCount >= row.size() if possible.
            if (paramCount >= row.size()) {
                try {
                    Object[] args = new Object[paramCount];
                    for (int i = 0; i < paramCount; i++) {
                        if (i < row.size()) {
                            args[i] = parseValue(row.get(i), paramTypes[i]);
                        } else {
                            args[i] = defaultForType(paramTypes[i]);
                        }
                    }
                    constructor.setAccessible(true);
                    return clazz.cast(constructor.newInstance(args));
                } catch (InstantiationException | IllegalAccessException |
                         InvocationTargetException | IllegalArgumentException e) {
                    // If one constructor fails, we move on to the next
                }
            }
        }
        return null;
    }

    /**
     * Parses a string value into an object of the given paramType.
     */
    private Object parseValue(String value, Class<?> paramType) {
        if (paramType == String.class) {
            return value;
        } else if (paramType == int.class || paramType == Integer.class) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else if (paramType == boolean.class || paramType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        // Extend to handle other types if needed
        return null;
    }

    /**
     * Default value if a row is missing columns.
     */
    private Object defaultForType(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return false;
        } else if (type == int.class || type == Integer.class) {
            return 0;
        } else if (type == String.class) {
            return "";
        }
        // Extend for other types
        return null;
    }

    /**
     * Converts "HHHMMSS" (e.g. "0710700") to total seconds.
     */
    public int toHMSToSeconds(String hms) {
        if (hms.length() != 7) {
            throw new IllegalArgumentException("Timestamp format is invalid: " + hms);
        }
        boolean isNegative = hms.charAt(0) == '-';
        int hours = Integer.parseInt(hms.substring(0, 3));
        int minutes = Integer.parseInt(hms.substring(3, 5));
        int seconds = Integer.parseInt(hms.substring(5, 7));
        int absNumber = abs(hours * 3600 + minutes * 60 + seconds);
        return isNegative ? -absNumber : absNumber;
    }

    /**
     * Converts a string like "00:00:00" or "-00:05:00" to seconds (with sign).
     */
    public int asSeconds(String timeString) {
        if (timeString == null) return 0;
        boolean isNegative = timeString.startsWith("-");
        String noSign = isNegative ? timeString.substring(1) : timeString;
        String[] parts = noSign.split(":");
        if (parts.length < 3) return 0;

        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        int totalSeconds = hours * 3600 + minutes * 60 + seconds;

        return isNegative ? -totalSeconds : totalSeconds;
    }
}