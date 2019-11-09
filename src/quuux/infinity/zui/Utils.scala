package quuux.infinity.zui

import content.ICode._
import content.{IMenu, IItem, IObject}
import edu.umd.cs.piccolo.PNode
import javax.swing.SwingUtilities
import java.io._
import java.nio.file.Paths
import edu.umd.cs.piccolo.util.PBounds
import java.lang.{Double, Thread}
import sys.process.{Process, ProcessIO}
import java.net.URI
import org.apache.batik.gvt.GraphicsNode
import scala.math.max
import java.awt.{Robot, Toolkit, AWTEvent, RenderingHints}
import java.awt.geom.{Point2D, AffineTransform}
import collection.JavaConversions._
import quuux.infinity.{Settings, Infinity}
import xml.{Node,NodeSeq, XML, PrettyPrinter}
import collection.mutable
import quuux.infinity.Infinity._
import java.awt.event.InputEvent
import java.awt.print.{Book, PageFormat, PrinterJob}
import java.awt.Cursor
import java.awt.Point
import java.awt.image.BufferedImage
import edu.umd.cs.piccolox.pswing.PSwingCanvas
import javax.swing.JFrame


/**
 * A utility class
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 24/10/11
 */

object Utils {
  // create invisible cursor
  val cursorHotSpot = new Point(0,0);
  val invisibleCursorImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB); 
  val invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(
      invisibleCursorImage, cursorHotSpot, "InvisibleCursor");     
  
  // simulation of events
  val robot = new Robot()

  // returns the current working directory
  def cwd = System.getProperty("user.dir")

  // base path to the theme folder
  def themePath = cwd+"/"+Settings.themesPath+Settings.theme+"/"

  // pretty printing of XML
  def prettyPrint(xml:Node) {
    println((new PrettyPrinter(120,2)).format(xml)+"\n\n")
  }

  // returns the filename for the given path
  def getFileName(path:String, withExtension:Boolean = true):String = {
    val name = (new File(path)).getName
    if(!withExtension) {
      val idx = name.lastIndexOf('.')
      if (idx>=0) return name.substring(0,idx)
    }
    name
  }

  // returns the extension (without dot and in lowercase) of the given file or an empty string
  def getExtension(path:String) = {
    val name = path.toLowerCase
    val idx = name.lastIndexOf('.')
    if (idx<0) "" else name.substring(idx+1)
  }


  // returns the depth of the obj within the scene graph
  def depth(node:PNode, d:Int=0):Int = if(node.getParent == null) d else depth(node.getParent, d+1)

  // same as depth() above but runs over IObjects only
  def depthObj(obj:IObject, d:Int=0):Int = obj.parent match {
    case Some(p) => depth(p, d+1)
    case None => d
  }

  // performs a deep copy of the given obj and attaches it to the at node
  def deepCopy(obj:IObject, at:PNode, shift:Boolean = false):IObject = {
    val cp = IObject(obj.toXML)     // copy has new uid
    cp match {                      // copied items get attached to root
      case item:IItem => item.transient = false; obj.getParent.addChild(item); attach(item, Infinity.root)
      case _ => at.addChild(cp)
    }
    if(shift) cp.translate(cp.getWidth/4,cp.getHeight/4)
    for(child <- obj.children.toList)
      deepCopy(child, cp)
    cp
  }

  // extracts value from xml for the given name
  def get(xml:NodeSeq, key:String) = (xml \\ key).text

  def set(xml:NodeSeq, key:String, f:(String) => Unit) { (xml \\ key) match {
     case ns:NodeSeq if ns.length == 0 => // do nothing
     case ns:NodeSeq => f(ns.text)
    }
  }

  def addXMLHeader(objects:Any):Node = {
    <Document>
      <meta>
        <software-name>{"infinity"}</software-name>
        <software-version>{Settings.version}</software-version>
        <software-date>{Settings.date}</software-date>
        <document-uri>{documentURI}</document-uri>
        <document-time>{System.currentTimeMillis}</document-time>
      </meta>
      {objects}
    </Document> }

  // returns deep copy of the given object as XML node
  def toDeepXML(obj:IObject):NodeSeq = obj match {
    case item:IItem if item.transient => NodeSeq.Empty  // don't save temporary menu items
    case _ => <Object>{obj.toXML}</Object> ++ obj.children.flatMap(toDeepXML)
  }

  // same as above but returns XML string with header
  def toDeepXMLStr(obj:IObject):String = addXMLHeader(toDeepXML(obj)).toString()

  // creates objects from the given XML. Returns top object
  def fromDeepXML(xml:Node):IObject = {
    val objects = mutable.LinkedHashMap[String,Entry]()
    case class Entry(xml:NodeSeq) {
      val obj = IObject(xml)          // create new object
      val puid = (xml\"parent").text  // uid of parent object
    }
    def link(entry:Entry) {
      val parent:Option[PNode] = entry match {
        case e if e.obj.pinned => Some(Infinity.camera)
        case e if e.puid.isEmpty => Some(Infinity.canvas.getLayer)
        case e => objects.get(e.puid).map(_.obj)
      }
      parent.foreach(_.addChild(entry.obj))
    }
    objects ++= ((xml\"Object") map {oxml => (get(oxml,"uid"),Entry(oxml))})
    objects.values.foreach(link)
    objects.values.head.obj
  }

  // same as above but XML is provided as string.
  def fromDeepXMLStr(xmlStr:String):IObject = fromDeepXML(XML.loadString(xmlStr))

  // returns all content objects on the layer (not pinned ones)
  def layerObjects = collectionAsScalaIterable(Infinity.root.getAllNodes).collect{case obj:IObject => obj}

  // returns all pinned objects
  def pinnedObjects = collectionAsScalaIterable(Infinity.camera.getChildrenReference).collect{case obj:IObject => obj}

  // returns top most object (has the largest depth) or root object if objects is empty
  def top(objects:Iterable[IObject]) =
    if(objects.isEmpty ) Infinity.root else objects.reduceLeft((a,b) => if(depth(a)>depth(b)) a else b)

  // returns the object underneath the given object that has the given objects in its bounds
  def underneath(obj:PNode) = {
    val b = obj.getGlobalBounds
    top(layerObjects.filter(o => o!=obj && o.getGlobalBounds.contains(b) && o.getVisible ))
  }

  // returns the object underneath the given position (excluding pinned objects)
  def underneath(x:Double, y:Double) =
    top(layerObjects.filter(o => o.getGlobalBounds.contains(x,y) && o.getVisible))
    
  // returns the object underneath the mouse pointer (excluding pinned objects)  
  def underneathMouse() = {
    val mousePos = Infinity.mousePosition
    underneath(mousePos.x, mousePos.y)   
  }  

  // returns the current menu visible or None
  def currentMenu:Option[IMenu] =
    layerObjects.collect{case menu:IMenu => menu}.headOption

  // returns the top most parent of the object (or the object) that is in bounds or None otherwise
  def topParent(obj:IObject, bounds:PBounds):Option[IObject] = {
    var last:Option[IObject] = None
    var curr:Option[IObject] = Some(obj)

    while(curr != None && bounds.contains(curr.get.getGlobalBounds) && curr.get.getVisible) {
      last = curr
      curr = curr.get.parent
    }
    last
  }

  // (re)attaches an object to destination object preserving object scaling and location
  def attach(obj:PNode, dest:PNode) {
    if(obj.getParent != null) {
      val offset = obj.getParent.localToGlobal(obj.getOffset)
      dest.globalToLocal(offset)
      obj.setOffset(offset)
    }
    dest.addChild(obj)
  }

  // drops the given obj at the current mouse pos and attaches it to the object underneath
  def drop(obj:PNode) {
    drop(obj, Infinity.mousePosition, true)
  }

  // drops the given obj at the specified global position and attaches it to the object underneath.
  // center = true => object gets centered at position
  def drop(obj:PNode, position:Point2D, center:Boolean) {
    val pos = new Point2D.Double(position.getX, position.getY)
    val under = underneath(pos.x, pos.y)
    drop(obj,pos,under,center)
  }

  // drops the given obj at the specified global position and attaches it to the at object.
  // center = true => object gets centered at position
  def drop(obj:PNode, position:Point2D, at:PNode, center:Boolean) {
    val pos = new Point2D.Double(position.getX, position.getY)
    at.globalToLocal(pos)
    at.addChild(obj)
    if(center) pos.setLocation(pos.x-obj.getWidth/2, pos.y-obj.getHeight/2) else pos.setLocation(pos.x, pos.y)
    obj.setOffset(pos)
  }


  // starts a new thread that runs the given code
  def invokeThread(code: => Unit) {
    new Thread( new Runnable { def run() {code} } ).start()
  }

  // invokes the given code on the event dispatch thread
  def invokeLater(code: => Unit) {
    SwingUtilities.invokeLater( new Runnable { def run() {code} })
  }

  // like invokeLater. Use when code creates Swing dialog or anything that causes frame to loose focus
  // simulates release of right mouse button after code execution to avoid event handling problems
  // when opening swing dialog (change focus) with menu gesture where right mouse button remains pressed
  def invokeDialog(code: => Unit) {
    invokeLater{
      code
      if(Infinity.gesturing) robot.mouseRelease(InputEvent.BUTTON3_MASK)
    }
  }

  // invokes the given code and waits. Cannot be called on event dispatch thread!
  def invokeAndWait(code: => Unit) {
    SwingUtilities.invokeAndWait( new Runnable { def run() {code} })
  }

  // note: URI.relativize does not create paths such as ../../file, see bug 6226081
  // therefore the following ugly workaround
  def relativize(uri:URI):URI =
    path2uri(Paths.get(documentDir).relativize(Paths.get(uri)).toString)

  // resolves the given URI against the document path
  // again a work around for the issue relating to bug 6226081
  def resolve(uri:String):URI =
    Paths.get(documentDir).resolve(Paths.get(uri.replace("file:///","").replace("%20"," "))).toUri

  // converts absolute or relative filepath to URI
  def path2uri(filepath:String):URI =
    URI.create(filepath.replace("\\","/").replace(" ","%20"))
    //(new URI("file",null,null,0,"///"+filepath.replace("\\","/"),null,null))

  // converts uri to absolute or relative filepath
  def uri2path(uri:URI):String =
    uri.toString.replace("file:///"," ").replace("file:/"," ").replace("%20"," ")

  // returns a valid URI to an existing file or throws an exception
  def retrieveURI(xml:NodeSeq):(URI,Boolean) = {
    def exists(uri:URI) = (new File(uri)).exists
    val relativeURI = resolve(get(xml,"uri-relative"))
    val absoluteURI = path2uri(get(xml,"uri-absolute"))
    if (exists(relativeURI)) return (relativeURI, true)
    if (exists(absoluteURI)) return (absoluteURI, false)
    throw new IOException("File does not exist: "+relativeURI)
  }

  // loads the entire file as a string
  def readFile(uri:URI) = {
    val source = scala.io.Source.fromFile(uri)
    val text = source.mkString
    source.close()
    text
  }

  // writes the entire text to the given path
  def writeFile(uri:URI, text:String) {
    val writer = new BufferedWriter(new FileWriter(new File(uri)))
    writer.write(text)
    writer.close()
  }

  // copies a file
  def copy(src:File, dest:File) {
    val (fis,fos) = (new FileInputStream(src), new FileOutputStream(dest))
    fos.getChannel.transferFrom(fis.getChannel, 0, Long.MaxValue )
    fis.close(); fos.close()
  }

  // creates a backup file
  def backup(uri:URI) {
    val src = new File(uri)
    if(src.exists()) copy(src, new File(src.toString+".bak"))
  }

  // returns the parent directory of the given file
  def dir(filepath:String) = (new File(filepath)).getParent match {
    case path:String => path
    case _ => "."
  }

  // opens the application associated with the path/uri (only under windows). Path/URI can be a file path, an url, or anything
  // the windows "start" command operates on.  In case of an error an output node is created and will be attached
  // to the given object node to show the error message.
  def open(uri:URI, obj:IObject) { open(uri.toString, obj) }
  def open(path:String, obj:IObject) {
    try {
      val pio = new ProcessIO(_ => (), stdout => (),  stderr => showOutput(obj,stderr,"stderr"))
      val cwd = new File(Infinity.documentDir)
      val cmd = "cmd /c start \"\" \"%s\"".format(path.trim)
      Process(cmd, cwd).run(pio)
    } catch {
      case e:Exception => showOutput(obj,e.toString,"stderr")
    }
  }

  /** loads an image in SVG format from the given URI. If size > 0 the image is scaled to the given size  */
  def loadSVG(uri:URI, size:Double):GraphicsNode = {
    import org.apache.batik.dom.svg.SAXSVGDocumentFactory
    import org.apache.batik.util.XMLResourceDescriptor
    import org.apache.batik.bridge._

    val xmlParser = XMLResourceDescriptor.getXMLParserClassName
    val df = new SAXSVGDocumentFactory(xmlParser)
    val doc = df.createSVGDocument(uri.toString)
    val userAgent = new UserAgentAdapter()
    val loader = new DocumentLoader(userAgent)
    val context = new BridgeContext(userAgent, loader)
    context.setDynamicState(BridgeContext.DYNAMIC)
    val builder = new GVTBuilder()
    val icon = builder.build(context, doc)
    if(size > 0) {
      val scale = size/max(icon.getPrimitiveBounds.getWidth, icon.getPrimitiveBounds.getHeight)
      icon.setTransform(new AffineTransform(scale,0,0,scale,0,0))
    }
    icon.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    icon
  }

  /** loads an image in SVG format from the given filepath, If size > 0 the image is scaled to the given size  */
  def loadSVG(filepath:String, size:Double=0.0):GraphicsNode =
    loadSVG((new File(filepath)).toURI, size)


  /** prints an object */
  def print(obj:IObject) {
    val printJob: PrinterJob = PrinterJob.getPrinterJob
    val pageFormat: PageFormat = printJob.defaultPage
    val book: Book = new Book
    book.append(obj, pageFormat)
    printJob.setPageable(book)
    printJob.setJobName(getFileName(Infinity.documentURI.getPath, false))
    if (printJob.printDialog) printJob.print()
  }
  
  /** Sets the cursor type: STANDARD, SYSTEM, CROSSHAIR, NONE */
  def setCursor(canvas:PSwingCanvas, shape:String) {
    val cursor = shape match {
      //case "STANDARD" => Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      case "STANDARD" => Cursor.getDefaultCursor()  // Default for infinity
      case "SYSTEM" => Cursor.getDefaultCursor()
      case "CROSSHAIR" => Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
      case "NONE" => invisibleCursor
      case _ => return;
    }
    canvas.setCursor(cursor)    
  }
  
  /** Centers the mouse within the frame */
  def centerCursor(frame:JFrame) {
    val pos = frame.getLocationOnScreen;
    val x:Int = pos.x + (frame.getWidth / 2);
    val y:Int = pos.y + (frame.getHeight / 2);
    (new Robot).mouseMove(x, y)
  }
}


