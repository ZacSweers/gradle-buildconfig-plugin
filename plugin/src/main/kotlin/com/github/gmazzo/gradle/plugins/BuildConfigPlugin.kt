package com.github.gmazzo.gradle.plugins

import com.github.gmazzo.gradle.plugins.generators.BuildConfigJavaGenerator
import com.github.gmazzo.gradle.plugins.internal.BuildConfigClassSpecInternal
import com.github.gmazzo.gradle.plugins.internal.BuildConfigSourceSetInternal
import com.github.gmazzo.gradle.plugins.internal.DefaultBuildConfigClassSpec
import com.github.gmazzo.gradle.plugins.internal.DefaultBuildConfigExtension
import com.github.gmazzo.gradle.plugins.internal.DefaultBuildConfigSourceSet
import com.github.gmazzo.gradle.plugins.internal.bindings.PluginBindings
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class BuildConfigPlugin : Plugin<Project> {

    private val logger = Logging.getLogger(javaClass)

    override fun apply(project: Project) {
        val sourceSets = project.objects.domainObjectContainer(BuildConfigSourceSetInternal::class.java) { name ->
            DefaultBuildConfigSourceSet(
                project.objects.newInstance(DefaultBuildConfigClassSpec::class.java, name),
                project.objects.domainObjectContainer(BuildConfigClassSpecInternal::class.java) { newName ->
                    project.objects.newInstance(DefaultBuildConfigClassSpec::class.java, newName)
                }
            )
        }

        val defaultSS = sourceSets.create(DEFAULT_SOURCE_SET_NAME)

        val extension = project.extensions.create(
            BuildConfigExtension::class.java,
            "buildConfig",
            DefaultBuildConfigExtension::class.java,
            sourceSets,
            defaultSS
        )

        val taskProviders = ConcurrentHashMap<String, TaskProvider<BuildConfigTask>>()
        sourceSets.configureEach {
            configureSourceSet(project, it, defaultSS.classSpec, taskProviders)
        }

        with(project) {
            val taskGraphLocked = AtomicBoolean()

            gradle.taskGraph.whenReady { taskGraphLocked.set(true) }

            PluginBindings.values().forEach {
                pluginManager.withPlugin(it.pluginId) { _ ->
                    it.handler(project, extension) { name: String, onSpec: (TaskProvider<BuildConfigTask>) -> Unit ->
                        sourceSets.maybeCreate(name).apply {
                            onSpec(taskProviders.getValue(name.interpolatedPrefix().interpolatedTaskName()))

                            extraSpecs.configureEach { extra ->
                                if (taskGraphLocked.get()) {
                                    throw IllegalStateException("Can't call 'forClass' after taskGraph was built!")
                                }
                                onSpec(taskProviders.getValue(extra.name.interpolatedPrefix().interpolatedTaskName()))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun String.interpolatedTaskName(): String {
        return "generate${this}BuildConfig"
    }

    private fun String.interpolatedPrefix(): String {
        return takeUnless { it.equals("main", ignoreCase = true) }?.capitalize() ?: ""
    }

    private fun configureSourceSet(
        project: Project,
        sourceSet: BuildConfigSourceSetInternal,
        defaultSpec: BuildConfigClassSpecInternal,
        taskProviders: MutableMap<String, TaskProvider<BuildConfigTask>>
    ) {
        logger.debug("Creating buildConfig sourceSet '${sourceSet.name}' for $project")

        val prefix = sourceSet.name.interpolatedPrefix()

        val task = createGenerateTask(
            project, prefix, sourceSet, sourceSet.classSpec, defaultSpec,
            descriptionSuffix = "'${sourceSet.name}' source"
        )
        taskProviders[task.name] = task

        sourceSet.extraSpecs.configureEach { subSpec ->
            val childPrefix = prefix + subSpec.name.capitalize()

            val subtask = createGenerateTask(
                project, childPrefix, sourceSet, subSpec, defaultSpec,
                descriptionSuffix = "'${subSpec.name}' spec on '${sourceSet.name}' source"
            )
            taskProviders[subtask.name] = subtask
        }
    }

    private fun createGenerateTask(
        project: Project,
        prefix: String,
        sourceSet: BuildConfigSourceSet,
        spec: BuildConfigClassSpecInternal,
        defaultSpec: BuildConfigClassSpecInternal,
        descriptionSuffix: String
    ): TaskProvider<BuildConfigTask> =
        project.tasks.register(prefix.interpolatedTaskName(), BuildConfigTask::class.java) { task ->
            task.group = "BuildConfig"
            task.description = "Generates the build constants class for $descriptionSuffix"

            task.fields.set(spec.fields.map { it.values })
            task.outputDir.set(project.layout.buildDirectory.dir("generated/source/buildConfig/${sourceSet.name}/${spec.name.decapitalize()}"))

            task.className.set(spec.className.orElse(defaultSpec.className).orElse("${prefix}BuildConfig"))
            task.packageName.set(spec.packageName
              .orElse(defaultSpec.packageName)
              .orElse(project.provider { project.defaultPackage.replace(PACKAGE_REPLACE_REGEX, "_") }))
            task.generator.set(spec.generator.orElse(defaultSpec.generator).orElse(BuildConfigJavaGenerator))

            task.doFirst {
                task.outputDir.asFile.get().deleteRecursively()
            }
        }

    private val Project.defaultPackage
        get() = group
            .toString()
            .takeUnless { it.isEmpty() }
            ?.let { "$it.${project.name}" }
            ?: project.name

    companion object {

        const val DEFAULT_SOURCE_SET_NAME = "main"
        private val PACKAGE_REPLACE_REGEX = "[^a-zA-Z._$]".toRegex()

    }

}
