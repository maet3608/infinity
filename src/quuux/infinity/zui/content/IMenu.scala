package quuux.infinity.zui.content

import quuux.infinity.Infinity
import quuux.infinity.zui.content.IObject._
import java.awt.geom.Point2D
import edu.umd.cs.piccolo.util.PPaintContext
import xml.{PrettyPrinter, NodeSeq}
import quuux.infinity.zui.Utils._

/**
 * Menus based on items
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

class IMenu(xml:NodeSeq) extends IItem(xml) {
  init("Menu", xml)

  for(item <- xml\\"item")  {
    val itemXML = {<name>{item.text}</name><menu>{this.uid}</menu>} ++ xml\\"position" ++ xml\\"target"
    add(IItem(itemXML), (item\"@location").text.toInt)
  }  

  override def act() { exit() }

  override def exit() {
    forget(this)
    this.removeFromParent()
    this.children.foreach(forget)
  }

  override def toXML:NodeSeq = super.toXML ++ xml

  private def add(item:IItem, location:Int) {
    val gap = 0
    val delta = IItem.backgroundSize+gap
    val n = location-1
    val (x,y) = (n%3-1,1-n/3)
    item.location = location
    addChild(item)
    item.translate(delta*x,delta*y)
  }

  def itemAt(location:Int):Option[IItem] =
    children.toList.collect{case item:IItem => item}.filter(_.location==location).headOption

  def show() {
    val position = new Point2D.Double((xml\\"position"\"@x").text.toDouble, (xml\\"position"\"@y").text.toDouble)
    val pos = Infinity.root.globalToLocal(position)
    Infinity.root.addChild(this)
    translate(pos.getX,pos.getY)
    setGlobalScale(1.0)
    translate(-getWidth/2,-getHeight/2)
  }

  def show(item:IItem) {
    val pos = item.localToGlobal(item.getBoundsReference.getOrigin)
    Infinity.root.globalToLocal(pos)
    Infinity.root.addChild(this)
    translate(pos.getX,pos.getY)
    setGlobalScale(item.getGlobalScale)
  }

  // paint back arrow as overlay
  override def paint(paintContext:PPaintContext) {
    super.paint(paintContext)
    val g2 = paintContext.getGraphics
    if(isSelected) IMenu.backSelected.paint(g2) else IMenu.backDefault.paint(g2)
  }
}


object IMenu {
  type Items = Iterable[(String,Int)]
  private val s = IItem.backgroundSize
  private val (backDefault,backSelected) = Icon("abort_menu", s/6, s/7,s/7)

  def apply(name:String, icon:String, targetUID:String, position:Point2D, items:Items):IMenu = {
    var xml:NodeSeq = {<name>{name}</name><icon>{icon}</icon><target>{targetUID}</target>
                       <position x={position.getX.toString} y={position.getY.toString}/>}
    for((name,location) <- items)
      xml = xml :+ <item location={location.toString}>{name}</item>
    IMenu(xml)
  }

  def apply(xml:NodeSeq):IMenu = new IMenu(xml)
}