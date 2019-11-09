package quuux.infinity.utilities

import java.io.{ByteArrayInputStream, File, InputStream}
import edu.umd.cs.piccolo.{PNode, PCanvas}

import java.awt.datatransfer._
import java.awt.dnd._

import collection.JavaConversions._
import javax.swing.JOptionPane
import quuux.infinity.Infinity
import quuux.infinity.zui.{Notification, Utils}
import quuux.infinity.zui.content._


/**
 * Classes and methods to transfer IObjects via drag&drop and copy&paste
 * Version: 1.00
 * Date   : 25/02/13
 */


/** A transferable object that wraps an IObject and provides various data flavors */
class IObjectSelection(obj:IObject) extends Transferable {
  import IObjectSelection._

  private var flavors = List(FlavorInfinity, FlavorXML)
  if(!text.isEmpty) flavors ::= FlavorText
  if(!latex.isEmpty) flavors ::= FlavorLatex

  private def text:String = obj match {
    case obj:IText => obj.getText
    case obj:IOutput => obj.getText
    case obj:ICode => obj.getCode
    case obj:IWebLink => obj.getURI
    case obj:IFormula => obj.getFormula
    case _ => ""
  }

  private def latex:String = obj match {
    case obj:IFormula => obj.getFormula
    case _ => ""
  }

  def getTransferDataFlavors:Array[DataFlavor] = flavors.toArray

  def isDataFlavorSupported(flavor:DataFlavor):Boolean = flavors.contains(flavor)

  def getTransferData(flavor:DataFlavor):Object = flavor match {
    case FlavorInfinity => Utils.toDeepXMLStr(obj)
    case FlavorXML => Utils.toDeepXMLStr(obj)
    case FlavorLatex => latex
    case FlavorText => text
    case _ => throw new Exception("Unsupported flavor: "+flavor)
  }
}

/** The different flavors that are supported */
object IObjectSelection {
  val FlavorInfinity = new DataFlavor("application/x-infinity+xml;class=java.lang.String")
  val FlavorXML      = new DataFlavor("application/xml;class=java.lang.String")
  val FlavorLatex    = new DataFlavor("application/x-latex;class=java.lang.String")
  val FlavorText     = new DataFlavor("text/plain;class=java.lang.String")
}


/** Copy and paste support */
object ClipBoard {
  val toolkit = java.awt.Toolkit.getDefaultToolkit
  val clipboard = toolkit.getSystemClipboard

  def write(obj:IObject) {
    clipboard.setContents(new IObjectSelection(obj), null)
    Notification("copied", 300)
  }

  def read() {
    val contents:Transferable = clipboard.getContents(null)
    if(contents != null) DataTransfer.readContents(contents)
  }
}

/** Drag and drop support. */
class DragDropSupport(canvas:PCanvas) extends DropTargetListener  {
  new DropTarget(canvas, this)

  def dragEnter(dtde:DropTargetDragEvent) {}
  def dragExit(dtde:DropTargetEvent)  {}
  def dragOver(dtde:DropTargetDragEvent)  {}
  def dropActionChanged(dtde:DropTargetDragEvent)  {}

  def drop(dtde:DropTargetDropEvent)  {
    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE)
    try {
      DataTransfer.readContents(dtde.getTransferable)
    } catch {
      case e:Exception => JOptionPane.showMessageDialog(Infinity.frame,
        "Drag&Drop not supported for this data type!",
        "Drag&Drop error", JOptionPane.ERROR_MESSAGE)
    }
    dtde.dropComplete(true)
  }
}



/** Takes a transferable object extracts the most appropriate data flavor and creates an IObject for it */
object DataTransfer {
  private val mime = """(.+); +class=([^;]+).*""".r
  private var canvas:PCanvas = _

  def apply(pcanvas:PCanvas) = {
    canvas = pcanvas
    new DragDropSupport(canvas)
  }


  def addFiles(files:Seq[File]) {
    var pos = Infinity.mousePosition
    val at = Utils.underneath(pos.getX, pos.getY)
    def dropMulti(obj:PNode) {
      Utils.drop(obj,pos,at,files.size==1)     // center if only one file
      pos.translate(obj.getWidth.toInt+10, 0)  // shift if multiple files
    }

    val isImage = Set("png","bmp","wbmp","jpg", "jpeg","gif","svg")
    val isInfinity  = Set("ipx")

    for(file <- files)  {
      val ext = Utils.getExtension(file.getAbsolutePath)
      if(isImage contains ext)
        dropMulti(IImageLink(file))
      else if(isInfinity contains ext)
        Infinity.load(file.toURI)
      else
        dropMulti(ITextLink(file.toURI))
    }
  }

  def readContents(contents:Transferable) {
    val flavors = contents.getTransferDataFlavors
    //flavors.map(_.getMimeType).foreach(println)

    def isMimeFileList(flavor:DataFlavor) = flavor.getMimeType.contains("application/x-java-file-list; class=java.util.List")
    def isMimeText(flavor:DataFlavor) = flavor.getMimeType.contains("text/plain; class=java.lang.String")
    def isMimeHTML(flavor:DataFlavor) = flavor.getMimeType.contains("text/html; class=java.lang.String")
    def isMimeURL(flavor:DataFlavor)  = flavor.getMimeType.contains("class=java.net.URL")
    def isMimeIObject(flavor:DataFlavor)  = flavor.getMimeType.contains("application/x-infinity+xml")

    def isText = flavors.exists(isMimeText)
    def isURL = flavors.exists(isMimeURL)
    def isHTML = flavors.exists(isMimeHTML)
    def isFileList = flavors.exists(isMimeFileList)
    def isIObject = contents.isDataFlavorSupported(IObjectSelection.FlavorInfinity)
    def isBitImage = contents.isDataFlavorSupported(DataFlavor.imageFlavor)
    def isInputStream = flavors.exists(_.isRepresentationClassInputStream)

    def getAs[T](flavor:DataFlavor) = contents.getTransferData(flavor).asInstanceOf[T]
    def getFileList = getAs[java.util.List[File]](flavors.filter(_.isFlavorJavaFileListType).head)
    def getBitImage = getAs[java.awt.Image](DataFlavor.imageFlavor)
    def getURL = getAs[java.net.URL](flavors.filter(isMimeURL).head)
    def getHTML = getAs[java.lang.String](flavors.filter(isMimeHTML).head)
    def getText = getAs[java.lang.String](flavors.filter(isMimeText).head)
    def getIObject = getAs[java.lang.String](IObjectSelection.FlavorInfinity)
    def getInputStream = getAs[InputStream](flavors.filter(_.isRepresentationClassInputStream).head)


    if(isIObject)
      Utils.drop(Utils.fromDeepXMLStr(getIObject))
    else if(isURL) {
      try {
        val url = getURL
        Utils.drop(IWebLink(url.toString, url.toURI.toString, edit=false))}
      catch {
        case e:Exception =>  Utils.drop(IText("paragraph", getText)) }
    }
    //else if(isHTML)
    //  println(getHTML)
    else if(isText)
      Utils.drop(IText("paragraph", getText))
    else if(isFileList)
      addFiles(getFileList)
    //else if(isBitImage)
    //  println("bitImage")
    //else if(isInputStream) {
    //   val reader = new BufferedReader(new InputStreamReader(getInputStream))
    //   println(reader.readLine())
    //   reader.close()
    // }
    else
      throw new RuntimeException("Unsupported data flavor: "+flavors.mkString("\n"))
  }  
}







