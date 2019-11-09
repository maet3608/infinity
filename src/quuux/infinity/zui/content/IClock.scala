package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolo.nodes.PText
import java.text.SimpleDateFormat
import quuux.infinity.zui.Utils._
import java.util.Calendar
import java.awt.{Color, Font}
import quuux.infinity.zui.content.IObject._
import quuux.infinity.Settings
import java.awt.geom.Point2D


/**
 * Simple clock
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

class IClock(xml:NodeSeq) extends PText("00:00") with IObject {
  init("Clock", xml)
  setGreekThreshold(Settings.greekThreshold)
  private val format = get(xml,"format")
  private var running = true
  setFont(IClock.font)
  setTextPaint(IClock.color)

  val formatter = new SimpleDateFormat(format)
  invokeThread {
    while(running) {
      invokeLater {
        setText(formatter.format(Calendar.getInstance.getTime))
        repaint()
      }
      Thread.sleep(30000)
    }
  }

  def stop() { running = false }

  override def toXML:NodeSeq = super.toXML ++ { <format>{format}</format> }

  def act() { }

  def menu(position:Point2D) = IMenu("action_clock", "clock", uid, position,  List(
    ("action_add_obj", 6),
    ("action_copy", 9),
    ("action_delete_clock", 4),
    ("action_properties_text", 3)
  ))
}

object IClock {
  val font = new Font("Calibri", Font.PLAIN, 26)
  val color = Color.gray
  def apply():IClock = IClock(<format>{"HH:mm"}</format>)
  def apply(xml:NodeSeq):IClock = new IClock(xml)
}