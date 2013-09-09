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
      "domain": {"data": "table", "field": "$numeric$"},
      "range": [20, 100]
    }
  ],
  "marks": [
    {
      "type": "arc",
      "from": {"data": "table", "field":"$numeric$"},
      "properties": {
        "enter": {
          "x": {"group": "width", "mult": 0.5},
          "y": {"group": "height", "mult": 0.5},
          "startAngle": {"field": "startAngle"},
          "endAngle": {"field": "endAngle"},
          "innerRadius": {"value": 20},
          "outerRadius": {"value": 200},
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
  val template = LinkedHashMap(("Bar chart", BatchVegaViewerNodeModel.DEFAULT_VEGA_SPEC), ("Arc", ARC))
}