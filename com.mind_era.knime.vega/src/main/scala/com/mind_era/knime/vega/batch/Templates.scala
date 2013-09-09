/*
 *
 */
package com.mind_era.knime.vega.batch

import scala.collection.mutable.LinkedHashMap

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
  val template = LinkedHashMap(("Bar chart", BatchVegaViewerNodeModel.DEFAULT_VEGA_SPEC), ("Arc", ARC), ("Area", AREA))
}