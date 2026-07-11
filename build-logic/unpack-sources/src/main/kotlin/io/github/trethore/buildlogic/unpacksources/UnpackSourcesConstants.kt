package io.github.trethore.buildlogic.unpacksources

object UnpackSourcesConstants {
    const val REFERENCES_EXTENSION_NAME = "references"
    const val REFERENCES_DIR_NAME = "references"
    const val TASK_GROUP = "references"
    const val UNPACK_CONFIGURATION_NAME = "unpack"
    const val CFR_CONFIGURATION_NAME = "cfrDecompiler"
    const val CFR_VERSION = "0.152"
    const val FABRIC_MOD_JSON = "fabric.mod.json"
    const val MAX_NESTED_JAR_DEPTH = 8
    const val NESTED_OUTPUT_DIR = "nested"

    val ARCHIVE_EXTENSIONS = setOf("jar", "zip")
    const val LOOM_MINECRAFT_ARTIFACT_CONFIGURATION = "minecraftNamedCompile"
    const val STAGE_MINECRAFT_ARTIFACT_TASK_NAME = "stageMinecraftReferenceArtifact"
}
