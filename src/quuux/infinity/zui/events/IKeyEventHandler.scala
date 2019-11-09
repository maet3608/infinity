package quuux.infinity.zui.events


import java.awt.event.InputEvent
import java.awt.event.KeyEvent._
import quuux.infinity.Infinity
import edu.umd.cs.piccolo.PNode
import edu.umd.cs.piccolo.event.{PInputEvent, PBasicInputEventHandler}
import quuux.infinity.zui.Operation._
import quuux.infinity.zui.content._
import quuux.infinity.utilities.{ClipBoard, Momentum}
import quuux.infinity.zui.Utils._
import quuux.infinity.zui.PresentationViewer


/**
 * Acquires keyboard focus for nodes under mouse,
 * highlights menu items and handles faded objects
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 26/10/11
 */

class IKeyEventHandler extends PBasicInputEventHandler {
  private val hMomentum = new Momentum(Infinity.root.translate(_,0))
  private val vMomentum = new Momentum(Infinity.root.translate(0,_))

  override def keyReleased(event:PInputEvent)  {
    //val currObj = event.getPickedNode  // bug: returns null instead of object 
    val currObj = underneathMouse
    def handled() { event.setHandled(true) }
    if(event.getModifiers == InputEvent.CTRL_MASK) {
      event.getKeyCode match {
        case VK_Z if !Infinity.focusOnText => undo(); handled()      // CTRL-Z: undo
        case VK_S =>  currObj match {      // CTRL-S: save presentation
          case textLink:ITextLink => textLink.save(); handled()
          case _ => Infinity.save(); handled()
        }
        case VK_L =>  Infinity.load(); handled()       // CTRL-L: load presentation
        case VK_R => currObj match {      // CTRL-R: run code
          case code:ICode => code.act(); handled()
          case _ =>
        }
        case VK_D => currObj match {      // CTRL-D: delete object
          case root:IRoot =>              // do not delete root!
          case _ => remove(currObj); handled()
        }
        case VK_P => currObj match {      // CTRL-P:  pin object
          case root:IRoot =>              // do not pin root!
          case obj:IObject => Infinity.pin(obj); handled()
          case _ =>
        }
        case VK_U => currObj match {      // CTRL-U: unpin objects
          case obj:IObject => Infinity.unpinLast(); handled()
          case _ =>
        }
        case VK_C => currObj match {      // CTRL-C: copy object to clipboard          
          case obj:IRoot =>  // don't copy root
          case obj:IObject if !Infinity.focusOnText => ClipBoard.write(obj); handled()
          case _ =>
        }
        case VK_V => currObj match {      // CTRL-V: paste object
          case obj:IObject if !Infinity.focusOnText => ClipBoard.read(); handled()
          case _ =>
        }

        case _ =>
      }
    } else if(event.getModifiers == InputEvent.ALT_MASK) {
      event.getKeyCode match {
        case _ =>
      }
    } else {
      event.getKeyCode match {
        case VK_F1 => PresentationViewer.toggle(); handled()          // F1: open viewer
        //case VK_HOME => PresentationViewer.first(); handled()         // first view
        //case VK_END => PresentationViewer.last(); handled()           // last view
        case VK_PAGE_UP => PresentationViewer.previous(); handled()   // previous view
        case VK_PAGE_DOWN => PresentationViewer.next(); handled()     // next view    
        case _ =>
      }
    }
  }

  override def keyPressed(event:PInputEvent)  {
    //val currObj = event.getPickedNode  // bug: returns null instead of object 
    val currObj = underneathMouse match {
      case obj:IObject => obj
      case _ => return
    }
    
    def handled() { event.setHandled(true) }
    def isNumeric(key:Int) =
      (key>=VK_NUMPAD0 && key<=VK_NUMPAD9) || (key>=VK_0 && key<=VK_9)
    def key2loc(key:Int) =
      if(key>=VK_NUMPAD0 && key<=VK_NUMPAD9) key-VK_NUMPAD0 else key-VK_0
    def is5(key:Int) = isNumeric(key) && key2loc(key)==5

    if(event.getModifiers == InputEvent.CTRL_MASK) {
      event.getKeyCode match {
        case key if is5(key) => currentMenu match {     // CTRL-5: open or close context menu
          case Some(menu) =>  menu.exit(); handled()    // close menu
          case None => currObj.menu(Infinity.mousePosition).show(); handled() // open
        }
        case key if isNumeric(key) => currentMenu match {  // CTRL-Number, menu items
          case Some(menu) => menu.itemAt(key2loc(key)).foreach(_.act()); handled()
          case None =>
        }
        case _ =>
      }
    }
    else {
      currObj match {
        case obj:IObject if !Infinity.focusOnText => event.getKeyCode match {
          case VK_UP    => vMomentum.inc(+1); handled()
          case VK_DOWN  => vMomentum.inc(-1); handled()
          case VK_LEFT  => hMomentum.inc(+1); handled()
          case VK_RIGHT => hMomentum.inc(-1); handled()          case _ =>
        }        
        case _ =>
      }
    }
  }

  override def mouseEntered(event:PInputEvent)  {
    event.getInputManager.setKeyboardFocus(this)
  }

  override def mouseExited(event:PInputEvent) {
    event.getInputManager.setKeyboardFocus(null)
  }
}