package com.lhc.dex.guard.tool.groovy

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class DexGuardPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException("只能用于Application")
        }

        project.extensions.create('dexGuard', DexGuardExtension)

        project.afterEvaluate {
            project.android.applicationVariants.all {
                ApplicationVariant variant ->
                    //任务1：向manifest中插入一条meta-data，保存密钥
                    DexGuardManifestTask manifestTask = project.tasks.create("dexGuardManifest${variant.flavorName.capitalize()}${variant.buildType.name.capitalize()}", DexGuardManifestTask)
                    def manifestFile = variant.outputs.first().processManifest.manifestOutputFile
                    manifestTask.manifest = manifestFile
            }
        }

    }
}