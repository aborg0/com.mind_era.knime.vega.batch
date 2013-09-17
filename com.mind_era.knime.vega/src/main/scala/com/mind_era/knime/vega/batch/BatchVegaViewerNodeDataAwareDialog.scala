/**
 *
 */
package com.mind_era.knime.vega.batch

import org.knime.core.node.DataAwareNodeDialogPane
import org.knime.core.node.NodeSettingsRO
import org.knime.core.node.port.PortObject
import org.knime.core.node.NotConfigurableException
import javax.swing.AbstractAction
import org.knime.core.node.port.PortObjectSpec
import org.knime.core.data.`def`.StringCell
import com.mind_era.knime.util.DialogComponentPairs
import java.util.ArrayList
import org.fife.ui.autocomplete.AutoCompletion
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection
import scala.collection.JavaConverters._
import java.util.Arrays
import org.fife.ui.autocomplete.DefaultCompletionProvider
import org.knime.core.node.defaultnodesettings.SettingsModelString
import org.fife.ui.autocomplete.BasicCompletion
import java.awt.event.ActionEvent
import javax.swing.event.ChangeListener
import java.util.EnumSet
import com.mind_era.knime.util.DialogComponentPairs.Columns
import javax.swing.event.ChangeEvent
import javax.swing.JOptionPane
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import javax.swing.JButton
import java.awt.GridLayout
import javax.swing.JPanel
import java.awt.BorderLayout
import javax.swing.JSplitPane
import org.mortbay.jetty.Server
import org.mortbay.jetty.nio.SelectChannelConnector
import org.mortbay.jetty.handler.ResourceHandler
import org.mortbay.jetty.handler.HandlerList
import org.mortbay.jetty.Handler
import org.mortbay.jetty.handler.DefaultHandler
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.browser.Browser
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.SWT
import org.fife.ui.autocomplete.TemplateCompletion
import org.fife.ui.autocomplete.ShorthandCompletion
import org.knime.core.data.DataTableSpec
import org.knime.core.node.util.ViewUtils
import org.eclipse.ui.PlatformUI
import java.net.URL
import org.eclipse.core.runtime.Platform
import com.mind_era.knime.vega.batch.internal.BatchVegaViewerNodePlugin
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.FileLocator
import org.knime.core.util.FileUtil
import java.io.File
import org.apache.commons.io.FileUtils

/**
 * @author Gabor Bakos
 */
class BatchVegaViewerNodeDataAwareDialog extends DataAwareNodeDialogPane {
  private[this] var opening = true
  import BatchVegaViewerNodeModel._
  //TODO Check whether the MPS could be easily embedded here, maybe 3.0
  private[this] val templateModel = createTemplateSettings
  private[this] val templateSelector = new DialogComponentStringSelection(templateModel, "Template", Templates.template.map(_._1).asJavaCollection)
  private[this] val format = new DialogComponentStringSelection(createFormatSettings, "Image format", POSSIBLE_FORMATS: _*)
  val mappingPairs = new DialogComponentPairs(createMappingSettings, "Key", "Replace", EnumSet.of(Columns.Add, Columns.Remove, Columns.Enable)) {
    override def rightSuggestions(spec: Array[PortObjectSpec]) = {
      val ret = new ArrayList(Seq(ROWKEY, COLOR, HILITED, SIZE_FACTOR, SHAPE).map(new StringCell(_)).asJavaCollection)
      ret.addAll(columnsFromSpec(spec, 0))
      ret
    }
    override def leftSuggestions(spec: Array[PortObjectSpec]) = {
      val arr = (new StringCell("$inputTable$") +: templateParameters.map(new StringCell(_))).toArray
      Arrays.asList(arr: _*)
    }
    override def hasSuggestions(spec: Array[PortObjectSpec], left: Boolean) = {
      true
    }
    private[this] var templateParameters: Seq[String] = Seq()

    def updateSuggestions(parameters: Seq[String]) = {
      templateParameters = parameters
      //checkConfigurabilityBeforeLoad(getLastTableSpecs)
    }
  }
  val specText = new DialogComponentSyntaxText(
      createVegaSettings, Some("Vega specification") /*, Templates.template.map(p=>(p._1, p._2.text))*/ )
  private[this] val server = new Server //(9999)
  private[this] var tempDir: File = null

  {
    val panel = getPanel
    val gl = new GridLayout(1,3)
    gl.setHgap(15)
    val smallPanel = new JPanel(gl)

    panel.setLayout(new BorderLayout)
    panel.add(smallPanel, BorderLayout.NORTH)
    smallPanel.add(templateSelector.getComponentPanel)
    smallPanel.add(format.getComponentPanel)
    val previewButton = new JButton(new AbstractAction("Preview") {
      override def actionPerformed(e: ActionEvent): Unit = {
//	        val window = new Display
//        val shell = new Shell(window)
//        shell.setLayout(new FillLayout)
//        window.asyncExec(new Runnable() {
//          override def run(): Unit = {
//          }
//        })
        PlatformUI.getWorkbench.getBrowserSupport.createBrowser("VegaViewer").openURL(new URL("http://localhost:9999/html/show.html"))
//        val browser = new Browser(shell, SWT.MOZILLA)
//	        browser.setJavascriptEnabled(true)
//	        browser.setSize(800, 600)
//	        shell.pack
//	        shell.open
//	        //browser.setUrl("http://trifacta.github.com/vega/editor")
//	        browser.setUrl("http://localhost:9999/html/show.html")
//	        //browser.setUrl("http://localhost:9999/js/vega/examples/editor/index.html")
//	        while (!shell.isDisposed) {
//	        	if (!window.readAndDispatch) window.sleep
//	        }
//	        window.close
      }
    })
    smallPanel.add(previewButton)
    val split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
    panel.add(split, BorderLayout.CENTER)
    split.add(specText.getComponentPanel)
    //addDialogComponent(templateSelector)
    //addDialogComponent(new DialogComponentStringSelection(createFormatSettings, "Image format", POSSIBLE_FORMATS: _*))
    //closeCurrentGroup

    //TODO add preferencepage to collect templates.
    //addDialogComponent(component)
    specText.textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT)
    val ac = new AutoCompletion(createProvider(
      parameterNames(templateModel), new DataTableSpec))
    ac.install(specText.textArea)

    //mappingPairs.getComponentPanel.setPreferredSize(new Dimension(700, 200))
    //mappingPairs.setPreferredSize(500, 150)
    import language.reflectiveCalls
    templateSelector.getModel.addChangeListener(new ChangeListener() {
      def stateChanged(e: ChangeEvent): Unit = {
        val template = Templates.template.get(templateModel.getStringValue)
        template.fold()(t =>
          {
            val templateSelected = templateModel.getStringValue
            val selected = Templates.template.get(templateSelected)
            val newText: String = selected.map(_.text).getOrElse(specText.currentText)
            if (!opening && specText.currentText != newText) {
              if (JOptionPane.showConfirmDialog(getPanel, s"Update text with template: ${templateSelected}?", "Apply template", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                specText.textArea.setText(newText)
              }
            }

            mappingPairs.updateSuggestions(t.parameters.map(_.name))
            ac.setCompletionProvider(createProvider(parameterNames(templateModel), new DataTableSpec()))
          })
        opening = false
      }
    })
    split.add(mappingPairs.getComponentPanel)
  }
  //  component.addTemlateChangeListener(new AbstractAction() {
  //    override def actionPerformed(e: ActionEvent): Unit = {
  //      val template = Templates.template.get(e.getSource().asInstanceOf[JComboBox[_]].getSelectedItem().asInstanceOf[String])
  //      template.fold()(t=> mappingPairs.updateSuggestions(t.parameters.map(_.name)))
  //    }
  //  })
  //  addDialogComponent(mappingPairs)

  def saveSettingsTo(settings: org.knime.core.node.NodeSettingsWO): Unit = {
    templateSelector.saveSettingsTo(settings)
    format.saveSettingsTo(settings)
    mappingPairs.saveSettingsTo(settings)
    specText.saveSettingsTo(settings)
  }

  @throws[NotConfigurableException]
  protected override def loadSettingsFrom(settings: NodeSettingsRO, 
    input: Array[PortObject]): Unit = {
    val specs = input.map(po => if (po == null) null else po.getSpec)
    templateSelector.loadSettingsFrom(settings, specs)
    format.loadSettingsFrom(settings, specs)
    mappingPairs.loadSettingsFrom(settings, specs)
    specText.loadSettingsFrom(settings, specs)
  }

  override def onOpen: Unit = {
    // TODO refactor to a new project to ease porting to new eclipse versions.
    val connector = new SelectChannelConnector()
    connector.setPort(9999)
    server.addConnector(connector)
  
    val resourceHandler = new ResourceHandler()
    //resourceHandler.setDirectoriesListed(true)
    //resourceHandler.setWelcomeFiles(Array[String]("index.html"))
  
    val bundle = BatchVegaViewerNodePlugin.getDefault.getBundle
    
    val commonUrl = bundle.getDataFile("src/main/js")
    tempDir = FileUtil.createTempDir("vegaData")
    val content = s"""<!DOCTYPE HTML>
<html>
  <head>
    <title>Vega Preview</title>
    <script src="${new File(commonUrl, "d3/d3.min.js").toString}"></script>
    <script src="${new File(commonUrl, "vega/vega.min.js").toString}"></script>
  </head>
  <body>
    <div id="vis"></div>
  </body>
<script type="text/javascript">
// parse a spec and create a visualization view
function parse(spec) {
  vg.parse.spec(spec, function(chart) { chart({el:"#vis"}).update(); });
}
//parse("file://C:/Users/Gábor/tmp/eclipse_knime_2.8.0_2013/workspace/com.mind_era.knime.vega/src/main/html/spec.json");
//parse("spec.json");
parse("http://trifacta.github.io/vega/data/jobs.json");
</script>
</html>"""
    FileUtils.writeStringToFile(new File(tempDir, "show.html"), content)
    resourceHandler.setResourceBase(tempDir.toString)
  
    val handlers = new HandlerList()
    val rh: Handler = resourceHandler
    handlers.setHandlers(Array[Handler](rh, new DefaultHandler(): Handler));
    server.setHandler(handlers)
  
    server.start
    //server.join()
  }

  override def onClose: Unit = {
    val connector = server.getConnectors()(0)
    connector.stop
    connector.close
    server.removeConnector(connector)
    server.stop
    assert(tempDir != null)
    FileUtil.deleteRecursively(tempDir)
  }
  override def closeOnESC = false

  def createProvider(colNames: Seq[String], spec: DataTableSpec) = {
    val ret = new DefaultCompletionProvider
    for (mark <- Seq("rect", "symbol", "path", "arc", "area", "line", "image", "text", "group")) {
      ret.addCompletion(new BasicCompletion(ret, '"' + mark + '"'))
    }
    for (col <- colNames) {
      ret.addCompletion(new BasicCompletion(ret, s""""data.$col""""))
    }
    for (mark <- Seq(ROWKEY, COLOR, SHAPE, SIZE_FACTOR, HILITED))
      ret.addCompletion(new BasicCompletion(ret, mark))
    val replacement = s""""values":{
        {
          ${(for (_<- 1 to 5) yield "{" + (for (colName<- colNames) yield '"' + colName + "\": ").mkString(", ")+ " }").mkString(",\n          ")}
        }
      }
"""
    ret.addCompletion(new ShorthandCompletion(ret, "sampleDataDefinition", replacement, replacement))
    ret
  }

  def parameterNames(templateModel: SettingsModelString): Seq[String] = {
    for (
      template <- Templates.template.get(templateModel.getStringValue).toList;
      parameter <- template.parameters
    ) yield parameter.name
  }

}