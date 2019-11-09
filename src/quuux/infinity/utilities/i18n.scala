package quuux.infinity.utilities

import java.util.Locale
import java.util.Properties
import java.io.{File, FileInputStream}

/**
 * Internationalization
 * @author Stefan Maetschke
 * @copyright www.quuux.com
 */

object i18n {
  private var DefaultBundleName = "i18n/Text"
  private var ErrorText = "! '%s' MISSING"
  private var bundle = new Properties()
  private var currentlocale: Locale = _

  /** returns the text for the given name or an error text if the name does not exist */
  def apply(key: String) = bundle.getProperty(key) match {
    case null => ErrorText format key
    case s: String => s
  }

  /** returns the formatted text for the given name and args or an error text if the name does not exist */
  def apply(key: String, args: Any*) = bundle.getProperty(key) match {
    case null => ErrorText format key
    case s: String => s.format(args: _*)
  }

  /** Takes locales in the format "de_DE", "en_US".  "" means the default locale of the system is chosen */
  def init(localeStr: String = "", bundleName: String = DefaultBundleName) {
    currentlocale = if (localeStr == "") Locale.getDefault else locale(localeStr)
    bundle.load(new FileInputStream(bundlePath(bundleName, currentlocale)))
  }

  /** returns the bundle path for the given local if it exits otherwise the default bundle is used */
  private def bundlePath(bundleName: String, locale: Locale) = {
    val path = bundleName + "_" + locale.toString + ".properties"
    if ((new File(path)).exists) path else bundleName + ".properties"
  }

  /** returns the currently set locale */
  def locale = currentlocale

  /** returns a locale for the given specifier which has to be in format "de_DE", "en_US", etc */
  def locale(localeStr: String) = {
    val Array(language, country) = localeStr.split('_')
    new Locale(language, country)
  }

  /** Returns the default locale of the system */
  def defaultLocal = Locale.getDefault


  // just a usage example
  def main(args: Array[String]) = {
    //init()
    //init("en_US")
    //init("en_AU")
    init("de_DE")

    println(locale.toString)
    println(locale.getDisplayLanguage.toString)
    println(locale.getDisplayCountry)
    println(i18n("welcome"))
    println(i18n("luckyNumber", 7))
    println(i18n("not_there"))
  }
}