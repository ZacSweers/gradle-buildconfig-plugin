package com.github.gmazzo.gradle.plugins.internal.bindings

import com.github.gmazzo.gradle.plugins.BuildConfigSourceSet
import com.github.gmazzo.gradle.plugins.BuildConfigTask
import org.gradle.api.tasks.TaskProvider

internal typealias SourceSetProvider = (name: String, onSpec: (TaskProvider<BuildConfigTask>) -> Unit) -> BuildConfigSourceSet
