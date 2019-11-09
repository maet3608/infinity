package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolo.nodes.PPath
import java.awt.{BasicStroke, Color}
import quuux.infinity.zui.content.IObject._
import java.awt.geom.Point2D
import quuux.infinity.zui.events.IHelperLinesEventHandler


/**
 * The root object for all other objects
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

class IRoot(xml:NodeSeq) extends PPath(bounds2rect(xml)) with IObject {
  init("Root", xml)
  setPaint(IRoot.paint)
  setStroke(IRoot.stroke)

  def act() { IHelperLinesEventHandler.toggle() }

  def menu(position:Point2D) = IMenu("action_doc", "document", uid, position,  List(
    ("action_home", 8),
    ("action_undo", 1),
    ("action_add_obj", 6),
    ("action_load_save", 9),
    ("action_clear_doc", 4),
    ("action_settings", 3),
    ("action_print", 7),
    ("action_exit_app", 2)
  ))
}



object IRoot {
  //val paint = new Color(250,250,250)
  val paint = Color.white
  val stroke = new BasicStroke(0.0f)

  def apply(s:Float=100f):IRoot = new IRoot({
    <bounds>{-s/2+","+(-s/2)+","+s+","+s}</bounds>
  })

  def apply(xml:NodeSeq):IRoot = new IRoot(xml)

}