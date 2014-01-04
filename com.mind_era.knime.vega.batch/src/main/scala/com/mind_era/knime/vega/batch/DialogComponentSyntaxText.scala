/**
 *
 */
package com.mind_era.knime.vega.batch

import org.knime.core.node.defaultnodesettings.DialogComponent
import org.knime.core.node.defaultnodesettings.SettingsModelString
import org.knime.core.node.port.PortObjectSpec
import org.fife.ui.rtextarea.RTextScrollPane
import javax.swing.border.TitledBorder
import org.fife.rsyntaxarea.internal.RSyntaxAreaActivator
import javax.swing.JPanel
import java.awt.BorderLayout
import javax.swing.JComboBox
import javax.swing.AbstractAction
import java.awt.event.ActionEvent
import javax.swing.JOptionPane
import java.awt.event.ActionListener

/**
 * Wraps an [RSyntaxTextArea].
 *
 * @author Gabor Bakos
 */
class DialogComponentSyntaxText(model: SettingsModelString, title: Option[String] = None, templates: collection.Map[String, String] = Map()) extends DialogComponent(model) {
  RSyntaxAreaActivator.ensureWorkaroundBug3692Applied
  val textArea = new _root_.org.fife.ui.rsyntaxtextarea.RSyntaxTextArea(11, 80)
  private[this] val scroll = new RTextScrollPane(textArea)
  title.fold()(t => scroll.setBorder(new TitledBorder(t)))
  private[this] val combo = new JComboBox[String]()
  if (templates.isEmpty) {
      getComponentPanel.setLayout(new BorderLayout)
	  getComponentPanel.add(scroll, BorderLayout.CENTER)
  } else {
    val panel = new JPanel(new BorderLayout)
    for (entry <- templates) {
      combo.addItem(entry._1)
    }
    combo.setEditable(false)
    combo.addActionListener(new AbstractAction() {
      override def actionPerformed(e: ActionEvent) = {
        val newText = templates.getOrElse(combo.getSelectedItem.asInstanceOf[String], textArea.getText)
        if (textArea.getText != newText) {
        	if (JOptionPane.showConfirmDialog(getComponentPanel, s"Update text with template: ${combo.getSelectedItem}?", "Apply template", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        		textArea.setText(newText)
        	}
        }
      }
    })
    panel.add(combo, BorderLayout.NORTH)
    panel.add(scroll, BorderLayout.CENTER)
    getComponentPanel.add(panel)
  }
  protected /*[package defaultnodesettings]*/ def checkConfigurabilityBeforeLoad(specs: Array[PortObjectSpec]): Unit = {}
  protected /*[package defaultnodesettings]*/ def setEnabledComponents(enabled: Boolean): Unit = textArea.setEnabled(enabled)
  def setToolTipText(tooltip: String): Unit = textArea.setToolTipText(tooltip)
  protected /*[package defaultnodesettings]*/ def updateComponent: Unit = textArea.setText(model.getStringValue)
  protected /*[package defaultnodesettings]*/ def validateSettingsBeforeSave: Unit = {model.setStringValue(textArea.getText)}
  def currentText = textArea.getText
  
  def modelString:SettingsModelString = model
  
  def addTemlateChangeListener(listener: ActionListener) {
    combo.addActionListener(listener)
  }
}