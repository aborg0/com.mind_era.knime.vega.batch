/*
 * 
 */
package com.mind_era.knime.vega.batch;

/**
 * The defined parameter kinds.
 * 
 * @author Gabor Bakos
 */
public enum ParameterKind {
  OrdinalAny, OrdinalString, OrdinalInt, OrdinalReal, CategoricalAny, CategoricalString, CategoricalInt, NumericAny, NumericInt, NumericReal;
  
  public static ParameterKind get(String text) {
//      <enumeration value="categorical/any">
//      <enumeration value="categorical/string">
//      <enumeration value="categorical/integer">
//      <enumeration value="numeric/any">
//      <enumeration value="numeric/integer">
//      <enumeration value="numeric/real">
//      <enumeration value="ordinal/any">
//      <enumeration value="ordinal/string">
//      <enumeration value="ordinal/integer">
//      <enumeration value="ordinal/real">

	  switch (text) {
	  case "categorical/any": return CategoricalAny;
	  case "categorical/string": return CategoricalString;
	  case "categorical/integer": return CategoricalInt;
	  case "numeric/any": return NumericAny;
	  case "numeric/integer": return NumericInt;
	  case "numeric/real": return NumericReal;
	  case "ordinal/any": return OrdinalAny;
	  case "ordinal/string": return OrdinalString;
	  case "ordinal/integer": return OrdinalInt;
	  case "ordinal/real": return OrdinalReal;
		  default:
			  throw new UnsupportedOperationException("Unknown: " + text);
	  }
  }
}
