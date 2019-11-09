package quuux.infinity.zui

import content.{IObject, IItem}
import edu.umd.cs.piccolo.PNode
import collection.mutable.Stack


// Undoable operations, currently only remove
object Operation {
  val stack = Stack[(PNode,PNode)]()

  def remove(obj:PNode) {
    obj match {
      case item:IItem if item.transient =>  // ignore menu items
      case _ => stack.push((obj.getParent, obj))
    }
    obj.removeFromParent()
    IObject.forget(obj.asInstanceOf[IObject])
  }

  def undo() {
    if(stack.isEmpty) return
    val (parent, obj) = stack.pop()
    parent.addChild(obj)
    IObject.remember(obj.asInstanceOf[IObject])
  }
}