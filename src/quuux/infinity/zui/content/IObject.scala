package quuux.infinity.zui.content

import edu.umd.cs.piccolo.PNode
import scala.collection.JavaConversions._
import edu.umd.cs.piccolox.handles.PBoundsHandle
import quuux.infinity.zui.content.IObject._
import java.awt.geom.{Point2D, Rectangle2D}
import scala.Predef._
import java.awt.{Color}
import quuux.infinity.zui.Utils._
import edu.umd.cs.piccolo.event.{PInputEvent, PBasicInputEventHandler}
import java.util.UUID
import edu.umd.cs.piccolo.util.PBounds
import xml.{PrettyPrinter, NodeSeq}
import quuux.infinity.Infinity


trait IObject extends PNode {
  var uid:String = null          // needs to be set during object initialization
  var pinned = false             // pinned to the canvas or not
  private var fading = 1.0f      // fading level, 1.0 = no fading, 0.0 = fully transparent
  private var fadingListener:PBasicInputEventHandler = null

  /** default action of this object, e.g. when double-clicked */
  def act()

  /** returns the context menu for this object */
  def menu(position:Point2D):IMenu

  /** toggles handles for resizing */
  def togglePBoundsHandle() {
    getChildrenReference.find(n => n.isInstanceOf[PBoundsHandle]) match {
      case Some(node) => PBoundsHandle.removeBoundsHandlesFrom(this)
      case None => PBoundsHandle.addBoundsHandlesTo(this); resizeBoundsHandles()
    }
  }

  def setFading(newFading:Float) { 
    fading = newFading
    if(fading < 1f && fadingListener == null) {
      fadingListener = new FadingListener(this)
      addInputEventListener(fadingListener)
    }
    if(fading == 1f && fadingListener != null) {
      removeInputEventListener(fadingListener)
      fadingListener = null
    }
    setTransparency(fading)
  }

  /** Resize bounds handles */
  protected def resizeBoundsHandles() { asScalaIterator(getChildrenIterator).
    filter(_.isInstanceOf[PBoundsHandle]).
    map(_.asInstanceOf[PBoundsHandle]).toList.foreach{_.setGlobalScale(0.5)} }


  def toXML:NodeSeq = {
    val xml = {
      <kind>{getName}</kind>
      <uid>{uid}</uid>
      <offset>{point2str(getOffset)}</offset>
      <bounds>{rect2str(getBoundsReference)}</bounds>
    }
    parent.foreach(p =>       xml &+ <parent>{p.uid}</parent>)
    if(getPaint != null)      xml &+ <paint>{color2str(getPaint.asInstanceOf[Color])}</paint>
    if(pinned)                xml &+ <pinned>{pinned}</pinned>
    if(fading < 1.0f)         xml &+ <fading>{fading}</fading>
    if(getScale != 1f)        xml &+ <scale>{getScale}</scale>
    if(getTransparency != 1f) xml &+ <transparency>{getTransparency}</transparency>
    xml
  }


  def init(kind:String, xml:NodeSeq):IObject = {
    setName(kind)
    uid = generateUID
    set(xml, "fading", v => setFading(v.toFloat))
    set(xml, "pinned", v => pinned = v.toBoolean)
    set(xml, "bounds", v => setBounds(str2rect(v)))
    set(xml, "offset", v => setOffset(str2point(v)))
    set(xml, "scale", v => setScale(v.toFloat))
    set(xml, "paint", v => setPaint(str2color(v)))
    set(xml, "transparency", v => setTransparency(v.toFloat))
    remember(this)
    this
  }

  def copy(shift:Boolean = true) = deepCopy(this, this.getParent, shift)

  def children = asScalaIterator(getChildrenIterator).collect{case obj:IObject => obj}

  def parent:Option[IObject] = getParent match {
      case p:IObject => Some(p)
      case _ => None
    }

  override def toString = (new PrettyPrinter(100,2)).formatNodes(toXML)
}



object IObject {
  // map of all existing objects
  private var _objects = Map[String,IObject]()

  // iterator over all objects
  def objects = _objects.valuesIterator

  // true if object with the given uid is remembered
  def contains(uid:String) = _objects.contains(uid)

  // remembers created objects
  def remember(obj:IObject) { _objects += obj.uid -> obj }

  // removes the given object from the memory
  def forget(obj:IObject) { _objects -= obj.uid }

  // removes all objects from memory
  def forgetAll() {_objects = Map[String,IObject]() }

  // creates a unique identifier
  def generateUID = UUID.randomUUID.toString

  // returns the object with the given uid or the root if the object does not exist
  def apply(uid:String):IObject = _objects.getOrElse(uid, Infinity.root)

  // create an object from its xml description
  def apply(xml:NodeSeq):IObject = get(xml,"kind") match {
      case "Root" => IRoot(xml)
      case "Group" => IGroup(xml)
      case "Rectangle" => IRectangle(xml)
      case "Ellipse" => IEllipse(xml)
      case "Text" => IText(xml)
      case "Formula" => IFormula(xml)
      case "Code" => ICode(xml)
      case "Output" => IOutput(xml)
      case "ImageLink" => IImageLink(xml)
      case "TextLink" => ITextLink(xml)
      case "Clock" => IClock(xml)
      case "Timer" => ITimer(xml)
      case "Link" => IWebLink(xml)
      case "Item" => IItem(xml)
      case _ => throw new IllegalArgumentException("Invalid kind: "+get(xml,"kind"))
    }


  def color2str(c:Color) = c.getRed+","+c.getGreen+","+c.getBlue+","+c.getAlpha
  def str2color(str:String) = str.split(",").map(_.toInt) match { case Array(r,g,b,a) => new Color(r,g,b,a) }
  def point2str(p:Point2D) = p.getX+","+p.getY
  def str2point(str:String) = str.split(",").map(_.toFloat) match { case Array(x,y) => new Point2D.Float(x,y) }
  def bounds2rect(xml:NodeSeq)= str2rect(get(xml, "bounds"))
  def str2rect(str:String) = str.split(",").map(_.toFloat) match { case Array(x,y,w,h) => new Rectangle2D.Double(x,y,w,h) }
  def rect2str(r:Rectangle2D) =  r.getX+","+r.getY+","+r.getWidth+","+r.getHeight
  def str2bounds(str:String) = new PBounds(str2rect(str))


  class FadingListener(obj:IObject) extends PBasicInputEventHandler {
    private var oldAlpha:Float = obj.getTransparency
    override def mouseEntered(event:PInputEvent)  {
      obj.setTransparency(oldAlpha)
    }
    override def mouseExited(event:PInputEvent) {
      obj.setTransparency(obj.fading)
    }
  }
}





