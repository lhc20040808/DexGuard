package com.lhc.dex.guard.tool.groovy

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class DexEncryptTask extends DefaultTask {

    File apkFile
    File aarFile
    String baseName

    DexEncryptTask() {
        group = 'dexGuard'
        description = '加密Dex'
        outputs.upToDateWhen {
            false
        }
    }

    @TaskAction
    def run() {
        //解压aar
        def outDir = outputs.files.singleFile
        ZipUtils.unZip(aarFile, outDir)

        File clzJar
        outDir.listFiles().each {
            if (it.name == 'classes.jar') {
                clzJar = it
            } else {
                it.delete()
            }
        }
        File aarDex = "${outDir.absolutePath}/classes.dex"
        def result = "./dx --dex --output ${aarDex} ${clzJar}".execute()
        def out = new StringBuffer()
        def err = new StringBuffer()
        result.waitForProcessOutput(out, err)

        if (result.exitValue() != 0) {
            throw new GradleException("Jar to Dex 执行失败")
        }

        def apkFile = new File(baseName)
        ZipUtils.unZip(apkFile, outDir)
    }

}