package quuux.infinity.utilities

import quuux.infinity.zui.content.{IOutput, IObject, ICode}
import quuux.infinity.zui.Utils._
import quuux.infinity.Infinity
import sys.process.{Process, ProcessIO}
import javax.swing.filechooser.FileSystemView
import java.io.{File, FileWriter, InputStream}
import java.util.Date

/**
* XXX
* Author : Stefan Maetschke
* Version: 1.00
* Date   : 25/10/11
*/

abstract class CodeRunner {
  def run(obj:ICode, env:CodeEnvironment):Unit
}


object CodeRunnerCMD extends CodeRunner {

  private def createOutput(obj:IObject, output:String, channel:String) = {
    val h = obj.getHeight
    val outputNode = IOutput(output, channel)
    obj.addChild(outputNode)
    outputNode.translate(3, h)
    outputNode
  }

  private def getOutput(obj:IObject, channel:String="stdout") =
    obj.children.collectFirst{case n:IOutput if n.channel == channel => n}

  private def showOutput(obj:IObject, output:String, channel:String="stdout") {
    if(output.length > 0) invokeLater {
      getOutput(obj, channel) match {
        case Some(node) => node.setText(output)
        case None => createOutput(obj, output, channel)
      }
    }
  }

  private def getJavaClassName(code:String):String = {
    val regex = """class\s+(\w+)\s*\{""".r
    try {
      regex.findFirstMatchIn(code).get.group(1)
    }
    catch { case e:Exception => throw new Exception("Cannot find: class <classname> in Java code!") }
  }

  private def createCodeFile(obj:ICode, env:CodeEnvironment) = {
    val ext = env.get("extension")
    if(ext == "java") new File(getJavaClassName(obj.getCode)+".java") else File.createTempFile(obj.syntax,"."+ext)
  }

  private def fillCodeFile(codeFile:File, obj:ICode, env:CodeEnvironment) {
    val writer = new FileWriter(codeFile)
    writer.write(env.get("prefix"))
    writer.write(obj.getCode)
    writer.write(env.get("suffix"))
    writer.close()
  }

  private def createCommand(codeFile:File, env:CodeEnvironment) = {
    val ext = env.get("extension")
    val stub = codeFile.getAbsolutePath.replace("."+ext,"")
    val vars = env.get ++ Map("cwd" -> cwd, "codepath"->codeFile.getAbsolutePath, "codepathstub"->stub)
    vars.foldLeft(env.get("commandLine")){case (t,(k,v)) => t.replace("{"+k+"}",v)}
  }

  private implicit def stream2str(stream:InputStream) = scala.io.Source.fromInputStream(stream).mkString

  def run(obj:ICode, env:CodeEnvironment) {
    try {
      val pio = new ProcessIO(_ => (), stdout => showOutput(obj, stdout),  stderr => showOutput(obj, stderr, "stderr"))
      val codeFile = createCodeFile(obj,env)
      val cmd = createCommand(codeFile,env)
      val wdir = new File(Infinity.documentDir)
      val pb = Process(cmd, wdir)
      fillCodeFile(codeFile, obj,env)
      pb.run(pio)
      codeFile.deleteOnExit()
    } catch {
      case e:Exception => showOutput(obj, e.toString, "stderr")
    }
  }

}

object CodeRunner {
  def main(args:Array[String]) {

  }
}

