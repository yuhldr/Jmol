/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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

package org.jmol.symmetry;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.BoxInfo;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

/*
 * Bob Hanson 4/2006
 * 
 * references: International Tables for Crystallography Vol. A. (2002) 
 *
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Ispace_group_symop_operation_xyz.html
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Isymmetry_equiv_pos_as_xyz.html
 *
 * LATT : http://macxray.chem.upenn.edu/LATT.pdf thank you, Patrick Carroll
 * 
 * NEVER ACCESS THESE METHODS DIRECTLY! ONLY THROUGH CLASS Symmetry
 */

public class SymmetryOperation extends M4 {
  String xyzOriginal;
  String xyzCanonical;
  String xyz;
  /**
   * "normalization" is the process of adjusting symmetry operator definitions
   * such that the center of geometry of a molecule is within the 555 unit cell
   * for each operation. It is carried out when "packed" is NOT issued and the
   * lattice is given as {i j k} or when the lattice is given as {nnn mmm 1}
   */
  private boolean doNormalize = true;
  boolean isFinalized;
  private int opId;
  private V3 centering;
  private Hashtable<String, Object> info;

  static P3 atomTest;
  


  final static int TYPE_UNKNOWN = -1;
  final static int TYPE_IDENTITY = 0;
  final static int TYPE_TRANSLATION = 1;
  final static int TYPE_ROTATION = 2;
  final static int TYPE_INVERSION = 4;
  final static int TYPE_REFLECTION = 8;  
  final static int TYPE_SCREW_ROTATION = TYPE_ROTATION | TYPE_TRANSLATION;
  final static int TYPE_ROTOINVERSION = TYPE_ROTATION | TYPE_INVERSION;
  final static int TYPE_GLIDE_REFLECTION = TYPE_REFLECTION | TYPE_TRANSLATION; 
  
  private int opType = TYPE_UNKNOWN;
  
  private int opOrder;
  private V3 opTrans;
  private P3 opPoint, opPoint2;
  private V3 opAxis;
  P4 opPlane;
  private Boolean opIsCCW;

  boolean isIrrelevant;
  boolean isCoincident;
  
  final static int PLANE_MODE_POSITION_ONLY = 0;
  final static int PLANE_MODE_NOTRANS = 1;
  final static int PLANE_MODE_FULL = 2;
  

  private String getOpName(int planeMode) {
    if (opType == TYPE_UNKNOWN)
      setOpTypeAndOrder();
    switch (opType) {
    case TYPE_IDENTITY:
      return "I";
    case TYPE_TRANSLATION:
      return "Trans" + op48(opTrans);
    case TYPE_ROTATION:
      return "Rot" + opOrder + op48(opPoint) + op48(opAxis) + opIsCCW;
    case TYPE_INVERSION:
      return "Inv" + op48(opPoint);
    case TYPE_REFLECTION:
      return (planeMode == PLANE_MODE_POSITION_ONLY ? "" : "Plane") + opPlane;
    case TYPE_SCREW_ROTATION:
      return "Screw" + opOrder + op48(opPoint) + op48(opAxis) + op48(opTrans) + opIsCCW;
    case TYPE_ROTOINVERSION:
      return "Nbar" + opOrder + op48(opPoint) + op48(opAxis) + opIsCCW;
    case TYPE_GLIDE_REFLECTION:
      return (planeMode == PLANE_MODE_POSITION_ONLY ? "" : "Glide") + opPlane + (planeMode == PLANE_MODE_FULL ? op48(opTrans) : "");
    }
    System.out.println("SymmetryOperation REJECTED TYPE FOR " + this);
    return "";
  }

  String getOpTitle() {
    if (opType == TYPE_UNKNOWN)
      setOpTypeAndOrder();
    switch (opType) {
    case TYPE_IDENTITY:
      return "identity ";
    case TYPE_TRANSLATION:
      return "translation " + opFrac(opTrans);
    case TYPE_ROTATION:
      return "rotation " + opOrder;
    case TYPE_INVERSION:
      return "inversion center " + opFrac(opPoint);
    case TYPE_REFLECTION:
      return "reflection ";
    case TYPE_SCREW_ROTATION:
      return "screw rotation " + opOrder + (opIsCCW == null ? "" : opIsCCW == Boolean.TRUE ? "(+) " : "(-) ") + opFrac(opTrans);
    case TYPE_ROTOINVERSION:
      return opOrder + "-bar "  + (opIsCCW == null ? "" : opIsCCW == Boolean.TRUE ? "(+) " : "(-) ") + opFrac(opPoint);
    case TYPE_GLIDE_REFLECTION:
      return "glide reflection " + opFrac(opTrans);
    }
    return "";
  }
  
  private static String opFrac(T3 p) {
    return "{" + opF(p.x) + " " + opF(p.y)  + " " + opF(p.z) + "}";
  }



  private static String opF(float x) {
    boolean neg = (x < 0);
    if (neg) {
      x = -x;
    }
    int n = 0;
    if (x >= 1) {
      n = (int) x;
      x -= n;
    }
    int n48 = (int) Math.round(x * 48);
    int div;
    if (n48 % 48 == 0) {
      div = 1;
    } else if (n48 % 24 == 0) {
      div = 2;
    } else if (n48 % 16 == 0) {
      div = 3;
    } else if (n48 % 12 == 0) {
      div = 4;
    } else if (n48 % 8 == 0) {
      div = 6;
    } else if (n48 % 6 == 0) {
      div = 8;
    } else if (n48 % 4 == 0) {
      div = 12;
    } else if (n48 % 3 == 0) {
      div = 16;
    } else if (n48 % 2 == 0) {
      div = 24;
    } else {
      div = 48;
    }
    return (neg ? "-" : "") + (n*div + n48 * div / 48) + (div == 1 ? "" : "/" + div);
  }

  private static String op48(T3 p) {
    if (p == null) {
      System.err.println("SymmetryOperation.op48 null");
      return "(null)";
    }

    return "{" + Math.round(p.x*48) + " " + Math.round(p.y*48)  + " " + Math.round(p.z*48) + "}";
  }

  
  private String[] myLabels;
  int modDim;

  /**
   * A linear array for the matrix. Note that the last value in this array may
   * indicate 120 to indicate that the integer divisor should be 120, not 12.
   */
  float[] linearRotTrans;

  /**
   * rsvs is the superspace group rotation-translation matrix. It is a (3 +
   * modDim + 1) x (3 + modDim + 1) matrix from which we can extract all
   * necessary parts; so 4x4 = 16, 5x5 = 25, 6x6 = 36, 7x7 = 49
   * 
   * <code>
     [ [(3+modDim)*x + 1]   
     [(3+modDim)*x + 1]     [ Gamma_R   [0x0]   | Gamma_S
     [(3+modDim)*x + 1]  ==    [0x0]    Gamma_e | Gamma_d 
     ...                       [0]       [0]    |   1     ]
     [0 0 0 0 0...   1] ]
     </code>
   */
  Matrix rsvs;

  boolean isBio;
  Matrix sigma;
  int number;
  String subsystemCode;
  int timeReversal;

  private boolean unCentered;
  boolean isCenteringOp;
  private int magOp = Integer.MAX_VALUE;
  int divisor = 12; // could be 120 for magnetic;
  private T3 opX;
  private String opAxisCode;
  public boolean opIsLong;

  void setSigma(String subsystemCode, Matrix sigma) {
    this.subsystemCode = subsystemCode;
    this.sigma = sigma;
  }

  /**
   * 
   * @param op operation to clone or null
   * @param id opId for this operation; ignored if cloning
   * @param doNormalize 
   */
  SymmetryOperation(SymmetryOperation op, int id, boolean doNormalize) {
    this.doNormalize = doNormalize;
    if (op == null) {
      opId = id;
      return;
    }
    /*
     * externalizes and transforms an operation for use in atom reader
     * 
     */
    xyzOriginal = op.xyzOriginal;
    xyz = op.xyz;
    divisor = op.divisor;
    opId = op.opId;
    modDim = op.modDim;
    myLabels = op.myLabels;
    number = op.number;
    linearRotTrans = op.linearRotTrans;
    sigma = op.sigma;
    subsystemCode = op.subsystemCode;
    timeReversal = op.timeReversal;
    setMatrix(false);
    if (!op.isFinalized)
      doFinalize();
  }

  private void setGamma(boolean isReverse) {
    // standard M4 (this)
    //
    //  [ [rot]   | [trans] 
    //     [0]    |   1     ]
    //
    // becomes for a superspace group
    //
    //  rows\cols    (3)    (modDim)    (1)
    // (3)        [ Gamma_R   [0x0]   | Gamma_S
    // (modDim)       m*      Gamma_e | Gamma_d 
    // (1)           [0]       [0]    |   1     ]

    int n = 3 + modDim;
    double[][] a = (rsvs = new Matrix(null, n + 1, n + 1)).getArray();
    double[] t = new double[n];
    int pt = 0;
    // first retrieve all n x n values from linearRotTrans
    // and get the translation as well
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++)
        a[i][j] = linearRotTrans[pt++];
      t[i] = (isReverse ? -1 : 1) * linearRotTrans[pt++];
    }
    a[n][n] = 1;
    if (isReverse)
      rsvs = rsvs.inverse();
    // t is already reversed; set it now.
    for (int i = 0; i < n; i++)
      a[i][n] = t[i];
    // then set this operation matrix as {R|t}
    a = rsvs.getSubmatrix(0, 0, 3, 3).getArray();
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 4; j++)
        setElement(i, j, (float)(j < 3 ? a[i][j] : t[i]));
    setElement(3, 3, 1);
  }

  void doFinalize() {
    div12(this, divisor);
    if (modDim > 0) {
      double[][] a = rsvs.getArray();
      for (int i = a.length - 1; --i >= 0;)
        a[i][3 + modDim] = finalizeD(a[i][3 + modDim],  divisor);
    }
    isFinalized = true;
  }

  private static M4 div12(M4 op, int divisor) {
    op.m03 = finalizeF(op.m03, divisor);
    op.m13 = finalizeF(op.m13, divisor);
    op.m23 = finalizeF(op.m23, divisor);
    return op;
  }

  private static float finalizeF(float m, int divisor) {
    if (divisor == 0) {
      if (m == 0)
        return 0;
      int n = (int) m;
      return ((n >> DIVISOR_OFFSET) * 1F / (n & DIVISOR_MASK));
    } 
    return m / divisor;
  }

  private static double finalizeD(double m, int divisor) {
    if (divisor == 0) {
      if (m == 0)
        return 0;
      int n = (int) m;
      return ((n >> DIVISOR_OFFSET) * 1F / (n & DIVISOR_MASK));
    } 
    return m / divisor;
  }

  String getXyz(boolean normalized) {
    return (normalized && modDim == 0 || xyzOriginal == null ? xyz
        : xyzOriginal);
  }

  public String getxyzTrans(T3 t) {
    M4 m = newM4(this);
    m.add(t);
    return getXYZFromMatrix(m, false, false, false);
  }

  String dumpInfo() {
    return "\n" + xyz + "\ninternal matrix representation:\n" + toString();
  }

  final static String dumpSeitz(M4 s, boolean isCanonical) {
    SB sb = new SB();
    float[] r = new float[4];
    for (int i = 0; i < 3; i++) {
      s.getRow(i, r);
      sb.append("[\t");
      for (int j = 0; j < 3; j++)
        sb.appendI((int) r[j]).append("\t");
      float trans =  r[3];
      if (trans != (int) trans)
        trans = 12 * trans;
      sb.append(twelfthsOf(isCanonical ? normalizeTwelfths(trans / 12, 12, true) : (int) trans)).append("\t]\n");
    }
    return sb.toString();
  }

  boolean setMatrixFromXYZ(String xyz, int modDim, boolean allowScaling) {
    /*
     * sets symmetry based on an operator string "x,-y,z+1/2", for example
     * 
     */
    if (xyz == null)
      return false;
    xyzOriginal = xyz;
    divisor = setDivisor(xyz);
    xyz = xyz.toLowerCase();
    setModDim(modDim);
    boolean isReverse = false;
    boolean halfOrLess = true;
    if (xyz.startsWith("!")) {
      if (xyz.startsWith("!nohalf!")) {
        halfOrLess = false;
        xyz = xyz.substring(8);
        xyzOriginal = xyz;
      } else {
        isReverse = false;
        xyz = xyz.substring(1);
      }
    }
    if (xyz.indexOf("xyz matrix:") == 0) {
      /* note: these terms must in unit cell fractional coordinates!
       * CASTEP CML matrix is in fractional coordinates, but do not take into account
       * hexagonal systems. Thus, in wurtzite.cml, for P 6c 2'c:
       *
       * "transform3": 
       * 
       * -5.000000000000e-1  8.660254037844e-1  0.000000000000e0   0.000000000000e0 
       * -8.660254037844e-1 -5.000000000000e-1  0.000000000000e0   0.000000000000e0 
       *  0.000000000000e0   0.000000000000e0   1.000000000000e0   0.000000000000e0 
       *  0.000000000000e0   0.000000000000e0   0.000000000000e0   1.000000000000e0
       *
       * These are transformations of the STANDARD xyz axes, not the unit cell. 
       * But, then, what coordinate would you feed this? Fractional coordinates of what?
       * The real transform is something like x-y,x,z here.
       * 
       */
      this.xyz = xyz;
      Parser.parseStringInfestedFloatArray(xyz, null, linearRotTrans);
      return setFromMatrix(null, isReverse);
    }
    if (xyz.indexOf("[[") == 0) {
      xyz = xyz.replace('[', ' ').replace(']', ' ').replace(',', ' ');
      Parser.parseStringInfestedFloatArray(xyz, null, linearRotTrans);
      for (int i = linearRotTrans.length; --i >= 0;)
        if (Float.isNaN(linearRotTrans[i]))
          return false;
      setMatrix(isReverse);
      isFinalized = true;
      isBio = (xyz.indexOf("bio") >= 0);
      this.xyz = (isBio ? (this.xyzOriginal = super.toString())
          : getXYZFromMatrix(this, false, false, false));
      return true;
    }
    if (modDim == 0 && xyz.indexOf("x4") >= 0) {
      for (int i = 14; --i >= 4;) {
        if (xyz.indexOf("x" + i) >= 0) {
          setModDim(i - 3);
          break;
        }
      }
    }
    String mxyz = null;
    // we use ",m" and ",-m" as internal notation for time reversal.
    if (xyz.endsWith("m")) {
      timeReversal = (xyz.indexOf("-m") >= 0 ? -1 : 1);
      allowScaling = true;
    } else if (xyz.indexOf("mz)") >= 0) {
      // alternatively, we accept notation indicating explicit spin transformation "(mx,my,mz)"
      int pt = xyz.indexOf("(");
      mxyz = xyz.substring(pt + 1, xyz.length() - 1);
      xyz = xyz.substring(0, pt);
      allowScaling = false;
    }
    String strOut = getMatrixFromString(this, xyz, linearRotTrans,
        allowScaling, halfOrLess);
    if (strOut == null)
      return false;
    xyzCanonical = strOut;
    if (mxyz != null) {
      // base time reversal on relationship between x and mx in relation to determinant
      boolean isProper = (M4.newA16(linearRotTrans).determinant3() == 1);
      timeReversal = (((xyz.indexOf("-x") < 0) == (mxyz
          .indexOf("-mx") < 0)) == isProper ? 1 : -1);
    }
    setMatrix(isReverse);
    this.xyz = (isReverse ? getXYZFromMatrix(this, true, false, false)
        : doNormalize ? strOut : xyz);
    if (timeReversal != 0)
      this.xyz += (timeReversal == 1 ? ",m" : ",-m");
    if (Logger.debugging)
      Logger.debug("" + this);
    return true;
  }

  /**
   * Sets the divisor to 0 for n/9 or n/mm
   * @param xyz
   * @return 0 or 12
   */
  private static int setDivisor(String xyz) {
    int pt = xyz.indexOf('/');
    int len = xyz.length();
    while (pt > 0 && pt < len - 1) {
      char c = xyz.charAt(pt + 1);
      if ("2346".indexOf(c) < 0 || pt < len - 2 && Character.isDigit(xyz.charAt(pt + 2))) {
        // any n/m where m is not 2,3,4,6
        // any n/nn
        return 0;
      }
      pt = xyz.indexOf('/', pt + 1);
    }
    return 12;
  }

  private void setModDim(int dim) {
    int n = (dim + 4) * (dim + 4);
    modDim = dim;
    if (dim > 0)
      myLabels = labelsXn;
    linearRotTrans = new float[n];
  }

  private void setMatrix(boolean isReverse) {
    if (linearRotTrans.length > 16) {
      setGamma(isReverse);
    } else {
      setA(linearRotTrans);
      if (isReverse) {
        P3 p3 = P3.new3(m03, m13, m23);
        invert();
        rotate(p3);
        p3.scale(-1);
        setTranslation(p3);
      }
    }
  }

  boolean setFromMatrix(float[] offset, boolean isReverse) {
    float v = 0;
    int pt = 0;
    myLabels = (modDim == 0 ? labelsXYZ : labelsXn);
    int rowPt = 0;
    int n = 3 + modDim;
    for (int i = 0; rowPt < n; i++) {
      if (Float.isNaN(linearRotTrans[i]))
        return false;
      v = linearRotTrans[i];
      
      if (Math.abs(v) < 0.00001f)
        v = 0;
      boolean isTrans = ((i + 1) % (n + 1) == 0);
      if (isTrans) {
        int denom =  (divisor == 0 ? ((int) v) & DIVISOR_MASK : divisor);
        if (denom == 0)
          denom = 12;
        v =  finalizeF(v, divisor);
        // offset == null only in the case of "xyz matrix:" option
        if (offset != null) {
          // magnetic centering only
          if (pt < offset.length)
            v += offset[pt++];
        }
        v = normalizeTwelfths(((v < 0 ? -1 : 1) * Math.abs(v * denom) / denom),
            denom, doNormalize);
        if (divisor == 0)
          v = toDivisor(v, denom);
        rowPt++;
      }
      linearRotTrans[i] = v;
    }
    linearRotTrans[linearRotTrans.length - 1] = this.divisor;
    setMatrix(isReverse);
    isFinalized = (offset == null);
    xyz = getXYZFromMatrix(this, true, false, false);
    return true;
  }

  public static M4 getMatrixFromXYZ(String xyz, boolean halfOrLess) {
    float[] linearRotTrans = new float[16];
    xyz = getMatrixFromString(null, xyz, linearRotTrans, false, halfOrLess);
    if (xyz == null)
      return null;
    M4 m = new M4();
    m.setA(linearRotTrans);
    return div12(m, setDivisor(xyz));
  }

  static String getJmolCanonicalXYZ(String xyz) {
    try {
      return getMatrixFromString(null, xyz, null, false, true);
    } catch (Exception e) {
      return null;
    }
  }
  /**
   * Convert the Jones-Faithful notation "x, -z+1/2, y" or "x1, x3-1/2, x2,
   * x5+1/2, -x6+1/2, x7..." to a linear array
   * 
   * Also allows a-b,-5a-5b,-c;0,0,0 format
   * 
   * @param op
   * @param xyz
   * @param linearRotTrans
   * @param allowScaling
   * @param halfOrLess 
   * @return canonized Jones-Faithful string
   */
  static String getMatrixFromString(SymmetryOperation op, String xyz,
                                    float[] linearRotTrans,
                                    boolean allowScaling, boolean halfOrLess) {    
    boolean isDenominator = false;
    boolean isDecimal = false;
    boolean isNegative = false;
    xyz = PT.rep(xyz,  "[bio[", "");
    int modDim = (op == null ? 0 : op.modDim);
    int nRows = 4 + modDim;
    int divisor = (op == null ? setDivisor(xyz) : op.divisor);
    boolean doNormalize = halfOrLess && (op == null ? !xyz.startsWith("!") : op.doNormalize);
    int dimOffset = (modDim > 0 ? 3 : 0); // allow a b c to represent x y z
    if (linearRotTrans != null)
      linearRotTrans[linearRotTrans.length - 1] = 1;
    // may be a-b,-5a-5b,-c;0,0,0 form
    int transPt = xyz.indexOf(';') + 1;
    if (transPt != 0) {
      allowScaling = true;
      if (transPt == xyz.length())
        xyz += "0,0,0";
    }
    int rotPt = -1;
    String[] myLabels = (op == null || modDim == 0 ? null : op.myLabels);
    if (myLabels == null)
      myLabels = labelsXYZ;
    xyz = xyz.toLowerCase() + ",";
    xyz = xyz.replace('(', ',');
    //        load =magndata/1.23
    //        draw symop "-x,-y,-z(mx,my,mz)"
    if (modDim > 0)
      xyz = replaceXn(xyz, modDim + 3);
    int xpt = 0;
    int tpt0 = 0;
    int rowPt = 0;
    char ch;
    float iValue = 0;
    int denom = 0;
    int numer = 0;
    float decimalMultiplier = 1f;
    String strT = "";
    String strOut = "";
    int[] ret = new int[1];
    int len = xyz.length();
    for (int i = 0; i < len; i++) {
      switch (ch = xyz.charAt(i)) {
      case ';':
        break;
      case '\'':
      case ' ':
      case '{':
      case '}':
      case '!':
        continue;
      case '-':
        isNegative = true;
        continue;
      case '+':
        isNegative = false;
        continue;
      case '/':
        denom = 0;
        isDenominator = true;
        continue;
      case 'x':
      case 'y':
      case 'z':
      case 'a':
      case 'b':
      case 'c':
      case 'd':
      case 'e':
      case 'f':
      case 'g':
      case 'h':
        tpt0 = rowPt * nRows;
        int ipt = (ch >= 'x' ? ch - 'x' : ch - 'a' + dimOffset);
        xpt = tpt0 + ipt;
        int val = (isNegative ? -1 : 1);
        if (allowScaling && iValue != 0) {
          if (linearRotTrans != null)
            linearRotTrans[xpt] = iValue;
          val = (int) iValue;
          iValue = 0;
        } else if (linearRotTrans != null) {
            linearRotTrans[xpt] = val;
        }
        strT += plusMinus(strT, val, myLabels[ipt], false);
        break;
      case ',':
        if (transPt != 0) {
          if (transPt > 0) {
            // now read translation
            rotPt = i;
            i = transPt - 1;
            transPt = -i;
            iValue = 0;
            denom = 0;
            continue;
          }
          transPt = i + 1;
          i = rotPt;
        }
        // add translation in 12ths
        iValue = normalizeTwelfths(iValue, denom == 0 ? 12 : divisor == 0 ? denom : divisor, doNormalize);
        if (linearRotTrans != null)
          linearRotTrans[tpt0 + nRows - 1] = (divisor == 0 && denom > 0 ? iValue = toDivisor(numer, denom) : iValue);
        strT += xyzFraction12(iValue, (divisor == 0 ? denom : divisor), false, halfOrLess);
        // strT += xyzFraction48(iValue, false, true);
        strOut += (strOut == "" ? "" : ",") + strT;
        if (rowPt == nRows - 2)
          return strOut;
        iValue = 0;
        numer = 0;
        denom = 0;
        strT = "";
        if (rowPt++ > 2 && modDim == 0) {
          Logger.warn("Symmetry Operation? " + xyz);
          return null;
        }
        break;
      case '.':
        isDecimal = true;
        decimalMultiplier = 1f;
        continue;
      case '0':
        if (!isDecimal && divisor == 12 && (isDenominator || !allowScaling))
          continue;
        //$FALL-THROUGH$
      default:
        //Logger.debug(isDecimal + " " + ch + " " + iValue);
        int ich = ch - '0';
        if (ich >= 0 && ich <= 9) {
          if (isDecimal) {
            decimalMultiplier /= 10f;
            if (iValue < 0)
              isNegative = true;
            iValue += decimalMultiplier * ich * (isNegative ? -1 : 1);
            continue;
          }
          if (isDenominator) {
            ret[0] = i;
            denom = PT.parseIntNext(xyz, ret);
            if (denom < 0)
              return null;
            i = ret[0] - 1;
            if (iValue == 0) {
              // a/2,....
              if (linearRotTrans != null)
                linearRotTrans[xpt] /= denom;
            } else {
              numer = (int) iValue;
              iValue /= denom;
            }
          } else {
            iValue = iValue * 10 + (isNegative ? -1 : 1) * ich;
            isNegative = false;
          }
        } else {
          Logger.warn("symmetry character?" + ch);
        }
      }
      isDecimal = isDenominator = isNegative = false;
    }
    return null;
  }

  static String replaceXn(String xyz, int n) {
    for (int i = n; --i >= 0;)
      xyz = PT.rep(xyz, labelsXn[i], labelsXnSub[i]);
    return xyz;
  }

  private final static int DIVISOR_MASK = 0xFF;
  private final static int DIVISOR_OFFSET = 8;
  
  private final static int toDivisor(float numer, int denom) {
    int n = (int) numer;
    if (n != numer) {
      // could happen with magnetic lattice centering 1/5 + 1/2 = 7/10
      float f = numer - n;
      denom = (int) Math.abs(denom/f);
      n = (int) (Math.abs(numer) / f);
    }
    return ((n << DIVISOR_OFFSET) + denom);
  }

  private final static String xyzFraction12(float n12ths, int denom, boolean allPositive,
                                            boolean halfOrLess) {
    if (n12ths == 0)
      return "";
    float n = n12ths;
    if (denom != 12) {
      int in = (int) n;
      denom = (in & DIVISOR_MASK);
      n = in >> DIVISOR_OFFSET;
    }
    int half = (denom / 2);
    if (allPositive) {
      while (n < 0)
        n += denom;
    } else if (halfOrLess) {
      while (n > half)
        n -= denom;
      while (n < -half)
        n += denom;
    }
    String s = (denom == 12 ? twelfthsOf(n) : n == 0 ? "0" : n + "/" + denom);
    return (s.charAt(0) == '0' ? "" : n > 0 ? "+" + s : s);
  }

  //  private final static String xyzFraction48ths(float n48ths, boolean allPositive, boolean halfOrLess) {
  //    float n = n48ths;
  //    if (allPositive) {
  //      while (n < 0)
  //        n += 48;
  //    } else if (halfOrLess) {
  //      while (n > 24f)
  //        n -= 48;
  //      while (n < -24f)
  //        n += 48;
  //    }
  //    String s = fortyEighthsOf(n);
  //    return (s.charAt(0) == '0' ? "" : n > 0 ? "+" + s : s);
  //  }

  final static String twelfthsOf(float n12ths) {
    String str = "";
    if (n12ths < 0) {
      n12ths = -n12ths;
      str = "-";
    }
    int m = 12;
    int n = (int) Math.round(n12ths);
    if (Math.abs(n - n12ths) > 0.01f) {
      // fifths? sevenths? eigths? ninths? sixteenths?
      // Juan Manuel suggests 10 is large enough here 
      float f = n12ths / 12;
      int max = 20;
      for (m = 3; m < max; m++) {
        float fm = f * m;
        n = (int) Math.round(fm);
        if (Math.abs(n - fm) < 0.01f)
          break;
      }
      if (m == max)
        return str + f;
    } else {
      if (n == 12)
        return str + "1";
      if (n < 12)
        return str + twelfths[n % 12];
      switch (n % 12) {
      case 0:
        return str + n / 12;
      case 2:
      case 10:
        m = 6;
        break;
      case 3:
      case 9:
        m = 4;
        break;
      case 4:
      case 8:
        m = 3;
        break;
      case 6:
        m = 2;
        break;
      default:
        break;
      }
      n = (n * m / 12);
    }
    return str + n + "/" + m;
  }

  //  final static String fortyEighthsOf(float n48ths) {
  //    String str = "";
  //    if (n48ths < 0) {
  //      n48ths = -n48ths;
  //      str = "-";
  //    }
  //    int m = 12;
  //    int n = (int) Math.round(n48ths);
  //    if (Math.abs(n - n48ths) > 0.01f) {
  //      // fifths? sevenths? eigths? ninths? sixteenths?
  //      // Juan Manuel suggests 10 is large enough here 
  //      float f = n48ths / 48;
  //      int max = 20;
  //      for (m = 5; m < max; m++) {
  //        float fm = f * m;
  //        n = (int) Math.round(fm);
  //        if (Math.abs(n - fm) < 0.01f)
  //          break;
  //      }
  //      if (m == max)
  //        return str + f;
  //    } else {
  //      if (n == 48)
  //        return str + "1";
  //      if (n < 48)
  //        return str + twelfths[n % 48];
  //      switch (n % 48) {
  //      case 0:
  //        return "" + n / 48;
  //      case 2:
  //      case 10:
  //        m = 6;
  //        break;
  //      case 3:
  //      case 9:
  //        m = 4;
  //        break;
  //      case 4:
  //      case 8:
  //        m = 3;
  //        break;
  //      case 6:
  //        m = 2;
  //        break;
  //      default:
  //        break;
  //      }
  //      n = (n * m / 12);
  //    }
  //    return str + n + "/" + m;
  //  }

  private final static String[] twelfths = { "0", "1/12", "1/6", "1/4", "1/3",
      "5/12", "1/2", "7/12", "2/3", "3/4", "5/6", "11/12" };

  //  private final static String[] fortyeigths = { "0", 
  //    "1/48", "1/24", "1/16", "1/12",
  //    "5/48", "1/8", "7/48", "1/6", 
  //    "3/16", "5/24", "11/48", "1/4",
  //    "13/48", "7/24", "5/16", "1/3",
  //    "17/48", "3/8", "19/48", "5/12",
  //    "7/16", "11/24", "23/48", "1/2",
  //    "25/48", "13/24", "9/16", "7/12",
  //    "29/48", "15/24", "31/48", "2/3",
  //    "11/12", "17/16", "35/48", "3/4",
  //    "37/48", "19/24", "13/16", "5/6",
  //    "41/48", "7/8", "43/48", "11/12",
  //    "15/16", "23/24", "47/48"
  //  };
  //
  private static String plusMinus(String strT, float x, String sx, boolean allowFractions) {
    float a;
    return (x == 0 ? ""
        : (x < 0 ? "-" : strT.length() == 0 ? "" : "+")
            + (x == 1 || x == -1 ? "" : (a = Math.abs(x)) < 1 && allowFractions ? twelfthsOf(a * 12) : "" + (int) a)) + sx;
  }

  private static float normalizeTwelfths(float iValue, int divisor,
                                         boolean doNormalize) {
    iValue *= divisor;
    int half = divisor / 2;
    if (doNormalize) {
      while (iValue > half)
        iValue -= divisor;
      while (iValue <= -half)
        iValue += divisor;
    }
    return iValue;
  }

  //  private static float normalize48ths(float iValue, boolean doNormalize) {
  //    iValue *= 48;
  //    if (doNormalize) {
  //      while (iValue > 24)
  //        iValue -= 48;
  //      while (iValue <= -24)
  //        iValue += 48;
  //    }
  //    return iValue;
  //  }
  //
  final static String[] labelsXYZ = new String[] { "x", "y", "z" };
  final static String[] labelsXn = new String[] { "x1", "x2", "x3", "x4", "x5",
      "x6", "x7", "x8", "x9", "x10", "x11", "x12", "x13" };
  final static String[] labelsXnSub = new String[] { "x", "y", "z", "a", "b",
      "c", "d", "e", "f", "g", "h", "i", "j" };

  final public static String getXYZFromMatrix(M4 mat, boolean is12ths,
                                              boolean allPositive,
                                              boolean halfOrLess) {
    return getXYZFromMatrixFrac(mat, is12ths, allPositive, halfOrLess, false);
  }
  
  final public static String getXYZFromMatrixFrac(M4 mat, boolean is12ths,
                                              boolean allPositive,
                                              boolean halfOrLess, boolean allowFractions) {
    String str = "";
    SymmetryOperation op = (mat instanceof SymmetryOperation
        ? (SymmetryOperation) mat
        : null);
    if (op != null && op.modDim > 0)
      return getXYZFromRsVs(op.rsvs.getRotation(), op.rsvs.getTranslation(),
          is12ths);
    float[] row = new float[4];
    int denom = (int) mat.getElement(3, 3);
    if (denom == 1)
      denom = 12;
    else
      mat.setElement(3, 3, 1);
    for (int i = 0; i < 3; i++) {
      int lpt = (i < 3 ? 0 : 3);
      mat.getRow(i, row);
      String term = "";
      for (int j = 0; j < 3; j++) {
        float x = row[j];
        if (approx(x) != 0) {
          term += plusMinus(term, x, labelsXYZ[j + lpt], allowFractions);
        }
      }
      if ((is12ths ? row[3] : approx(row[3])) != 0)
        term += xyzFraction12((is12ths ? row[3] : row[3] * denom), denom,
            allPositive, halfOrLess);
      str += "," + term;
    }
    return str.substring(1);
  }

  V3[] rotateAxes(V3[] vectors, UnitCell unitcell, P3 ptTemp, M3 mTemp) {
    V3[] vRot = new V3[3];
    getRotationScale(mTemp);
    for (int i = vectors.length; --i >= 0;) {
      ptTemp.setT(vectors[i]);
      unitcell.toFractional(ptTemp, true);
      mTemp.rotate(ptTemp);
      unitcell.toCartesian(ptTemp, true);
      vRot[i] = V3.newV(ptTemp);
    }
    return vRot;
  }

  public String fcoord2(T3 p) {
    if (divisor == 12)
      return fcoord(p);
    return fc2((int) linearRotTrans[3]) + " " + fc2((int) linearRotTrans[7]) + " " + fc2((int) linearRotTrans[11]);
  }

  /**
   * Get string version of fraction when divisor == 0 
   * 
   * @param num
   * @return "1/2" for example
   */
  private String fc2(int num) {
      int denom = (num & DIVISOR_MASK);
      num = num >> DIVISOR_OFFSET;
    return (num == 0 ? "0" : num + "/" + denom);
  }

  /**
   * Get string version of fraction 
   * 
   * @param p
   * @return "1/2" for example
   */
  static String fcoord(T3 p) {
    // Castep reader only
    return fc(p.x) + " " + fc(p.y) + " " + fc(p.z);
  }

  private static String fc(float x) {
    // Castep reader only
    float xabs = Math.abs(x);
    String m = (x < 0 ? "-" : "");
    int x24 = (int) approx(xabs * 24);
    if (x24 / 24f == (int) (x24 / 24f))
      return m + (x24 / 24);
    if (x24 % 8 != 0) {
      return m + twelfthsOf(x24 >> 1);
    }
    return (x24 == 0 ? "0" : x24 == 24 ? m + "1" : m + (x24 / 8) + "/3");
  }
  
  static float approx(float f) {
    return PT.approx(f, 100);
  }

  static float approx6(float f) {
    return PT.approx(f, 1000000);
  }
  
  static String getXYZFromRsVs(Matrix rs, Matrix vs, boolean is12ths) {
    double[][] ra = rs.getArray();
    double[][] va = vs.getArray();
    int d = ra.length;
    String s = "";
    for (int i = 0; i < d; i++) {
      s += ",";
      for (int j = 0; j < d; j++) {
        double r = ra[i][j];
        if (r != 0) {
          s += (r < 0 ? "-" : s.endsWith(",") ? "" : "+")
              + (Math.abs(r) == 1 ? "" : "" + (int) Math.abs(r)) + "x"
              + (j + 1);
        }
      }
      s += xyzFraction12((int) (va[i][0] * (is12ths ? 1 : 12)), 12, false, true);
    }
    return PT.rep(s.substring(1), ",+", ",");
  }

  @Override
  public String toString() {
    return (rsvs == null ? super.toString()
        : super.toString() + " " + rsvs.toString());
  }

  /**
   * Magnetic spin is a pseudo (or "axial") vector. This means that it acts as a
   * rotation, not a vector. When a rotation about x is passed through the
   * mirror plane xz, it is reversed; when it is passed through the mirror plane
   * yz, it is not reversed -- exactly opposite what you would imagine from a
   * standard "polar" vector.
   * 
   * For example, a vector perpendicular to a plane of symmetry (det=-1) will be
   * flipped (m=1), while a vector parallel to that plane will not be flipped
   * (m=-1)
   * 
   * In addition, magnetic spin operations have a flag m=1 or m=-1 (m or -m)
   * that indicates how the vector quantity changes with symmetry. This is
   * called "time reversal" and stored here as timeReversal.
   * 
   * To apply, timeReversal must be multiplied by the 3x3 determinant, which is
   * always 1 (standard rotation) or -1 (rotation-inversion). This we store as
   * magOp. See https://en.wikipedia.org/wiki/Pseudovector
   * 
   * @return +1, -1, or 0
   */
  int getMagneticOp() {
    return (magOp == Integer.MAX_VALUE ? magOp = (int) (determinant3() * timeReversal)
        : magOp);
  }

  /**
   * set the time reversal, and indicate internally in xyz as appended ",m" or
   * ",-m"
   * 
   * @param magRev
   */
  void setTimeReversal(int magRev) {
    timeReversal = magRev;
    if (xyz.indexOf("m") >= 0)
      xyz = xyz.substring(0, xyz.indexOf("m"));
    if (magRev != 0) {
      xyz += (magRev == 1 ? ",m" : ",-m");
    }
  }

  /**
   * assumption here is that these are in order of sets, as in ITA
   * 
   * @return centering
   */
  V3 getCentering() {
    if (!isFinalized)
      doFinalize();
    if (centering == null && !unCentered) {
      if (modDim == 0 && m00 == 1 && m11 == 1 && m22 == 1 && m01 == 0
          && m02 == 0 && m10 == 0 && m12 == 0 && m20 == 0 && m21 == 0
          && (m03 != 0 || m13 != 0 || m23 != 0)) {
        isCenteringOp = true;
        centering = V3.new3( m03, m13, m23);
      } else {
        unCentered = true;
        centering = null;
      }
    }
    return centering;
  }

  String fixMagneticXYZ(M4 m, String xyz, boolean addMag) {
    if (timeReversal == 0)
      return xyz;
    int pt = xyz.indexOf("m");
    pt -= (3 - timeReversal) / 2;
    xyz = (pt < 0 ? xyz : xyz.substring(0, pt));
    if (!addMag)
      return xyz + (timeReversal > 0 ? " +1" : " -1");
    M4 m2 = M4.newM4(m);
    m2.m03 = m2.m13 = m2.m23 = 0;
    if (getMagneticOp() < 0)
      m2.scale(-1); // does not matter that we flip m33 - it is never checked
    xyz += "(" + PT.rep(PT
        .rep(PT.rep(getXYZFromMatrix(m2, false, false, false),
            "x", "mx"), "y", "my"),
        "z", "mz") + ")";
    return xyz;
  }

  public Map<String, Object> getInfo() {
    if (info == null) {
      info = new Hashtable<String, Object>();
      info.put("xyz", xyz);
      if (centering != null)
        info.put("centering", centering);
      info.put("index", Integer.valueOf(number - 1));
      info.put("isCenteringOp", Boolean.valueOf(isCenteringOp));
      if (linearRotTrans != null)
        info.put("linearRotTrans", linearRotTrans);
      info.put("modulationDimension", Integer.valueOf(modDim));
      info.put("matrix", M4.newM4(this));
      if (magOp != Float.MAX_VALUE)
        info.put("magOp", Float.valueOf(magOp));
      info.put("id", Integer.valueOf(opId));
      info.put("timeReversal", Integer.valueOf(timeReversal));
      if (xyzOriginal != null)
        info.put("xyzOriginal", xyzOriginal);
    }
    return info;
  }

  /**
   * Adjust the translation for this operator so that it moves the center of
   * mass of the full set of atoms into the cell.
   * 
   * @param dim
   * @param m
   * @param atoms
   * @param atomIndex first index
   * @param count number of atoms
   */
  public static void normalizeOperationToCentroid(int dim, M4 m, P3[] atoms, int atomIndex, int count) {
    if (count <= 0)
      return;
    float x = 0;
    float y = 0;
    float z = 0;
    if (atomTest == null)
      atomTest = new P3();
    for (int i = atomIndex, i2 = i + count; i < i2; i++) {
      Symmetry.newPoint(m, atoms[i], 0, 0, 0, atomTest);
      x += atomTest.x;
      y += atomTest.y;
      z += atomTest.z;
    }
    x /= count;
    y /= count;
    z /= count;
    while (x < -0.001 || x >= 1.001) {
      m.m03 += (x < 0 ? 1 : -1);
      x += (x < 0 ? 1 : -1);
    }
    if (dim > 1)
    while (y < -0.001 || y >= 1.001) {
      m.m13 += (y < 0 ? 1 : -1);
      y += (y < 0 ? 1 : -1);
    }
    if (dim > 2)
    while (z < -0.001 || z >= 1.001) {
      m.m23 += (z < 0 ? 1 : -1);
      z += (z < 0 ? 1 : -1);
    }
  }

  public static Lst<P3> getLatticeCentering(SymmetryOperation[] ops) {
    Lst<P3> list = new Lst<P3>();
    for (int i = 0; i < ops.length; i++) {
      T3 c = (ops[i]  == null ? null : ops[i].getCentering());
      if (c != null)
        list.addLast(P3.newP(c));
    }
    return list;
  }

  public Boolean getOpIsCCW() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opIsCCW;
  }

  public int getOpType() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opType;
  }

  public int getOpOrder() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opOrder;
  }
  
  public P3 getOpPoint() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opPoint;
  }

  public V3 getOpAxis() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opAxis;
  }
  public P3 getOpPoint2() {
   return opPoint2;
  }

  public V3 getOpTrans() {
    if (opType == TYPE_UNKNOWN) {
      setOpTypeAndOrder();
    }
    return opTrans;
  }

  private final static P3 x = P3.new3((float)Math.PI, (float)Math.E, (float)(Math.PI * Math.E));
  
  /**
   * The problem is that the 3-fold axes for cubic groups
   * have (seemingly arbitrary assignments for axes
   */
  private final static int[] C3codes = { //
      0x031112, // 3+(-1 1-1)
      0x121301, // 3-(-1 1-1)
      0x130112, // 3+(-1-1 1)
      0x021311, // 3-(-1-1 1)
      0x130102, // -3+(-1 1-1)
      0x020311, // -3-(-1 1-1)
      0x031102, // -3+(-1-1 1)
      0x120301, // -3-( 1-1-1)
  };
  
  private static int opGet3code(M4 m) {
    int c = 0;
    float[] row = new float[4];
    for (int r = 0; r < 3; r++) {
      m.getRow(r,  row);
      for (int i = 0; i < 3; i++) {
        switch ((int) row[i]) {
        case 1:
          c |= (i+1) << ((2-r)<<3);
          break;
        case -1:
          c |= (0x10 + i+1) << ((2-r)<<3);
          break;
        }
      }
    }
    return c;
  }
  
  //private static V3 xpos;
  private static V3 xneg;
  private static T3 opGet3x(M4 m) {
    if (m.m22 != 0) // z-axis
      return x;
    int c = opGet3code(m);
    for (int i = 0; i < 8; i++)
      if (c == C3codes[i]) {
        if (xneg == null) {
          xneg = V3.newV(x);
          xneg.scale(-1);
        }
        return xneg;
      }
    return x;
  }

  private void setOpTypeAndOrder() {
    clearOp();
    // From International Tables for Crystallography, Volume A, Chapter 1.2, by H. Wondratschek and M. I. Aroyo
    int det = (int) Math.round(determinant3());
    int trace = (int) Math.round(m00 + m11 + m22);
    //int code1 = ((trace + 3) << 1) + ((det + 1) >> 1);
    int order = 0;
    // handle special cases of identity and inversion
    int angle = 0;
    T3 px = x;
    switch (trace) {
    case 3:
      if (hasTrans(this)) {
        opType = TYPE_TRANSLATION;
        opTrans = new V3();
        getTranslation(opTrans);
        opOrder = 2;
      } else {
        opType = TYPE_IDENTITY;
        opOrder = 1;
      }
      return;
    case -3:
      opType = TYPE_INVERSION;
      order = 2;
      break;
    default:
      // not identity or inversion
      order = trace * det + 3; // will be 2, 3, 4, or 5
      if (order == 5)
        order = 6;
      if (det > 0) {
        opType = TYPE_ROTATION;
        angle = (int) (Math.acos((trace - 1) / 2f) * 180 / Math.PI);
        if (angle == 120) {
          if (opX == null)
            opX = opGet3x(this);
          px = opX;
        }
      } else {
        // negative determinant
        // not simple rotation    
        if (order == 2) {
          opType = TYPE_REFLECTION;
        } else {
          opType = TYPE_ROTOINVERSION;
          if (order == 3)
            order = 6;
          angle = (int) (Math.acos((-trace - 1) / 2f) * 180 / Math.PI);
          if (angle == 120) {
            if (opX == null)
              opX = opGet3x(this);
            px = opX;
          }
        }
      }
      break;
    }
    opOrder = order;
    M4 m4 = new M4();
    P3 p1 = new P3(); // PURPOSELY 0 0 0
    P3 p2 = P3.newP(px);

    m4.setM4(this);
    P3 p1sum = new P3();
    P3 p2sum = P3.newP(p2);
    P3 p2odd = new P3();
    P3 p2even = P3.newP(p2);
    P3 p21 = new P3();
    for (int i = 1; i < order; i++) {
      m4.mul(this);
      rotTrans(p1);
      rotTrans(p2);
      if (i == 1)
        p21.setT(p2);
      p1sum.add(p1);
      p2sum.add(p2);
      if (opType == TYPE_ROTOINVERSION) {
        if (i % 2 == 0) {
          p2even.add(p2);
        } else {
          p2odd.add(p2);
        }
      }
    }
    opTrans = new V3();
    m4.getTranslation(opTrans);
    opTrans.scale(1f / order);
    float d = approx6(opTrans.length());
    float dmax = 1;
    opPoint = new P3();
    V3 v = null;
    boolean isOK = true;
    switch (opType) {
    case TYPE_INVERSION:
      // just get average of p2 and x
      p2sum.add2(p2, px);
      p2sum.scale(0.5f);
      opPoint = P3.newP(opClean6(p2sum));
      isOK = checkOpPoint(opPoint);
      break;
    case TYPE_ROTOINVERSION:
      // get the vector from the centroids off the odd and even sets
      p2odd.scale(2f / order);
      p2even.scale(2f / order);
      v = V3.newVsub(p2odd, p2even);
      v.normalize();
      opAxis = (V3) opClean6(v);
      //      opAxisCode = opGetAxisCode(opAxis);
      p1sum.add2(p2odd, p2even);
      p2sum.scale(1f / order);
      opPoint.setT(opClean6(p2sum));
      isOK = checkOpPoint(opPoint);
      if (angle != 180) {
        p2.cross(px, p2);
        opIsCCW = Boolean.valueOf(p2.dot(v) < 0);
      }
      break;
    case TYPE_ROTATION:
      // the sum divided by the order gives us a point on the vector
      // both {0 0 0} and x will be translated by the same amount
      // so their difference will be the rotation vector
      // but this could be reversed if no translation

      //      rotTrans(p1);
      //      p1.scale(1f / order);
      //      d = approx6(p1.length()); // because we started at 0 0 0
      v = V3.newVsub(p2sum, p1sum);
      v.normalize();
      opAxis = (V3) opClean6(v);
      // for the point, we do a final rotation on p1 to get it full circle
      p1sum.scale(1f / order);
      p1.setT(p1sum);
      if (d > 0) {
        p1sum.sub(opTrans);
      }
      opPoint.setT(opClean6(p1sum));
      // average point for origin
      if (angle != 180) {
        p2.cross(px, p2);
        opIsCCW = Boolean.valueOf(p2.dot(v) < 0);
      }
      isOK &= checkOpAxis(p1, (d == 0 ? opAxis : opTrans), p1sum, new V3(),
          new V3(), null);
      if (isOK) {
        opPoint.setT(p1sum);
        if (checkOpAxis(opPoint, opAxis, p1sum, new V3(), new V3(),
            opPoint)) {
          opPoint2 = P3.newP(p1sum);
          // was for vertical offset of screw components 4(+) and 4(-)
          //          if (order != 2 && opIsCCW == Boolean.FALSE && d > 0 && d <= 0.5f) {
          //            opPoint.add(opTrans);
          //          }
        }
        if (d > 0) {
          // all screws must start and terminate within the cell
          p1sum.scaleAdd2(0.5f, opTrans, opPoint);
          //          p1sum.add2(opPoint, opTrans);
          isOK = checkOpPoint(p1sum);
          if (opPoint2 != null) {
            // or at least half...
            p1sum.scaleAdd2(0.5f, opTrans, opPoint2);
            if (!checkOpPoint(p1sum))
              opPoint2 = null;
          }
        }
      }
      // real question here...
      // problem here with p1 not being a vector, just the base point along the axis.
      if (v.dot(p1) < 0) {
        isOK = false;
      }
      if (d > 0 && opTrans.z == 0 && opTrans.lengthSquared() == 1.25f) {
    	  // SG 177
        dmax = 1.25f;
        opIsLong = true;
      } else {
        dmax = 1.0f;
      }
      break;
    case TYPE_REFLECTION:
      // first plane point is half way from 0 to p1 - trans
      p1.sub(opTrans);
      p1.scale(0.5f);
      opPoint.setT(p1);
      // p2 - px - opTrans gets us the plane's normal (opAxis)
      // (we don't do this with origin because it is likely on the plane)
      p21.sub(opTrans);
      opAxis = V3.newVsub(p21, px);
      p2.scaleAdd2(0.5f, opAxis, px);
      opAxis.normalize();
      opPlane = new P4();
      p1.set(px.x + 1.1f, px.y + 1.7f, px.z + 2.1f); // just need a third point
      p1.scale(0.5f);
      rotTrans(p1);
      p1.sub(opTrans);
      p1.scaleAdd2(0.5f, px, p1);
      p1.scale(0.5f);
      v = new V3();
      isOK = checkOpPlane(opPoint, p1, p2, opAxis, opPlane, v, new V3());
      opClean6(opPlane);
      if (approx6(opPlane.w) == 0)
        opPlane.w = 0;
      approx6Pt(opAxis);
      normalizePlane(opPlane);
//      
//      opAxis.setT(opPlane);
      if (d > 0 && 
          (opTrans.z == 0 && opTrans.lengthSquared() == 1.25f
          || opTrans.z == 0.5f && opTrans.lengthSquared() == 1.5f)) {
        // SG 186
        // +/-0.5x +/-y, +/-x +/-0.5y
        dmax = 1.25f;
        opIsLong = true;
      } else {
        dmax = 0.78f;
      }
      break;
    }
    if (d > 0) {
      opClean6(opTrans);
      if (opType == TYPE_REFLECTION) {
        // being careful here not to disallow this for vertical planes in #156; only for #88
        if ((opTrans.x == 1 || opTrans.y == 1 || opTrans.z == 1) && m22 == -1)
          isOK = false;
      }
      opType |= TYPE_TRANSLATION;
      if (Math.abs(approx(opTrans.x)) >= dmax
          || Math.abs(approx(opTrans.y)) >= dmax
          || Math.abs(approx(opTrans.z)) >= dmax) {
        isOK = false;
      }
    } else {
      opTrans = null;
    }
    if (!isOK) {
      isIrrelevant = true;
    }
  }

  //  private static String opGetAxisCode(V3 v) {
  //    float d = Double.MAX_VALUE;
  //    if (v.x != 0 && Math.abs(v.x)< d)
  //       d = Math.abs(v.x);
  //    if (v.y != 0 && Math.abs(v.y)< d)
  //      d = Math.abs(v.y);
  //    if (v.z != 0 && Math.abs(v.z)< d)
  //      d = Math.abs(v.z);
  //    V3 v1 = V3.newV(v);
  //    v1.scale(1/d);
  //    return "" + ((int)approx(v1.x)) + ((int)approx(v1.y)) + ((int)approx(v1.z));
  //  }

  private static void normalizePlane(P4 plane) {
    approx6Pt(plane);
    plane.w = approx6(plane.w);
    if (plane.w > 0 
        || plane.w == 0 && (plane.x < 0 
                || plane.x == 0 && plane.y < 0 
                || plane.y == 0 && plane.z < 0)) {
      plane.scale4(-1);
    }
    // unsure no -0 values; we need this for the maps
    opClean6(plane);
    plane.w = approx6(plane.w);
  }

  private static boolean isCoaxial(T3 v) {
    return (Math.abs(approx(v.x)) == 1 || Math.abs(approx(v.y)) == 1 || Math.abs(approx(v.z)) == 1);
  }
  private void clearOp() {
    if (!isFinalized)
      doFinalize();
    isIrrelevant = false;
    opTrans = null;
    opPoint = opPoint2 = null;
    opPlane = null;
    opIsCCW = null;
    opIsLong = false;
  }

  private static boolean hasTrans(M4 m4) {
    return (approx6(m4.m03) != 0 || approx6(m4.m13) != 0 || approx6(m4.m23) != 0);
  }
  
  private static P4[] opPlanes;

  private static boolean checkOpAxis(P3 pt, V3 axis, P3 ptRet, V3 t1,
                                     V3 t2, P3 ptNot) {
    if (opPlanes == null) {
      opPlanes = BoxInfo.getBoxFacesFromOABC(null);
    }
    int[] map = BoxInfo.faceOrder;
    float f = (ptNot == null ? 1 : -1);
    for (int i = 0; i < 6; i++) {
      P3 p = Measure.getIntersection(pt, axis, opPlanes[map[i]], ptRet, t1, t2);
      if (p != null && checkOpPoint(p) && axis.dot(t1) * f < 0 && (ptNot == null || approx(ptNot.distance(p) - 0.5f) >= 0)) {
        return true;
      }
    }
 
    return false;
  }

  static T3 opClean6(T3 t) {
    if (approx6(t.x) == 0)
      t.x = 0;
    if (approx6(t.y)== 0)
      t.y = 0;
    if (approx6(t.z) == 0)
      t.z = 0;
    return t;
  }

  static boolean checkOpPoint(T3 pt) {
    return checkOK(pt.x, 0) && checkOK(pt.y, 0) && checkOK(pt.z, 0);
  }

  private static boolean checkOK(float p, float a) {
    return (a != 0 || approx(p) >= 0 && approx(p) <= 1);
  }

  private static boolean checkOpPlane(P3 p1, P3 p2, P3 p3, V3 v, P4 plane, V3 vtemp1, V3 vtemp2) {
    // just check all 8 cell points for directed distance to the plane
    // any mix of + and - and 0 is OK; all + or all - is a fail
    Measure.getPlaneThroughPoints(p1, p2, p3, vtemp1, vtemp2, plane);
    //System.out.println( "draw plane " + p1 + p2 + p3 + "//" + v + vtemp1 + vtemp2);
    
    P3[] pts = BoxInfo.unitCubePoints;
    int nPos = 0;
    int nNeg = 0;
    for (int i = 8; --i >= 0;) {
      float d = Measure.getPlaneProjection(pts[i], plane, p1, vtemp1);
      switch ((int) Math.signum(approx6(d))) {
      case 1:
        if (nNeg > 0)
          return true;
        nPos++;
        break;
      case 0:
        break;
      case -1:
        if (nPos > 0)
          return true;
        nNeg++;
      }
    }
    // all + or all - means the plane is out of scope
    return !(nNeg == 8 || nPos == 8);
  }
  

  public static SymmetryOperation[] getAdditionalOperations(SymmetryOperation[] ops) {
    int n = ops.length;
    Lst<SymmetryOperation> lst = new Lst<SymmetryOperation>();
    HashSet<String> xyzLst = new HashSet<String>();

    Map<String, Lst<SymmetryOperation>> mapPlanes = new Hashtable<String, Lst<SymmetryOperation>>();
    for (int i = 0; i < n; i++) {
      SymmetryOperation op = ops[i];
      lst.addLast(op);
      String s = op.getOpName(PLANE_MODE_NOTRANS);
      xyzLst.add(s + ";");
      if ((op.getOpType() & TYPE_REFLECTION) != 0)
        addPlaneMap(mapPlanes, op);
    }
    for (int i = 1; i < n; i++) { // skip x,y,z
      ops[i].addOps(xyzLst, lst, mapPlanes, n, i);
    }
    //System.out.println("SO TEST " + xyzLst.toString().replace(';', '\n') + "\n" + mapPlanes);
    return lst.toArray(new SymmetryOperation[lst.size()]);
  }

  /**
   * add translated copies of this operation that contribute to the unit cell
   * [0,1]
   * 
   * @param xyzList
   * @param lst
   * @param mapPlanes
   * @param n0
   */
  void addOps(HashSet<String> xyzList, Lst<SymmetryOperation> lst,
              Map<String, Lst<SymmetryOperation>> mapPlanes, int n0, int isym) {
    V3 t0 = new V3();
    getTranslation(t0);
    boolean isPlane = ((getOpType() & TYPE_REFLECTION) == TYPE_REFLECTION);
    V3 t = new V3();

    SymmetryOperation opTemp = null;
    // from -2 to 2, starting with + so that we get the + version
    for (int i = 3; --i >= -2;) {
      for (int j = 3; --j >= -2;) {
        for (int k = 3; --k >= -2;) {
          if (opTemp == null)
            opTemp = new SymmetryOperation(null, 0, false);
          t.set(i, j, k);
          if (checkOpSimilar(t))
            continue;
          if (opTemp.opCheckAdd(this, t0, n0, t, xyzList, lst, isym + 1)) {
            if (isPlane)
              addPlaneMap(mapPlanes, opTemp);
            opTemp = null;
          }
        }
      }
    }
  }

  /**
   * Looking for coincidence. We only concern ourselves if there is 
   * at least one non-glide reflection.
   * 
   * @param mapPlanes
   * @param op
   */
  private static void addPlaneMap(Map<String, Lst<SymmetryOperation>> mapPlanes,
                             SymmetryOperation op) {
    String s = op.getOpName(PLANE_MODE_POSITION_ONLY);
    Lst<SymmetryOperation> l = mapPlanes.get(s);
    //System.out.println("SO ====" + s + "====" + op.getOpName(PLANE_MODE_FULL));
    op.isCoincident = false;
    boolean havePlane = (op.opType == TYPE_REFLECTION);
    if (l == null) {
      mapPlanes.put(s, l = new Lst<SymmetryOperation>());
    } else {
      SymmetryOperation op0 = l.get(0);
      if (op0.isCoincident) {
           op.isCoincident = true;
      } else if (havePlane || (op0.opType == TYPE_REFLECTION)) {
        op.isCoincident = true;
        for (int i = l.size(); --i >= 0;) {        
          l.get(i).isCoincident = true;
        }
      }
    }
    l.addLast(op);
  }

  /**
   * No need to check lattice translations that are only
   * going to contribute to the inherent translation 
   * of the element. Yes, these exist. But they are 
   * inconsequential and are never shown.
   * 
   * Reflections: anything perpendicular to the normal is discarded.
   * 
   * Rotations: anything parallel to the normal is discarded.
   *  
   * @param t
   * @return true if 
   */
  private boolean checkOpSimilar(V3 t) {
    switch (getOpType() &~ TYPE_TRANSLATION) {
    default:
      return false;
    case TYPE_IDENTITY:
      return true;
    case TYPE_ROTATION: // includes screw rotation
      return (approx6(t.dot(opAxis) - t.length()) == 0);
    case TYPE_REFLECTION: // includes glide reflection
      return (approx6(t.dot(opAxis)) == 0);
    }
  }

  /**
   * @param opThis 
   * @param t0 
   * @param n0  
   * @param t 
   * @param xyzList 
   * @param lst 
   * @return true if added
   */
  private boolean opCheckAdd(SymmetryOperation opThis, V3 t0, int n0, V3 t,
                          HashSet<String> xyzList, Lst<SymmetryOperation> lst, int itno) {
    //int nnew = 0;
    setM4(opThis);
    V3 t1 = V3.newV(t);
    t1.add(t0);
    

    
    setTranslation(t1);
    isFinalized = true;
    if (itno == 11 && t.x == 1 && t.y == 0 && t.z == 0)
      System.out.println("SO test" + opThis);
    setOpTypeAndOrder();
    if (!isIrrelevant && opType != TYPE_IDENTITY && opType != TYPE_TRANSLATION) {
      String s = getOpName(PLANE_MODE_NOTRANS) + ";";
      if (!xyzList.contains(s)) {
        xyzList.add(s);
        lst.addLast(this);
        isFinalized = true;
        xyz = getXYZFromMatrix(this, false, false, false);
        return true;
      }
    }
    return false;
  }

  static void approx6Pt(T3 pt) {
    if (pt != null) {
      pt.x = approx6(pt.x);
      pt.y = approx6(pt.y);
      pt.z = approx6(pt.z);
    }
  }

  public static void normalize12ths(V3 vtrans) {
    vtrans.x = PT.approx(vtrans.x, 12);
    vtrans.y = PT.approx(vtrans.y, 12);
    vtrans.z = PT.approx(vtrans.z, 12);    
  }

  public String getCode() {
    if (opAxisCode != null) {
      return opAxisCode;
    }
    char t = getOpName(PLANE_MODE_FULL).charAt(0); // four bits
    int o = opOrder; // 1,2,3,4,6   // 3 bits
    int ccw = (opIsCCW == null ? 0 : opIsCCW == Boolean.TRUE ? 1 : 2);
    String g = "", m = "";
    switch (t) {
    case 'G':
      t = getGlideFromTrans(opTrans, opPlane);
      //a,b,c,n,g
      //$FALL-THROUGH$
    case 'P':
      if (!isCoaxial(opAxis)) {
        t = (t == 'P' ? 'p' : (char) (t - 32));
      }
      break;
    case 'S':
      float d = opTrans.length();
      if (opIsCCW != null && (d < (d > 1 ? 6 : 0.5f)) == (opIsCCW == Boolean.TRUE))
        t = 'w';
      break;
    case 'R':
      if (!isCoaxial(opAxis)) {
        t = 'o';
      }
      if (opPoint.length() == 0)
        t = (t == 'o' ? 'q' : 'Q');
      break;
    default:
      break;
    }
    String s = g + m + t + "."
        + ((char) ('0' + o)) + "." + ccw + "."
    //+ ((char)('@' + o * 2 + ccw))
    ;
//    System.out.println("!!" + s + " " + getOpName(PLANE_MODE_FULL));
    return opAxisCode = s;
  }
  public static char getGlideFromTrans(T3 ftrans, T3 ax1) {
    float fx = Math.abs(approx(ftrans.x * 12));
    float fy = Math.abs(approx(ftrans.y * 12));
    float fz = Math.abs(approx(ftrans.z * 12));
    if (fx == 9)
      fx = 3;
    if (fy == 9)
      fy = 3;
    if (fz == 9)
      fz = 3;
    if (fx != 0 && fy != 0 && fz != 0) {
      return (fx == 3 && fy == 3
          && fz == 3 ? 'd'
              : fx == 6 && fy == 6 && fz == 6 ? 'n' 
              : 'g');
    }
    if (fx != 0 && fy != 0 
        || fy != 0 && fz != 0 
        || fz != 0 && fx != 0) {
      // any two
      if (fx == 3 && fy == 3 || fx == 3 && fz == 3
          || fy == 3 && fz == 3) {
        return 'd';
      }
      if (fx == 6 && fy == 6 || fx == 6 && fz == 6
          || fy == 6 && fz == 6) {
        // making sure here that this is truly a diagonal in the plane, not just
        // a glide parallel to a face on a diagonal plane! Mois Aroyo 2018
        if (fx == 0 && ax1.x == 0 || fy == 0 && ax1.y == 0
            || fz == 0 && ax1.z == 0) {
          return 'g';
        }
        return 'n';
      }
      return 'g';
    }
    return (fx != 0 ? 'a' : fy != 0 ? 'b' : 'c');
  }
  

  // https://crystalsymmetry.wordpress.com/space-group-diagrams/
 
}
