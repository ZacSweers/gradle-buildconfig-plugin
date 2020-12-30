package com.github.gmazzo.gradle.plugins.internal.bindings

import com.github.gmazzo.gradle.plugins.BuildConfigClassSpec
import com.github.gmazzo.gradle.plugins.BuildConfigExtension
import com.github.gmazzo.gradle.plugins.BuildConfigTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal val Project.kotlinExtension
    get() = extensions.getByType(KotlinProjectExtension::class.java)

internal abstract class KotlinBindingHandler : PluginBindingHandler {

    abstract val KotlinSourceSet.compileTaskName: String

    internal val String.taskPrefix
        get() = takeUnless { it.equals("main", ignoreCase = true) }?.capitalize() ?: ""

    override fun invoke(project: Project, extension: BuildConfigExtension, sourceSetProvider: SourceSetProvider) {
        extension.useKotlinOutput()

        project.kotlinExtension.sourceSets.configureEach { ss ->
            sourceSetProvider(ss.name) { project.bindSpec(it, ss) }
        }
    }

    private fun Project.bindSpec(taskProvider: TaskProvider<BuildConfigTask>, sourceSet: KotlinSourceSet) {
        sourceSet.kotlin.srcDir(taskProvider.map { it.outputDir })
        tasks.getByName(sourceSet.compileTaskName).dependsOn(taskProvider)
    }

}
