package tytoo.graphene

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.process.ExecOperations
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class UnpackSourcesTask : DefaultTask() {

	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val sourceDeps: ConfigurableFileCollection

	@get:Input
	@get:Optional
	abstract val gitRepos: ListProperty<String>

	@get:InputDirectory
	@get:Optional
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val minecraftCacheDir: DirectoryProperty

	@get:InputDirectory
	@get:Optional
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val fabricCacheDir: DirectoryProperty

	@get:OutputDirectory
	abstract val outputDir: DirectoryProperty

	@get:Inject
	protected abstract val fileSystemOperations: FileSystemOperations

	@get:Inject
	protected abstract val archiveOperations: ArchiveOperations

	@get:Inject
	protected abstract val execOperations: ExecOperations

	@TaskAction
	fun runTask() {
		val outputDirFile = outputDir.get().asFile
		outputDirFile.mkdirs()

		unpackSourceDependencies(outputDirFile)
		cloneGitRepositories(outputDirFile)
		unpackMinecraftSources(outputDirFile)
		unpackFabricSources(outputDirFile)
	}

	private fun unpackSourceDependencies(outputDirFile: File) {
		sourceDeps.files.forEach { srcJar ->
			val baseName = srcJar.name
				.replace(Regex("\\.jar$"), "")
				.replace(Regex("-sources$"), "")
			val targetDir = File(outputDirFile, baseName)

			fileSystemOperations.delete { delete(targetDir) }
			fileSystemOperations.copy {
				from(archiveOperations.zipTree(srcJar))
				into(targetDir)
			}
		}
	}

	private fun cloneGitRepositories(outputDirFile: File) {
		val repositories = gitRepos.orNull.orEmpty()
		repositories.forEach { repoUrl ->
			val targetDir = File(outputDirFile, deriveRepoDirectoryName(repoUrl))

			fileSystemOperations.delete { delete(targetDir) }
			execOperations.exec {
				commandLine("git", "clone", "--depth", "1", repoUrl, targetDir.absolutePath)
			}
		}
	}

	private fun unpackMinecraftSources(outputDirFile: File) {
		val minecraftClientSourcesJar = findMinecraftSourcesJar("minecraft-clientOnly-")
		val minecraftCommonSourcesJar = findMinecraftSourcesJar("minecraft-common-")

		if (minecraftClientSourcesJar == null && minecraftCommonSourcesJar == null) {
			logger.warn("Could not locate minecraft sources jars in loom cache")
			return
		}

		val minecraftTarget = File(outputDirFile, "minecraft")
		fileSystemOperations.delete { delete(minecraftTarget) }
		minecraftTarget.mkdirs()

		minecraftCommonSourcesJar?.let {
			unpackJarSources(it, File(minecraftTarget, "common"))
		}
		minecraftClientSourcesJar?.let {
			unpackJarSources(it, File(minecraftTarget, "client"))
		}
	}

	private fun unpackJarSources(jarFile: File, targetDir: File) {
		fileSystemOperations.delete { delete(targetDir) }
		targetDir.mkdirs()

		fileSystemOperations.copy {
			from(archiveOperations.zipTree(jarFile))
			into(targetDir)
		}
	}

	private fun findMinecraftSourcesJar(prefix: String): File? {
		if (!minecraftCacheDir.isPresent) {
			return null
		}

		val root = minecraftCacheDir.get().asFile
		if (!root.exists()) {
			return null
		}

		return root.walkTopDown()
			.filter { it.isFile }
			.filter { file ->
				file.name.startsWith(prefix) &&
					file.name.endsWith("-sources.jar") &&
					!file.name.endsWith(".backup.jar")
			}
			.maxByOrNull { it.lastModified() }
	}

	private fun deriveRepoDirectoryName(repoUrl: String): String {
		val trimmedUrl = repoUrl.trim().removeSuffix("/")
		val repoName = trimmedUrl.substringAfterLast('/').substringBeforeLast(".git")
		if (repoName.isBlank()) {
			throw GradleException("Unable to derive repository name from URL: $repoUrl")
		}
		return repoName
	}

	private fun unpackFabricSources(outputDirFile: File) {
		val fabricJars = findFabricSourceJars()

		if (fabricJars.isEmpty()) {
			if (fabricCacheDir.isPresent) {
				logger.warn("Fabric cache found but no sources jars were present")
			} else {
				logger.warn("Fabric cache directory not found, skipping fabric unpack")
			}
			return
		}

		val fabricTarget = File(outputDirFile, "fabric")
		fileSystemOperations.delete { delete(fabricTarget) }
		fabricTarget.mkdirs()

		fabricJars.forEach { jar ->
			val moduleName = jar.name.replace(Regex("-sources\\.jar$"), "")
			val moduleTarget = File(fabricTarget, moduleName)

			fileSystemOperations.delete { delete(moduleTarget) }
			fileSystemOperations.copy {
				from(archiveOperations.zipTree(jar))
				into(moduleTarget)
			}
		}
	}

	private fun findFabricSourceJars(): List<File> {
		if (!fabricCacheDir.isPresent) {
			return emptyList()
		}

		val root = fabricCacheDir.get().asFile
		if (!root.exists()) {
			return emptyList()
		}

		return root.walkTopDown()
			.filter { it.isFile && it.name.endsWith("-sources.jar") }
			.toList()
	}
}
