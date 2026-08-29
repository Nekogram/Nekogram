package tw.nekomimi.nekogram.tlv

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.telegram.tlrpc.schema.TlSchema
import org.telegram.tlrpc.schema.TlSchemaJsonParser
import org.telegram.tlrpc.schema.TlSchemaObject
import org.telegram.tlrpc.schema.TlSchemaParamType
import org.telegram.tlrpc.schema.TlSchemaPrimitiveType
import java.io.File
import java.util.ArrayDeque
import java.util.Locale

abstract class GenerateTlReadersTask : DefaultTask() {

    companion object {
        private val TYPES_TO_ADD = listOf(
            "message",
            "messageService",
            "messageEmpty",

            "channel",
            "channelForbidden",
            "channelFull",

            "chatEmpty",
            "chat",
            "chatForbidden",
            "chatFull",

            "userEmpty",
            "user",
            "userFull",

            "channelAdminLogEvent",
        )

        private val BUILTIN_OBJECT_TYPES = setOf(
            "Bool",
            "True",
            "Null",
        )
    }

    @get:InputDirectory
    abstract val schemaDir: DirectoryProperty

    @get:InputFile
    abstract val tlrpcFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: Property<String>

    @get:OutputDirectory
    abstract val outputFolder: DirectoryProperty

    @get:Input
    val typesToAdd: List<String> = TYPES_TO_ADD

    @TaskAction
    fun generate() {
        val layer = readLayer(tlrpcFile.get().asFile)
        val currentSchemaFile = File(schemaDir.get().asFile, "$layer.json")
        if (!currentSchemaFile.isFile) {
            throw GradleException("Schema file not found for layer $layer: ${currentSchemaFile.absolutePath}")
        }

        val currentSchema = TlSchemaJsonParser.parse(currentSchemaFile)
        val currentObjects = resolveObjects(currentSchema, requireRoots = true)
        val currentIds = currentObjects.map { it.magic }.toSet()

        val legacyObjectsById = linkedMapOf<UInt, LegacyEntry>()
        legacyFiles(layer).forEach { (legacyLayer, legacyFile) ->
            val legacySchema = TlSchemaJsonParser.parse(legacyFile)
            resolveObjects(legacySchema, requireRoots = false)
                .asSequence()
                .filter { it.magic !in currentIds }
                .forEach { entry ->
                    legacyObjectsById[entry.magic] = LegacyEntry(legacyLayer, entry)
                }
        }

        val outputFile = outputFolder.get().file(outputFile.get()).asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            generateJavaReaders(
                layer,
                currentObjects,
                legacyObjectsById.values.toList()
            )
        )

        logger.lifecycle(
            "Generated ${outputFile.absolutePath} with ${currentObjects.size} current readers and ${legacyObjectsById.size} legacy readers"
        )
    }

    private fun legacyFiles(currentLayer: Int): List<Pair<Int, File>> {
        return schemaDir.get().asFile.listFiles()
            .orEmpty()
            .mapNotNull { file ->
                val layer = file.nameWithoutExtension.toIntOrNull() ?: return@mapNotNull null
                if (file.extension != "json" || layer >= currentLayer || layer <= currentLayer - 3) return@mapNotNull null
                layer to file
            }
            .sortedBy { it.first }
    }

    private fun readLayer(file: File): Int {
        val content = file.readText()
        val match = Regex("""public\s+static\s+final\s+int\s+LAYER\s*=\s*(\d+)\s*;""")
            .find(content)
            ?: throw GradleException("Could not find TLRPC.LAYER in ${file.absolutePath}")
        return match.groupValues[1].toInt()
    }

    private fun resolveObjects(schema: TlSchema, requireRoots: Boolean): List<TlSchemaObject> {
        val constructorsByName = schema.constructors.associateBy { it.name }
        val constructorsByType = schema.constructors.groupBy { it.type }
        val queue = ArrayDeque<TlSchemaObject>()

        for (type in typesToAdd) {
            val root = constructorsByName[type]
            if (root == null) {
                if (requireRoots) {
                    throw GradleException("Root type '$type' was not found in schema")
                }
                continue
            }
            queue.addLast(root)
        }

        val resolved = linkedMapOf<UInt, TlSchemaObject>()

        while (queue.isNotEmpty()) {
            val entry = queue.removeLast()
            if (resolved.putIfAbsent(entry.magic, entry) != null) {
                continue
            }

            entry.params.forEach { param ->
                for (typeName in collectReferencedTypes(param.type)) {
                    if (typeName in BUILTIN_OBJECT_TYPES) {
                        continue
                    }
                    constructorsByType[typeName].orEmpty().forEach(queue::addLast)
                }
            }
        }

        return resolved.values.toList()
    }

    private fun collectReferencedTypes(type: TlSchemaParamType): Set<String> {
        return when (type) {
            TlSchemaParamType.Flags -> emptySet()
            is TlSchemaParamType.Flag.True -> emptySet()
            is TlSchemaParamType.Flag.Optional -> collectReferencedTypes(type.type)
            is TlSchemaParamType.Primary.Primitive -> emptySet()
            is TlSchemaParamType.Primary.Vector -> collectReferencedTypes(type.type)
            is TlSchemaParamType.Primary.Object -> setOf(type.name)
            TlSchemaParamType.Primary.X -> emptySet()
        }
    }

    private fun generateJavaReaders(
        layer: Int,
        currentObjects: List<TlSchemaObject>,
        legacyObjects: List<LegacyEntry>,
    ): String {
        val totalCount = currentObjects.size + legacyObjects.size
        val out = StringBuilder()

        out.appendLine("package tw.nekomimi.nekogram.tlv;")
        out.appendLine()
        out.appendLine("import android.util.Base64;")
        out.appendLine()
        out.appendLine("import com.google.gson.JsonObject;")
        out.appendLine()
        out.appendLine("import org.telegram.tgnet.TLObject;")
        out.appendLine()
        out.appendLine("import java.util.HashMap;")
        out.appendLine("import java.util.Map;")
        out.appendLine("import java.util.function.Function;")
        out.appendLine()
        out.appendLine("public final class TlReaders {")
        out.appendLine()
        out.appendLine("    public static final Map<Integer, Function<TlBinaryReader, JsonObject>>")
        out.appendLine("            READERS = new HashMap<>($totalCount);")
        out.appendLine()
        out.appendLine("    static {")

        currentObjects.forEach { entry ->
            out.appendLine(
                "        READERS.put(0x${entry.magic.toString(16)}, TlReaders::${
                    javaReadMethodName(
                        entry.name
                    )
                });"
            )
        }
        legacyObjects.forEach { legacy ->
            out.appendLine(
                "        READERS.put(0x${legacy.entry.magic.toString(16)}, TlReaders::${
                    javaReadMethodName(
                        legacy.entry.name
                    )
                }Layer${legacy.layer});"
            )
        }
        out.appendLine("    }")
        out.appendLine()

        currentObjects.forEach { entry ->
            appendReaderMethod(out, entry, null)
            out.appendLine()
        }
        legacyObjects.forEachIndexed { index, legacy ->
            appendReaderMethod(out, legacy.entry, legacy.layer)
            if (index != legacyObjects.lastIndex) {
                out.appendLine()
            }
        }

        out.appendLine("}")
        out.appendLine("// LAYER $layer")
        return out.toString()
    }

    private fun appendReaderMethod(out: StringBuilder, entry: TlSchemaObject, layer: Int?) {
        val methodName = if (layer == null) {
            javaReadMethodName(entry.name)
        } else {
            "${javaReadMethodName(entry.name)}Layer$layer"
        }

        out.appendLine("    private static JsonObject $methodName(TlBinaryReader r) {")
        out.appendLine("        var o = new JsonObject();")
        out.appendLine("        o.addProperty(\"_\", \"${entry.name}\");")

        entry.params.forEach { param ->
            appendParamRead(out, param.name, param.type)
        }

        out.appendLine("        return o;")
        out.appendLine("    }")
    }

    private fun appendParamRead(out: StringBuilder, fieldName: String, type: TlSchemaParamType) {
        when (type) {
            TlSchemaParamType.Flags -> {
                out.appendLine("        var $fieldName = r.readInt();")
                out.appendLine("        o.addProperty(\"$fieldName\", $fieldName);")
            }

            is TlSchemaParamType.Flag.True -> {
                out.appendLine(
                    "        o.addProperty(\"$fieldName\", ${
                        flagCondition(
                            type.flag,
                            type.num
                        )
                    });"
                )
            }

            is TlSchemaParamType.Flag.Optional -> {
                out.appendLine("        if (${flagCondition(type.flag, type.num)}) {")
                val readExpr = readExpression(type.type)
                val addMethod = if (isPrimitiveJsonValue(type.type)) "addProperty" else "add"
                out.appendLine("            o.$addMethod(\"$fieldName\", $readExpr);")
                out.appendLine("        }")
            }

            else -> {
                val readExpr = readExpression(type)
                val addMethod = if (isPrimitiveJsonValue(type)) "addProperty" else "add"
                out.appendLine("        o.$addMethod(\"$fieldName\", $readExpr);")
            }
        }
    }

    private fun readExpression(type: TlSchemaParamType): String {
        return when (type) {
            TlSchemaParamType.Flags -> error("Flags cannot be read as a value")
            is TlSchemaParamType.Flag.True -> error("Flag.True cannot be read as a value")
            is TlSchemaParamType.Flag.Optional -> readExpression(type.type)
            is TlSchemaParamType.Primary.Vector -> "r.readVector(${vectorReader(type.type)})"
            is TlSchemaParamType.Primary.Primitive -> when (type.type) {
                TlSchemaPrimitiveType.INT -> "r.readInt()"
                TlSchemaPrimitiveType.LONG -> "r.readLong()"
                TlSchemaPrimitiveType.DOUBLE -> "r.readDouble()"
                TlSchemaPrimitiveType.STRING -> "r.readString()"
                TlSchemaPrimitiveType.BYTES,
                TlSchemaPrimitiveType.INT256 -> "Base64.encodeToString(r.readBytes(), Base64.NO_WRAP)"
            }

            is TlSchemaParamType.Primary.Object -> when (type.name) {
                "Bool" -> "r.readBoolean()"
                else -> "r.readObject()"
            }

            TlSchemaParamType.Primary.X -> "r.readObject()"
        }
    }

    private fun vectorReader(type: TlSchemaParamType.Primary): String {
        return when (type) {
            is TlSchemaParamType.Primary.Primitive -> when (type.type) {
                TlSchemaPrimitiveType.INT -> "TlBinaryReader::readInt"
                TlSchemaPrimitiveType.LONG -> "TlBinaryReader::readLong"
                TlSchemaPrimitiveType.DOUBLE -> "TlBinaryReader::readDouble"
                TlSchemaPrimitiveType.STRING -> "TlBinaryReader::readString"
                TlSchemaPrimitiveType.BYTES,
                TlSchemaPrimitiveType.INT256 -> "TlBinaryReader::readBytes"
            }

            is TlSchemaParamType.Primary.Vector -> "TlBinaryReader::readObject"
            is TlSchemaParamType.Primary.Object -> when (type.name) {
                "Bool" -> "TlBinaryReader::readBoolean"
                else -> "TlBinaryReader::readObject"
            }

            TlSchemaParamType.Primary.X -> "TlBinaryReader::readObject"
        }
    }

    private fun isPrimitiveJsonValue(type: TlSchemaParamType): Boolean {
        return when (type) {
            TlSchemaParamType.Flags -> false
            is TlSchemaParamType.Flag.True -> true
            is TlSchemaParamType.Flag.Optional -> isPrimitiveJsonValue(type.type)
            is TlSchemaParamType.Primary.Primitive -> true
            is TlSchemaParamType.Primary.Vector -> false
            is TlSchemaParamType.Primary.Object -> type.name == "Bool"
            TlSchemaParamType.Primary.X -> false
        }
    }

    private fun flagCondition(flagName: String, flagNum: Int): String {
        return "TLObject.hasFlag($flagName, TLObject.FLAG_$flagNum)"
    }

    private fun javaReadMethodName(name: String): String {
        val normalized = name.replace('.', '_')
        val capitalized = normalized.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        return "read$capitalized"
    }

    private data class LegacyEntry(
        val layer: Int,
        val entry: TlSchemaObject,
    )
}

