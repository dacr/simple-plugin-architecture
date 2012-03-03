package fr.janalyse.plugthem
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import java.io.File.{separator=>fs, pathSeparator=>ps}
import java.io.File
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.Global
import java.net.URLClassLoader


trait Plugin {
  val name:String
  val description:String
  val version:String
  val author:String
  val started = now()

  def now() = System.currentTimeMillis()
  def uptime() = now()-started
  def alive():Boolean
}


class PluginManager[P<%Plugin](home:File, appConfig:Config) {
  private val defaults = ConfigFactory.load.getConfig("PluginManagerDefaults")
  private val config   = appConfig.withFallback(defaults)
  private def file(filename:String) =  new File(filename)
  private def file(dir:File, filename:String) =  new File(dir, filename)
  
  private def getStringConfigOption(key:String) = try {
    Some(config.getString("name"))
    } catch {
      case _:ConfigException.Missing => None
    }
    
  // Optional name for the plugin system
  val name = getStringConfigOption("name")
  
  // Where plugins sources are installed
  val dir  = file(home, config.getString("dir")+(name.map(fs+_) getOrElse ""))
  
  // Where plugin manager temporary data go
  val cacheDir  = file(home, config.getString("cacheDir")+(name.map(fs+_) getOrElse ""))
  if (!cacheDir.exists) cacheDir.mkdirs()

  // Where compiled plugin classes stands
  val classesDir = file(cacheDir, "plugins-classes")
  if (!classesDir.exists()) classesDir.mkdirs()
  
  // Plugin class name mask
  val pluginRE = config.getString("mask").r

  // List of libraries made available to plugins
  val libs = filesLookup(dir, _ endsWith ".jar") map {file(dir, _)}
  
  // List of all compiled class names
  val allClassNames = dynamicPluginCompileAndGetCompiledClassname(dir)
  
  // List of found compiled class names
  val plugins = allClassNames collect {case x@pluginRE() => x}
  
  // The plugin class loader, to dynamically load plugin classes
  val pluginClassLoader =  new URLClassLoader(Array(classesDir.toURI.toURL)++(libs map {_.toURI.toURL}), this.getClass().getClassLoader())
  
  // Create a new instance of a plugin
  def makePlugin(pname:String, pcfg:Config):P = {
    val cl = pluginClassLoader
    cl.loadClass(pname).getConstructor(classOf[Config]).newInstance(pcfg).asInstanceOf[P]
  }
    
  // ---------------------------------------------------------------------------
  private def dynamicPluginCompileAndGetCompiledClassname(pluginSrc:File) = {
    val files2Compile = filesLookup(pluginSrc, _ endsWith ".scala") map {pluginSrc.getPath+fs+_}
    val alreadyCompiledClasses = filesLookup(classesDir, _ endsWith ".class") map {file(classesDir, _)}
    val sourcesLastModified = files2Compile map {file(_)} map {_.lastModified}
    val classesLastModified =  alreadyCompiledClasses map {_.lastModified}
    if (alreadyCompiledClasses.isEmpty || (sourcesLastModified.max > classesLastModified.max)) {
      println("Plugins compilation triggered")
      compile(files2Compile, classesDir)
    }
    classesLookup(classesDir)
  }
  // ---------------------------------------------------------------------------
  private def filesLookup(file:File, selector:String=>Boolean, path:List[String]=Nil):List[String] = {
    file.listFiles.toList flatMap {_ match {
	      case f if f.isDirectory() => filesLookup(f, selector, path:+f.getName)
	      case f if selector(f.getName()) => path.mkString(fs)+f.getName::Nil
	      case _ => Nil
	    }
    }
  }
  // ---------------------------------------------------------------------------
  private def classesLookup(file:File, pkg:List[String]=Nil):List[String] = {
    file.listFiles.toList flatMap {_ match {
	      case f if f.isDirectory() => classesLookup(f, f.getName::pkg)
	      case f if f.getName.endsWith(".class") => pkg.reverse.mkString(".")+f.getName.replace(".class","")::Nil
	      case _ => Nil
	    }
    }
  }
  // ---------------------------------------------------------------------------
  private def compile(files2Compile:List[String], pluginDestClassesDir:File) = {
    deleteFileTree(pluginDestClassesDir)
    def error(message: String) = println(message)
    val jcp = System.getProperty("java.class.path")
    val settings = new  Settings(error)
    settings.outdir.value = pluginDestClassesDir.getPath()
    settings.deprecation.value = true
    settings.unchecked.value = true
    settings.verbose.value=false
    settings.classpath.value=jcp+ps+libs.map(_.getPath).mkString(ps)
    val reporter = new ConsoleReporter(settings)
    val compiler = new Global(settings, reporter)
    (new compiler.Run).compile(files2Compile)
    if (reporter.hasErrors || reporter.WARNING.count > 0) {
      reporter.printSummary
      if (reporter.hasErrors) System.exit(1)
    }
  }
  // ---------------------------------------------------------------------------
  private def deleteFileTree(dir:File, preserveDir:Boolean=true) {
    for (file <- dir.listFiles.toList) {
      file match {
        case d if d.isDirectory() =>
          deleteFileTree(d, false)
          if (!preserveDir) d.delete()
        case f => f.delete()
      }
    }
  }

}
