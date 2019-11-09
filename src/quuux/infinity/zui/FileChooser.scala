package quuux.infinity.zui

import javax.swing.JFileChooser
import quuux.infinity.Infinity
import java.net.URI
import quuux.infinity.Settings.documentExt
import javax.swing.filechooser.{FileNameExtensionFilter, FileFilter}


/**
 * Creates file choosers
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 24/10/11
 */

object FileChooser {
  private def create(title:String, button:String, fileFilter:FileFilter) = {
    val fc = new JFileChooser(Infinity.documentDir)
    fc.setDialogTitle(title)
    fc.setApproveButtonText(button)
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY)
    if(fileFilter != null) fc.setFileFilter(fileFilter)
    fc
  }

  // single file selection
  private def single(title:String, button:String, fileFilter:FileFilter):Option[URI] = {
    val fc = create(title, button, fileFilter)
    fc.setMultiSelectionEnabled(false)
    fc.showOpenDialog(Infinity.frame)  match {
       case JFileChooser.APPROVE_OPTION =>  Some(fc.getSelectedFile.toURI)
       case _ => None
    }
  }

  // multiple file selection
  private def multiple(title:String, button:String, fileFilter:FileFilter):Array[URI] = {
    val fc = create(title, button, fileFilter)
    fc.setMultiSelectionEnabled(true)
    fc.showOpenDialog(Infinity.frame)  match {
       case JFileChooser.APPROVE_OPTION => fc.getSelectedFiles.map(_.toURI)
       case _ => Array[URI]()
    }
  }

  def loadDoc = single("Load document ...", "Load", new FileNameExtensionFilter("Documents", documentExt))
  def saveDoc = single("Save document ...", "Save", new FileNameExtensionFilter("Documents", documentExt))
  def loadFiles = multiple("Load files ...", "Load", null)
  def loadImages = multiple("Load image ...", "Load",
    new FileNameExtensionFilter("Images", "gif", "jpg", "jepg", "tiff", "tif", "png", "svg"))

}