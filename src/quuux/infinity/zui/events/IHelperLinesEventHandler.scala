package quuux.infinity.zui.events

import edu.umd.cs.piccolo.event.PBasicInputEventHandler._
import edu.umd.cs.piccolo.event.{PInputEvent, PInputEventFilter, PBasicInputEventHandler}
import quuux.infinity.Infinity
import java.awt.{BasicStroke, Color}
import edu.umd.cs.piccolo.nodes.PPath

/**
 * Draws a horizontal and a vertical helper line crossing at the mouse position
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 26/10/11
 */


class IHelperLinesEventHandler extends PBasicInputEventHandler {
  import IHelperLinesEventHandler._
  setEventFilter(new PInputEventFilter {
    override def acceptsEvent(e:PInputEvent,kind:Int) = showHelpLines
  })

  override  def mouseDragged(event:PInputEvent) {
    mouseMoved(event)
  }

  override def mouseMoved(event:PInputEvent) {
    val pos = event.getPosition
    val camera = Infinity.camera
    vLine.setBounds(pos.getX, camera.getY, 1, camera.getHeight)
    hLine.setBounds(camera.getX, pos.getY, camera.getWidth, 1)
  }
}


object IHelperLinesEventHandler extends PBasicInputEventHandler {
  var showHelpLines = false
  val alpha = 0.9f
  val color = Color.black
  val stroke = new BasicStroke(0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, Array(2,3), 0)
  val hLine = PPath.createLine(-1,0,1,0)
  val vLine = PPath.createLine(0,-1,0,1)

  def show() {
    showHelpLines = true
    val dest = Infinity.camera
    dest.addChild(vLine)
    dest.addChild(hLine)
  }

  def hide() {
    showHelpLines = false
    val dest = Infinity.camera
    dest.removeChild(vLine)
    dest.removeChild(hLine)
  }

  def toggle() {
    if(showHelpLines) hide() else show()
  }

  def apply() = {
    hLine.setTransparency(alpha)
    hLine.setStroke(stroke)
    hLine.setStrokePaint(color)
    hLine.setPickable(false)
    vLine.setTransparency(alpha)
    vLine.setStroke(stroke)
    vLine.setStrokePaint(color)
    vLine.setPickable(false)
    new IHelperLinesEventHandler
  }
}