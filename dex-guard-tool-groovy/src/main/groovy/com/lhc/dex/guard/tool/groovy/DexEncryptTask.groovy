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

        String key = project.dexGuard.key
        if (key != null && !key.isEmpty()) {
            AES.init(key)
        } else {
            AES.init(AES.DEFAULT_PWD)
        }

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
                if (it.directory) {
                    it.deleteDir()
                } else {
                    it.delete()
                }
            }
        }

        //将jar转换成dex
        File aarDex = new File("${outDir.absolutePath}/classes.dex")
        def result = "./dx --dex --output ${aarDex} ${clzJar}".execute()
        def out = new StringBuffer()
        def err = new StringBuffer()
        result.waitForProcessOutput(out, err)

        if (result.exitValue() != 0) {
            throw new GradleException("Jar to Dex 执行失败")
        }

        //解压apk
        def unzipFile = new File(outDir, baseName)
        ZipUtils.unZip(apkFile, unzipFile)

        def dexFiles = unzipFile.listFiles().findAll {
            it.name.endsWith(".dex")
        }

        //对原始的dex加密
        dexFiles.each {
            def encryptData = AES.encrypt(it.bytes)
            it.withOutputStream {
                it.write(encryptData)
            }
            it.renameTo("secret-${it.name}")
        }

    }

}