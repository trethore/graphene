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
import java.util.zip.ZipFile
import javax.inject.Inject

abstract class UnpackSourcesTask : DefaultTask() {

	private data class GitRepository(val url: String, val branch: String?)

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
		val outputDirFile = ensureDirectory(outputDir.get().asFile)

		unpackDependencySources(outputDirFile)
		cloneConfiguredRepositories(outputDirFile)
		unpackMinecraftSources(outputDirFile)
		unpackFabricSources(outputDirFile)
	}

	private fun unpackDependencySources(outputDirFile: File) {
		sourceDeps.files.sortedBy { it.name }.forEach { sourceArchive ->
			val targetDir = File(
				outputDirFile,
				sourceArchive.name.removeSuffix(".jar").removeSuffix("-sources")
			)
			unpackDependencySourceArchive(sourceArchive, targetDir)
		}
	}

	private fun cloneConfiguredRepositories(outputDirFile: File) {
		gitRepos.orNull.orEmpty().forEach { repositorySpec ->
			val repository = parseGitRepository(repositorySpec)
			val targetDir = File(outputDirFile, deriveRepoDirectoryName(repository.url))
			val commandLineArguments = mutableListOf("git", "clone", "--depth", "1")

			fileSystemOperations.delete { delete(targetDir) }
			repository.branch?.let { branch ->
				commandLineArguments.add("--branch")
				commandLineArguments.add(branch)
				commandLineArguments.add("--single-branch")
			}
			commandLineArguments.add(repository.url)
			commandLineArguments.add(targetDir.absolutePath)

			execOperations.exec {
				commandLine(commandLineArguments)
			}
		}
	}

	private fun parseGitRepository(repositorySpec: String): GitRepository {
		val trimmedSpec = repositorySpec.trim()
		if (trimmedSpec.isBlank()) {
			throw GradleException("Git repository specification cannot be blank")
		}

		val splitSpec = trimmedSpec.split('#', limit = 2)
		if (splitSpec.size == 1) {
			return GitRepository(trimmedSpec, null)
		}

		val url = splitSpec[0].trim()
		val branch = splitSpec[1].trim()
		if (url.isBlank() || branch.isBlank()) {
			throw GradleException("Invalid git repository specification: $repositorySpec. Use <url>#<branch>.")
		}

		return GitRepository(url, branch)
	}

	private fun unpackMinecraftSources(outputDirFile: File) {
		val minecraftCacheRoot = resolveExistingDirectory(minecraftCacheDir)
		val minecraftSources = listOfNotNull(
			findNewestSourceJar(minecraftCacheRoot, "minecraft-common-")?.let { "common" to it },
			findNewestSourceJar(minecraftCacheRoot, "minecraft-clientOnly-")?.let { "client" to it }
		)

		if (minecraftSources.isEmpty()) {
			logger.warn("Could not locate minecraft sources jars in loom cache")
			return
		}

		val minecraftTarget = recreateDirectory(File(outputDirFile, "minecraft"))
		minecraftSources.forEach { (name, archive) ->
			unpackArchive(archive, File(minecraftTarget, name))
		}
	}

	private fun deriveRepoDirectoryName(repoUrl: String): String {
		val trimmedUrl = repoUrl.trim().removeSuffix("/")
		val cleanedUrl = trimmedUrl.substringBefore('?').substringBeforeLast(".git")
		val ownerAndRepoMatch = Regex("[:/]([^/:]+)/([^/:]+)$").find(cleanedUrl)

		val repoName = ownerAndRepoMatch?.let { matchResult ->
			val owner = matchResult.groupValues[1]
			val repository = matchResult.groupValues[2]
			"$owner-$repository"
		} ?: cleanedUrl.substringAfterLast('/').substringAfterLast(':')

		if (repoName.isBlank()) {
			throw GradleException("Unable to derive repository name from URL: $repoUrl")
		}
		return repoName
	}

	private fun unpackFabricSources(outputDirFile: File) {
		val fabricCacheRoot = resolveExistingDirectory(fabricCacheDir)
		if (fabricCacheRoot == null) {
			logger.warn("Fabric cache directory not found, skipping fabric unpack")
			return
		}

		val fabricJars = findSourceJars(fabricCacheRoot) { file ->
			file.name.endsWith(SOURCES_JAR_SUFFIX)
		}.sortedBy { it.name }

		if (fabricJars.isEmpty()) {
			logger.warn("Fabric cache found but no sources jars were present")
			return
		}

		val fabricTarget = recreateDirectory(File(outputDirFile, "fabric"))

		fabricJars.forEach { jar ->
			val moduleTarget = File(fabricTarget, jar.name.removeSuffix(SOURCES_JAR_SUFFIX))
			unpackArchive(jar, moduleTarget)
		}
	}

	private fun unpackDependencySourceArchive(archiveFile: File, targetDir: File) {
		requireSourceArchive(archiveFile)
		unpackArchive(archiveFile, targetDir)
	}

	private fun unpackArchive(archiveFile: File, targetDir: File) {
		recreateDirectory(targetDir)

		fileSystemOperations.copy {
			from(archiveOperations.zipTree(archiveFile))
			into(targetDir)
		}
	}

	private fun requireSourceArchive(archiveFile: File) {
		val containsSources = ZipFile(archiveFile).use { zipFile ->
			zipFile.entries().asSequence().any { entry ->
				!entry.isDirectory && SOURCE_FILE_EXTENSIONS.any { extension ->
					entry.name.endsWith(extension)
				}
			}
		}

		if (!containsSources) {
			throw GradleException(
				"Resolved source archive ${archiveFile.name} does not contain source files. " +
					"Check the dependency classifier or published sources artifact."
			)
		}
	}

	private fun resolveExistingDirectory(directoryProperty: DirectoryProperty): File? {
		if (!directoryProperty.isPresent) {
			return null
		}

		val directory = directoryProperty.get().asFile
		return directory.takeIf(File::isDirectory)
	}

	private fun ensureDirectory(directory: File): File {
		directory.mkdirs()
		return directory
	}

	private fun recreateDirectory(directory: File): File {
		fileSystemOperations.delete { delete(directory) }
		return ensureDirectory(directory)
	}

	private fun findNewestSourceJar(root: File?, prefix: String): File? {
		if (root == null) {
			return null
		}

		return root.walkTopDown()
			.filter { file ->
				file.isFile &&
					file.name.startsWith(prefix) &&
					file.name.endsWith(SOURCES_JAR_SUFFIX) &&
					!file.name.endsWith(".backup.jar")
			}
			.maxByOrNull(File::lastModified)
	}

	private fun findSourceJars(root: File, predicate: (File) -> Boolean): List<File> {
		return root.walkTopDown()
			.filter { file -> file.isFile && predicate(file) }
			.toList()
	}

	private companion object {
		private const val SOURCES_JAR_SUFFIX = "-sources.jar"
		private val SOURCE_FILE_EXTENSIONS = listOf(".java", ".kt", ".groovy", ".scala")
	}
}
