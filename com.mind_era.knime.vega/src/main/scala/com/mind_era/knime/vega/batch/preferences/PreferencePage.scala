package com.mind_era.knime.vega.batch.preferences

import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.jface.preference.FieldEditorPreferencePage
import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.graphics.Point
import com.mind_era.knime.vega.batch.internal.BatchVegaViewerNodePlugin
import org.eclipse.ui.IWorkbench
import org.eclipse.jface.preference.FileFieldEditor
import org.eclipse.jface.preference.DirectoryFieldEditor

/**
 * @author Gabor Bakos
 *
 */
class PreferencePage(title: String, image: ImageDescriptor, style: Int) extends FieldEditorPreferencePage(title, image, style) with IWorkbenchPreferencePage {
  setPreferenceStore(BatchVegaViewerNodePlugin.getDefault().getPreferenceStore)
  setDescription("Node.js related preferences for Vega.")

  def this() = this("Vega", null, FieldEditorPreferencePage.FLAT)
  override def createFieldEditors = {
    addField(new FileFieldEditor(PreferenceConstants.nodeJSLocation, "Node.js installation location, the executable", getFieldEditorParent))
    addField(new DirectoryFieldEditor(PreferenceConstants.vegaLocation, "vega installation location's bin dir", getFieldEditorParent))
  }

  override def init(workbench: IWorkbench) = {
    // Do nothing special yet.
  }
}