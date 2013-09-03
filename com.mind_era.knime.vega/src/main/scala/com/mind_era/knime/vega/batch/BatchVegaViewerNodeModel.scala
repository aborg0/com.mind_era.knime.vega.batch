package com.mind_era.knime.vega.batch;

import java.io.File
import java.io.IOException
import java.io.File
import java.io.IOException
import org.knime.core.data.DataCell
import org.knime.core.data.DataColumnSpec
import org.knime.core.data.DataColumnSpecCreator
import org.knime.core.data.DataRow
import org.knime.core.data.DataTableSpec
import org.knime.core.data.RowKey
import org.knime.core.data.`def`.DefaultRow
import org.knime.core.data.`def`.DoubleCell
import org.knime.core.data.`def`.IntCell
import org.knime.core.data.`def`.StringCell
import org.knime.core.node.BufferedDataTable
import org.knime.core.node.CanceledExecutionException
import org.knime.core.node.ExecutionContext
import org.knime.core.node.ExecutionMonitor
import org.knime.core.node.InvalidSettingsException
import org.knime.core.node.NodeLogger
import org.knime.core.node.NodeModel
import org.knime.core.node.NodeSettingsRO
import org.knime.core.node.NodeSettingsWO
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded
import _root_.scala.collection.JavaConverters._
import org.codehaus.jackson.JsonFactory
import org.apache.commons.io.FileUtils
import org.codehaus.jackson.JsonEncoding
import org.knime.core.data.DoubleValue
import org.codehaus.jackson.JsonGenerator
import org.knime.core.data.DataType
import org.knime.core.data.StringValue
import org.knime.core.node.defaultnodesettings.SettingsModelString
import com.mind_era.knime.vega.batch.internal.BatchVegaViewerNodePlugin
import com.mind_era.knime.vega.batch.preferences.PreferenceConstants
import org.knime.core.util.FileUtil
import scala.io.Source
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import org.knime.core.node.workflow.NodeContext
import org.knime.core.node.Node
import org.knime.core.node.port.PortType
import org.knime.core.node.port.image.ImagePortObject
import org.knime.core.node.port.PortObject
import org.knime.core.node.port.image.ImagePortObjectSpec
import org.knime.core.data.image.ImageContent
import org.knime.core.data.image.png.PNGImageContent
import org.knime.core.node.KNIMEConstants
import org.knime.base.data.xml.SvgImageContent
import java.io.BufferedInputStream
import java.io.FileInputStream
import org.knime.base.data.xml.SvgCell
import scala.util.control.NonFatal

/**
 * Companion object for BatchVegaViewerNodeModel.
 */
object BatchVegaViewerNodeModel {

  // the logger instance
  private[BatchVegaViewerNodeModel] final val logger = NodeLogger.getLogger(classOf[BatchVegaViewerNodeModel])

  /**
   * the settings key which is used to retrieve and
   * store the settings (from the dialog or from a settings file)
   * (package visibility to be usable from the dialog).
   */
  private[batch] final val CFGKEY_COUNT = "Count"

  /** initial default count value. */
  private[batch] final val DEFAULT_COUNT = 100

  private[batch] final val CFGKEY_VEGA_SPEC = "Vega Specification"

  private[batch] final val DEFAULT_VEGA_SPEC = """
{
  "width": 400,
  "height": 200,
  "padding": {"top": 10, "left": 30, "bottom": 20, "right": 10},
  "data": [
    {
      "name": "table",
      "url": "data.json"
    }
  ],
  "scales": [
    {"name":"x", "type":"ordinal", "range":"width", "domain":{"data":"table", "field":"data.text"}},
    {"name":"y", "range":"height", "nice":true, "domain":{"data":"table", "field":"data.alma"}}
  ],
  "axes": [
    {"type":"x", "scale":"x"},
    {"type":"y", "scale":"y"}
  ],
  "marks": [
    {
      "type": "rect",
      "from": {"data":"table"},
      "properties": {
        "enter": {
          "x": {"scale":"x", "field":"data.text"},
          "width": {"scale":"x", "band":true, "offset":-1},
          "y": {"scale":"y", "field":"data.alma"},
          "y2": {"scale":"y", "value":0}
        },
        "update": { "fill": {"value":"steelblue"} },
        "hover": { "fill": {"value":"red"} }
      }
    }
  ]
}""".trim

  private[batch] final val CFGKEY_MAPPING = "column mapping"

  private[batch] final val DEFAULT_MAPPING = ""

  private[batch] final val CFGKEY_FORMAT = "image format"
  private[batch] final val SVG = "SVG"
  private[batch] final val PNG = "png"
  private[batch] final val svgSupported = BatchVegaViewerNodePlugin.getDefault.getBundle.getBundleContext.getBundles.exists(_.getSymbolicName == "org.knime.ext.svg")
  private[batch] final val POSSIBLE_FORMATS = if (svgSupported) Array(SVG, PNG) else Array(PNG)

  private[batch] final val DEFAULT_FORMAT = POSSIBLE_FORMATS(0)

  private[batch] final val VEGA_ERROR_PREFIX = "[Vega Err] "
}

/**
 * This is the model implementation of BatchVegaViewer.
 * Converts data to vega (https://github.com/trifacta/vega) images using custom figure descriptor.
 *
 * @author Mind Eratosthenes Kft.
 */
class BatchVegaViewerNodeModel extends NodeModel(Array[PortType](BufferedDataTable.TYPE_OPTIONAL), Array[PortType](ImagePortObject.TYPE)) {
  import BatchVegaViewerNodeModel._

  private[this] final val vegaSpecification = new SettingsModelString(CFGKEY_VEGA_SPEC, DEFAULT_VEGA_SPEC)
  private[this] final val mapping = new SettingsModelString(CFGKEY_MAPPING, DEFAULT_MAPPING)
  private[this] final val imageFormat = new SettingsModelString(CFGKEY_FORMAT, DEFAULT_FORMAT)

  /**
   * @inheritdoc
   */
  @throws[Exception]
  protected override def execute(inData: Array[PortObject],
                                 exec: ExecutionContext): Array[PortObject] = {
    val tempFile = inData match {
      case Array(data: BufferedDataTable) => generateJSONTable(data)
      case _                              => None
    }
    val resultFile = try {
      val specFile = FileUtil.createTempFile("spec", ".json", true)
      try {
        writeSpec(tempFile, specFile)
        //        val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(specFile), "UTF-8"))
        //        try {
        //          val rawSpecText = vegaSpecification.getStringValue
        //          val specText = tempFile.fold(rawSpecText)(file => rawSpecText.replaceAll("data\\.json", file.toURI.toURL.toString.replaceFirst("file:/", "file://")))
        //          writer.write(specText)
        //        } finally {
        //          writer.close
        //        }
        //        import sys.process._
        //        val store = BatchVegaViewerNodePlugin.getDefault.getPreferenceStore
        //        val outputFile = FileUtil.createTempFile("vega_output", ".img", true)
        //        val command = '"' + store.getString(PreferenceConstants.nodeJSLocation) + "\" \"" +
        //          store.getString(PreferenceConstants.vegaLocation) + File.separatorChar + (imageFormat.getStringValue match {
        //            case SVG => "vg2svg\" -h"
        //            case PNG => "vg2png\""
        //          }) /*+"-b \"" + tempFile.getParentFile.getAbsolutePath.replace('\\', '/') + "\" "*/ + "\"" + specFile.getAbsolutePath + "\" \"" + outputFile.getAbsolutePath + '"'
        //        logger.debug("Command to execute: " + command)
        //        val process = Process.apply(command)
        //        process.!!(ProcessLogger.apply(
        //          out => {
        //            logger.warn(out)
        //          },
        //          (err: String) => {
        //            logger.error(err)
        //          }))
        executeProcess(specFile)
      } finally {
        specFile.delete
      }
    } finally {
      tempFile.fold(())(_.delete)
    }
    val output = new BufferedInputStream(new FileInputStream(resultFile))
    val (content, dataType) = try {
      imageFormat.getStringValue match {
        case SVG => (new SvgImageContent(output), SvgCell.TYPE)
        case PNG => (new PNGImageContent(output), PNGImageContent.TYPE)
        case _   => throw new UnsupportedOperationException("Unknown image type: " + imageFormat.getStringValue)
      }
    } finally {
      output.close
      resultFile.delete
    }
    Array[PortObject](new ImagePortObject(content, new ImagePortObjectSpec(dataType)))
  }

  @throws[IOException]
  private[this] def generateJSONTable(data: BufferedDataTable) = {
    val jsonFactory = new JsonFactory;
    val tempFile = FileUtil.createTempFile("data", ".json", true)
    val spec = data.getSpec
    val gen = jsonFactory.createJsonGenerator(tempFile, JsonEncoding.UTF8)
    try {
      val processingFunctions = createProcessingFunctions(spec, gen)
      gen.writeStartArray
      for (row <- data.asScala) {
        gen.writeStartObject
        for (colIndex <- 0 until spec.getNumColumns) {
          processingFunctions(colIndex)(row.getCell(colIndex))
        }
        gen.writeEndObject
      }
      gen.writeEndArray
      Some(tempFile)
    } finally {
      gen.close
    }
  }

  private[this] def createProcessingFunctions(spec: DataTableSpec, gen: JsonGenerator): Int => DataCell => Unit = {
    def processingFunction(idx: Int, name: String, dataType: DataType): (Int, DataCell => Unit) = {
      val partialFunction: PartialFunction[DataCell, Unit] = new PartialFunction[DataCell, Unit] {
        override def isDefinedAt(cell: DataCell) = cell.isMissing
        override def apply(cell: DataCell) = gen.writeFieldName(name)
      }
      (idx, if (dataType.isCompatible(classOf[DoubleValue])) {
        partialFunction.orElse(PartialFunction((cell: DataCell) => gen.writeNumberField(name, cell.asInstanceOf[DoubleValue].getDoubleValue)))
      } else if (dataType.isCompatible(classOf[StringValue])) {
        partialFunction.orElse(PartialFunction((cell: DataCell) => gen.writeStringField(name, cell.asInstanceOf[StringValue].getStringValue)))
      } else {
        (cell: DataCell) => ()
      })
    }
    (for ((col, idx) <- spec.asScala.zipWithIndex) yield processingFunction(idx, col.getName, col.getType)).toMap
  }

  /**
   * @inheritdoc
   */
  protected override def reset {
    //FileUtils.cleanDirectory(localFolder)
    // TODO Code executed on reset.
    // Models build during execute are cleared here.
    // Also data handled in load/saveInternals will be erased here.
  }

  /**
   * @inheritdoc
   */
  @throws[InvalidSettingsException]
  protected override def configure(inSpecs: Array[DataTableSpec]): Array[DataTableSpec] = {

    // TODO: check if user settings are available, fit to the incoming
    // table structure, and the incoming types are feasible for the node
    // to execute. If the node can execute in its current state return
    // the spec of its output data table(s) (if you can, otherwise an array
    // with null elements), or throw an exception with a useful user message

    Array[DataTableSpec] { null: DataTableSpec }
  }

  /**
   * @inheritdoc
   */
  protected override def saveSettingsTo(settings: NodeSettingsWO) {
    vegaSpecification.saveSettingsTo(settings)
    mapping.saveSettingsTo(settings)
    imageFormat.saveSettingsTo(settings)

  }

  /**
   * @inheritdoc
   */
  @throws[InvalidSettingsException]
  protected override def loadValidatedSettingsFrom(settings: NodeSettingsRO) {
    vegaSpecification.loadSettingsFrom(settings)
    mapping.loadSettingsFrom(settings)
    imageFormat.loadSettingsFrom(settings)
  }

  /**
   * @inheritdoc
   */
  @throws[InvalidSettingsException]
  protected override def validateSettings(settings: NodeSettingsRO) {
    new SettingsModelString(CFGKEY_VEGA_SPEC, DEFAULT_VEGA_SPEC).validateSettings(settings)
    new SettingsModelString(CFGKEY_MAPPING, DEFAULT_MAPPING).validateSettings(settings)
    new SettingsModelString(CFGKEY_FORMAT, DEFAULT_FORMAT).validateSettings(settings)
  }

  /**
   * @inheritdoc
   */
  @throws[IOException]
  @throws[CanceledExecutionException]
  protected override def loadInternals(internDir: File,
                                       exec: ExecutionMonitor) {
    // TODO load internal data. 
    // Everything handed to output ports is loaded automatically (data
    // returned by the execute method, models loaded in loadModelContent,
    // and user settings set through loadSettingsFrom - is all taken care 
    // of). Load here only the other internals that need to be restored
    // (e.g. data used by the views).

  }

  /**
   * @inheritdoc
   */
  @throws[IOException]
  @throws[CanceledExecutionException]
  protected override def saveInternals(internDir: File,
                                       exec: ExecutionMonitor) {
    logger.debug(internDir.getAbsolutePath)
    // TODO save internal models. 
    // Everything written to output ports is saved automatically (data
    // returned by the execute method, models saved in the saveModelContent,
    // and user settings saved through saveSettingsTo - is all taken care 
    // of). Save here only the other internals that need to be preserved
    // (e.g. data used by the views).

  }

  @throws[IOException]
  private[this] def writeSpec(tempFile: Option[File], specFile: File): Unit = {
    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(specFile), "UTF-8"))
    try {
      val rawSpecText = vegaSpecification.getStringValue
      val specText = tempFile.fold(rawSpecText)(file => rawSpecText.replaceAll("data\\.json", file.toURI.toURL.toString.replaceFirst("file:/", "file://")))
      writer.write(specText)
    } finally {
      writer.close
    }
  }

  @throws[Exception]
  private[this] def executeProcess(specFile: File): File = {
    import sys.process._
    val store = BatchVegaViewerNodePlugin.getDefault.getPreferenceStore
    val outputFile = FileUtil.createTempFile("vega_output", ".img", true)
    val command = '"' + store.getString(PreferenceConstants.nodeJSLocation) + "\" \"" +
      store.getString(PreferenceConstants.vegaLocation) + File.separatorChar + (imageFormat.getStringValue match {
        case SVG => "vg2svg\" -h"
        case PNG => "vg2png\""
      }) /*+"-b \"" + tempFile.getParentFile.getAbsolutePath.replace('\\', '/') + "\" "*/ + "\"" + specFile.getAbsolutePath + "\" \"" + outputFile.getAbsolutePath + '"'
    logger.debug("Command to execute: " + command)
    val process = Process(command)
    val sb = new StringBuilder
    try {
      val output = process.!!(ProcessLogger(
        out => {
          logger.warn(out)
        },
        (err: String) => {
          logger.error(err)
          sb.append(err).append('\n')
        }))
    } catch {
      case NonFatal(e) => {
        logger.info(sb.toString)
        val errorMessage = sb.split('\n').filter(_.startsWith(VEGA_ERROR_PREFIX)).map(_.substring(VEGA_ERROR_PREFIX.length)).mkString("\n")
        throw new Exception(errorMessage, e)
      }
    }
    outputFile
  }
}

