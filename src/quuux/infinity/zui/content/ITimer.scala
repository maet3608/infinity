package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolo.nodes.PText
import quuux.infinity.zui.Utils._
import java.awt.{Color, Font}
import quuux.infinity.zui.content.IObject._
import quuux.infinity.Settings
import java.awt.geom.Point2D


/**
 * Simple timer object
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

class ITimer(xml:NodeSeq) extends PText("00:00:00") with IObject {
  init("Timer", xml)
  setGreekThreshold(Settings.greekThreshold)
  private var running = false
  private val time = get(xml, "time").toLong
  private var diff = 0L
  setFont(ITimer.font)
  setTextPaint(ITimer.color)
  start(time)

  override def act() { if(running) stop() else continue() }

  def stop() { running = false }

  def continue() { start(System.currentTimeMillis()-diff) } 

  def start(time:Long) {
    stop()
    new Thread {
      running = true
      override def run() {
        while(running) {
          invokeLater {
            diff = System.currentTimeMillis()-time
            val (h,m,s) = (((diff / 1000) / 3600), (((diff / 1000) / 60) % 60), ((diff / 1000) % 60))
            setText("%02d:%02d:%02d" format(h,m,s))
            repaint()
          }
          Thread.sleep(1000)
        }
      }
    }.start()
  }

  override def toXML:NodeSeq = super.toXML ++ { <time>{time}</time> }

  def menu(position:Point2D) = IMenu("action_timer", "timer", this.uid, position,  List(
    ("action_start_timer", 8),
    ("action_stop_timer", 2),
    ("action_add_obj", 6),
    ("action_copy", 9),
    ("action_delete_timer", 4),
    ("action_properties_text", 3)
  ))
}


object ITimer {
  val font = new Font("Calibri", Font.PLAIN, 26)
  val color = Color.darkGray
  def apply():ITimer = ITimer(<time>{System.currentTimeMillis()}</time>)
  def apply(xml:NodeSeq):ITimer = new ITimer(xml)
}