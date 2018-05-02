package com.lhc.dex.guard.tool.groovy

import groovy.xml.Namespace
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DexGuardManifestTask extends DefaultTask {
    def static final KEY = "guard_key"
    File manifest

    DexGuardManifestTask() {
        group = 'dexGuard'
        description = '将AES密钥插入manifest'
        outputs.upToDateWhen {
            false
        }
    }

    @TaskAction
    def run() {
        String key = project.extensions.dexGuard.key
        if (key == null || key.isEmpty()) {
            return
        }

        project.logger.quiet("DexGuard:插入${key}")
        def ns = new Namespace('http://schemas.android.com/apk/res/android', 'android')
        def xml = new XmlParser().parse(manifest)
        Node application = xml.application[0]
        def metaDataTag = application['meta-data']
        metaDataTag.findAll {
            Node node ->
                node.attributes()[ns.name] == KEY
        }.each {
            Node node ->
                node.parent().remove(node)
        }

        application.appendNode('meta-data', [(ns.name): KEY, (ns.value): key])
        def pm = new XmlNodePrinter(new PrintWriter(manifest, "UTF-8"))
        pm.print(xml)
    }

}