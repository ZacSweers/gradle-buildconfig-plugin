package com.github.gmazzo.gradle.plugins

import com.github.gmazzo.gradle.plugins.generators.BuildConfigGenerator
import com.github.gmazzo.gradle.plugins.generators.BuildConfigJavaGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class BuildConfigTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask(), BuildConfigTaskSpec {

    @get:Input
    override val className: Property<String> = objects.property(String::class.java)
        .convention("BuildConfig")

    @get:Input
    override val packageName: Property<String> = objects.property(String::class.java)
        .convention("")

    @get:Internal
    override val fields: SetProperty<BuildConfigField> = objects.setProperty(BuildConfigField::class.java)
        .convention(emptyList())

    @get:Internal
    val generator: Property<BuildConfigGenerator> = objects.property(BuildConfigGenerator::class.java)
        .convention(BuildConfigJavaGenerator)

    @get:Input
    internal val generatorProperty
        get() = generator::class.java

    @get:Input
    internal val fieldsProperty
        get() = fields.map { it.toString() }

    @get:OutputDirectory
    abstract override val outputDir: DirectoryProperty

    init {
        onlyIf { fields.get().isNotEmpty() }
    }

    @TaskAction
    protected fun generateBuildConfigFile() {
        generator.get().execute(this)
    }

}
