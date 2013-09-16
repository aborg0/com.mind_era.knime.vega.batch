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
import java.util.regex.Pattern
import com.mind_era.knime.util.SettingsModelPairs
import java.util.Collections
import org.knime.core.node.port.PortObjectSpec
import scala.compat.Platform

/**
 * Companion object for BatchVegaViewerNodeModel.
 */
object BatchVegaViewerNodeModel {

  // the logger instance
  private[BatchVegaViewerNodeModel] final val logger = NodeLogger.getLogger(classOf[BatchVegaViewerNodeModel])

  private[batch] final val CFGKEY_VEGA_SPEC = "Vega Specification"

  private[batch] final val DEFAULT_VEGA_SPEC = """
{
  "width": 400,
  "height": 200,
  "padding": {"top": 10, "left": 30, "bottom": 20, "right": 10},
  "data": [
    {
      "name": "table",
      "url": "$inputTable$"
    }
  ],
  "scales": [
    {"name":"x", "type":"ordinal", "range":"width", "domain":{"data":"table", "field":"data.$nominal$"}},
    {"name":"y", "range":"height", "nice":true, "domain":{"data":"table", "field":"data.$numeric$"}}
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
          "x": {"scale":"x", "field":"data.$nominal$"},
          "width": {"scale":"x", "band":true, "offset":-1},
          "y": {"scale":"y", "field":"data.$numeric$"},
          "y2": {"scale":"y", "value":0}
        },
        "update": { "fill": {"value":"steelblue"} },
        "hover": { "fill": {"value":"red"} }
      }
    }
  ]
}""".trim

  private[batch] final val CFGKEY_MAPPING = "column mapping"

  private[batch] final val DEFAULT_MAPPING: java.util.List[org.knime.core.util.Pair[StringCell, StringCell]] = new java.util.ArrayList

  private[batch] final val CFGKEY_FORMAT = "image format"
  private[batch] final val SVG = "SVG"
  private[batch] final val PNG = "PNG"
  private[batch] final val svgSupported = BatchVegaViewerNodePlugin.getDefault.getBundle.getBundleContext.getBundles.exists(_.getSymbolicName == "org.knime.ext.svg")
  private[batch] final val POSSIBLE_FORMATS = if (svgSupported) Array(SVG, PNG) else Array(PNG)

  private[batch] final val DEFAULT_FORMAT = POSSIBLE_FORMATS(0)

  private[batch] final val VEGA_ERROR_PREFIX = "[Vega Err] "

  //Helper methods to create the [SettingsModel]s

  protected[batch] def createVegaSettings: SettingsModelString =
    new SettingsModelString(CFGKEY_VEGA_SPEC, DEFAULT_VEGA_SPEC)

  protected[batch] def createMappingSettings: SettingsModelPairs[StringCell, StringCell] =
    new SettingsModelPairs(CFGKEY_MAPPING, StringCell.TYPE, StringCell.TYPE, DEFAULT_MAPPING, true, false)

  protected[batch] def createFormatSettings: SettingsModelString =
    new SettingsModelString(CFGKEY_FORMAT, DEFAULT_FORMAT)

  protected[batch] final val ROWKEY = "KNIMERowKey"
  protected[batch] final val COLOR = "KNIMEColor"
  protected[batch] final val SHAPE = "KNIMEShape"
  protected[batch] final val SIZE_FACTOR = "KNIMESizeFactor"
  protected[batch] final val HILITED = "KNIMEHiLited"
}

/**
 * This is the model implementation of BatchVegaViewer.
 * Converts data to vega (https://github.com/trifacta/vega) images using custom figure descriptor.
 *
 * @author Mind Eratosthenes Kft.
 */
class BatchVegaViewerNodeModel extends NodeModel(Array[PortType](BufferedDataTable.TYPE_OPTIONAL), Array[PortType](ImagePortObject.TYPE)) {
  import BatchVegaViewerNodeModel._

  private[this] final val vegaSpecification = createVegaSettings
  private[this] final val mapping = createMappingSettings
  private[this] final val imageFormat = createFormatSettings

  /**
   * @inheritdoc
   */
  @throws[Exception]
  protected override def execute(inData: Array[PortObject],
    exec: ExecutionContext): Array[PortObject] = {
    val tempFile = inData match {
      case Array(data: BufferedDataTable) => generateJSONTable(data)
      case _ => None
    }
    val resultFile = try {
      val specFile = FileUtil.createTempFile("spec", ".json", true)
      try {
        writeSpec(tempFile, specFile)
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
        case SVG => ({ debugContent(resultFile); new SvgImageContent(output) }, SvgCell.TYPE)
        case PNG => (new PNGImageContent(output), PNGImageContent.TYPE)
        case _ => throw new UnsupportedOperationException("Unknown image type: " + imageFormat.getStringValue)
      }
    } catch {
      case e: RuntimeException => {
        debugContent(resultFile)
        throw e
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
        gen.writeStringField(ROWKEY, row.getKey.getString)
        val hiLited = getInHiLiteHandler(0).getHiLitKeys().contains(row.getKey)
        gen.writeStringField(COLOR, "#" + Integer.toHexString(data.getSpec.getRowColor(row).getColor(false, hiLited).getRGB))
        gen.writeStringField(SHAPE, data.getSpec.getRowShape(row).toString)
        gen.writeNumberField(SIZE_FACTOR, data.getSpec.getRowSizeFactor(row))
        gen.writeBooleanField(HILITED, hiLited)
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
  protected override def configure(inSpecs: Array[PortObjectSpec]): Array[PortObjectSpec] = {

    // TODO: check if user settings are available, fit to the incoming
    // table structure, and the incoming types are feasible for the node
    // to execute. If the node can execute in its current state return
    // the spec of its output data table(s) (if you can, otherwise an array
    // with null elements), or throw an exception with a useful user message

    if (imageFormat.getStringValue == SVG && !svgSupported) {
      throw new InvalidSettingsException("SVG is not supported, please install org.knime.ext.svg.")
    }
    Array[PortObjectSpec] { new ImagePortObjectSpec(cellType) }
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
    createVegaSettings.validateSettings(settings)
    val m = createMappingSettings
    m.validateSettings(settings)
    val wrongGroups = m.getEnabledPairs.asScala.groupBy(_.getFirst).collect { case g if g._2.size > 1 => g }
    if (wrongGroups.size > 0) {
      throw new InvalidSettingsException("There are multiple substitutions for the following keys: " + wrongGroups.keys.map(_.getStringValue))
    }
    val f = createFormatSettings
    f.validateSettings(settings)
    if (f.getStringValue == SVG && !svgSupported) {
      throw new InvalidSettingsException("SVG is not supported, please install org.knime.ext.svg.")
    }
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
      val pattern = Pattern.compile("$inputTable$", Pattern.LITERAL)
      val specText = tempFile.fold(rawSpecText)(file => replacePairs(pattern.matcher(rawSpecText).replaceAll(file.toURI.toURL.toString.replaceFirst("file:/", "file://"))))
      writer.write(specText)
    } finally {
      writer.close
    }
  }

  private[this] def replacePairs(spec: String) = {
    mapping.getEnabledPairs().asScala.to[Vector].sortBy(-_.getFirst.getStringValue.length).foldLeft(spec)((spec, pair) => {
      val pattern = Pattern.compile(pair.getFirst.getStringValue, Pattern.LITERAL)
      pattern.matcher(spec).replaceAll(pair.getSecond.getStringValue)
    })
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
          logger.debug(out)
        },
        (err: String) => {
          logger.debug(err)
          sb.append(err).append('\n')
        }))
    } catch {
      case NonFatal(e) => {
        logger.info(sb.toString)
        val errorMessage = if (sb.indexOf(VEGA_ERROR_PREFIX) >= 0) {
          sb.substring(sb.toString.indexOf(VEGA_ERROR_PREFIX))
        } else sb.toString
        throw new Exception(errorMessage, e)
      }
    }
    outputFile
  }

  private[this] def cellType = imageFormat.getStringValue match {
    case SVG => SvgCell.TYPE
    case PNG => PNGImageContent.TYPE
    case _@ f => throw new UnsupportedOperationException("Not supported image format: " + f)
  }

  @throws[IOException]
  private def debugContent(resultFile: java.io.File): Unit = {
    var source: Source = null
    try {
      source = Source.fromFile(resultFile)
      val svg = source.getLines.mkString("\n")
      logger.debug(svg)
    } catch {
      case NonFatal(readError) => logger.coding(readError.getMessage)
    } finally {
      if (source != null) {
        try {
          source.close
        } catch {
          case NonFatal(closeError) => logger.coding(closeError.getMessage)
        }
      }
    }
  }
}

