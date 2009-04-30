//
// VirtualImagePlus.java
//

/*
LOCI Plugins for ImageJ: a collection of ImageJ plugins including the
Bio-Formats Importer, Bio-Formats Exporter, Bio-Formats Macro Extensions,
Data Browser, Stack Colorizer and Stack Slicer. Copyright (C) 2005-@year@
Melissa Linkert, Curtis Rueden and Christopher Peterson.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.plugins.util;

import ij.*;
import java.io.IOException;
import java.util.Vector;
import loci.formats.IFormatReader;

/**
 * TODO
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/loci-plugins/src/loci/plugins/util/VirtualImagePlus.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/loci-plugins/src/loci/plugins/util/VirtualImagePlus.java">SVN</a></dd></dl>
 */
public class VirtualImagePlus extends ImagePlus {

  // -- Fields --

  private IFormatReader r;

  // -- Constructor --

  public VirtualImagePlus(String title, ImageStack stack) {
    super(title, stack);
  }

  // -- VirtualImagePlus API methods --

  public void setReader(IFormatReader r) {
    this.r = r;
  }

  // -- ImagePlus API methods --

  public synchronized void setSlice(int index) {
    super.setSlice(index);

    BFVirtualStack stack = null;
    if (getStack() instanceof BFVirtualStack) {
      stack = (BFVirtualStack) getStack();
      RecordedImageProcessor proc = stack.getRecordedProcessor();
      Vector methods = stack.getMethodStack();
      if (methods != null) {
        proc.applyMethodStack(methods);
      }
      // if we call setProcessor(getTitle(), proc), the type will be set
      // to GRAY32 (regardless of the actual processor type)
      setProcessor(getTitle(), proc.getChild());
      this.ip = proc;
    }
  }

  public void close() {
    super.close();
    try {
      r.close();
    }
    catch (IOException e) { }
  }

}