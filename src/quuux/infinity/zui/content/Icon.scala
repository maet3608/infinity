package quuux.infinity.zui.content

import org.apache.batik.gvt.GraphicsNode
import org.w3c.dom.svg.SVGDocument
import scala.math.max
import java.awt.geom.AffineTransform
import java.awt.RenderingHints
import java.io.File
import quuux.infinity.Settings
import org.apache.batik.util.XMLResourceDescriptor
import org.apache.batik.dom.svg.SAXSVGDocumentFactory
import org.apache.batik.bridge.{DocumentLoader, UserAgentAdapter, BridgeContext, GVTBuilder}

/**
 * Icon factory for actions. Loads icons and caches them in map
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

object Icon {
  type Icons = (GraphicsNode,GraphicsNode)
  private var icons = Map[String,Icons]()

  def apply(name:String, size:Double, dx:Double, dy:Double):Icons = {
    if(!icons.contains(name))
      icons += name -> loadIcons(name,size,dx,dy)
    icons(name)
  }

  private def buildIcon(builder:GVTBuilder, context:BridgeContext, doc:SVGDocument, opacity:Double, size:Double, dx:Double, dy:Double) = {
    doc.getDocumentElement.setAttribute("opacity",opacity.toString)
	  val icon = builder.build(context, doc)
    val scale = size/max(icon.getPrimitiveBounds.getWidth, icon.getPrimitiveBounds.getHeight)
    icon.setTransform(new AffineTransform(scale,0,0,scale,dx,dy))
    icon.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    icon
  }

  private def loadIcons(name:String, size:Double, dx:Double, dy:Double):Icons = {
    val uri = (new File(Settings.iconsPath,name+".svg")).toURI.toString
    val xmlParser = XMLResourceDescriptor.getXMLParserClassName
    val df = new SAXSVGDocumentFactory(xmlParser)
    val userAgent = new UserAgentAdapter()
    val loader = new DocumentLoader(userAgent)
    val context = new BridgeContext(userAgent, loader)
    context.setDynamicState(BridgeContext.DYNAMIC)
    val builder = new GVTBuilder()
    val iconDefault = buildIcon(builder, context, df.createSVGDocument(uri), 0.7, size, dx, dy)
    val iconSelected = buildIcon(builder, context, df.createSVGDocument(uri), 1.0, size, dx, dy)
    (iconDefault,iconSelected)
  }
}
