package quuux.infinity.utilities

import quuux.infinity.zui.Utils

/**
 * Implements the mechanics for kinetic scrolling, zooming
 * Author : Stefan Maetschke
 * Version: 1.00
 * Date   : 9/09/12
 */
class Momentum(f: (Double) => Unit, decayRate: Double = 0.9, minValue: Double = 1.0, step: Long = 20) {
  private var _value = 0.0
  private var t = System.currentTimeMillis

  def inc(amount: Double) {
    synchronized {
      _value += amount; t = System.currentTimeMillis; Thread.sleep(5)
    }
  }

  def decay() {
    synchronized {
      _value *= decayRate
    }
  }

  Utils.invokeThread {
    while (true) {
      if (_value.abs > minValue) Utils.invokeLater {
        f(_value)
      }
      if (System.currentTimeMillis - t > 200) decay()
      Thread.sleep(step)
    }
  }
}