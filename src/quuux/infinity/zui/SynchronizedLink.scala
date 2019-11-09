package quuux.infinity.zui

import java.net.URI
import quuux.infinity.zui.Utils._
import java.io.{IOException, File}
import java.nio.file.{FileSystems, Paths}
import java.nio.file.StandardWatchEventKinds._
import collection.JavaConversions._
import xml.NodeSeq

/**
 * A link to a file that updates the display of the corresponding IObject if the file changes,
 * e.g. links to text files or images
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 2/03/13
 */
trait SynchronizedLink {
  private var isWatching = true

  // called when linked file has changed. Should update display of file content
  protected def update()

  // starts file watcher
  protected def watchFile(uri:URI) {
    val absolutePath = (new File(uri)).toString
    val filename = getFileName(absolutePath)
    val dir = Paths.get(absolutePath).getParent
    val watcher = FileSystems.getDefault.newWatchService()
    var key = dir.register(watcher, ENTRY_MODIFY)
    invokeThread {
      while(isWatching) {
        key = watcher.take()
        for(event <- collectionAsScalaIterable(key.pollEvents()))
          if(event.kind == ENTRY_MODIFY && event.context.toString == filename ) {
            Thread.sleep(200)  // wait for file modifications to be finished
            invokeLater{ update() }
          }
        key.reset()
      }
      key.cancel()
    }
  }

  // stops file watcher
  override def finalize() { isWatching = false  }

}
