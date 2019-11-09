package quuux.infinity

import javax.swing.UIManager._
import utilities.{ErrorHandler, DataTransfer, LanguageInstaller, i18n}
import zui._
import content._
import events._
import zui.Utils._
import javax.swing._
import edu.umd.cs.piccolox.pswing.PSwingCanvas
import java.awt.event.{WindowEvent, WindowAdapter}
import java.io.File
import java.net.URI
import text.JTextComponent
import xml.XML
import java.awt.{Graphics2D, Point, Frame, MouseInfo, Toolkit, RenderingHints}
import java.awt.Cursor



/**
 * Infinity Presentations.
 * Zoomable presentation manager
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 29/08/11
 */
class Infinity {
  Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler)

  i18n.init()  // internationalization of item, menu and message texts

  val settings = Settings
  var frame = new JFrame

  private val canvas = new PSwingCanvas()
  private var root = createRoot(canvas)
  private val listeners = List(IHelperLinesEventHandler(),
    new IDragEventHandler(canvas), new IScaleEventHandler(canvas), 
    new IKeyEventHandler, new IMenuEventHandler, new ICreateGroupEventHandler)

  DataTransfer(canvas)
  initFrame(canvas,true)
  Utils.setCursor(canvas, "STANDARD");
  addLogo()
  removeDefaultEventListeners()
  addEventListeners()
  PresentationViewer()


  private def createRoot(canvas:PSwingCanvas):IRoot = {
    val root = IRoot(Settings.rootSize)
    canvas.getLayer.addChild(root)
    root
  }

  private def setCursor() {
    // all attempts to set a custom cursor lead to really ugly cursors without transparency/antialising
    // therefore an empty custom cursor is set and IMouseCursorEventHandler renders a cursor from an SVG image
    //val toolkit = Toolkit.getDefaultToolkit
    //val image = toolkit.getImage(Utils.themePath+"cursor.png")
    //val icon = loadSVG(Utils.themePath+"cursor.svg",1000)
    //val image = new BufferedImage(32,32, BufferedImage.TYPE_INT_ARGB)  // empty
    //val g2d = image.createGraphics.asInstanceOf[Graphics2D]
    //icon.paint(g2d)
    //g2d.dispose()
    //val cursor = toolkit.createCustomCursor(image, new Point(0,0),"default")
    val cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    canvas.setCursor(cursor)
  }

  private def addLogo() {
    val logo = IImageLink(Utils.themePath+"logo.svg")
    logo.pinned = true
    logo.setOffset(30,30); logo.scale(0.25)
    canvas.getCamera.addChild(logo)
    canvas.repaint()
  }

  /** sets the input listeners */
  protected def addEventListeners()  {
    listeners.foreach(canvas.addInputEventListener)
  }

  /** removes all event listeners */
  protected def removeEventListeners()  {
    listeners.foreach(canvas.removeInputEventListener)
  }

  /** removes all default event listeners */
  protected def removeDefaultEventListeners()  {
    //canvas.getInputEventListeners.foreach(canvas.removeInputEventListener)
    canvas.removeInputEventListener(canvas.getPanEventHandler)
    canvas.removeInputEventListener(canvas.getZoomEventHandler)
  }

  /** sets title and screen size */
  private def initFrame(canvas:PSwingCanvas, setDefaultSize:Boolean) {
    //frame.setTitle("infinity")
    frame.setIconImage(Toolkit.getDefaultToolkit.getImage("resources/themes/default/infinity.png"))
    frame.setContentPane(canvas)
    if(setDefaultSize) {
      frame.setSize(settings.appDefaultSize)
      frame.setLocationRelativeTo(null) // center on screen
    }
    frame.addWindowListener(new WindowAdapter() {
      override def windowClosing(event:WindowEvent) { Infinity.exit() }
    })
    frame.validate()
    frame.setVisible(true)

  }

  /** Switches fullscreen mode on or off */
  def setFullscreen(isFullscreen:Boolean) {
    if(settings.isFullscreen == isFullscreen) return
    if(isFullscreen) settings.oldBounds = frame.getBounds
    frame.dispose()      // get rid of current frame
    frame = new JFrame   // create new frame. Required due to setUndecorated
    frame.setUndecorated(isFullscreen)
    frame.setExtendedState(if(isFullscreen) Frame.MAXIMIZED_BOTH else Frame.NORMAL)
    frame.setBounds(if(isFullscreen) settings.appMaxBounds else settings.oldBounds )
    initFrame(canvas, false)
    settings.isFullscreen = isFullscreen
    canvas.requestFocusInWindow()
  }


}

/** Main */
object Infinity extends App {
  println(Settings.about)
  Settings.load()
  setLookAndFeel(getSystemLookAndFeelClassName)

  // suppresses Batik warning "Graphics2D from BufferedImage lacks BUFFERED_IMAGE hint"
  // google "batik warn_destination false" for more info
  System.setProperty("org.apache.batik.warn_destination", "false")

  // path to VLC player to be able to play media files
  //val libpath: String = "c:/Maet/Software/VLC64"
  //NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName, libpath)

  var documentURI:URI = (new File(cwd, Settings.defaultDoc)).toURI
  def documentDir:String = (new File(documentURI)).getParent

  private val app = new Infinity

  if(args.length==1) {  // load presentation specified on command line
    documentURI = (new File(args(0)).toURI)
    load(documentURI)
  }


  def root:IRoot = app.root
  def frame = app.frame
  def canvas = app.canvas
  def camera = app.canvas.getCamera

  def focusOwner = frame.getFocusOwner

  // returns true if a text element has the focus
  def focusOnText = focusOwner.isInstanceOf[JTextComponent]

  // = right mouse button pressed: see Utils.invokeDialog, IMenuEventHandler
  var gesturing = false

  def mousePosition = {
    val compPos = app.canvas.getLocationOnScreen
    val mousePos = MouseInfo.getPointerInfo.getLocation
    mousePos.translate(-compPos.x,-compPos.y)
    mousePos
  }

  // opens dialog to install programming languages
  def installLanguages() {
    LanguageInstaller.loadLanguageJARs("resources/languages")
    val msg = LanguageInstaller.languageNames.mkString("Installed:\n", "\n", "\n")
    JOptionPane.showMessageDialog(frame, msg)
  }

  // go to home position
  def home() {
    root.setScale(1.0)
    root.setOffset(0,0)
  }
  
  // show about information
  def about() {
    JOptionPane.showMessageDialog(frame, Settings.about,
      "About information", JOptionPane.INFORMATION_MESSAGE)
  }

  // delete all objects
  def clearAll() {
    app.canvas.getLayer.removeAllChildren()    // delete canvas
    app.canvas.getCamera.removeAllChildren()   // delete pinned objects
    if(IHelperLinesEventHandler.showHelpLines) // helplines active?
        IHelperLinesEventHandler.show()        // restore helplines
    IObject.forgetAll()                        // clear memory
  }
  
  // delete all objects and recreate root
  def clear() {
    clearAll()
    val root = IRoot(Settings.rootSize)
    app.canvas.getLayer.addChild(root)
    app.root = root
  }

  // exit the application
  def exit() {
    Settings.save()
    System.exit(0)
  }

  // switch to fullscreen and back
  def toggleFullscreen() {
    app.setFullscreen(!app.settings.isFullscreen)
  }

  def pin(obj:IObject) {
    val scale = obj.getGlobalScale
    attach(obj,camera)
    obj.setGlobalScale(scale)
    obj.setTransparency(0.5f)
    obj.pinned = true
  }

  def unpin(obj:IObject, scaleToDestination:Boolean=false) {
    val scale = obj.getGlobalScale
    val dest = underneath(obj)
    attach(obj,dest)
    if(scaleToDestination) obj.setGlobalScale(dest.getGlobalScale) else obj.setGlobalScale(scale)
    obj.setTransparency(1.0f)
    obj.pinned = false
  }

  def togglePin(obj:IObject) {
    if(obj.pinned) unpin(obj) else pin(obj)
  }

  def unpinLast() { pinnedObjects.lastOption.foreach(unpin(_)) }
  def unpinAll() { pinnedObjects.foreach(unpin(_)) }


  def save(uri:URI = documentURI) {
    documentURI = uri
    //frame.setTitle(uri2path(documentURI))
    val xml = addXMLHeader(toDeepXML(root)++pinnedObjects.map(toDeepXML)++PresentationViewer.toXML)
    //prettyPrint(xml)
    backup(uri)
    XML.save((new File(uri)).toString, xml, "UTF-8")
    Notification("saved",500)
  }

  def load(uri:URI = documentURI) {
    documentURI = uri
    //frame.setTitle(uri2path(documentURI))
    clearAll()
    val notification = Notification("loading ...")
    notification.repaint()
    invokeLater{
      val xml = XML.load(uri.toURL)
      //prettyPrint(xml)
      app.root = Utils.fromDeepXML(xml).asInstanceOf[IRoot]
      notification.remove()
      PresentationViewer.fromXML(xml)
    }
  }

}