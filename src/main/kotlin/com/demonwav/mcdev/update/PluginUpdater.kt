/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.update

import com.demonwav.mcdev.util.forEachNotNull
import com.demonwav.mcdev.util.invokeLater
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerMain
import com.intellij.ide.plugins.PluginNode
import com.intellij.ide.plugins.RepositoryHelper
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.util.io.HttpRequests
import org.jdom.Element
import org.jdom.JDOMException
import java.io.IOException
import java.net.URLEncoder

object PluginUpdater {
    fun runUpdateCheck(callback: (PluginUpdateStatus) -> Boolean) {
        ApplicationManager.getApplication().executeOnPooledThread { updateCheck(callback) }
    }

    private fun updateCheck(callback: (PluginUpdateStatus) -> Boolean) {
        var updateStatus: PluginUpdateStatus
        try {
            updateStatus = checkUpdatesInMainRepo()

            RepositoryHelper.getPluginHosts()
                .stream()
                .forEachNotNull { updateStatus = updateStatus.mergeWith(checkUpdatesInCustomRepo(it)) }

            val finalUpdate = updateStatus
            invokeLater { callback(finalUpdate) }
        } catch (e: Exception) {
            PluginUpdateStatus.CheckFailed("Minecraft Development plugin update check failed")
        }

    }

    private fun checkUpdatesInMainRepo(): PluginUpdateStatus {
        val buildNumber = ApplicationInfo.getInstance().build.asString()
        val currentVersion = PluginUtil.pluginVersion
        val os = URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION, CharsetToolkit.UTF8)
        val url = "https://plugins.jetbrains.com/plugins/list?pluginId=8327&build=$buildNumber&pluginVersion=$currentVersion&os=$os"

        val responseDoc = HttpRequests.request(url).connect<Element> { request ->
            try {
                return@connect JDOMUtil.load(request.inputStream)
            } catch (e: JDOMException) {
                e.printStackTrace()
            }

            null
        } ?: return PluginUpdateStatus.CheckFailed("Unexpected plugin repository response")

        if (responseDoc.name != "plugin-repository") {
            return PluginUpdateStatus.CheckFailed("Unexpected plugin repository response")
        }
        if (responseDoc.children.isEmpty()) {
            return PluginUpdateStatus.LatestVersionInstalled()
        }

        val newVersion = responseDoc.getChild("category")?.getChild("idea-plugin")?.getChild("version")?.text ?:
            return PluginUpdateStatus.CheckFailed("Couldn't find plugin version in repository response")

        val plugin = PluginManager.getPlugin(PluginUtil.PLUGIN_ID)!!
        val pluginNode = PluginNode(PluginUtil.PLUGIN_ID)
        pluginNode.version = newVersion
        pluginNode.name = plugin.name
        pluginNode.description = plugin.description

        if (pluginNode.version == PluginUtil.pluginVersion) {
            return PluginUpdateStatus.LatestVersionInstalled()
        }

        return PluginUpdateStatus.Update(pluginNode, null)
    }

    private fun checkUpdatesInCustomRepo(host: String): PluginUpdateStatus {
        val plugins: List<IdeaPluginDescriptor>
        try {
            plugins = RepositoryHelper.loadPlugins(host, null)
        } catch (e: IOException) {
            return PluginUpdateStatus.CheckFailed("Checking custom plugin repository $host  failed")
        }

        val minecraftPlugin = plugins.stream()
            .filter { plugin -> plugin.pluginId == PluginUtil.PLUGIN_ID }
            .findFirst().orElse(null) ?: return PluginUpdateStatus.LatestVersionInstalled() // Effectively remove isEmpty call

        return updateIfNotLatest(minecraftPlugin, host)
    }

    private fun updateIfNotLatest(plugin: IdeaPluginDescriptor, host: String): PluginUpdateStatus {
        if (plugin.version == PluginUtil.pluginVersion) {
            return PluginUpdateStatus.LatestVersionInstalled()
        }
        return PluginUpdateStatus.Update(plugin, host)
    }

    fun installPluginUpdate(update: PluginUpdateStatus.Update) {
        val plugin = update.pluginDescriptor
        val downloader = PluginDownloader.createDownloader(plugin, update.hostToInstallFrom, null)
        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Downloading Plugin", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    if (downloader.prepareToInstall(indicator)) {
                        val descriptor = downloader.descriptor
                        if (descriptor != null) {
                            downloader.install()

                            ApplicationManager.getApplication().invokeLater { PluginManagerMain.notifyPluginsUpdated(null) }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        })
    }
}
