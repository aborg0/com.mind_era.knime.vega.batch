/*
 *
 */
package com.mind_era.knime.vega.batch

import scala.collection.mutable.LinkedHashMap
import org.eclipse.core.runtime.Platform

/**
 * Templates for the dialog of Vega specification.
 * 
 * @author Gabor Bakos
 */
object Templates {

  private[this] val predefinedTemplates = Seq(
      Template("Bar chart", BatchVegaViewerNodeModel.DEFAULT_VEGA_SPEC, Seq(Parameter("$numeric$", ParameterKind.NumericAny, ""), Parameter("$nominal$", ParameterKind.OrdinalAny, ""))))
  private[this] val reg = Platform.getExtensionRegistry
  
  private[this] val contributedTemplates = for (
      template <- reg.getConfigurationElementsFor("com.mind_era.knime.vega.batch.vega_templates")
      ) yield Template(template.getAttribute("name"),
          template.getChildren("text")(0).getValue.trim,
          template.getChildren("parameter").map(
              p=>Parameter(p.getAttribute("name"), ParameterKind.get(p.getAttribute("paramType")), p.getValue())).to[Seq])
  val template = LinkedHashMap((/*predefinedTemplates ++ */contributedTemplates).map((t: Template)=>(t.name, t)):_*)
}