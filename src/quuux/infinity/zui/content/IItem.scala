package quuux.infinity.zui.content

import quuux.infinity.zui.content.IObject._
import edu.umd.cs.piccolo.util.PPaintContext
import java.awt.{Font, Color}
import quuux.infinity.utilities.i18n
import quuux.infinity.Infinity
import quuux.infinity.zui.Utils._
import java.awt.geom.{Point2D}
import quuux.infinity.zui.Operation._
import quuux.infinity.zui.events.IHelperLinesEventHandler
import quuux.infinity.zui.FileChooser
import edu.umd.cs.piccolo.event.{PInputEventFilter, PInputEvent, PBasicInputEventHandler}
import java.awt.event.InputEvent
import edu.umd.cs.piccolo.PNode
import xml.{PrettyPrinter, NodeSeq}

/**
 * Menu items
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

// An invisible node on top of the item to define a smaller sensitive area that reacts to mouse events
// this avoids accidental activation of nearby item when using gestures
// Note: For IItemSensors events in IScaleEventHandler, IMenuEventHandler, IDragEventHandler are
// passed on to the underlying item
class IItemSensor(val item:IItem) extends PNode {
  val size = item.getBoundsReference.getWidth
  setBounds(0,0,size/2,size/2)
  translate(size/4,size/4)
  addInputEventListener(new ItemEvent)

  class ItemEvent extends PBasicInputEventHandler {
    setEventFilter(new PInputEventFilter(InputEvent.BUTTON3_MASK&InputEvent.BUTTON1_MASK))
    override def mouseEntered(event:PInputEvent) {
      item.isSelected = true
      item.repaint()
      if(item != item.owner.getOrElse(null) && event.isRightMouseButton) {
        item.act()
        event.setHandled(true)
      }
    }
    override def mouseExited(event:PInputEvent) {
      item.isSelected = false
      item.repaint()
      event.setHandled(true)
    }
    override def mouseReleased(event:PInputEvent) {
      if(!event.isControlDown && !event.isRightMouseButton ) {
        item.act()
        event.setHandled(true)
      }
    }
  }
}


class IItem(xml:NodeSeq) extends IObject {
  init("Item", xml)
  import IItem._
  val (iconDefault,iconSelected) = Icon(get(xml,"icon"), iconSize, dx,dy)
  var transient = get(xml,"transient") match { case "" => true; case str => str.toBoolean }
  val label = i18n(get(xml,"name"))
  val target = IObject(get(xml,"target"))
  var location = 5  // location [1-9] within current menu
  var isSelected = false

  setBounds(0,0,backgroundSize,backgroundSize)
  addChild(new IItemSensor(this))

  // menu that owns this item or none if that menu does not exist anymore
  def owner:Option[IItem] = get(xml,"menu") match {
    case "" => Some(this)
    case uid:String => if(IObject.contains(uid)) Some(IObject(uid).asInstanceOf[IItem]) else None
  }

  def exit() { owner.foreach(_.exit()) }

  override def paint(paintContext:PPaintContext) {
    val g2 = paintContext.getGraphics
    if(isSelected) {
      backgroundSelected.paint(g2)
      iconSelected.paint(g2)
      g2.setColor(IItem.colorSelected)
    } else {
      backgroundDefault.paint(g2)
      iconDefault.paint(g2)
      g2.setColor(IItem.colorDefault)
    }
    g2.setFont(IItem.font)
    val fm = g2.getFontMetrics
    val (w,h) = (fm.stringWidth(label),fm.getDescent)
    g2.drawString(label, ((getWidth-w)/2.0).toFloat, (getHeight-6.0*h).toFloat)
    g2.setFont(IItem.fontLocation)
    g2.drawString(location.toString, (getWidth*0.8).toFloat, (getHeight*0.2).toFloat)
  }

  override def toXML:NodeSeq = super.toXML ++
    {<transient>{transient}</transient><target>{target.uid}</target>} ++
    xml\\"name" ++ xml\\"position" ++ xml\\"icon"

  override def act() { }

  def menu(position:Point2D) = IMenu("action_item", "item", this.uid, position,  List(
    ("action_add_obj", 6),
    ("action_copy", 7),
    ("action_delete", 4),
    ("action_pin", 1),
    ("action_scale", 9),
    ("action_order", 3)
  ))
}



object IItem {
  val (backgroundSize, iconSize) = (80, 38)
  val (dx,dy) = ((backgroundSize-iconSize)/2.0, (backgroundSize-iconSize)/3.0)
  val (backgroundDefault, backgroundSelected) = Icon("background",backgroundSize,0,0)
  val font = new Font("Calibri", Font.PLAIN, 14)
  val fontLocation = new Font("Calibri", Font.PLAIN, 11)
  val colorDefault = new Color(255,255,255,255)
  val colorSelected = new Color(255,255,255,255)


  def apply(xml:NodeSeq):IItem = {
    val position = new Point2D.Double((xml\\"position"\"@x").text.toDouble, (xml\\"position"\"@y").text.toDouble)
    val name = get(xml,"name")
    val targetUID = get(xml,"target")
    def createItem(icon:String, f:(IItem) => Unit) = {
      val itemXML = if(get(xml,"icon")=="") xml++{<icon>{icon}</icon>} else xml
      new IItem(itemXML)  {override def act() { f(this); if(transient) exit() }}
    }  
    def showMenu(item:IItem, name:String, icon:String, items:IMenu.Items) {
      IMenu(name,icon,targetUID,position,items).show(item)}
    def add(obj:IObject) { invokeLater {
      drop(obj, position, IObject(targetUID), false) }}
    val javaCode = "public class Main {\n  public static void main(String[] args) {\n    System.out.println(\"java\");\n  }\n}"

    name match {
      case "action_exit_app_ok" => createItem("okay", item => Infinity.exit())
      case "action_exit_app_cancel" => createItem("cancel", item => {})

      case "action_info" => createItem("info", item => invokeDialog{Infinity.about()})
      case "action_delete" => createItem("minus", item => remove(item.target))
      case "action_delete_clock" => createItem("minus", item => {item.target.asInstanceOf[IClock].stop(); invokeLater{remove(item.target)}})
      case "action_delete_timer" => createItem("minus", item => {item.target.asInstanceOf[ITimer].stop(); invokeLater{remove(item.target)}})
      case "action_start_timer" => createItem("run", item => item.target.asInstanceOf[ITimer].continue())
      case "action_stop_timer" => createItem("stop", item => item.target.asInstanceOf[ITimer].stop())
      case "action_edit_link" => createItem("edit_link", item => invokeLater{item.target.asInstanceOf[IWebLink].toggleEditor()})
      case "action_open_link" => createItem("run", item => invokeLater{item.target.asInstanceOf[IWebLink].act()})
      case "action_open_imagelink" => createItem("run", item => item.target.asInstanceOf[IImageLink].act())
      case "action_edit_formula" => createItem("edit_formula", item => invokeLater{item.target.asInstanceOf[IFormula].toggleEditor()})
      case "action_run_code" => createItem("run", item => ICode.run(item.target.asInstanceOf[ICode]))
      case "action_clear_doc_ok" => createItem("okay", item => invokeLater{Infinity.clear()})
      case "action_clear_doc_cancel" => createItem("cancel", item => {})
      case "action_undo" => createItem("undo", item => undo())
      case "action_pin" => createItem("pin", item => invokeLater{Infinity.togglePin(item.target)})
      case "action_ungroup_group" => createItem("ungroup", item => invokeLater{item.target.asInstanceOf[IGroup].ungroup()})
      case "action_open_group" => createItem("open_group", item => invokeLater{item.target.asInstanceOf[IGroup].toggleOpen()})
      case "action_save_textlink" => createItem("save", item => item.target.asInstanceOf[ITextLink].save())
      case "action_open_textlink" => createItem("run", item => item.target.asInstanceOf[ITextLink].act())
      case "action_order_top" => createItem("order_top", item => invokeLater{item.target.moveToFront()})
      case "action_order_bottom" => createItem("order_bottom", item => invokeLater{item.target.moveToBack()})
      case "action_resize" => createItem("resize", item => invokeLater{item.target.togglePBoundsHandle()})
      case "action_copy" => createItem("copy", item => invokeLater{item.target.copy()})
      case "action_print" => createItem("print", item => invokeDialog{print(item.target)})

      case "action_scale_p4" => createItem("scale_p4", item => item.target.scale(4.0))
      case "action_scale_p3" => createItem("scale_p3", item => item.target.scale(3.0))
      case "action_scale_p2" => createItem("scale_p2", item => item.target.scale(2.0))
      case "action_scale_m4" => createItem("scale_m4", item => item.target.scale(1/4.0))
      case "action_scale_m3" => createItem("scale_m3", item => item.target.scale(1/3.0))
      case "action_scale_m2" => createItem("scale_m2", item => item.target.scale(1/2.0))
      case "action_scale_0"  => createItem("scale_0", item => item.target.setScale(1.0))

      case "action_fading_000" => createItem("fading_000", item => item.target.setFading(1.00f))
      case "action_fading_025" => createItem("fading_025", item => item.target.setFading(0.15f))
      case "action_fading_050" => createItem("fading_050", item => item.target.setFading(0.10f))
      case "action_fading_075" => createItem("fading_075", item => item.target.setFading(0.05f))
      case "action_fading_100" => createItem("fading_100", item => item.target.setFading(0.02f))

      case "action_home" => createItem("home", item => Infinity.home())
      case "action_helplines" => createItem("helplines", item => invokeLater{IHelperLinesEventHandler.toggle()})
      case "action_fullscreen" => createItem("fullscreen", item => invokeDialog{Infinity.toggleFullscreen()})
      case "action_languages" => createItem("languages", item => invokeDialog{Infinity.installLanguages()})

      case "action_load" => createItem("load", item => invokeLater{Infinity.load()})
      case "action_load_from" => createItem("load_from", item => invokeDialog{FileChooser.loadDoc.foreach(uri => Infinity.load(uri))})
      case "action_save" => createItem("save", item => Infinity.save())
      case "action_save_as" => createItem("save_as", item => invokeDialog{FileChooser.saveDoc.foreach(uri => Infinity.save(uri))})
      case "action_save&exit" => createItem("save", item => invokeDialog{Infinity.save(); Infinity.exit()})

      case "action_add_java" => createItem("java", item => add(ICode(javaCode, "Java", "java")))
      case "action_add_python" => createItem("python", item => add(ICode("print('python')", "Python", "python")))
      case "action_add_jython" => createItem("jython", item => add(ICode("print 'jython'", "Jython", "python")))
      case "action_add_pylab" => createItem("pylab", item => add(ICode("hist(randn(1000))", "Pylab", "python")))
      case "action_add_haskell" => createItem("haskell", item => add(ICode("""main = print "haskell"""", "Haskell", "plain")))
      case "action_add_perl" => createItem("perl", item => add(ICode("""print "perl"""", "Perl", "plain")))
      case "action_add_scala" => createItem("scala", item => add(ICode("""println("scala")""", "Scala", "scala")))
      case "action_add_scalala" => createItem("scalala", item => add(ICode("""hist(DenseVector.randn(1000))""", "Scalala", "scala")))
      case "action_add_ruby" => createItem("ruby", item => add(ICode("""puts "ruby"""", "Ruby", "ruby")))
      case "action_add_clojure" => createItem("clojure", item => add(ICode("""(print "clojure")""", "Clojure", "clojure")))
      case "action_add_incanter" => createItem("incanter", item => add(ICode("""(view (histogram (sample-normal 1000)))""", "Incanter", "clojure")))
      case "action_add_r" => createItem("r_simple", item => add(ICode("""print("R")""", "R", "plain")))
      case "action_add_dos" => createItem("dos", item => add(ICode("echo 'dos'", "DOS", "dosbatch")))
      case "action_add_bash" => createItem("bash", item => add(ICode("echo 'bash'", "BASH", "bash")))

      case "action_add_formula" => createItem("formula", item => add(IFormula()))
      case "action_add_textlink" => createItem("file", item => invokeDialog{FileChooser.loadFiles.foreach{uri => add(ITextLink(uri))}})
      case "action_add_imagelink" => createItem("load_graphic", item => invokeDialog{FileChooser.loadImages.foreach{uri => add(IImageLink(uri))}})
      case "action_add_clock" => createItem("clock", item => add(IClock()))
      case "action_add_timer" => createItem("timer", item => add(ITimer()))
      case "action_add_link" => createItem("link", item => add(IWebLink("Infinity","http://www.quuux.com/infinity")))
      case "action_add_rectangle" => createItem("rectangle", item => add(IRectangle()))
      case "action_add_ellipse" => createItem("circle", item => add(IEllipse(50)))

      case "action_add_title" => createItem("title", item => add(IText("title","Title")))
      case "action_add_subtitle" => createItem("subtitle", item => add(IText("subtitle","Subtitle")))
      case "action_add_subsubtitle" => createItem("subsubtitle", item => add(IText("subsubtitle","Subsubtitle")))
      case "action_add_paragraph" => createItem("paragraph", item => add(IText("text","Paragraph")))


      case "action_load_save" => createItem("harddisk", item =>
        showMenu(item,"action_load_save","harddisk", List(
          ("action_save", 1),
          ("action_save_as", 7),
          ("action_load", 3),
          ("action_load_from", 9)
        )))

      case "action_add_text" => createItem("text", item =>
        showMenu(item,"action_add_text","text", List(
          ("action_add_paragraph", 8),
          ("action_add_title", 9),
          ("action_add_subtitle", 6),
          ("action_add_subsubtitle", 3)
        )))

      case "action_add_time" => createItem("time", item =>
        showMenu(item,"action_add_time","time", List(
          ("action_add_clock", 9),
          ("action_add_timer", 3)
        )))

      case "action_add_pythons" => createItem("python", item =>
        showMenu(item,"action_add_pythons","python", List(
          ("action_add_python", 9),
          ("action_add_jython", 3),
          ("action_add_pylab", 6)
        )))

      case "action_add_scalas" => createItem("scala", item =>
        showMenu(item,"action_add_scalas","scala", List(
          ("action_add_scala", 9),
          ("action_add_scalala", 6)
        )))

      case "action_add_clojures" => createItem("clojure", item =>
        showMenu(item,"action_add_clojures","clojure", List(
          ("action_add_clojure", 9),
          ("action_add_incanter", 6)
        )))

      case "action_add_shell" => createItem("shell", item =>
        showMenu(item,"action_add_shell","shell", List(
          ("action_add_dos", 9),
          ("action_add_bash", 3)
        )))

      case "action_add_code" => createItem("code", item =>
        showMenu(item, "action_add_code","code", List(
          ("action_add_ruby", 1),
          ("action_add_haskell", 2),
          ("action_add_clojures", 3),
          ("action_add_perl", 4),
          ("action_add_scalas", 6),
          ("action_add_shell", 7),
          ("action_add_java", 8),
          ("action_add_pythons", 9)
        )))

      case "action_add_obj" => createItem("plus", item =>
        showMenu(item,"action_add_obj","plus", List(
          ("action_add_formula", 1),
          ("action_add_time", 3),
          ("action_add_graphic", 4),
          ("action_add_code", 6),
          ("action_add_textlink", 7),
          ("action_add_text", 8),
          ("action_add_link", 9)
        )))

      case "action_add_graphic" => createItem("graphic", item =>
        showMenu(item,"action_add_graphic","graphic", List(
          ("action_add_imagelink", 4),
          ("action_add_rectangle", 9),
          ("action_add_ellipse", 3)
        )))

      case "action_exit_app" => createItem("power", item =>
        showMenu(item,"action_exit_app","power", List(
          ("action_save&exit", 2),
          ("action_exit_app_ok", 6),
          ("action_exit_app_cancel", 4)
        )))

      case "action_clear_doc" => createItem("litterbin", item =>
        showMenu(item,"action_clear_doc","litterbin", List(
          ("action_clear_doc_ok", 6),
          ("action_clear_doc_cancel", 4)
        )))

      case "action_order" => createItem("order", item =>
        showMenu(item,"action_order","order", List(
          ("action_order_top", 9),
          ("action_order_bottom", 3)
        )))

      case "action_scale" => createItem("scale", item =>
        showMenu(item,"action_scale","scale", List(
          ("action_scale_p4", 9),
          ("action_scale_p3", 6),
          ("action_scale_p2", 3),
          ("action_scale_m4", 7),
          ("action_scale_m3", 4),
          ("action_scale_m2", 1),
          ("action_scale_0", 8)
        )))

      case "action_fading" => createItem("fading", item =>
        showMenu(item,"action_fading","fading", List(
          ("action_fading_000", 8),
          ("action_fading_025", 9),
          ("action_fading_050", 6),
          ("action_fading_075", 3),
          ("action_fading_100", 2)
        )))

      case "action_properties" => createItem("properties", item =>
        showMenu(item,"action_properties","properties",  List(
        ("action_pin", 4),
        ("action_scale", 9),
        ("action_order", 6),
        ("action_resize", 3)
      )))

      case "action_properties_text" => createItem("properties", item =>
        showMenu(item,"action_properties_text","properties",  List(
        ("action_pin", 4),
        ("action_scale", 9),
        ("action_order", 6),
        ("action_fading", 3)
      )))

      case "action_settings" => createItem("settings", item =>
        showMenu(item,"action_settings","settings", List(
          ("action_info", 9),
          ("action_resize", 3),
          //("action_languages", 4),
          ("action_helplines", 1),
          ("action_fullscreen", 7)
        )))

      case _ => throw new IllegalArgumentException("Invalid item name: "+name)
    }
  }

}





