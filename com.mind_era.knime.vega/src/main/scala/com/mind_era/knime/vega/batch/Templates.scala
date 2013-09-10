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
  final val ARC = """
{
  "name": "arc",
  "width": 400,
  "height": 400,
  "data": [
    {
      "name": "table",
      "url": "$inputTable$",
      "transform": [
        {"type": "pie", "value": "data.$numeric$"}
      ]
    }
  ],
  "scales": [
    {
      "name": "r",
      "type": "sqrt",
      "domain": {"data": "table", "field": "data.$numeric$"},
      "range": [20, 100]
    }
  ],
  "marks": [
    {
      "type": "arc",
      "from": {"data": "table"},
      "properties": {
        "enter": {
          "x": {"group": "width", "mult": 0.5},
          "y": {"group": "height", "mult": 0.5},
          "startAngle": {"field": "startAngle"},
          "endAngle": {"field": "endAngle"},
          "innerRadius": {"value": 20},
          "outerRadius": {"scale": "r", "field": "data.$numeric$"},
          "stroke": {"value": "#fff"}
        },
        "update": {
          "fill": {"value": "#ccc"}
        },
        "hover": {
          "fill": {"value": "pink"}
        }
      }
    }
  ]
}""".trim
  val AREA = """
{
  "width": 500,
  "height": 200,
  "padding": {"top": 10, "left": 30, "bottom": 30, "right": 10},
  "data": [
    {
      "name": "table",
      "url": "$inputTable$"
,
      "transform": [
        {"type": "sort", "by": "data.$numeric1$"}
      ]
    }
  ],
  "scales": [
    {
      "name": "x",
      "type": "linear",
      "range": "width",
      "zero": false,
      "domain": {"data": "table", "field": "data.$numeric1$"}
    },
    {
      "name": "y",
      "type": "linear",
      "range": "height",
      "nice": true,
      "domain": {"data": "table", "field": "data.$numeric2$"}
    }
  ],
  "axes": [
    {"type": "x", "scale": "x", "ticks": 20},
    {"type": "y", "scale": "y"}
  ],
  "marks": [
    {
      "type": "area",
      "from": {"data": "table"},
      "properties": {
        "enter": {
          "interpolate": {"value": "monotone"},
          "x": {"scale": "x", "field": "data.$numeric1$"},
          "y": {"scale": "y", "field": "data.$numeric2$"},
          "y2": {"scale": "y", "value": 0},
          "fill": {"value": "steelblue"}
        },
        "update": {
          "fillOpacity": {"value": 1}
        },
        "hover": {
          "fillOpacity": {"value": 0.5}
        }
      }
    }
  ]
}""".trim
  val GROUPED_BAR = """
{
  "width": 300,
  "height": 240,
  "data": [
    {
      "name": "table",
      "url": "$inputTable$"
    }
  ],
  "scales": [
    {
      "name": "cat",
      "type": "ordinal",
      "range": "height",
      "padding": 0.2,
      "domain": {"data": "table", "field": "data.$nominal1$"}
    },
    {
      "name": "val",
      "range": "width",
      "round": true,
      "nice": true,
      "domain": {"data": "table", "field": "data.$numeric$"}
    },
    {
      "name": "color",
      "type": "ordinal",
      "range": "category20"
    }
  ],
  "axes": [
    {"type": "y", "scale": "cat", "tickSize": 0, "tickPadding": 8},
    {"type": "x", "scale": "val"}
  ],
  "marks": [
    {
      "type": "group",
      "from": {
        "data": "table",
        "transform": [{"type":"facet", "keys":["data.$nominal1$"]}]
      },
      "properties": {
        "enter": {
          "y": {"scale": "cat", "field": "key"},
          "height": {"scale": "cat", "band": true}
        }
      },
      "scales": [
        {
          "name": "pos",
          "type": "ordinal",
          "range": "height",
          "domain": {"field": "data.$nominal2$"}
        }
      ],
      "marks": [
        {
          "type": "rect",
          "properties": {
            "enter": {
              "y": {"scale": "pos", "field": "data.$nominal2$"},
              "height": {"scale": "pos", "band": true},
              "x": {"scale": "val", "field": "data.$numeric$"},
              "x2": {"scale": "val", "value": 0},
              "fill": {"scale": "color", "field": "data.$nominal2$"}
            }
          }
        },
        {
          "type": "text",
          "properties": {
            "enter": {
              "y": {"scale": "pos", "field": "data.$nominal2$"},
              "dy": {"scale": "pos", "band": true, "mult": 0.5},
              "x": {"scale": "val", "field": "data.$numeric$", "offset": -4},
              "fill": {"value": "white"},
              "align": {"value": "right"},
              "baseline": {"value": "middle"},
              "text": {"field": "data.$numeric$"}
            }
          }
        }
      ]
    }
  ]
}""".trim

  private[this] val predefinedTemplates = Seq(
      Template("Bar chart", BatchVegaViewerNodeModel.DEFAULT_VEGA_SPEC, Seq(Parameter("$numeric$", ParameterKind.NumericAny, ""), Parameter("$nominal$", ParameterKind.OrdinalAny, "")))/*,
      Template("Arc", ARC, Seq()),
      Template("Area", AREA, Seq()),
      Template("Grouped bar", GROUPED_BAR, Seq())*/
      )
  private[this] val reg = Platform.getExtensionRegistry
  
  private[this] val contributedTemplates = for (
      template <- reg.getConfigurationElementsFor("com.mind_era.knime.vega.batch.vega_templates")
      ) yield Template(template.getAttribute("name"),
          template.getChildren("text")(0).getValue.trim,
          template.getChildren("parameter").map(
              p=>Parameter(p.getAttribute("name"), ParameterKind.get(p.getAttribute("paramType")), p.getValue())).to[Seq])
  val template = LinkedHashMap((predefinedTemplates ++ contributedTemplates).map((t: Template)=>(t.name, t)):_*)
}