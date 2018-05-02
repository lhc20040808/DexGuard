package com.lhc.dex.guard.tool.groovy

import com.android.build.gradle.internal.dsl.SigningConfig
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class DexAssembleTask extends DefaultTask {

    String baseName
    SigningConfig signConfig

    DexAssembleTask() {
        group = 'dexGuard'
        description = '将文件打包成apk'
        outputs.upToDateWhen {
            false
        }
    }

    @TaskAction
    def run() {
        project.logger.quiet("---开始打包---")
        def inDir = new File(inputs.files.singleFile, baseName)
        def outs = outputs.files.singleFile

        //打包成未签名的apk
        File unsignedApk = new File(outs, "${baseName}-unsigned.apk")
        ZipUtils.zip(inDir, unsignedApk)

        if (!signConfig) {
            throw new GradleException("签名信息异常")
        }

        File unsignedAlignedApk = new File(outs, "${baseName}-unsigned-aligned.apk")

        def zipAlignResult = "./zipalign -f 4  ${unsignedApk} ${unsignedAlignedApk}".execute()
        def zipOut = new StringBuffer()
        def zipErr = new StringBuffer()
        zipAlignResult.waitForProcessOutput(zipOut, zipErr)

        if (zipAlignResult.exitValue() != 0) {
            throw new GradleException("APK 对齐失败")
        }

        File signedApk = new File(outs, "${baseName}-signed.apk")

        def signResult = "./apksigner sign --ks ${signConfig.storeFile} --ks-key-alias ${signConfig.keyAlias} --ks-pass pass:${signConfig.keyPassword} --out  ${signedApk} ${unsignedAlignedApk}".execute()
        def signOut = new StringBuffer()
        def signErr = new StringBuffer()
        signResult.waitForProcessOutput(signOut, signErr)
        if (signResult.exitValue() != 0) {
            throw new GradleException("APK 打包失败")
        }

    }

}