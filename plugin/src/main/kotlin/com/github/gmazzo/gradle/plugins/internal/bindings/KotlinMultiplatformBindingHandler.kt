package com.github.gmazzo.gradle.plugins.internal.bindings

import com.github.gmazzo.gradle.plugins.BuildConfigClassSpec
import com.github.gmazzo.gradle.plugins.BuildConfigExtension
import com.github.gmazzo.gradle.plugins.BuildConfigPlugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation

internal object KotlinMultiplatformBindingHandler : PluginBindingHandler {

    override fun invoke(project: Project, extension: BuildConfigExtension, sourceSetProvider: SourceSetProvider) {
        extension.useKotlinOutput()

        (project.kotlinExtension as KotlinTargetsContainer).targets.configureEach { target ->
            target.compilations.configureEach { compilation ->
                compilation.allKotlinSourceSets.forEach { ss ->
                    val name = when (ss.name) {
                        KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME -> BuildConfigPlugin.DEFAULT_SOURCE_SET_NAME
                        else -> ss.name
                    }

                    sourceSetProvider(name) { project.bindSpec(compilation, it, ss) }
                }

                if (compilation is KotlinMetadataCompilation) {
                    sourceSetProvider(BuildConfigPlugin.DEFAULT_SOURCE_SET_NAME) {
                        project.tasks.getByName(compilation.compileKotlinTaskName).dependsOn(it.generateTask)
                    }
                }
            }
        }
    }

    private fun Project.bindSpec(
        compilation: KotlinCompilation<*>,
        spec: BuildConfigClassSpec,
        sourceSet: KotlinSourceSet
    ) {
        with(spec.generateTask) {
            sourceSet.kotlin.srcDir(outputDir)

            tasks.getByName(compilation.compileKotlinTaskName).dependsOn(this)
        }
    }

}
