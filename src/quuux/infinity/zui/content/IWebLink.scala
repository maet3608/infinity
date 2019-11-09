package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolox.pswing.PSwing
import quuux.infinity.zui.Utils.{get,set}
import javax.swing.event.{DocumentEvent, DocumentListener}
import java.awt.{Color, Font}
import javax.swing.{JTextArea}
import quuux.infinity.Settings
import quuux.infinity.zui.Utils
import java.awt.geom.Point2D


/**
 * Link to a file, a folder, a webpage, ...
 * Link will be opened via start command of cmd (works only on windows)
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */


class IWebLinkEditor(uri:String) extends PSwing(IWebLinkEditor.createTextArea(uri)) {
  private val docChangeListener = new DocChangeListener(this)
  private def textArea = getComponent.asInstanceOf[JTextArea]

  def getText = textArea.getText

  textArea.getDocument.addDocumentListener(docChangeListener)

  class DocChangeListener(linkEditor:IWebLinkEditor) extends DocumentListener() {
    def changed() {
      val link = linkEditor.getParent.asInstanceOf[IWebLink]
      link.uri = getText
    }
    def changedUpdate(e:DocumentEvent) { changed() }
    def removeUpdate(e:DocumentEvent)  { changed() }
    def insertUpdate(e:DocumentEvent) { changed() }
  }
}

object IWebLinkEditor {
  def createTextArea(uri:String) = {
    val jTextArea = new JTextArea(uri)
    jTextArea.setFont(font)
    jTextArea.setOpaque(false)  // no background
    jTextArea.setForeground(paint)
    jTextArea
  }

  val font = new Font("Consolas", Font.PLAIN, 20)
  val paint = Color.gray

  def apply(uri:String):IWebLinkEditor = new IWebLinkEditor(uri)
}



class IWebLink(xml:NodeSeq) extends PSwing(IWebLink.xml2jtext(xml)) with IObject {
  init("Link",xml)
  setGreekThreshold(Settings.greekThreshold)
  var uri =  get(xml,"uri")
  private var label = get(xml,"label")
  private var edit = get(xml,"edit").toBoolean
  private val docChangeListener = new DocChangeListener(this)
  private def textArea = getComponent.asInstanceOf[JTextArea]


  val linkEditor = IWebLinkEditor(uri)
  addChild(linkEditor)
  linkEditor.translate(0, this.getHeight)
  linkEditor.scale(0.5f)
  linkEditor.setVisible(edit)

  textArea.getDocument.addDocumentListener(docChangeListener)

  class DocChangeListener(link:IWebLink) extends DocumentListener() {
    def changed() { link.label = textArea.getText }
    def changedUpdate(e:DocumentEvent) { changed() }
    def removeUpdate(e:DocumentEvent)  { changed() }
    def insertUpdate(e:DocumentEvent) { changed() }
  }

  def getURI = linkEditor.getText

  def toggleEditor() {
    edit = !edit
    linkEditor.setVisible(edit)
  }

  override def act() { Utils.open(uri,this) }

  override def toXML:NodeSeq = super.toXML ++ {
    <uri>{uri}</uri>
    <label>{label}</label>
    <edit>{edit}</edit>
  }


  def menu(position:Point2D) = IMenu("action_link", "link", uid, position,  List(
    ("action_print", 7),
    ("action_edit_link", 2),
    ("action_open_link", 8),
    ("action_add_obj", 6),
    ("action_copy", 9),
    ("action_delete", 4),
    ("action_properties_text", 3)
  ))
}


object IWebLink {
  def xml2jtext(xml:NodeSeq) = {
    val text = get(xml,"label")
    val jTextArea = new JTextArea(text)
    jTextArea.setFont(font)
    jTextArea.setForeground(paint)
    jTextArea.setOpaque(false)
    jTextArea
  }
  val font = new Font("Consolas", Font.PLAIN, 18)
  val paint = new Color(57,88,162)

  def apply(label:String, uri:String, edit:Boolean):IWebLink =
    IWebLink(<label>{label}</label><uri>{uri}</uri><edit>{edit}</edit>)
  def apply(label:String, uri:String):IWebLink =
    apply(label,uri,true)
  def apply(xml:NodeSeq):IWebLink = new IWebLink(xml)
}