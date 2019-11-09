package quuux.infinity

import java.awt.Toolkit._
import java.awt.{GraphicsEnvironment, Rectangle, Dimension}
import xml.XML
import java.io.File


/**
 * Properties of the main application
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 19/09/11
 */

object Settings {
  private val SettingsPath = "infinity.properties"


  def version = "0.24"
  def date = "12 October 2014"
  def name = "infinity"
  def about =
  """
     %s
     version:  %s
     date: %s
     copyright:  Stefan Maetschke
  """ format(name, version, date)

  // general settings
  var isFullscreen    = false
  var greekThreshold  = 0.3f       // draw text as rect when too small
  var rootSize        = 20000f     // initial size of root container

  // trailing slashes required
  var resourcesPath = "resources/"
  var themesPath = resourcesPath+"themes/"
  var theme = "default"
  var iconsPath = themesPath+theme+"/icons/"

  val documentExt = "ipx"
  val defaultDoc = name+".ipx"

  // cmd lines to call external tools
  var cmdPython = "python %1$s"
  var cmdPylab = "python %1$s"
  var cmdScala = "C:/Maet/Software/Scala/bin/scala.bat -deprecation %1$s"
  var cmdScalala = """C:/Maet/Software/Scala/bin/scala.bat -classpath "c:/Maet/Software/Scalala/scalala.jar"  %1$s"""
  var cmdClojure = """java -cp "C:/Maet/Software/Clojure/clojure.jar" clojure.main %1$s"""
  var cmdIncanter = """java -cp "C:/Maet/Software/Clojure/clojure.jar;C:/Maet/Software/Clojure/incanter.jar" clojure.main %1$s"""
  var cmdRuby = "c:/Maet/Software/Ruby/bin/ruby.exe %1$s"
  var cmdR = "c:/Maet/Software/R/R-2.12.2/bin/RScript.exe %1$s"
  var cmdPerl = "perl.exe %1$s"
  var cmdHaskell = """"c:/Maet/Software/Haskell Platform/2011.2.0.1/bin/runhaskell.exe" %1$s"""
  var cmdDOS = "cmd /c %1$s"
  var cmdBASH = "C:/Maet/Software/Cygwin/bin/bash --login %1$s"


  var oldBounds = new Rectangle(0,0,0,0)  /** old bounds of the application's window before full screen mode */


  /** returns the default size of the application's windows */
  def appDefaultSize = {
    val preferredSize = new Dimension(1310, 770)  // Udemy screen recording size 1280x720
    val screen = getDefaultToolkit.getScreenSize
    val doesFit = screen.getWidth > preferredSize.getWidth && screen.getHeight > preferredSize.getHeight
    if (doesFit) preferredSize else new Dimension((screen.getWidth*0.8).toInt,(screen.getHeight*0.8).toInt)    
  }

  /** returns the maximum bounds of the application's window */
  def appMaxBounds = {
    val screen = getDefaultToolkit.getScreenSize
    new Rectangle(0,0,screen.getWidth.toInt,screen.getHeight.toInt)
  }


  /** saves settings to a file in XML format */
  def save(filepath:String = SettingsPath) {
    val xml = {
      <Settings>
        <software-name>{name}</software-name>
        <software-version>{version}</software-version>
        <software-date>{date}</software-date>
        <greekThreshold>{greekThreshold}</greekThreshold>
        <isFullscreen>{isFullscreen}</isFullscreen>
        <rootSize>{rootSize}</rootSize>
        <cmdPython>{cmdPython}</cmdPython>
        <cmdPylab>{cmdPylab}</cmdPylab>
        <cmdScala>{cmdScala}</cmdScala>
        <cmdScalala>{cmdScalala}</cmdScalala>
        <cmdRuby>{cmdRuby}</cmdRuby>
        <cmdPerl>{cmdPerl}</cmdPerl>
        <cmdR>{cmdR}</cmdR>
        <cmdHaskell>{cmdHaskell}</cmdHaskell>
        <cmdClojure>{cmdClojure}</cmdClojure>
        <cmdIncanter>{cmdIncanter}</cmdIncanter>
        <cmdDOS>{cmdDOS}</cmdDOS>
        <cmdBASH>{cmdBASH}</cmdBASH>
      </Settings>
    }
    XML.save(filepath, xml, "UTF-8")
  }

  /** loads settings from a file in XML format */
  def load(filepath:String = SettingsPath) {
    if(!(new File(SettingsPath)).exists) save(filepath)
    val xml = XML.loadFile(filepath)
    def get(name:String) = (xml \\ name).text

    isFullscreen = get("isFullscreen").toBoolean
    greekThreshold = get("greekThreshold").toFloat
    rootSize = get("rootSize").toFloat
    cmdPython = get("cmdPython").toString
    cmdPylab = get("cmdPylab").toString
    cmdScala = get("cmdScala").toString
    cmdScala = get("cmdScalala").toString
    cmdRuby = get("cmdRuby").toString
    cmdPerl = get("cmdPerl").toString
    cmdR = get("cmdR").toString
    cmdHaskell = get("cmdHaskell").toString
    cmdClojure = get("cmdClojure").toString
    cmdIncanter = get("cmdIncanter").toString
    cmdDOS = get("cmdDOS").toString
    cmdBASH = get("cmdBASH").toString
  }
}