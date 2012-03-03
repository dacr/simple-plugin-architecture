/*
 * Copyright 2012 David Crosson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.janalyse.plugthem

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import java.io.File.{separator=>fs}
import java.io.File

@RunWith(classOf[JUnitRunner])
class DummyPluginManagerTest extends FunSuite with ShouldMatchers {
  
  ignore("Simple test") {
    val home = new File("tests", "simple")
    val cfg = ConfigFactory.parseString("""name = dummy""")
    val pm = new PluginManager[Plugin](home, cfg)
    
    import pm.plugins
    
    plugins should have size (1)
    plugins.head should equal ("SimplePlugin")
    
    val pluginCfg = ConfigFactory.parseString("""message = Hello world""")
    val plugin = pm.makePlugin(plugins.head, pluginCfg)
    
    plugin.name should equal("simple")
    plugin.description should include("example")
    plugin.alive() should equal(true)
    
  }

}
