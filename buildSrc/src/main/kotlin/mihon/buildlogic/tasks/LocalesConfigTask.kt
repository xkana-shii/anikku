package mihon.buildlogic.tasks

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import java.io.File

private val emptyResourcesElement = "<resources>\\s*</resources>|<resources\\s*/>".toRegex()

fun Project.getLocalesConfigTask(outputResourceDir: File): TaskProvider<Task> {
    val resourceFiles = fileTree("$projectDir/src/commonMain/moko-resources/")
        .matching { include("**/strings.xml") }
    val outputFile = outputResourceDir.resolve("xml/locales_config.xml")

    return tasks.register("generateLocalesConfig") {
        // This resource is consumed by the app manifest. Declare both inputs and output so a
        // clean build cannot incorrectly treat the task as up-to-date after its directory is gone.
        inputs.files(resourceFiles).withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.file(outputFile)

        doLast {
            val locales = resourceFiles
                .filterNot { it.readText().contains(emptyResourcesElement) }
                .map {
                    it.parentFile.name
                        .replace("base", "en")
                        .replace("-r", "-")
                        .replace("+", "-")
                }
                .sorted()
                .joinToString("\n") { "|   <locale android:name=\"$it\"/>" }

            val content = """
            |<?xml version="1.0" encoding="utf-8"?>
            |<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
            $locales
            |</locale-config>
            """.trimMargin()

            outputFile.apply {
                parentFile.mkdirs()
                writeText(content)
            }
        }
    }
}
