package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolox.pswing.PSwing

import quuux.infinity.zui.Utils._
import quuux.infinity.zui.content.IObject._
import tools.nsc.{NewLinePrintWriter, Settings}
import tools.nsc.interpreter.{Results, IMain}
import quuux.infinity.{Infinity, Settings}
import edu.umd.cs.piccolo.PNode
import quuux.infinity.utilities.JavaRunner
import jsyntaxpane.DefaultSyntaxKit
import java.awt.{Color, Font}
import java.io._
import java.awt.geom.Point2D
import javax.swing.{JComponent, JScrollPane, JEditorPane}

/**
 * Code that can be edited and executed
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

class ICode(xml:NodeSeq) extends PSwing(ICode.xml2jeditor(xml)) with IObject {
  init("Code", xml)
  setGreekThreshold(Settings.greekThreshold)
  def syntax = get(xml, "syntax")
  def application = get(xml, "application")

  def getCode = getComponent match {
    //case comp:ScaledTextPane => comp.getText
    //case comp:JScrollPane => comp.getComponent(0).asInstanceOf[ScaledTextPane].getText
    case comp:JEditorPane => comp.getText
    case comp:JScrollPane => comp.getComponent(0).asInstanceOf[JEditorPane].getText
  }

  def getAllCode = {
    def getCodeParents(node:PNode):Seq[ICode] = node match {
      case null => Seq()
      case node:ICode => getCodeParents(node.getParent) :+ node
      case node:PNode => getCodeParents(node.getParent)
    }
    getCodeParents(this).map(_.getCode).mkString("\n\n")
  }


  def act() { ICode.run(this) }

  override def toXML:NodeSeq = super.toXML ++ {
    <code xml:space ="preserve">{getCode}</code>
    <syntax>{syntax}</syntax>
    <application>{application}</application>
  }

  def menu(position:Point2D) = IMenu("action_code", "code", uid, position,  List(
    ("action_print", 7),
    ("action_run_code", 8),
    ("action_add_obj", 6),
    ("action_copy", 9),
    ("action_delete", 4),
    ("action_properties_text", 3)
  ))
}


object ICode {

  DefaultSyntaxKit.initKit()

  val dots = "..."
  val font = new Font("Consolas", Font.PLAIN, 16)
  val scalalaImports = List("tensor.dense","scalar","tensor","tensor.mutable","tensor.dense","tensor.sparse",
    "library.Library","library.LinearAlgebra","library.Statistics","library.Plotting","operators.Implicits"
  ).map(name => "import scalala."+name+"._;\n").mkString


  implicit def stream2str(stream:InputStream) = scala.io.Source.fromInputStream(stream).mkString

  private def xml2jeditor(xml:NodeSeq):JComponent = {
    //val jEditorPane = new ScaledTextPane()
    val jEditorPane = new JEditorPane()
    //jEditorPane.setBorder(new LineBorder(Color.lightGray, 1))
    val scrollEditor = (new JScrollPane(jEditorPane))
    //scrollEditor.setPreferredSize(new Dimension(100, 150))
    //jEditorPane.getDocument.putProperty("i18n", java.lang.Boolean.TRUE)
    jEditorPane.setContentType("text/"+get(xml,"syntax"))
    jEditorPane.setText(get(xml,"code"))
    jEditorPane.setFont(font)
    jEditorPane.setOpaque(false)
    jEditorPane.setComponentPopupMenu(null)
    //jEditorPane.putClientProperty(SwingUtilities2.AA_TEXT_PROPERTY_KEY, null) //  antialiasing off
    jEditorPane
  }

  private def createOutput(obj:IObject, output:String, channel:String) = {
    val h = obj.getHeight
    val outputNode = IOutput(output, channel)
    obj.addChild(outputNode)
    outputNode.translate(3, h)
    outputNode
  }

  private def getOutput(obj:IObject, channel:String="stdout") =
    obj.children.collectFirst{case n:IOutput if n.channel == channel => n}

  def showOutput(obj:IObject, output:String, channel:String="stdout") {
    if(output.length > 0) invokeLater {
      getOutput(obj, channel) match {
        case Some(node) => node.setText(output)
        case None => createOutput(obj, output, channel)
      }
    }
  }

  def removeDots(obj:IObject) {
    invokeLater { getOutput(obj).foreach(o => if(o.getText==dots) obj.removeChild(o)) }
  }

  // run code by creating a script file and calling an external process to execute it
  private def runExternal(obj:ICode) {
    showOutput(obj,dots)
    try {
      import scala.sys.process._
      val pio = new ProcessIO(_ => (), stdout => showOutput(obj,stdout),  stderr => showOutput(obj,stderr,"stderr"))
      val (cmd, ext, prefix, suffix) = obj.application match {
        case "Scala" => (Settings.cmdScala, "scala", "","")
        case "Scalala" => (Settings.cmdScalala, "scala", scalalaImports,"")
        case "Clojure" => (Settings.cmdClojure, "clj","","")
        case "Incanter" => (Settings.cmdIncanter, "clj","(use '(incanter core stats charts))","")
        case "DOS" => (Settings.cmdDOS, "cmd", "","")
        case "BASH" => (Settings.cmdBASH, "sh", "","")
        case "Ruby" => (Settings.cmdRuby, "rb", "", "")
        case "Perl" => (Settings.cmdPerl, "pl", "", "")
        case "R" => (Settings.cmdR, "r", "", "")
        case "Haskell" => (Settings.cmdHaskell, "hs", "", "")
        case "Python" => (Settings.cmdPython, "py", "", "")
        case "Pylab" => (Settings.cmdPylab, "py", "from pylab import *\n", "\nshow()")
      }
      val cwd = new File(Infinity.documentDir)
      val temp = File.createTempFile(obj.syntax,"."+ext,cwd)
      val stub = temp.getAbsolutePath.replace("."+ext,"")
      val writer = new FileWriter(temp)
      writer.write(prefix)
      writer.write(obj.getAllCode)
      writer.write(suffix)
      writer.close()
      val pb = Process(cmd.format("\""+temp.getAbsolutePath+"\"",stub,ext), cwd)
      pb.run(pio)
      temp.deleteOnExit()
    } catch {
      case e:IOException => forward(obj, e)
      case e:Exception => showOutput(obj,e.toString,"stderr")
    }
  }

  // forwards running of code to JVM version if external program cannot be found
  def forward(obj:ICode, e:IOException) {
    obj.application match {
      case "Python" => runJython(obj)
      case "Pylab" => runJython(obj)
      case "Scala" => runScala(obj)
      case _ => showOutput(obj,e.toString,"stderr")
    }
  }

  // compile and run java code
  private def runJava(obj:ICode) {
    showOutput(obj,dots)
    invokeThread {
      try {
        val runner = new JavaRunner // TODO Redirects stdout; not thread safe
        val out = runner.exec(obj.getCode)
        showOutput(obj,out.toString)
      } catch {
        case e:Exception => showOutput(obj,e.toString,"stderr")
      }
    }
  }

  // run jython code via embedded interpreter
  private def runJython(obj:ICode) {
    import org.python.util.PythonInterpreter
    val chdir = "import os; os.chdir('%s')" format Infinity.documentDir
    showOutput(obj,dots)
    invokeThread {
      try {
        val out = new StringWriter
        val interpreter = new PythonInterpreter
        interpreter.setOut(out)
        interpreter.exec(chdir)
        interpreter.exec(obj.getCode)
        showOutput(obj,out.toString)
      } catch {
        case e:Exception => showOutput(obj,e.toString,"stderr")
      }
    }
  }

  private def runScala(obj:ICode) {
    showOutput(obj,dots)
    invokeThread {
      try {
        val settings = new Settings
        val out = new ByteArrayOutputStream
        val (stdout, stderr) = (System.out, System.err)
        scala.Console.setOut(out)      // TODO not thread safe
        scala.Console.setErr(out)      // TODO not thread safe
        settings.usejavacp.value = true
        val interpreter = new IMain(settings, new PrintWriter(out))
        val result = interpreter.quietRun(obj.getCode)
        result match {
          case Results.Success => invokeLater{showOutput(obj,out.toString)}
          case Results.Incomplete => showOutput(obj,result.toString,"stderr")
          case Results.Error => showOutput(obj,out.toString,"stderr")
        }
        interpreter.reset()
        interpreter.close()
        scala.Console.setOut(stdout)   // TODO not thread safe
        scala.Console.setErr(stderr)   // TODO not thread safe
      } catch {
        case e:Exception => showOutput(obj,e.toString,"stderr")
      }
    }
  }  

  def run(obj:ICode) {
    obj.application match {
      case "Java" => runJava(obj)
      case "Jython" => runJython(obj)
      case "Scala"  => runScala(obj)
      //case _ => CodeRunner(obj.application).run(obj)
      case _ => runExternal(obj)
    }
  }

  def apply(code:String, application:String, syntax:String):ICode = ICode({
    <code>{code}</code>
    <application>{application}</application>
    <syntax>{syntax}</syntax>
  })
  def apply(xml:NodeSeq):ICode = new ICode(xml)
}