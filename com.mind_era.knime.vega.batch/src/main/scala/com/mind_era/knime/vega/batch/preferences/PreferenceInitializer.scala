package com.mind_era.knime.vega.batch.preferences

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import com.mind_era.knime.vega.batch.internal.BatchVegaViewerNodePlugin
import org.eclipse.core.runtime.Platform

/**
 * @author Gabor Bakos
 */
class PreferenceInitializer extends AbstractPreferenceInitializer {
  override def initializeDefaultPreferences = {
    val store = BatchVegaViewerNodePlugin.getDefault.getPreferenceStore
    store.setDefault(PreferenceConstants.nodeJSLocation, Platform.getOS match {
      case Platform.OS_WIN32  => "C:\\Program Files (x86)\\nodejs\\node.exe"
      case Platform.OS_LINUX  => if (Platform.getOSArch == Platform.ARCH_X86_64) "" else ""
      case Platform.OS_MACOSX => ""
      case _                  => ""
    })
    store.setDefault(PreferenceConstants.vegaLocation, Platform.getOS match {
      case Platform.OS_WIN32  => "C:\\Program Files (x86)\\nodejs\\node_modules\\vega\\bin"
      case Platform.OS_LINUX  => ""
      case Platform.OS_MACOSX => ""
      case _                  => ""
    })
  }
}