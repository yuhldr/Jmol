/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.jmol.viewer.Viewer;

import javajs.J2SIgnoreImport;

@J2SIgnoreImport({java.util.regex.Pattern.class})
public class PatternMatcher {

  /**
   * Super-simplified for JavaScript
   */
  public static class JSPattern {

    @SuppressWarnings("unused")
    private Object regexp; // JavaScript RegExp
    String strString = null;
    @SuppressWarnings("unused")
    private int leftBound = -1;
    private int rightBound = -1;
    private String[] results;

    public JSPattern(@SuppressWarnings("unused") String regex, boolean isCaseInsensitive) {
      @SuppressWarnings("unused")
      String flagStr = (isCaseInsensitive ? "gi" : "g");
      /**
       * @j2sNative this.regexp = new RegExp(regex, flagStr);
       */
      {
      }
    }

    public boolean find() {
      @SuppressWarnings("unused")
      String s = (this.rightBound == this.strString.length() ? this.strString
          : this.strString.substring(0, this.rightBound));

      /**
       * @j2sNative
       * 
       *            this.regexp.lastIndex = this.leftBound; 
       *            this.results = this.regexp.exec(s); 
       *            this.leftBound = this.regexp.lastIndex;
       */
      {
      }
      return (this.results != null);
    }

    public int start() {
      /**
       * @j2sNative
       * 
       *            return this.regexp.lastIndex - this.results[0].length;
       * 
       */
      {
        return 0;
      }
    }

    public int end() {
      /**
       * @j2sNative return this.regexp.lastIndex;
       */
      {
        return 0;
      }
    }

    public String group() {
      if (results == null || results.length == 0) {
        return null;
      }
      return this.results[0];
    }

    public Matcher matcher(String s) {
      this.strString = s;
      this.leftBound = 0;
      this.rightBound = s.length();
      return (Matcher) (Object) this;
    }
  }

  public PatternMatcher() {
    // for reflection  
  }

  public Pattern compile(String regex, boolean isCaseInsensitive) {    
    if (Viewer.isJS && !Viewer.isSwingJS) {
      return (java.util.regex.Pattern) (Object) new JSPattern(regex,
          isCaseInsensitive);
    }
    /** @j2sNative 
     * 
     */
    {
    // used to avoid core loading of rarely used java classes
      return Pattern.compile(regex,
        isCaseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
    }
  }

}
