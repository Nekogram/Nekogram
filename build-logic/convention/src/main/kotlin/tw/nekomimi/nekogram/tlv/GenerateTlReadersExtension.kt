package tw.nekomimi.nekogram.tlv

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class GenerateTlReadersExtension {

    abstract val schemaDir: DirectoryProperty

    abstract val tlrpcFile: RegularFileProperty

    abstract val outputFile: Property<String>
}
