package quuux.infinity.zui

import xml.NodeSeq
import java.awt.{Color, Font}
import edu.umd.cs.piccolo.nodes.{PPath, PText}
import quuux.infinity.{Infinity, Settings}
import quuux.infinity.zui.Utils.{get,set}



/**
 * Notification message. Disappears after specified time.
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 22/08/13
 */
class Notification(xml:NodeSeq) extends PPath() {
  val duration = get(xml,"duration").toInt
  val pos = Infinity.mousePosition

  val textNode = new PText(get(xml,"text"))
  textNode.setTextPaint(Notification.textColor)
  textNode.setFont(Notification.font)

  setPathToEllipse(0,0, (textNode.getWidth*2).toFloat, (textNode.getHeight*2).toFloat)
  setOffset(pos.x-getWidth/2, pos.y-getHeight/2)
  setPaint(Notification.backgroundColor)
  setStrokePaint(null)
  setTransparency(0.9f)
  setPickable(false)
  addChild(textNode)
  textNode.centerBoundsOnPoint(getWidth/2,getHeight/2)
  Infinity.camera.addChild(this)

  private class Remover(notification:Notification) extends Thread {
    override def run() {
      Thread.sleep(notification.duration)
      remove()
    }
  }
  if(duration > 0) (new Remover(this)).start()

  def remove() { this.removeFromParent() }
}


object Notification {
  val textColor = Color.white
  val backgroundColor = Color.lightGray
  val font = new Font("Calibri", Font.PLAIN, 30)

  def apply(text:String, duration:Int=0):Notification =
    Notification(<text>{text}</text><duration>{duration}</duration>)

  def apply(xml:NodeSeq):Notification = new Notification(xml)
}