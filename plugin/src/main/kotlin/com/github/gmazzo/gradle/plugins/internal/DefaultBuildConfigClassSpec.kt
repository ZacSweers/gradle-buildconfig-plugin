package com.github.gmazzo.gradle.plugins.internal

import com.github.gmazzo.gradle.plugins.BuildConfigField
import javax.inject.Inject

internal abstract class DefaultBuildConfigClassSpec @Inject constructor(
    private val name: String
) : BuildConfigClassSpecInternal {

    override fun getName() = name

    override fun buildConfigField(field: BuildConfigField) =
        field.also { fields.put(it.name, it) }

}
