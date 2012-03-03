import fr.janalyse.plugthem.Plugin
import com.typesafe.config.Config

case class SimplePlugin(cfg:Config) extends Plugin {
  val        name = "simple"
  val description = "simple example plugin"
  val     version = "1.0"
  val      author = "David Crosson"

  def alive() = true
}
