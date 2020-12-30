package com.github.gmazzo.gradle.plugins.internal.bindings

import com.github.gmazzo.gradle.plugins.BuildConfigExtension
import com.github.gmazzo.gradle.plugins.BuildConfigTask
import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider

internal object JavaBindingHandler : PluginBindingHandler {

    override fun invoke(project: Project, extension: BuildConfigExtension, sourceSetProvider: SourceSetProvider) {
        extension.useJavaOutput()

        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.configureEach { ss ->
            DslObject(ss).convention.plugins["buildConfig"] = sourceSetProvider(ss.name) { project.bindSpec(it, ss) }
        }
    }

    private fun Project.bindSpec(taskProvider: TaskProvider<BuildConfigTask>, sourceSet: SourceSet) {
        sourceSet.java.srcDir(taskProvider.map { it.outputDir })
        tasks.getAt(sourceSet.compileJavaTaskName).dependsOn(taskProvider)
    }

}
