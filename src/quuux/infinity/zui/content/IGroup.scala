package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolo.nodes.PPath
import quuux.infinity.zui.Utils._
import quuux.infinity.zui.content.IObject._
import edu.umd.cs.piccolo.util.PPaintContext
import quuux.infinity.Infinity
import java.awt.geom.Point2D
import quuux.infinity.zui.Operation
import edu.umd.cs.piccolox.pswing.PSwing
import java.awt.{BasicStroke, Color, Font, Graphics2D}
import edu.umd.cs.piccolox.pswing.PSwing._
import edu.umd.cs.piccolo.PNode
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.{JComponent, JTextArea}


/**
 * Group of objects
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

class IGroupLabel(component:JComponent) extends PSwing(component)


class IGroup(xml:NodeSeq) extends PPath(bounds2rect(xml)) with IObject {
  init("Group", xml)
  var open = get(xml,"open").toBoolean
  var opaque = true

  private val label = IGroup.createLabel(get(xml,"label"))
  private def labelText = label.getComponent.asInstanceOf[JTextArea].getText
  private def updateChildren() { children foreach { _.setVisible(open) } }
  private val openedBounds = str2bounds(get(xml,"opened-bounds"))
  private val closedBounds = str2bounds(get(xml,"closed-bounds"))

  setPaint(IGroup.paint)  // enables dragging when clicking into frame. Group remains transparent if not set opaque
  addChild(label)
  label.translate((xml\\"label"\"@x").text.toDouble,(xml\\"label"\"@y").text.toDouble)
  label.setScale((xml\\"label"\"@scale").text.toDouble)

  // make children that are added to closed group (e.g. during loading) invisible
  addPropertyChangeListener(PNode.PROPERTY_CHILDREN, new PropertyChangeListener() {
    def propertyChange(evt: PropertyChangeEvent) { updateChildren() }
  })

  def act() { toggleOpen() }
  
  def toggleOpen() {
    open = !open
    updateChildren()
    if(open)
      { closedBounds.setRect(getBoundsReference); setBounds(openedBounds) }
    else
      { openedBounds.setRect(getBoundsReference); setBounds(closedBounds) }
  }

  def ungroup() {
    val dest = underneath(this)    
    children.toList foreach {obj => obj.setVisible(true); attach(obj,dest)}
    Operation.remove(this)
    dest.repaint()
  }

  override def paint(paintContext:PPaintContext) {
    val cb = Infinity.camera.getFullBoundsReference
    val g2: Graphics2D = paintContext.getGraphics
    if(opaque) {
      g2.setPaint(IGroup.paint)
      g2.fill(getPathReference)
    }
    // only draw frame if completely visible in camera or if group is closed
    if(!open || cb.contains(getGlobalFullBounds)) {
      g2.setPaint(IGroup.strokePaint)
      g2.setStroke(IGroup.stroke)
      g2.draw(getPathReference)
    }
  }

  override def toXML:NodeSeq = super.toXML ++ {
    <label x={label.getXOffset.toString} y={label.getYOffset.toString} scale={label.getScale.toString}>{labelText}</label>
    <open>{open}</open>
    <opened-bounds>{rect2str(openedBounds)}</opened-bounds>
    <closed-bounds>{rect2str(closedBounds)}</closed-bounds>
  }

  def menu(position:Point2D) = IMenu("action_group", "group", this.uid, position,  List(
    ("action_print", 7),
    ("action_open_group", 8),
    ("action_ungroup_group", 2),
    ("action_add_obj", 6),
    ("action_copy", 9),
    ("action_delete", 4),
    ("action_properties", 3)
  ))
}


object IGroup {
  val paint = IRoot.paint
  val strokePaint = Color.gray
  val stroke = new BasicStroke(0f)
  val labelFont = new Font("Calibri", Font.PLAIN, 14)
  val labelPaint = Color.lightGray

  protected def createLabel(label:String):IGroupLabel = {
    val jTextArea = new JTextArea(label)
    jTextArea.setFont(labelFont)
    jTextArea.setForeground(labelPaint)
    jTextArea.setOpaque(false)
    new IGroupLabel(jTextArea)
  }

  def apply(x:Double,y:Double,w:Double,h:Double):IGroup = new IGroup({
    <label x="5" y="5" scale="1.0">group</label>
    <open>true</open>
    <bounds>{x+","+y+","+w+","+h}</bounds>
    <opened-bounds>{x+","+y+","+w+","+h}</opened-bounds>
    <closed-bounds>{x+","+y+","+w+","+30}</closed-bounds>
  })
  def apply(xml:NodeSeq):IGroup = new IGroup(xml)
}


