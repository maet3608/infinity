package quuux.infinity.zui.events

import java.awt.event.InputEvent._
import edu.umd.cs.piccolo.event.{PInputEvent, PInputEventFilter, PBasicInputEventHandler}
import quuux.infinity.zui.content.{IItemSensor, IObject}
import quuux.infinity.zui.Utils._
import quuux.infinity.Infinity


/**
 * Context menu for objects
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 26/10/11
 */

class IMenuEventHandler extends PBasicInputEventHandler {
  setEventFilter(new PInputEventFilter(BUTTON3_MASK))
  override def mousePressed(event:PInputEvent) {
    Infinity.gesturing = true
    def showMenu(obj:IObject) { obj.menu(Infinity.mousePosition).show(); event.setHandled(true) }
    event.getPickedNode match {
      case picked:IItemSensor => showMenu(picked.item)
      case picked:IObject => showMenu(picked)
      case _ =>
    }
  }

  override def mouseReleased(event:PInputEvent) {
    Infinity.gesturing = false
  }
}
