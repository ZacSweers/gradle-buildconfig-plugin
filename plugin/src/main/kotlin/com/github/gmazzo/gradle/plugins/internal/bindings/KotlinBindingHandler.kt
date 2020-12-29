package com.github.gmazzo.gradle.plugins.internal.bindings

import com.github.gmazzo.gradle.plugins.BuildConfigClassSpec
import com.github.gmazzo.gradle.plugins.BuildConfigExtension
import org.gradle.api.Project
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

    private fun Project.bindSpec(spec: BuildConfigClassSpec, sourceSet: KotlinSourceSet) {
        sourceSet.kotlin.srcDir(spec.generateTask.map { it.outputDir })
        tasks.getByName(sourceSet.compileTaskName).dependsOn(spec.generateTask)
    }

}
