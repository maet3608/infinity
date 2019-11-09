package quuux.infinity.utilities

import java.io.{File, FileWriter, BufferedWriter}
import quuux.infinity.Settings
import javax.swing.JOptionPane

/**
 * Replaces the default error handler and catches top level exceptions
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 9/09/12
 */
class ErrorHandler extends Thread.UncaughtExceptionHandler {

  def uncaughtException(t: Thread, e: Throwable) {
    val error = "ERROR: " + e.toString + "\n" + e.getStackTraceString
    println(error)
    val writer = new BufferedWriter(new FileWriter(new File("error.log")))
    writer.write(Settings.about + "\n\n" + error)
    writer.close()
    JOptionPane.showMessageDialog(null, e.toString, "Infinity error", JOptionPane.ERROR_MESSAGE)
  }

}