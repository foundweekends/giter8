package org.clapper.scalasti

import org.stringtemplate.v4.{ST => _ST}

object STHelper {
  import org.clapper.scalasti.{ST, STGroup}
  import org.stringtemplate.v4.{STGroup => _STGroup, ST => _ST}
  def apply(group: STGroup, template: String): ST = new ST(new _ST(group.nativeGroup, template))
}