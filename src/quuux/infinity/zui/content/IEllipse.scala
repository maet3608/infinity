package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolo.nodes.PPath
import java.awt.{BasicStroke, Color}
import quuux.infinity.zui.content.IObject._
import java.awt.geom.{Point2D, Ellipse2D}

/**
 * Ellipse object
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

class IEllipse(xml:NodeSeq) extends PPath(IEllipse.bounds2ellipse(xml)) with IObject {
  init("Ellipse",xml)
  setPaint(IEllipse.color)
  setStroke(IEllipse.stroke)

  def act() { togglePBoundsHandle() }

  def menu(position:Point2D) = IMenu("action_ellipse", "circle", uid, position,  List(
    ("action_add_obj", 6),
    ("action_copy", 9),
    ("action_delete", 4),
    ("action_properties", 3)
  ))
}


object IEllipse {
  val color = new Color(250,250,250)
  val stroke = new BasicStroke(0.0f)

  private def bounds2ellipse(xml:NodeSeq)= {
    val b = bounds2rect(xml)
    new Ellipse2D.Double(b.getX, b.getY, b.getWidth, b.getHeight)
  }

  def apply(s:Float=100f):IEllipse = new IEllipse({
    <bounds>{-s/2+","+(-s/2)+","+s+","+s}</bounds>
  })

  def apply(xml:NodeSeq):IEllipse = new IEllipse(xml)
}