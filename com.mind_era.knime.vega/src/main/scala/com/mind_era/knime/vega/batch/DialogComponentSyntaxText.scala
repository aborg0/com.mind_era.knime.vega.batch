/**
 *
 */
package com.mind_era.knime.vega.batch

import org.knime.core.node.defaultnodesettings.DialogComponent
import org.knime.core.node.defaultnodesettings.SettingsModelString
import org.knime.core.node.port.PortObjectSpec
import org.fife.ui.rtextarea.RTextScrollPane
import javax.swing.border.TitledBorder

/**
 * Wraps an [RSyntaxTextArea].
 *
 * @author Gabor Bakos
 */
class DialogComponentSyntaxText(model: SettingsModelString, title: Option[String] = None) extends DialogComponent(model) {
  val textArea = new _root_.org.fife.ui.rsyntaxtextarea.RSyntaxTextArea(11, 80)
  private[this] val scroll = new RTextScrollPane(textArea)
  title.fold()(t => scroll.setBorder(new TitledBorder(t)))
  getComponentPanel.add(scroll)
  protected /*[package defaultnodesettings]*/ def checkConfigurabilityBeforeLoad(specs: Array[PortObjectSpec]): Unit = {}
  protected /*[package defaultnodesettings]*/ def setEnabledComponents(enabled: Boolean): Unit = textArea.setEnabled(enabled)
  def setToolTipText(tooltip: String): Unit = textArea.setToolTipText(tooltip)
  protected /*[package defaultnodesettings]*/ def updateComponent: Unit = textArea.setText(model.getStringValue)
  protected /*[package defaultnodesettings]*/ def validateSettingsBeforeSave: Unit = {}
}