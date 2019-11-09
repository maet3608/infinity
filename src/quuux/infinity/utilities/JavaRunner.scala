package quuux.infinity.utilities

import javax.tools._
import javax.tools.JavaFileObject.Kind
import java.net.{URLClassLoader, URI}
import javax.tools.JavaCompiler.CompilationTask
import java.util
import java.io.{PrintStream, ByteArrayOutputStream, File}
import collection.JavaConversions._

/**
 * compiles Java code and runs it.
 * derived from
 * http://www.java2s.com/Code/Java/JDK-6/CompilingfromMemory.htm
 */
class JavaRunner {
  val compiler:JavaCompiler = ToolProvider.getSystemJavaCompiler
  val diagnostics:DiagnosticCollector[JavaFileObject] = new DiagnosticCollector[JavaFileObject]

  def error(msg:String) = throw new RuntimeException(msg)

  private class JavaSourceFromString(name:String, code:String) extends SimpleJavaFileObject(
    URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension), Kind.SOURCE) {
    override def getCharContent(ignoreEncodingErrors:Boolean):CharSequence = code
  }

  def getClassName(code:String) = {
    val regex = """class\s+(\w+)\s*\{""".r
    try {
      regex.findFirstMatchIn(code).get.group(1)
    }
    catch { case e:Exception => error("Cannot find: class <classname>!") }
  }

  def exec(code:String, args: Array[String] = Array[String]()):String = {
    // TODO not thread safe. Creates file <name.class>
    val name = getClassName(code)
    val file:JavaFileObject = new JavaSourceFromString(name,code)
    val task:CompilationTask = compiler.getTask(null, null, diagnostics, null, null, util.Arrays.asList(file))

    if( task.call()) {
      try {
        val cwd = new File(".")
        val classLoader = URLClassLoader.newInstance(Array(cwd.toURI.toURL))
        val cls = Class.forName(name, true, classLoader)
        val method = cls.getDeclaredMethod("main", args.getClass)
        val (stdout, baos) = (System.out,  new ByteArrayOutputStream)
        System.setOut(new PrintStream(baos))  // TODO not thread safe
        method.setAccessible(true)            // main class might not be public
        method.invoke(null, args)
        System.setOut(stdout)                 // TODO not thread safe
        (new File(cwd, name+".class")).delete()
        return baos.toString
      } catch { case e:Exception => error(e.toString) }
    } else {
      error(diagnostics.getDiagnostics.map(_.getMessage(null)).mkString("ERROR:\n","\n","\n"))
    }
    error("Compilation failed!")
  }
}


object JavaRunner {
  def main(args: Array[String]) {

    val code =
      """
        |class HelloWorld {
        |  public static void main(String args[]) {
        |      System.out.println("This is Hello world");
        |  };
        |};
      """.stripMargin

    val runner = new JavaRunner
    println( runner.exec(code) )
  }
}
