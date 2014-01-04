/*
 *
 */
package com.mind_era.knime.vega.batch

/**
 * The template class which is compatible with the extension point.
 *
 * @author Gabor Bakos
 */
case class Template(name: String, text: String, parameters: Seq[Parameter]) {

}

/**
 * The definition of parameters.
 */
case class Parameter(name: String, kind: ParameterKind, description: String) {
  //TODO support (optional) parameters for constants for example color, size ...
}