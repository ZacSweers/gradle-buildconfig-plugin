package com.github.gmazzo.gradle.plugins

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface BuildConfigTaskSpec {
    val className: Property<String>

    val packageName: Property<String>

    val fields: SetProperty<BuildConfigField>

    val outputDir: DirectoryProperty
}
