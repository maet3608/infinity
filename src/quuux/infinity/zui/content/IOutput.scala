package quuux.infinity.zui.content

import xml.NodeSeq
import edu.umd.cs.piccolo.nodes.PText
import java.awt.{Font, Color}
import quuux.infinity.zui.Utils.{get,set}
import quuux.infinity.Settings
import java.awt.geom.Point2D
import quuux.infinity.zui.Operation

/**
 * Textual output of a program.
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 25/10/11
 */

class IOutput(xml:NodeSeq) extends PText(get(xml,"text")) with IObject {
  init("Output", xml)
  setGreekThreshold(Settings.greekThreshold)
  val channel = get(xml, "channel")
  setPaint(if(channel=="stderr") IOutput.stderrPaint else IOutput.stdoutPaint)
  setFont(if(channel=="stderr") IOutput.stderrFont else IOutput.stdoutFont)

  override def toXML:NodeSeq =
    super.toXML ++ { <channel>{channel}</channel><text xml:space ="preserve">{getText}</text> }

  def act() { Operation.remove(this) }

  def menu(position:Point2D) = IMenu("action_output", "output", uid, position,  List(
    ("action_copy", 9),
    ("action_delete", 4)
  ))
}


object IOutput {
  val stderrFont = new Font("Consolas", Font.PLAIN, 10)
  val stderrPaint = new Color(255,99,71)
  val stdoutFont = new Font("Consolas", Font.PLAIN, 17)
  val stdoutPaint = new Color(255,255,200)
  def apply(text:String,channel:String):IOutput = IOutput(<channel>{channel}</channel><text>{text}</text>)
  def apply(xml:NodeSeq):IOutput = new IOutput(xml)
}