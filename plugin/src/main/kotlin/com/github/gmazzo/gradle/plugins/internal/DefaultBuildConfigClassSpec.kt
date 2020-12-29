package com.github.gmazzo.gradle.plugins.internal

import com.github.gmazzo.gradle.plugins.BuildConfigField
import com.github.gmazzo.gradle.plugins.BuildConfigTask
import com.github.gmazzo.gradle.plugins.generators.BuildConfigGenerator
import org.gradle.api.tasks.TaskProvider

internal open class DefaultBuildConfigClassSpec(
    private val name: String
) : BuildConfigClassSpecInternal {

    override fun getName() = name

    override var className: String? = null

    override var packageName: String? = null

    override var generator: BuildConfigGenerator? = null

    override val fields = linkedMapOf<String, BuildConfigField>()

    override lateinit var generateTask: TaskProvider<BuildConfigTask>

    override fun buildConfigField(field: BuildConfigField) =
        field.also { fields[it.name] = it }

}
