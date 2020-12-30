package com.github.gmazzo.gradle.plugins.internal

import com.github.gmazzo.gradle.plugins.BuildConfigClassSpec
import com.github.gmazzo.gradle.plugins.BuildConfigField
import org.gradle.api.provider.MapProperty

internal interface BuildConfigClassSpecInternal : BuildConfigClassSpec {

    val fields: MapProperty<String, BuildConfigField>

}
