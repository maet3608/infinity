package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolo.util.PPaintContext
import javax.imageio.ImageIO
import java.io.File
import java.awt.{Font, Graphics2D}
import quuux.infinity.zui.{SynchronizedLink, Utils}
import quuux.infinity.zui.Utils._
import java.awt.geom.Point2D
import java.net.URI
import java.awt.image.BufferedImage
import org.apache.batik.gvt.GraphicsNode
import collection.JavaConversions._


/**
 * Image object. Image is linked to a file.
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */


abstract class ImageRenderer {
  def update(uri:URI)
  def width:Double
  def height:Double
  def paint(g2:Graphics2D)
}

private class RasterRenderer(uri:URI) extends ImageRenderer {
  var image:BufferedImage = _
  var width:Double  = _
  var height:Double = _

  update(uri)

  def update(uri:URI) {
    image = ImageIO.read(uri.toURL)
    if (image == null) throw new RuntimeException("Can't read image "+uri)
    width = image.getWidth(null)
    height = image.getHeight(null)
  }

  def paint(g2:Graphics2D) {
    if(image == null) return
    g2.drawImage(image, 0,0, null)
  }
}


private class SVGRendererBatik(uri:URI) extends ImageRenderer {
  var icon:GraphicsNode = _
  var width:Double = _
  var height:Double = _

  update(uri)

  def update(uri:URI) {
    icon = Utils.loadSVG(uri,0.0)
    width = icon.getPrimitiveBounds.getWidth
    height = icon.getPrimitiveBounds.getHeight
  }

  def paint(g2:Graphics2D) {
    icon.paint(g2)
  }
}


class IImageLink(xml:NodeSeq) extends IObject with SynchronizedLink {
  init("ImageLink", xml)
  private val (uri,_) = retrieveURI(xml)
  private def isSVG(uri:URI) = uri.toString.toLowerCase.endsWith(".svg")
  private val renderer = if(isSVG(uri)) new SVGRendererBatik(uri) else new RasterRenderer(uri)

  setBounds(0,0,renderer.width,renderer.height)
  watchFile(uri)

  def update() {
    renderer.update(uri)
    invalidatePaint()
  }

  def act() { Utils.open(uri,this) }

  override def paint(paintContext:PPaintContext) {
    renderer.paint(paintContext.getGraphics)
  }

  override def toXML:NodeSeq = super.toXML ++
    { <uri-absolute>{uri}</uri-absolute> <uri-relative>{relativize(uri)}</uri-relative> }


  def menu(position:Point2D) = IMenu("action_imagelink", "load_graphic", uid, position,  List(
    ("action_print", 7),
    ("action_open_imagelink", 8),
    ("action_add_obj", 6),
    ("action_copy", 9),
    ("action_delete", 4),
    ("action_properties", 3)
  ))
}


object IImageLink {
  val font = new Font("Calibri", Font.PLAIN, 18)

  def apply(path:String):IImageLink = IImageLink(new File(path))
  def apply(file:File):IImageLink = IImageLink(file.toURI)
  def apply(uri:URI):IImageLink =
    IImageLink(<uri-absolute>{uri}</uri-absolute><uri-relative>{relativize(uri)}</uri-relative>)
  def apply(xml:NodeSeq):IImageLink = new IImageLink(xml)
}
