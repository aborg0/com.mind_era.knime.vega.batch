/*
 *
 */
package com.mind_era.knime.vega.batch

/**
 * The template class which is compatible with the extension point. 
 * 
 * @author Gabor Bakos
 */
case class Template(name: String, text: String, parameters: Seq[ParameterType]) {

}

case class ParameterType(name: String, kind: ParameterKind) {
  
}