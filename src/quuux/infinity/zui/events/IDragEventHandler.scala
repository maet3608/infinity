package quuux.infinity.zui.events

import java.lang.System.currentTimeMillis
import java.awt.event.InputEvent
import edu.umd.cs.piccolo.util.PDimension
import edu.umd.cs.piccolo.event.{PInputEvent, PInputEventFilter, PBasicInputEventHandler}
import quuux.infinity.zui.Utils._
import scala.Predef._
import java.awt.Point
import quuux.infinity.Infinity
import quuux.infinity.zui.content._
import java.awt.Cursor
import edu.umd.cs.piccolox.pswing.PSwingCanvas

/**
 * Handles dragging or panning
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 26/10/11
 */


class IDragEventHandler(val canvas:PSwingCanvas) extends PBasicInputEventHandler {
  setEventFilter(new PInputEventFilter(InputEvent.BUTTON2_MASK))
  private var alpha:Float = 0.0f
  private var node:IObject = null
  private var delta:PDimension = new PDimension(0.0,0.0)
  private var t:Long = currentTimeMillis
  private var dt:Long = 0
  private var panRoot:Boolean = _
  private var gliding = false
  private var pos:Point = _

  protected def dist(delta:PDimension) = delta.getWidth.abs+delta.getHeight.abs

  protected override def mousePressed(event:PInputEvent) {
    gliding = false
    panRoot = (event.getModifiers & InputEvent.CTRL_MASK) == 0
    node = event.getPickedNode match { 
      case picked:IItemSensor => picked.getParent.asInstanceOf[IItem]
      case picked:IGroupLabel => picked.getParent.asInstanceOf[IGroup]
      case picked if panRoot => Infinity.root
      case picked:IObject => picked
      case _ => return
    }
    alpha = node.getTransparency
    if(!panRoot) node.setTransparency(0.5f)
    pos = Infinity.mousePosition
    setCursor(canvas, "NONE")
    //don't  event.setHandled(true) or context menus don't work
  }

  protected override def mouseDragged(event:PInputEvent) {
    if(node == null || pos == null) return
    val newPos = Infinity.mousePosition
    val c = node.getGlobalScale
    delta.setSize((newPos.getX-pos.getX)/c, (newPos.getY-pos.getY)/c)
    pos = Infinity.mousePosition
    dt = currentTimeMillis-t
    t = currentTimeMillis
    node.localToParent(delta)
    node.offset(delta.getWidth, delta.getHeight)
    // don't  event.setHandled(true) otherwise help lines are not moving: IHelperLinesEventHandler
  }

  protected override def mouseReleased(event:PInputEvent) {
    setCursor(canvas, "STANDARD")
    if(node == null) return
    if(!panRoot) node.setTransparency(alpha)
    if(dist(delta) > 2 && dist(delta) < 1000) glide() else node = null
    //don't event.setHandled(true) or context menus don't work
  }

  protected override def mouseClicked(event: PInputEvent) {
    if(gliding || event.getClickCount != 2) return
    val obj = event.getPickedNode match {
      case picked:IGroupLabel => picked.getParent.asInstanceOf[IGroup]
      case picked:IObject => picked
      case _ => return
    }
    obj.act()
  }

  private def glide() {
    invokeThread {
      gliding = true
      val s = 20
      val c = node.getScale
      var (vx,vy) = (s*delta.getWidth/(dt+1)/c, s*delta.getHeight/(dt+1)/c)
      delta.setSize(0,0)
      while(gliding & vx.abs+vy.abs > 1 && dt < 10) {
        Thread.sleep(s)
        invokeLater {
          vx *= 0.90; vy *= 0.90
          node.translate(vx,vy)
        }
      }
    }
  }
}