package quuux.infinity.utilities

import collection.JavaConversions._
import java.io.File
import java.net.URLClassLoader
import javax.script.ScriptEngineManager

/**
 * Installs programming languages by either loading language JAR files or paths to
 * executables.
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 30/09/12
 */

object LanguageInstaller {

  /** adds the given jar file to the class path */
  private def addLanguageJAR(filePath:String) {
    val url = (new File(filePath)).toURI.toURL
    val classLoader = ClassLoader.getSystemClassLoader.asInstanceOf[URLClassLoader]
    val cls = Class.forName("java.net.URLClassLoader", true, classLoader)
    val method = cls.getDeclaredMethod("addURL", url.getClass)
    method.setAccessible(true)
    method.invoke(classLoader, url)
  }

  /** returns languages loaded and available through script engine */
  def languageNames:Iterable[String] = {
    (new ScriptEngineManager()).getEngineFactories.map(_.getLanguageName)
  }

  /** load language from JAR files */
  def loadLanguageJARs(baseDir:String) {
    val files = (new File(baseDir)).listFiles
    for (file <- files if files != null) {
      if( file.isFile && file.getName.toLowerCase.endsWith(".jar"))
        addLanguageJAR(file.getAbsolutePath)
      if( file.isDirectory )
        loadLanguageJARs(file.getAbsolutePath)
    }
  }

  def main(args:Array[String]) {
    println("running")
    loadLanguageJARs("resources/languages")
    for(name <- languageNames)
      println(name)
  }

}
