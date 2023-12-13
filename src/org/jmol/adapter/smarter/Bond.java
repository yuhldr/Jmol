/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2023-01-05 14:42:06 +0000 (Thu, 05 Jan 2023) $
 * $Revision: 22500 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.adapter.smarter;
public class Bond extends AtomSetObject {
  public int atomIndex1;
  public int atomIndex2;
  public int order;
  public float radius = -1;
  public short colix = -1;
  public int uniqueID = -1;
  public float distance;


  /**
   * @param atomIndex1 
   * @param atomIndex2 
   * @param order 
   */
  public Bond (int atomIndex1, int atomIndex2, int order) {
    this.atomIndex1 = atomIndex1;
    this.atomIndex2 = atomIndex2;
    this.order = order;
  }

  @Override
  public String toString() {
    return "[Bond "+atomIndex1 +" " + atomIndex2 + " " + order+"]";
  }
}
