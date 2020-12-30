package com.github.gmazzo.gradle.plugins.internal

import com.github.gmazzo.gradle.plugins.BuildConfigField
import com.github.gmazzo.gradle.plugins.BuildConfigTask
import org.gradle.api.tasks.TaskProvider
import javax.inject.Inject

internal abstract class DefaultBuildConfigClassSpec @Inject constructor(
    private val name: String
) : BuildConfigClassSpecInternal {

    override fun getName() = name

    override lateinit var generateTask: TaskProvider<BuildConfigTask>

    override fun buildConfigField(field: BuildConfigField) =
        field.also { fields.put(it.name, it) }

}
