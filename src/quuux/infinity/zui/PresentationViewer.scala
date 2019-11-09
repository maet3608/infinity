package quuux.infinity.zui

import javax.swing.JDialog
import quuux.infinity.Infinity
import javax.swing.JButton
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants
import javax.swing.BoxLayout
import javax.swing.JTextField
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.BorderFactory
import javax.swing.ListSelectionModel
import javax.swing.table.TableModel
import javax.swing.table.AbstractTableModel
import javax.swing.JTable
import javax.swing.DropMode
import javax.swing.event.TableModelListener
import javax.swing.event.TableModelEvent
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import scala.collection.mutable.ArrayBuffer
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import java.awt.Dimension
import edu.umd.cs.piccolo.util.PBounds
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.event.DocumentListener
import javax.swing.event.DocumentEvent
import edu.umd.cs.piccolo.activities.PActivity
import quuux.infinity.zui.Utils._
import java.awt.Robot
import java.awt.Point
import scala.xml.NodeSeq
import scala.xml.Node

/**
 * Shows modeless dialog to add, delete and control transitions between views.
 */
object PresentationViewer {
  private var dialog:JDialog = _
  private val model = new Model()
  private val table = new JTable(model)
  private val notesArea = new JTextArea()
    
  private class View(var name:String, var notes:String,
                     val x:Double, val y:Double, val scale:Double,
                     val w:Double, h:Double) {
    
    def this(name:String, notes:String) { 
      this(name, notes,
      Infinity.root.getOffset.getX,
      Infinity.root.getOffset.getY,
      Infinity.root.getGlobalScale,
      Infinity.frame.getSize.getWidth,
      Infinity.frame.getSize.getHeight) 
    } 
    
    def show(duration:Int = 700) {
      val frame = Infinity.frame.getSize  // window size.
      val f = Math.min(frame.getWidth/w, frame.getHeight/h)  // adjust to different window size.
      val activity = Infinity.root.animateToPositionScaleRotation(x*f, y*f, scale*f, 0, duration)
      activity.setDelegate(new ActivityListener())
    }
    
    def toXML:NodeSeq = {
      <view>
        <name xml:space ="preserve">{name}</name>
        <x>{x}</x><y>{y}</y><scale>{scale}</scale><w>{w}</w><h>{h}</h>
        <notes xml:space ="preserve">{notes}</notes>
      </view>
    } 
    
    def this(xml:Node) {
      this(get(xml, "name"), get(xml, "notes"),
           get(xml, "x").toDouble,
           get(xml, "y").toDouble,
           get(xml, "scale").toDouble,
           get(xml, "w").toDouble,
           get(xml, "h").toDouble)
    }
  }
  
  private class Model extends AbstractTableModel  {
    private var views = ArrayBuffer[View]()
   
    def getRowCount():Int = views.size 
    def getColumnCount():Int = 1 
    def getValueAt(row:Int, col:Int):Object = views(row).name
    
    override def getColumnName(col:Int):String = "Views"
    override def isCellEditable(row:Int, col:Int) = true
    override def getColumnClass(c:Int) = getValueAt(0, c).getClass

    override def setValueAt(value:Object, row:Int, col:Int) {
      views(row).name = value.toString
      fireTableCellUpdated(row, col)
    }    
     
    def add(xml:Node) { 
      val row = views.size
      views += new View(xml)
      fireTableRowsInserted(row, row)
    }
    
    def insert(row:Int, name:String) {
      views.insert(row, new View(name, ""))
      fireTableRowsInserted(row, row)
    }
    
    def remove(row:Int) {
      views.remove(row)
      fireTableRowsDeleted(row, row)
    }
    
    def swap(from:Int, to:Int) {
      val temp = views(to)
      views(to) = views(from)
      views(from) = temp
      fireTableCellUpdated(from, 0)
      fireTableCellUpdated(to, 0)
    }   
    
    def clear() { 
      views.clear()
      fireTableDataChanged()
    }
    
    def isRowValid(row:Int) = row >= 0 && row < getRowCount
    def setViewAt(row:Int, view:View) { views(row) = view }
    def getViewAt(row:Int) = views(row) 
    def getView:Option[View] = {
      val row = table.getSelectedRow
      if (model.isRowValid(row)) Option(views(row)) else None
    } 
    
    def toXML = views.map(_.toXML)
  }
  
  private class ActivityListener extends PActivity.PActivityDelegate() {
    def activityStarted(activity:PActivity) { setCursor(Infinity.canvas, "NONE") }
    def activityStepped(activity:PActivity) { }
    def activityFinished(activity:PActivity) { setCursor(Infinity.canvas, "STANDARD") }
  }
  
  private class RowSelectionListener() extends ListSelectionListener {
    private def update(view:View) {
      notesArea.setText(view.notes)
      notesArea.setEnabled(true)
      view.show()
    }
    def valueChanged(e:ListSelectionEvent) {
      if (!e.getValueIsAdjusting) model.getView.foreach(update)
    }
  }
  
  private class AddButtonListener() extends ActionListener {
    def actionPerformed(e:ActionEvent) {
      var row = if(table.getSelectedRow < 0) table.getRowCount else table.getSelectedRow+1
      model.insert(row, "View" + row)
      table.setRowSelectionInterval(row, row)
    } 
  }
  
  private class UpdateButtonListener() extends ActionListener {
    def actionPerformed(e:ActionEvent) {
      val row = table.getSelectedRow      
      if (model.isRowValid(row)) {
        val view = model.getViewAt(row)
        model.setViewAt(row, new View(view.name, view.notes))
      }
    } 
  }  
  
  private class DeleteButtonListener() extends ActionListener {
    def actionPerformed(e:ActionEvent) {
      val row = table.getSelectedRow
      if (model.isRowValid(row)) model.remove(row)
      if (row-1 >= 0) {
        table.setRowSelectionInterval(row-1, row-1)
      } else {
        notesArea.setEnabled(false)
        notesArea.setText("")
      }  
    } 
  }  
  
  private class UpButtonListener() extends ActionListener {
    def actionPerformed(e:ActionEvent) {
      val row = table.getSelectedRow
      if (row <= 0) return
      model.swap(row, row-1)
      table.setRowSelectionInterval(row-1, row-1)
    } 
  }    
  
  private class DownButtonListener() extends ActionListener {
    def actionPerformed(e:ActionEvent) {
      val row = table.getSelectedRow
      if (row >= model.getRowCount-1) return
      model.swap(row, row+1)
      table.setRowSelectionInterval(row+1, row+1)
    } 
  }   
  
  private class NotesListener extends DocumentListener() {
    private def update() { model.getView.foreach(_.notes = notesArea.getText) }
    def changedUpdate(e:DocumentEvent) { update() }
    def removeUpdate(e:DocumentEvent) { update() }
    def insertUpdate(e:DocumentEvent) { update() }
  }
  
  private def selectRowAndPositionCursor(row:Int) {
    table.setRowSelectionInterval(row, row)
    //centerCursor(Infinity.frame)
    val pos = Infinity.frame.getLocationOnScreen;
    (new Robot).mouseMove(pos.x+30, pos.y+50)     // left upper corner.    
  }
  
  def previous() {
    val row = table.getSelectedRow
    if (row > 0) selectRowAndPositionCursor(row-1)
  }  
  
  def next() {
    val row = table.getSelectedRow
    if (row < table.getRowCount-1) selectRowAndPositionCursor(row+1)
  }    
  
  def first() {
    if (table.getRowCount > 0) selectRowAndPositionCursor(0)
  }  
  
  def last() {
    val last = table.getRowCount-1
    if (last >= 0) selectRowAndPositionCursor(last)
  }    
  
  def toXML:NodeSeq = {
    <presentation><views>{model.toXML}</views></presentation>
  } 
  
  def fromXML(xml:Node) {
    model.clear()
    (xml\"presentation"\"views"\"view") foreach {vxml => model.add(vxml) }
  }
  
  def toggle() {
    dialog.setVisible(!dialog.isVisible)
  }
  
  private def create() {
    dialog = new JDialog()
    dialog.setTitle("View controller")
    
    table.getTableHeader().setReorderingAllowed(false)
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    table.setColumnSelectionAllowed(false) 
    table.getSelectionModel().addListSelectionListener(new RowSelectionListener())
    
    val viewsScrollPane = new JScrollPane(table)
    viewsScrollPane.setMinimumSize(new Dimension(100, 50))
    
    val addButton = new JButton(" + ")
    addButton.setToolTipText("Add view")
    addButton.addActionListener(new AddButtonListener())
    
    val updateButton = new JButton(" * ")
    updateButton.setToolTipText("Update view")
    updateButton.addActionListener(new UpdateButtonListener())
    
    val delButton = new JButton(" - ")
    delButton.setToolTipText("Delete view")
    delButton.addActionListener(new DeleteButtonListener())
    
    val upButton = new JButton(" ^ ")
    upButton.setToolTipText("Move view up")
    upButton.addActionListener(new UpButtonListener())
    
    val downButton = new JButton(" v ")
    downButton.setToolTipText("Move view down")
    downButton.addActionListener(new DownButtonListener())
    
    val buttonPane = new JPanel()
    buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS))
    buttonPane.add(addButton)
    buttonPane.add(updateButton)
    buttonPane.add(delButton)
    buttonPane.add(upButton)
    buttonPane.add(downButton)    
    buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5))
    
    val viewsPane = new JPanel()
    viewsPane.setLayout(new BorderLayout)
    viewsPane.add(viewsScrollPane, BorderLayout.CENTER)
    viewsPane.add(buttonPane, BorderLayout.PAGE_END);  
        
    notesArea.setEnabled(false)
    notesArea.setLineWrap(true)
    notesArea.setWrapStyleWord(true)
    notesArea.getDocument.addDocumentListener(new NotesListener())
    val notesScrollPane = new JScrollPane(notesArea)
    notesScrollPane.setMinimumSize(new Dimension(200, 50))
    
    val splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                           viewsPane, notesScrollPane);
    splitPane.setOneTouchExpandable(true);
    splitPane.setDividerLocation(240)

    dialog.setPreferredSize(new Dimension(800, 300))
    dialog.setLayout(new BorderLayout())    
    dialog.add(splitPane, BorderLayout.CENTER)       
    dialog.pack()
  }

  def apply() = {
    create()
  }  
}