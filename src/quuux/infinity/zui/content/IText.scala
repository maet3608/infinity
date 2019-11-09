package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolox.pswing.PSwing
import java.awt.Font
import quuux.infinity.zui.Utils.{get,set}
import quuux.infinity.Settings
import java.awt.geom.Point2D
import javax.swing.{JEditorPane, JTextArea}


/**
 * Editable text object
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

class IText(xml:NodeSeq) extends PSwing(IText.xml2jtext(xml)) with IObject {
  init("Text",xml)
  setGreekThreshold(if(isTitle) Settings.greekThreshold/10 else Settings.greekThreshold)
  val level = get(xml,"level")

  def getText = getComponent.asInstanceOf[JTextArea].getText

  def isTitle = get(xml,"level").contains("title")

  override def toXML:NodeSeq = super.toXML ++ {
    <level>{level}</level>
    <text xml:space ="preserve">{getText}</text>
  }

  def act() { copy() }

  def menu(position:Point2D) = IMenu("action_text", "text", uid, position,  List(
    ("action_print", 7),
    ("action_add_obj", 6),
    ("action_copy", 9),
    ("action_delete", 4),
    ("action_properties_text", 3)
  ))
}

object IText {
  def xml2jtext(xml:NodeSeq) = {
    val jTextArea = new JTextArea(get(xml,"text"))
    val font = get(xml,"level") match {
      case "paragraph" => fontParagraph
      case "title" => fontTitle
      case "subtitle" => fontSubtitle
      case "subsubtitle" => fontSubsubtitle
      case _ => fontParagraph
    }
    jTextArea.setFont(font)
    jTextArea.setOpaque(false)
    jTextArea
  }
  val fontParagraph = new Font("Calibri", Font.PLAIN, 18)
  val fontTitle = new Font("Calibri", Font.PLAIN, 80)
  val fontSubtitle = new Font("Calibri", Font.PLAIN, 40)
  val fontSubsubtitle = new Font("Calibri", Font.PLAIN, 22)

  def apply(level:String, text:String):IText = IText(<text>{text}</text><level>{level}</level>)
  def apply(xml:NodeSeq):IText = new IText(xml)
}