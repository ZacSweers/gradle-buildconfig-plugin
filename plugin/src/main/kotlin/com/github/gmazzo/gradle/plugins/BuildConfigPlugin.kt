package com.github.gmazzo.gradle.plugins

import com.github.gmazzo.gradle.plugins.generators.BuildConfigJavaGenerator
import com.github.gmazzo.gradle.plugins.internal.*
import com.github.gmazzo.gradle.plugins.internal.bindings.PluginBindings
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
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

        sourceSets.configureEach {
            configureSourceSet(project, it, defaultSS.classSpec)
        }

        with(project) {
            val taskGraphLocked = AtomicBoolean()

            gradle.taskGraph.whenReady { taskGraphLocked.set(true) }

            PluginBindings.values().forEach {
                pluginManager.withPlugin(it.pluginId) { _ ->
                    it.handler(project, extension) { name, onSpec ->
                        sourceSets.maybeCreate(name).apply {
                            onSpec(classSpec)

                            extraSpecs.configureEach { extra ->
                                if (taskGraphLocked.get()) {
                                    throw IllegalStateException("Can't call 'forClass' after taskGraph was built!")
                                }
                                onSpec(extra)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun configureSourceSet(
        project: Project,
        sourceSet: BuildConfigSourceSetInternal,
        defaultSpec: BuildConfigClassSpecInternal
    ) {
        logger.debug("Creating buildConfig sourceSet '${sourceSet.name}' for $project")

        val prefix = sourceSet.name.takeUnless { it.equals("main", ignoreCase = true) }?.capitalize() ?: ""

        createGenerateTask(
            project, prefix, sourceSet, sourceSet.classSpec, defaultSpec,
            descriptionSuffix = "'${sourceSet.name}' source"
        )

        sourceSet.extraSpecs.configureEach { subSpec ->
            val childPrefix = prefix + subSpec.name.capitalize()

            createGenerateTask(
                project, childPrefix, sourceSet, subSpec, defaultSpec,
                descriptionSuffix = "'${subSpec.name}' spec on '${sourceSet.name}' source"
            )
        }
    }

    private fun createGenerateTask(
        project: Project,
        prefix: String,
        sourceSet: BuildConfigSourceSet,
        spec: BuildConfigClassSpecInternal,
        defaultSpec: BuildConfigClassSpecInternal,
        descriptionSuffix: String
    ) {
        spec.generateTask = project.tasks.register("generate${prefix}BuildConfig", BuildConfigTask::class.java) { task ->
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
