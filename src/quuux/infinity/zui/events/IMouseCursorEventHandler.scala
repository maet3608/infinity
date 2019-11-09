package quuux.infinity.zui.events

import edu.umd.cs.piccolo.event.{PInputEvent, PBasicInputEventHandler}
import quuux.infinity.zui.Utils.{loadSVG, themePath}
import edu.umd.cs.piccolo.util.PPaintContext
import edu.umd.cs.piccolo.{PCanvas, PNode}
import java.awt.MouseInfo
import quuux.infinity.Infinity


/**
 * Draws a mouse cursor
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 07/02/12
 */

private class ICursorNode() extends PNode {
  val size = 30
  val icon = loadSVG(themePath+"cursor.svg", size)
  setBounds(0,0,size,size)
  setPickable(false)

  override def paint(paintContext:PPaintContext) {
    icon.paint(paintContext.getGraphics)
  }
}


class IMouseCursorEventHandler(canvas:PCanvas) extends PBasicInputEventHandler {
  private val cursor = new ICursorNode
  canvas.getCamera.addChild(cursor)
  cursor.moveToFront()

  override def mouseEntered(event:PInputEvent) { mouseMoved(event) }
  override def mouseDragged(event:PInputEvent) { mouseMoved(event) }
  override def mouseMoved(event:PInputEvent) { cursor.setOffset(event.getPosition) }
}

