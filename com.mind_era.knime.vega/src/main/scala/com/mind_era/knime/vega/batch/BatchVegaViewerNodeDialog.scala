package com.mind_era.knime.vega.batch

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane
import org.knime.core.node.defaultnodesettings.DialogComponentNumber
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded
import org.knime.core.node.defaultnodesettings.DialogComponentButton
import javax.swing.Action
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import org.eclipse.swt.browser.Browser
import org.eclipse.swt.browser.BrowserFactory
import org.eclipse.swt.browser.BrowserFactory
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.mortbay.jetty.Server
import org.mortbay.jetty.nio.SelectChannelConnector
import org.mortbay.jetty.handler.HandlerList
import org.mortbay.jetty.handler.ResourceHandler
import org.mortbay.jetty.Handler
import org.mortbay.jetty.handler.DefaultHandler
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString
import org.knime.core.node.defaultnodesettings.SettingsModelString
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection
import com.mind_era.knime.util.DialogComponentPairs
import com.mind_era.knime.util.SettingsModelPairs
import org.knime.core.data.`def`.StringCell
import java.util.EnumSet
import com.mind_era.knime.util.DialogComponentPairs.Columns
import java.awt.Dimension

/**
 * <code>NodeDialog</code> for the "BatchVegaViewer" Node.
 * Converts data to vega (https://github.com/trifacta/vega) images using custom figure descriptor.
 *
 * This node dialog derives from [[DefaultNodeSettingsPane]] which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * [[org.knime.core.node.NodeDialogPane]].
 *
 * @constructor New pane for configuring BatchVegaViewer node.
 * This is just a suggestion to demonstrate possible default dialog
 * components.
 *
 * @author Gabor Bakos
 */
class BatchVegaViewerNodeDialog protected[batch] () extends DefaultNodeSettingsPane {

  import BatchVegaViewerNodeModel._
  //TODO replace with RSyntaxTextarea
  addDialogComponent(new DialogComponentMultiLineString(
    createVegaSettings, "Vega specification"))
  
  val mappingPairs = new DialogComponentPairs(
      createMappingSettings, "Key", "Replace", EnumSet.of(Columns.Add, Columns.Remove, Columns.Enable))
  //mappingPairs.getComponentPanel.setPreferredSize(new Dimension(700, 200))
  mappingPairs.setPreferredSize(500, 150)
  addDialogComponent(mappingPairs)
  addDialogComponent(new DialogComponentStringSelection(createFormatSettings, "Image format", POSSIBLE_FORMATS: _*))

//  val server = new Server //(9999)
//  val connector = new SelectChannelConnector()
//  connector.setPort(9999)
//  server.addConnector(connector)
//
//  val resourceHandler = new ResourceHandler()
//  //resourceHandler.setDirectoriesListed(true)
//  //resourceHandler.setWelcomeFiles(Array[String]("index.html"))
//
//  resourceHandler.setResourceBase(".")
//
//  val handlers = new HandlerList()
//  val rh: Handler = resourceHandler
//  handlers.setHandlers(Array[Handler](rh, new DefaultHandler(): Handler));
//  server.setHandler(handlers)
//
//  server.start()
//  //server.join()
//  val button = new DialogComponentButton("Preview")
//  button.addActionListener(new AbstractAction() {
//    override def actionPerformed(e: ActionEvent) {
//      val window = new Display
//      val shell = new Shell(window)
//      shell.setLayout(new FillLayout)
//      val browser = new Browser(shell, SWT.NONE)
//      browser.setSize(800, 600)
//      shell.pack
//      shell.open
//      //browser.setUrl("http://trifacta.github.com/vega/editor")
//      browser.setUrl("http://localhost:9999/show.html")
//      while (!shell.isDisposed) {
//        if (!window.readAndDispatch) window.sleep
//      }
//      window.close
//    }
//  })
//  addDialogComponent(button)
}
