package quuux.infinity.utilities

/**
 * A code environment is a simple dictonary of variables and their values. It
 * contains the information (e.g paths) necessary to run code via the JVM
 * or by calling a command line tools.
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 2/10/12
 */

/**
 * Code environment
 * @param vars Variable names and their values.
 */
class CodeEnvironment(vars:Tuple2[String,String]*) {
  private val variables = Map(vars:_*)
  def get = variables
  def get(name:String, default:String="") = variables.getOrElse(name,default)
}


object CodeEnvironment {

  def main(args:Array[String]) {
    val env = new CodeEnvironment(
        "prefix" -> "from pylab import *\n",
        "suffix" -> "\nshow()",
        "extension" -> "py")
  }
}
