package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolox.pswing.PSwing
import quuux.infinity.zui.Utils._
import javax.swing.JTextArea
import edu.umd.cs.piccolo.nodes.PText
import javax.swing.event.{DocumentEvent, DocumentListener}
import java.awt.{Color, Font}
import collection.JavaConversions._
import java.awt.geom.Point2D
import quuux.infinity.zui.{SynchronizedLink, Utils}
import quuux.infinity.Settings
import java.net.URI


class ITextLink(xml:NodeSeq) extends PSwing(ITextLink.xml2jtext(xml)) with IObject with SynchronizedLink {
  init("TextLink",xml)
  setGreekThreshold(Settings.greekThreshold)
  private val (uri,isRelative) = retrieveURI(xml)
  private val docChangeListener = new DocChangeListener(this)
  textArea.getDocument.addDocumentListener(docChangeListener)
  private def textArea = getComponent.asInstanceOf[JTextArea]
  watchFile(uri)
  addTitle()

  class DocChangeListener(textLink:ITextLink) extends DocumentListener() {
    def changed() { textLink.textArea.setForeground(ITextLink.paintChanged) }
    def changedUpdate(e:DocumentEvent) { changed() }
    def removeUpdate(e:DocumentEvent)  { changed() }
    def insertUpdate(e:DocumentEvent) { changed() }
  }

  def update() {
    textArea.getDocument.removeDocumentListener(docChangeListener)
    textArea.setText(readFile(uri))
    textArea.getDocument.addDocumentListener(docChangeListener)
  }

  override def toXML:NodeSeq = super.toXML ++
    { <uri-absolute>{uri}</uri-absolute> <uri-relative>{relativize(uri)}</uri-relative> }

  private def addTitle()  {
    val uri = URI.create(if(isRelative) get(xml,"uri-relative") else get(xml,"uri-absolute"))
    val title = new PText(uri2path(uri))
    title.setTextPaint(ITextLink.titlePaint)
    title.setFont(ITextLink.titleFont)
    title.setPickable(false)
    addChild(title)
    title.translate(0,-title.getHeight)
  }

  override def act() { Utils.open(uri,this) }

  def save() {
    writeFile(uri, textArea.getText)
    textArea.setForeground(ITextLink.paint)
  }

  def menu(position:Point2D) = IMenu("action_textlink", "file", uid, position,  List(
    ("action_print", 7),
    ("action_open_textlink", 8),
    ("action_save_textlink", 2),
    ("action_add_obj", 6),
    ("action_copy", 9),
    ("action_delete", 4),
    ("action_properties_text", 3)
  ))
}


object ITextLink {

  def xml2jtext(xml:NodeSeq) = {
    val (uri,_) = retrieveURI(xml)
    val text = readFile(uri)
    val jTextArea = new JTextArea(text)
    jTextArea.setFont(font)
    jTextArea.setBackground(background)
    jTextArea.setForeground(paint)
    jTextArea
  }

  val font = new Font("Consolas", Font.PLAIN, 18)
  val paint = Color.black
  val paintChanged = Color.lightGray
  val background = Color.white
  val titleFont = new Font("Consolas", Font.PLAIN, 8)
  val titlePaint = Color.gray

  def apply(uri:URI):ITextLink =
    ITextLink(<uri-absolute>{uri}</uri-absolute><uri-relative>{relativize(uri)}</uri-relative>)
  def apply(xml:NodeSeq):ITextLink = new ITextLink(xml)
}