/*
 *  Copyright (c) 2017 Dmitry Zhuravlev, Sergei Stepanov
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.hotswap.agent.plugin.startup

import com.hotswap.agent.plugin.services.DownloadManager
import com.hotswap.agent.plugin.services.DownloadManagerException
import com.hotswap.agent.plugin.services.HotSwapAgentPluginNotification
import com.hotswap.agent.plugin.settings.HotSwapAgentPluginSettingsProvider
import com.hotswap.agent.plugin.util.HotSwapAgentPathUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import java.io.File

/**
 * @author Dmitry Zhuravlev
 *         Date:  10.03.2017
 */
class HotSwapAgentPluginStartupActivity : StartupActivity {
    companion object {
        internal val log = Logger.getInstance(HotSwapAgentPluginStartupActivity::class.java)
    }

    override fun runActivity(project: Project) {
        val stateProvider = HotSwapAgentPluginSettingsProvider.getInstance(project)
        checkForNewAgentVersion(project, stateProvider)
        downloadLatestAgentSilentlyIfNeeded(project, stateProvider)
    }

    private fun downloadLatestAgentSilentlyIfNeeded(project: Project, stateProvider: HotSwapAgentPluginSettingsProvider) = with(DownloadManager.getInstance(project)) {
        if (!File(stateProvider.currentState.agentPath).exists()) {
            val latestVersion = getLatestAgentVersionOrDefault()
            val defaultAgentJarPath = HotSwapAgentPathUtil.getAgentJarPath(latestVersion)
            if (File(defaultAgentJarPath).exists()) {
                stateProvider.currentState.agentPath = defaultAgentJarPath
                return
            }
            try {
                downloadAgentJarAsynchronously(project, latestVersion) { downloadedAgentPath ->
                    stateProvider.currentState.agentPath = downloadedAgentPath
                }
            } catch(e: DownloadManagerException) {
                log.error("Cannot download agent jar: ", e)
            }
        }
    }

    private fun checkForNewAgentVersion(project: Project, stateProvider: HotSwapAgentPluginSettingsProvider) = with(DownloadManager.getInstance(project)) {
        if (File(stateProvider.currentState.agentPath).exists()) {
            val currentVersion = HotSwapAgentPathUtil.determineAgentVersionFromPath(stateProvider.currentState.agentPath) ?: return
            val latestVersion = getLatestAgentVersionOrDefault()
            if (latestVersion > currentVersion) {
                HotSwapAgentPluginNotification.getInstance(project).showNotificationAboutNewAgentVersion {
                    downloadAgentJarAsynchronously(project, latestVersion) { downloadedAgentPath ->
                        stateProvider.currentState.agentPath = downloadedAgentPath
                    }
                }
            }
        }
    }

}