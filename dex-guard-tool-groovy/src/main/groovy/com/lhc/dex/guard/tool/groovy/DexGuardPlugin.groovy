package com.lhc.dex.guard.tool.groovy

import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.model.AndroidProject
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

class DexGuardPlugin implements Plugin<Project> {

    Project project

    @Override
    void apply(Project project) {

        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException("只能用于Application")
        }
        this.project = project
        def aarFile = loadFakeDex()
        //创建DSL
        project.extensions.create('dexGuard', DexGuardExtension)

        project.afterEvaluate {
            project.android.applicationVariants.all {
                ApplicationVariant variant ->
                    String taskName = "${variant.flavorName.capitalize()}${variant.buildType.name.capitalize()}"
                    //任务1:向manifest中插入一条meta-data，保存原application
                    DexGuardManifestTask manifestTask = project.tasks.create("dexGuardManifest${taskName}", DexGuardManifestTask)
                    def manifestFile = variant.outputs.first().processManifest.manifestOutputDirectory.listFiles().find {
                        File file ->
                            file.name == "AndroidManifest.xml"
                    }
                    manifestTask.manifest = manifestFile
                    //已经存在Manifest文件并且在打包前执行任务
                    manifestTask.mustRunAfter variant.outputs.first().processManifest
                    variant.outputs.first().processResources.dependsOn manifestTask

                    //任务2:加密任务
                    DexEncryptTask dexEncryptTask = project.tasks.create("dexEncrypt${taskName}", DexEncryptTask)
                    dexEncryptTask.aarFile = aarFile
                    dexEncryptTask.apkFile = variant.outputs.first().outputFile//拿到APK文件

                    String path = "${project.buildDir}/${AndroidProject.FD_OUTPUTS}/temp"

                    dexEncryptTask.outputs.file("${path}")
                    dexEncryptTask.baseName = "${project.name}-${variant.baseName}"

                    def assembleTask = project.tasks.getByName("assemble${taskName}")
                    def packageTask = project.tasks.getByName("package${taskName}")

                    //任务3:打包apk
                    DexAssembleTask dexAssembleTask = project.tasks.create("dexAssemble${taskName}", DexAssembleTask)
                    dexAssembleTask.outputs.file("${project.buildDir}/outputs/apk/${taskName}")
                    dexAssembleTask.inputs.file("${path}")
                    dexAssembleTask.baseName = "${project.name}-${variant.baseName}"
                    dexAssembleTask.signConfig = variant.variantData.variantConfiguration.signingConfig

                    dexAssembleTask.dependsOn dexEncryptTask
                    assembleTask.dependsOn dexAssembleTask
                    dexEncryptTask.mustRunAfter packageTask
            }
        }
    }

    def loadFakeDex() {
        //创建一个依赖分组
        def config = project.configurations.create("dexProxyClasspath")
        //创建需要拉取的工件信息
        def notation = [group  : 'com.lhc.dexguard.core',
                        name   : 'dexGuard',
                        version: '1.0']
        //添加依赖。会去仓库解析并拉取工件
        Dependency dependency = project.dependencies.add(config.name, notation)
        def aarFile = config.fileCollection { dependency }.singleFile
        project.logger.quiet("DexProxy:获取${notation} 依赖 ${aarFile}")
        aarFile
    }
}