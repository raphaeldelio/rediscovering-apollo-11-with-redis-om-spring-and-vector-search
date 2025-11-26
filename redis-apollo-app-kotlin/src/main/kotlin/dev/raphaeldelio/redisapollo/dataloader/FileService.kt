package dev.raphaeldelio.redisapollo.dataloader

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs

@Service
class FileService {
    private val objectMapper = ObjectMapper()

    /**
     * Reads a file containing a JSON array of arrays of strings,
     * then processes each inner array into an object of type T using reflection.
     */
    fun <T : Any> readAndProcessFile(filePath: String, clazz: Class<T>, process: (List<T>) -> Unit) {
        try {
            val content = Files.readString(Path.of(filePath))
            val data: List<List<String>> = objectMapper.readValue(content, object : TypeReference<List<List<String>>>() {})
            val results = data.mapNotNull { row -> convertRowToObject(row, clazz) }
            process(results)
        } catch (e: IOException) {
            throw RuntimeException("Error reading or processing file: $filePath", e)
        }
    }

    /**
     * Constructs an instance of T from a row by matching the row to a constructor.
     */
    private fun <T : Any> convertRowToObject(row: List<String>, clazz: Class<T>): T? {
        val constructors = clazz.declaredConstructors
        for (constructor in constructors) {
            val paramTypes = constructor.parameterTypes
            if (paramTypes.size == row.size) {
                try {
                    val args = Array(paramTypes.size) { i ->
                        if (i < row.size) parseValue(row[i], paramTypes[i]) else defaultForType(paramTypes[i])
                    }
                    constructor.isAccessible = true
                    return clazz.cast(constructor.newInstance(*args))
                } catch (_: Exception) {
                    // Try next constructor
                }
            }
        }
        return null
    }

    private fun parseValue(value: String, paramType: Class<*>): Any? = when (paramType) {
        String::class.java -> value
        Int::class.java, Integer::class.java -> value.toIntOrNull() ?: 0
        Boolean::class.java, java.lang.Boolean::class.java -> value.toBoolean()
        else -> null
    }

    private fun defaultForType(type: Class<*>): Any? = when (type) {
        Boolean::class.java, java.lang.Boolean::class.java -> false
        Int::class.java, Integer::class.java -> 0
        String::class.java -> ""
        else -> null
    }

    /**
     * Converts "HHHMMSS" (e.g. "0710700") to total seconds.
     */
    fun toHMSToSeconds(hms: String): Int {
        require(hms.length == 7) { "Timestamp format is invalid: $hms" }
        val isNegative = hms[0] == '-'
        val hours = hms.substring(0, 3).toInt()
        val minutes = hms.substring(3, 5).toInt()
        val seconds = hms.substring(5, 7).toInt()
        val absSeconds = abs(hours * 3600 + minutes * 60 + seconds)
        return if (isNegative) -absSeconds else absSeconds
    }

    /**
     * Converts "HH:MM:SS" or "-HH:MM:SS" to total seconds.
     */
    fun asSeconds(timeString: String?): Int {
        if (timeString == null) return 0
        val isNegative = timeString.startsWith("-")
        val noSign = if (isNegative) timeString.substring(1) else timeString
        val parts = noSign.split(":")
        if (parts.size < 3) return 0

        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        val seconds = parts[2].toInt()
        val total = hours * 3600 + minutes * 60 + seconds

        return if (isNegative) -total else total
    }
}