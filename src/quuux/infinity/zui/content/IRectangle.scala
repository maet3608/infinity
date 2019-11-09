package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolo.nodes.PPath
import java.awt.{BasicStroke, Color}
import quuux.infinity.zui.content.IObject._
import java.awt.geom.Point2D

/**
 * A rectangle object
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

class IRectangle(xml:NodeSeq) extends PPath(bounds2rect(xml)) with IObject {
  init("Rectangle", xml)
  setPaint(IRectangle.color)
  setStroke(IRectangle.stroke)

  def act() { togglePBoundsHandle() }

  def menu(position:Point2D) = IMenu("action_rectangle", "rectangle", this.uid, position,  List(
    ("action_add_obj", 6),
    ("action_copy", 9),
    ("action_delete", 4),
    ("action_properties", 3)
  ))
}


object IRectangle {
  val color = new Color(250,250,250)
  val stroke = new BasicStroke(0.0f)

  def apply(s:Float=100f):IRectangle = new IRectangle({
    <bounds>{-s/2+","+(-s/2)+","+s+","+s}</bounds>
  })

  def apply(xml:NodeSeq):IRectangle = new IRectangle(xml)
}
