package quuux.infinity.zui.content

import xml.NodeSeq
import quuux.infinity.zui.Utils.{get,set}
import edu.umd.cs.piccolo.util.PPaintContext
import org.scilab.forge.jlatexmath.{TeXConstants, TeXIcon, TeXFormula}
import java.awt.{Insets, Color, Font}
import edu.umd.cs.piccolox.pswing.PSwing
import javax.swing.JTextArea
import javax.swing.event.{DocumentEvent, DocumentListener}
import java.awt.geom.Point2D

/**
 * Display of formulas (using JLatexMath)
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */


protected class IFormulaEditor(latex:String) extends PSwing(IFormulaEditor.createTextArea(latex)) {
  private val docChangeListener = new DocChangeListener(this)
  textArea.getDocument.addDocumentListener(docChangeListener)
  private def textArea = getComponent.asInstanceOf[JTextArea]

  def getText = textArea.getText

  class DocChangeListener(formulaEditor:IFormulaEditor) extends DocumentListener() {
    def changed() {
      val formula = formulaEditor.getParent.asInstanceOf[IFormula]
      formula.update(getText, true)
    }
    def changedUpdate(e:DocumentEvent) { changed() }
    def removeUpdate(e:DocumentEvent)  { changed() }
    def insertUpdate(e:DocumentEvent) { changed() }
  }

}

protected object IFormulaEditor {
  def createTextArea(latex:String) = {
    val jTextArea = new JTextArea(latex)
    jTextArea.setFont(font)
    //jTextArea.setBackground(background)
    jTextArea.setOpaque(false)  // no background
    jTextArea.setForeground(paint)
    jTextArea
  }

  val font = new Font("Consolas", Font.PLAIN, 20)
  val paint = Color.gray
  //val background = Color.white

  def apply(latex:String):IFormulaEditor = new IFormulaEditor(latex)
}



class IFormula(xml:NodeSeq) extends IObject {
  init("Formula", xml)
  var latex = get(xml,"latex")
  var edit = get(xml,"edit").toBoolean
  var icon:TeXIcon = _

  update(latex)

  val formulaEditor = IFormulaEditor(latex)
  addChild(formulaEditor)
  formulaEditor.translate(0, icon.getIconHeight)
  formulaEditor.scale(0.5f)
  formulaEditor.setVisible(edit)

  def getFormula = formulaEditor.getText

  def act() { toggleEditor() }
  
  def toggleEditor() {
    edit = !edit
    formulaEditor.setVisible(edit)
  }

  def update(newLatex:String, doRepaint:Boolean=false) {
    latex = newLatex
    val formula = new TeXFormula(true, newLatex)
    icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, 20)
    icon.setForeground(IFormula.color)
    setBounds(0,0,icon.getIconWidth,icon.getIconHeight)
    if(doRepaint) invalidatePaint()
  }

  override def paint(paintContext:PPaintContext) {
    val g2 = paintContext.getGraphics
    icon.paintIcon(null, g2, 0, 0)
  }

  override def toXML:NodeSeq =
    super.toXML ++ { <latex xml:space ="preserve">{latex}</latex><edit>{edit}</edit> }

  def menu(position:Point2D) = IMenu("action_formula", "formula", uid, position,  List(
    ("action_print", 7),
    ("action_edit_formula", 2),
    ("action_add_obj", 6),
    ("action_copy", 9),
    ("action_delete", 4),
    ("action_properties", 3)
  ))
}

object IFormula {
  val color = Color.black
  def apply():IFormula = IFormula(<latex>{"""y = \sum_i^n x_i"""}</latex><edit>true</edit>)
  def apply(xml:NodeSeq):IFormula = new IFormula(xml)
}