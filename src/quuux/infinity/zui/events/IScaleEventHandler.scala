package quuux.infinity.zui.events

import edu.umd.cs.piccolo.event.{PInputEvent, PBasicInputEventHandler}
import quuux.infinity.Infinity
import java.awt.event.InputEvent
import java.lang.System.currentTimeMillis
import quuux.infinity.zui.Utils._
import quuux.infinity.zui.content.{IObject, IItem, IGroup, IGroupLabel, IItemSensor}
import edu.umd.cs.piccolox.pswing.PSwingCanvas
import java.awt.Cursor

/**
 * Handles scaling or zooming
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 26/10/11
 */

class IScaleEventHandler(val canvas:PSwingCanvas) extends PBasicInputEventHandler {
  private val speed = 0.05
  private var node:IObject = _
  private var t:Long = currentTimeMillis
  private var dt:Long = 0
  private var clicks:Long = 0
  private var gliding = false

  override def mouseWheelRotated(event:PInputEvent) {
    gliding = false
    val isRoot = (event.getModifiers & InputEvent.CTRL_MASK) == 0
    node = event.getPickedNode match {
      case picked:IItemSensor => picked.getParent.asInstanceOf[IItem]
      case picked:IGroupLabel => picked.getParent.asInstanceOf[IGroup]
      case picked if isRoot => Infinity.root
      case picked:IObject => picked
      case _ => return
    }
    clicks = event.getWheelRotation
    val pos = node.globalToLocal(Infinity.mousePosition)
    node.scaleAboutPoint(1.0 - speed*clicks, pos)

    dt = currentTimeMillis-t
    t = currentTimeMillis
    if(dt < 30) glide()
    event.setHandled(true)
  }

  private def glide() {
    invokeThread {
      gliding = true
      var s = speed
      while(gliding && s > speed/10) {
        Thread.sleep(dt)
        val pos = node.globalToLocal(Infinity.mousePosition)
        s *= 0.95
        invokeLater { node.scaleAboutPoint(1.0 - s*clicks, pos) }
      }
    }
  }
}