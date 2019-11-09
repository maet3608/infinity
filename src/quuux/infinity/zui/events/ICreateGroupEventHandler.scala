package quuux.infinity.zui.events

import java.awt.event.InputEvent._
import edu.umd.cs.piccolo.nodes.PPath
import java.awt.geom.Point2D
import edu.umd.cs.piccolo.event.{PInputEvent, PInputEventFilter, PBasicInputEventHandler}
import java.awt.{Color, BasicStroke}
import quuux.infinity.zui.Utils._
import quuux.infinity.Infinity
import edu.umd.cs.piccolo.util.{PBounds}
import edu.umd.cs.piccolo.{PLayer, PNode, PCanvas}
import quuux.infinity.zui.content.{IObject, IGroup, IRoot}
import quuux.infinity.zui.Utils.layerObjects
import quuux.infinity.zui.Operation

/**
 * Creates a group with the objects under the selection rectangle
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 26/10/11
 */


class ICreateGroupEventHandler extends PBasicInputEventHandler {
  setEventFilter(new PInputEventFilter(BUTTON1_MASK))
  private var rectangle:PPath = null
  private var startPoint:Point2D = null
  private var node:PNode = null
  private var layer:PLayer = null
  private val stroke = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, Array(2,3), 0)

  override def mousePressed(event:PInputEvent) {
    node = event.getPickedNode 
    if(!(node.isInstanceOf[IRoot] || node.isInstanceOf[IGroup])) return
    layer = (event.getComponent).asInstanceOf[PCanvas].getLayer
    startPoint = Infinity.mousePosition
    rectangle = new PPath()
    rectangle.setStroke(stroke)
    rectangle.setPaint(null)
    rectangle.setStrokePaint(Color.black)
    rectangle.setTransparency(0.7f)
    layer.addChild(rectangle)
    // don't do setHandled(true) otherwise context menu doesn't appear
  }

  override def mouseDragged(event:PInputEvent) {
    if(startPoint != null) updateRectangle(Infinity.mousePosition)
  }

  override def mouseReleased(event:PInputEvent) {
    val pos = Infinity.mousePosition
    if(pos == null || node == null || startPoint == null) return
    if(startPoint.distance(pos) < 15.0) { layer.removeChild(rectangle); return }
    updateRectangle(pos)

    // find objects on layer within selection
    val rb = rectangle.getGlobalBounds
    val d = depth(node)
    val selected = layerObjects.map(topParent(_,rb)).collect{case Some(obj) if depth(obj) > d => obj}.toSet[IObject]
    if(selected.size > 0) {
      val dest = underneath(rectangle)
      val scale = dest.getGlobalScale
      val group = IGroup(0,0,rectangle.getWidth/scale,rectangle.getHeight/scale)
      group.setOffset(rectangle.getX, rectangle.getY)
      val offset = dest.globalToLocal(group.getOffset)
      group.setOffset(offset)
      dest.addChild(group)
      selected.foreach(obj => attach(obj, group))
    }

    layer.removeChild(rectangle)
    node = null
    startPoint = null
    event.setHandled(true)
  }

  private def updateRectangle(endPoint:Point2D) {
    if(endPoint == null) return
    val b = new PBounds()
    b.add(startPoint)
    b.add(endPoint)
    rectangle.setPathTo(b)
  }
}