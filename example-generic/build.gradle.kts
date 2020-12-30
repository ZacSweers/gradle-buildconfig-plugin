import com.github.gmazzo.gradle.plugins.BuildConfigTask
import org.gradle.api.tasks.PathSensitivity.NONE

plugins {
    id("com.github.gmazzo.buildconfig") version "<latest>"
}

buildConfig {
    buildConfigField("String", "APP_NAME", "\"${project.name}\"")
    buildConfigField("String", "APP_SECRET", "\"Z3JhZGxlLWphdmEtYnVpbGRjb25maWctcGx1Z2lu\"")
    buildConfigField("long", "BUILD_TIME", "${TimeUnit.DAYS.toMillis(2)}L")
    buildConfigField("boolean", "FEATURE_ENABLED", "${true}")

    buildConfig.forClass("BuildResources") {
        buildConfigField("String", "A_CONSTANT", "\"aConstant\"")
    }
}

// everything below here are just helper code to allow testing the plugin as we can't rely on any framework like JUnit

val generateBuildConfig = tasks.named<BuildConfigTask>("generateBuildConfig")
val generateBuildResourcesBuildConfig = tasks.named<BuildConfigTask>("generateBuildResourcesBuildConfig")

val generateBuildConfigTest = task<AssertGeneratedFile>("generateBuildConfigTest") {
    filePath.set(generateBuildConfig.flatMap { it.outputDir.file("com/github/gmazzo/example_generic/BuildConfig.java") })
    expectedContent.set(
        """
        package com.github.gmazzo.example_generic;
        
        import java.lang.String;
        
        public final class BuildConfig {
          public static final String APP_NAME = "example-generic";
        
          public static final String APP_SECRET = "Z3JhZGxlLWphdmEtYnVpbGRjb25maWctcGx1Z2lu";
        
          public static final long BUILD_TIME = 172800000L;
        
          public static final boolean FEATURE_ENABLED = true;
        
          private BuildConfig() {
          }
        }
        """
    )
}

val generateBuildResourcesBuildConfigTest = task<AssertGeneratedFile>("generateBuildResourcesBuildConfigTest") {
    filePath.set(generateBuildResourcesBuildConfig.flatMap { it.outputDir.file("com/github/gmazzo/example_generic/BuildResources.java") })
    expectedContent.set(
        """
        package com.github.gmazzo.example_generic;
        
        import java.lang.String;
        
        public final class BuildResources {
          public static final String A_CONSTANT = "aConstant";
        
          private BuildResources() {
          }
        }
        """
    )
}

task<Delete>("clean") {
    delete(buildDir)
}

task("test") {
    dependsOn(generateBuildConfigTest, generateBuildResourcesBuildConfigTest)
}

abstract class AssertGeneratedFile : DefaultTask() {
    @get:PathSensitive(NONE)
    @get:InputFile
    abstract val filePath: RegularFileProperty

    @get:Input
    abstract val expectedContent: Property<String>

    @TaskAction
    fun performAssert() {
        val actualFile = filePath.asFile.get()
        if (!actualFile.isFile) {
            throw AssertionError("Expected file doesn't exist: $actualFile")
        }

        val content = expectedContent.get().trimIndent().trim()
        val actualContent = actualFile.readText().trim()
        if (actualContent != content) {
            throw AssertionError("Expected:\n$content\n\n but was:\n$actualContent")
        }
    }

}
