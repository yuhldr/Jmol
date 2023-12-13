/* $RCSfiodelle$allrueFFFF
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

import java.io.BufferedReader;
import java.util.Map;

import org.jmol.api.AtomIndexIterator;
import org.jmol.api.GenericPlatform;
import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.bspt.Bspt;
import org.jmol.bspt.CubeIterator;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.Escape;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Tensor;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.Rdr;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

/* Symmetry is a wrapper class that allows access to the package-local
 * classes PointGroup, SpaceGroup, SymmetryInfo, and UnitCell.
 * 
 * When symmetry is detected in ANY model being loaded, a SymmetryInterface
 * is established for ALL models.
 * 
 * The SpaceGroup information could be saved with each model, but because this 
 * depends closely on what atoms have been selected, and since tracking that with atom
 * deletion is a bit complicated, instead we just use local instances of that class.
 * 
 * The three PointGroup methods here could be their own interface; they are just here
 * for convenience.
 * 
 * The file readers use SpaceGroup and UnitCell methods
 * 
 * The modelSet and modelLoader classes use UnitCell and SymmetryInfo 
 * 
 */
public class Symmetry implements SymmetryInterface {
  // NOTE: THIS CLASS IS VERY IMPORTANT.
  // IN ORDER TO MODULARIZE IT, IT IS REFERENCED USING 
  // xxxx = Interface.getSymmetry();

  SpaceGroup spaceGroup;
  private PointGroup pointGroup;
  private SymmetryInfo symmetryInfo;
  private UnitCell unitCell;
  private CIPChirality cip;
  private static WyckoffFinder wyckoffFinder;
  private boolean isBio;

  @Override
  public boolean isBio() {
    return isBio;
  }

  int test = 1;
  
  public Symmetry() {
    // instantiated ONLY using
    // symmetry = Interface.getSymmetry();
    // DO NOT use symmetry = new Symmetry();
    // as that will invalidate the Jar file modularization    
  }

  @Override
  public SymmetryInterface setPointGroup(SymmetryInterface siLast, T3 center,
                                         T3[] atomset, BS bsAtoms,
                                         boolean haveVibration,
                                         float distanceTolerance,
                                         float linearTolerance,
                                         int maxAtoms, boolean localEnvOnly) {
    pointGroup = PointGroup.getPointGroup(
        siLast == null ? null : ((Symmetry) siLast).pointGroup, center, atomset,
        bsAtoms, haveVibration, distanceTolerance, linearTolerance, maxAtoms,
        localEnvOnly);
    return this;
  }

  @Override
  public String getPointGroupName() {
    return pointGroup.getName();
  }

  @Override
  public Object getPointGroupInfo(int modelIndex, String drawID, boolean asInfo,
                                  String type, int index, float scale) {
    if (drawID == null && !asInfo && pointGroup.textInfo != null)
      return pointGroup.textInfo;
    else if (drawID == null && pointGroup.isDrawType(type, index, scale))
      return pointGroup.drawInfo;
    else if (asInfo && pointGroup.info != null)
      return pointGroup.info;
    return pointGroup.getInfo(modelIndex, drawID, asInfo, type, index, scale);
  }

  // SpaceGroup methods

  @Override
  public void setSpaceGroup(boolean doNormalize) {
    if (spaceGroup == null)
      spaceGroup = SpaceGroup.getNull(true, doNormalize, false);
  }

  @Override
  public int addSpaceGroupOperation(String xyz, int opId) {
    return spaceGroup.addSymmetry(xyz, opId, false);
  }

  @Override
  public int addBioMoleculeOperation(M4 mat, boolean isReverse) {
    isBio = spaceGroup.isBio = true;
    return spaceGroup.addSymmetry((isReverse ? "!" : "") + "[[bio" + mat, 0,
        false);
  }

  @Override
  public void setLattice(int latt) {
    spaceGroup.setLatticeParam(latt);
  }

  @Override
  public Object getSpaceGroup() {
    return spaceGroup;
  }

  /**
   * 
   * @param desiredSpaceGroupIndex
   * @param name
   * @param data
   *        a Lst<SymmetryOperation> or Lst<M4>
   * @param modDim
   *        in [3+d] modulation dimension
   * @return true if a known space group
   */
  @Override
  public boolean createSpaceGroup(int desiredSpaceGroupIndex, String name,
                                  Object data, int modDim) {
    spaceGroup = SpaceGroup.createSpaceGroup(desiredSpaceGroupIndex, name, data,
        modDim);
    if (spaceGroup != null && Logger.debugging)
      Logger.debug("using generated space group " + spaceGroup.dumpInfo());
    return spaceGroup != null;
  }

  @Override
  public Object getSpaceGroupInfoObj(String name, float[] params,
                                     boolean isFull, boolean addNonstandard) {
    return SpaceGroup.getInfo(spaceGroup, name, params, isFull,
        addNonstandard);
  }

  @Override
  public Object getLatticeDesignation() {
    return spaceGroup.getLatticeDesignation();
  }

  @Override
  public void setFinalOperations(int dim, String name, P3[] atoms,
                                 int iAtomFirst, int noSymmetryCount,
                                 boolean doNormalize, String filterSymop) {
    if (name != null && (name.startsWith("bio") || name.indexOf(" *(") >= 0)) // filter SYMOP
      spaceGroup.name = name;
    if (filterSymop != null) {
      Lst<SymmetryOperation> lst = new Lst<SymmetryOperation>();
      lst.addLast(spaceGroup.operations[0]);
      for (int i = 1; i < spaceGroup.operationCount; i++)
        if (filterSymop.contains(" " + (i + 1) + " "))
          lst.addLast(spaceGroup.operations[i]);
      spaceGroup = SpaceGroup.createSpaceGroup(-1,
          name + " *(" + filterSymop.trim() + ")", lst, -1);
    }
    spaceGroup.setFinalOperationsForAtoms(dim, atoms, iAtomFirst,
        noSymmetryCount, doNormalize);
  }

  @Override
  public M4 getSpaceGroupOperation(int i) {
    return (spaceGroup == null || spaceGroup.operations == null // bio 
        || i >= spaceGroup.operations.length ? null
            : spaceGroup.finalOperations == null ? spaceGroup.operations[i]
                : spaceGroup.finalOperations[i]);
  }

//  @Override
//  public M4 getSpaceGroupOperationRaw(int i) {
//    return spaceGroup.getRawOperation(i);
//  }

  @Override
  public String getSpaceGroupXyz(int i, boolean doNormalize) {
    return spaceGroup.getXyz(i, doNormalize);
  }

  @Override
  public void newSpaceGroupPoint(P3 pt, int i, M4 o, int transX,
                                 int transY, int transZ, P3 retPoint) {
    if (o == null && spaceGroup.finalOperations == null) {
      SymmetryOperation op = spaceGroup.operations[i];
      // temporary spacegroups don't have to have finalOperations
      if (!op.isFinalized)
        op.doFinalize();
      o = op;
    }
    newPoint((o == null ? spaceGroup.finalOperations[i] : o), pt, transX,
        transY, transZ, retPoint);
  }

  @Override
  public V3[] rotateAxes(int iop, V3[] axes, P3 ptTemp, M3 mTemp) {
    return (iop == 0 ? axes
        : spaceGroup.finalOperations[iop].rotateAxes(axes, unitCell, ptTemp,
            mTemp));
  }

  @Override
  public String getSpaceGroupOperationCode(int iOp) {
    return spaceGroup.operations[iOp].subsystemCode;
  }

  @Override
  public void setTimeReversal(int op, int val) {
    spaceGroup.operations[op].setTimeReversal(val);
  }

  @Override
  public int getSpinOp(int op) {
    return spaceGroup.operations[op].getMagneticOp();
  }

  @Override
  public boolean addLatticeVectors(Lst<float[]> lattvecs) {
    return spaceGroup.addLatticeVectors(lattvecs);
  }

  @Override
  public int getLatticeOp() {
    return spaceGroup.latticeOp;
  }

  @Override
  public Lst<P3> getLatticeCentering() {
    return SymmetryOperation.getLatticeCentering(getSymmetryOperations());
  }

  @Override
  public Matrix getOperationRsVs(int iop) {
    return (spaceGroup.finalOperations == null ? spaceGroup.operations
        : spaceGroup.finalOperations)[iop].rsvs;
  }

  @Override
  public int getSiteMultiplicity(P3 pt) {
    return spaceGroup.getSiteMultiplicity(pt, unitCell);
  }

  @Override
  /**
   * @param rot
   *        is a full (3+d)x(3+d) array of epsilons
   * @param trans
   *        is a (3+d)x(1) array of translations
   * @return Jones-Faithful representation
   */
  public String addSubSystemOp(String code, Matrix rs, Matrix vs,
                               Matrix sigma) {
    spaceGroup.isSSG = true;
    String s = SymmetryOperation.getXYZFromRsVs(rs, vs, false);
    int i = spaceGroup.addSymmetry(s, -1, true);
    spaceGroup.operations[i].setSigma(code, sigma);
    return s;
  }

  @Override
  public String getMatrixFromString(String xyz, float[] rotTransMatrix,
                                    boolean allowScaling, int modDim) {

    // MMCifReader only
    return SymmetryOperation.getMatrixFromString(null, xyz, rotTransMatrix,
        allowScaling, true);
  }

  /// symmetryInfo ////

  // symmetryInfo is (inefficiently) passed to Jmol from the adapter 
  // in lieu of saving the actual unit cell read in the reader. Not perfect.
  // The idea was to be able to create the unit cell from "scratch" independent
  // of the reader. 

  @Override
  public String getSpaceGroupName() {
    return (symmetryInfo != null ? symmetryInfo.sgName
        : spaceGroup != null ? spaceGroup.getName()
            : unitCell != null && unitCell.name.length() > 0
                ? "cell=" + unitCell.name
                : "");
  }

  @Override
  public String getSpaceGroupNameType(String type) {
    return (spaceGroup == null ? null : spaceGroup.getNameType(type, this));
  }

  @Override
  public void setSpaceGroupName(String name) {
    if (spaceGroup != null)
      spaceGroup.setName(name);
  }

  @Override
  public char getLatticeType() {
    return (symmetryInfo != null ? symmetryInfo.latticeType
        : spaceGroup == null ? 'P' : spaceGroup.latticeType);
  }

  @Override
  public String getIntTableNumber() {
    return (symmetryInfo != null ? symmetryInfo.intlTableNo
        : spaceGroup == null ? null : spaceGroup.intlTableNumber);
  }

  @Override
  public String getIntTableNumberFull() {
    return (symmetryInfo != null ? symmetryInfo.intlTableNoFull
        : spaceGroup == null ? null : spaceGroup.intlTableNumberFull != null ? spaceGroup.intlTableNumberFull : spaceGroup.intlTableNumber);
  }
  
  @Override
  public boolean getCoordinatesAreFractional() {
    return symmetryInfo == null || symmetryInfo.coordinatesAreFractional;
  }

  @Override
  public int[] getCellRange() {
    return symmetryInfo == null ? null : symmetryInfo.cellRange;
  }

  @Override
  public String getSymmetryInfoStr() {
    if (symmetryInfo != null)
      return symmetryInfo.infoStr;
    if (spaceGroup == null)
      return "";
    symmetryInfo = new SymmetryInfo();
    symmetryInfo.setSymmetryInfo(null, getUnitCellParams(), spaceGroup);
    return symmetryInfo.infoStr;
  }

  @Override
  public int getSpaceGroupOperationCount() {
    return (symmetryInfo != null 
        && symmetryInfo.symmetryOperations != null ? // null here for PDB 
            symmetryInfo.symmetryOperations.length
        : spaceGroup != null && spaceGroup.finalOperations != null
            ? spaceGroup.finalOperations.length
            : 0);
  }

  @Override
  public SymmetryOperation[] getSymmetryOperations() {
    if (symmetryInfo != null)
      return symmetryInfo.symmetryOperations;
    if (spaceGroup == null)
      spaceGroup = SpaceGroup.getNull(true, false, true);
    spaceGroup.setFinalOperations();
    return spaceGroup.finalOperations;
  }


  @Override
  public int getAdditionalOperationsCount() {
    return (symmetryInfo != null 
        && symmetryInfo.symmetryOperations != null &&
            symmetryInfo.getAdditionalOperations() != null ?
                symmetryInfo.additionalOperations.length
        : spaceGroup != null && spaceGroup.finalOperations != null
            ? spaceGroup.getAdditionalOperationsCount()
            : 0);
  }

  @Override
  public M4[] getAdditionalOperations() {
    if (symmetryInfo != null)
      return symmetryInfo.getAdditionalOperations();
    getSymmetryOperations();
    return spaceGroup.getAdditionalOperations();
  }

  @Override
  public boolean isSimple() {
    return (spaceGroup == null
        && (symmetryInfo == null || symmetryInfo.symmetryOperations == null));
  }

  /**
   * Set space group and unit cell from the auxiliary info generated by the
   * model adapter.
   * 
   */
  @SuppressWarnings("unchecked")
  @Override
  public SymmetryInterface setSymmetryInfo(int modelIndex,
                                           Map<String, Object> modelAuxiliaryInfo,
                                           float[] unitCellParams) {
    symmetryInfo = new SymmetryInfo();
    float[] params = symmetryInfo.setSymmetryInfo(modelAuxiliaryInfo,
        unitCellParams, null);
    if (params != null) {
      setUnitCell(params, modelAuxiliaryInfo.containsKey("jmolData"));
      unitCell.moreInfo = (Lst<String>) modelAuxiliaryInfo
          .get("moreUnitCellInfo");
      modelAuxiliaryInfo.put("infoUnitCell", getUnitCellAsArray(false));
      setOffsetPt((T3) modelAuxiliaryInfo.get("unitCellOffset"));
      M3 matUnitCellOrientation = (M3) modelAuxiliaryInfo
          .get("matUnitCellOrientation");
      if (matUnitCellOrientation != null)
        initializeOrientation(matUnitCellOrientation);
      if (Logger.debugging)
        Logger.debug("symmetryInfos[" + modelIndex + "]:\n"
            + unitCell.dumpInfo(true, true));
    }
    return this;
  }

  // UnitCell methods

  @Override
  public boolean haveUnitCell() {
    return (unitCell != null);
  }

  @Override
  public SymmetryInterface setUnitCell(float[] unitCellParams,
                                       boolean setRelative) {
    if (unitCellParams == null)
      unitCellParams = new float[] { 1, 1, 1, 90, 90, 90 };
    unitCell = UnitCell.fromParams(unitCellParams, setRelative);
    return this;
  }

  @Override
  public boolean unitCellEquals(SymmetryInterface uc2) {
    return ((Symmetry) (uc2)).unitCell.isSameAs(unitCell);
  }

  @Override
  public String getUnitCellState() {
    if (unitCell == null)
      return "";
    return unitCell.getState();
  }

  @Override
  public Lst<String> getMoreInfo() {
    return unitCell.moreInfo;
  }

  public String getUnitsymmetryInfo() {
    // not used in Jmol?
    return unitCell.dumpInfo(false, true);
  }

  @Override
  public void initializeOrientation(M3 mat) {
    unitCell.initOrientation(mat);
  }

  @Override
  public void unitize(T3 ptFrac) {
    unitCell.unitize(ptFrac);
  }

  @Override
  public void toUnitCell(T3 pt, T3 offset) {
    unitCell.toUnitCell(pt, offset);
  }

  @Override
  public void toUnitCellRnd(T3 pt, T3 offset) {
    unitCell.toUnitCellRnd(pt, offset);
  }

  @Override
  public P3 toSupercell(P3 fpt) {
    return unitCell.toSupercell(fpt);
  }

  @Override
  public void toFractional(T3 pt, boolean ignoreOffset) {
    if (!isBio)
      unitCell.toFractional(pt, ignoreOffset);
  }

  @Override
  public void toFractionalM(M4 m) {
    if (!isBio)
      unitCell.toFractionalM(m);
  }

  @Override
  public void toCartesian(T3 pt, boolean ignoreOffset) {
    if (!isBio)
      unitCell.toCartesian(pt, ignoreOffset);
  }

  @Override
  public float[] getUnitCellParams() {
    return unitCell.getUnitCellParams();
  }

//  @Override
//  public float[] getUnitCellParamsF() {
//    return unitCell.getUnitCellParamsF();
//  }
//
  @Override
  public float[] getUnitCellAsArray(boolean vectorsOnly) {
    return unitCell.getUnitCellAsArray(vectorsOnly);
  }

  @Override
  public Tensor getTensor(Viewer vwr, float[] parBorU) {
    if (parBorU == null)
      return null;
    if (unitCell == null)
      unitCell = UnitCell.fromParams(new float[] { 1, 1, 1, 90, 90, 90 }, true);
    return unitCell.getTensor(vwr, parBorU);
  }

  @Override
  public P3[] getUnitCellVerticesNoOffset() {
    return unitCell.getVertices();
  }

  @Override
  public P3 getCartesianOffset() {
    return unitCell.getCartesianOffset();
  }

  @Override
  public P3 getFractionalOffset() {
    return unitCell.getFractionalOffset();
  }

  @Override
  public void setOffsetPt(T3 pt) {
    unitCell.setOffset(pt);
  }

  @Override
  public void setOffset(int nnn) {
    P3 pt = new P3();
    SimpleUnitCell.ijkToPoint3f(nnn, pt, 0, 0);
    unitCell.setOffset(pt);
  }

  @Override
  public T3 getUnitCellMultiplier() {
    return unitCell.getUnitCellMultiplier();
  }

  @Override
  public SymmetryInterface getUnitCellMultiplied() {
    UnitCell uc = unitCell.getUnitCellMultiplied();
    if (uc == unitCell)
      return this;
    Symmetry s = new Symmetry();
    s.unitCell = uc;
    return s;
  }

  @Override
  public P3[] getCanonicalCopy(float scale, boolean withOffset) {
    return unitCell.getCanonicalCopy(scale, withOffset);
  }

  @Override
  public float getUnitCellInfoType(int infoType) {
    return unitCell.getInfo(infoType);
  }

  @Override
  public String getUnitCellInfo(boolean scaled) {
    return unitCell.dumpInfo(false, scaled);
  }

  @Override
  public boolean isSlab() {
    return unitCell.isSlab();
  }

  @Override
  public boolean isPolymer() {
    return unitCell.isPolymer();
  }

  @Override
  public boolean checkDistance(P3 f1, P3 f2, float distance, float dx,
                               int iRange, int jRange, int kRange,
                               P3 ptOffset) {
    return unitCell.checkDistance(f1, f2, distance, dx, iRange, jRange, kRange,
        ptOffset);
  }

  @Override
  public P3[] getUnitCellVectors() {
    return unitCell.getUnitCellVectors();
  }

  /**
   * @param oabc
   *        [ptorigin, va, vb, vc]
   * @param setRelative
   *        a flag only set true for IsosurfaceMesh
   * @param name
   * @return this SymmetryInterface
   */
  @Override
  public SymmetryInterface getUnitCell(T3[] oabc, boolean setRelative,
                                       String name) {
    if (oabc == null)
      return null;
    unitCell = UnitCell.fromOABC(oabc, setRelative);
    if (name != null)
      unitCell.name = name;
    return this;
  }

  @Override
  public SymmetryInterface getUnitCelld(T3[] oabc, boolean setRelative,
                                       String name) {
    if (oabc == null)
      return null;
    unitCell = UnitCell.fromOABC(oabc, setRelative);
    if (name != null)
      unitCell.name = name;
    return this;
  }

  @Override
  public boolean isSupercell() {
    return unitCell.isSupercell();
  }

  @Override
  public BS notInCentroid(ModelSet modelSet, BS bsAtoms, int[] minmax) {
    try {
      BS bsDelete = new BS();
      int iAtom0 = bsAtoms.nextSetBit(0);
      JmolMolecule[] molecules = modelSet.getMolecules();
      int moleculeCount = molecules.length;
      Atom[] atoms = modelSet.at;
      boolean isOneMolecule = (molecules[moleculeCount
          - 1].firstAtomIndex == modelSet.am[atoms[iAtom0].mi].firstAtomIndex);
      P3 center = new P3();
      boolean centroidPacked = (minmax[6] == 1);
      nextMol: for (int i = moleculeCount; --i >= 0
          && bsAtoms.get(molecules[i].firstAtomIndex);) {
        BS bs = molecules[i].atomList;
        center.set(0, 0, 0);
        int n = 0;
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          if (isOneMolecule || centroidPacked) {
            center.setT(atoms[j]);
            if (isNotCentroid(center, 1, minmax, centroidPacked)) {
              if (isOneMolecule)
                bsDelete.set(j);
            } else if (!isOneMolecule) {
              continue nextMol;
            }
          } else {
            center.add(atoms[j]);
            n++;
          }
        }
        if (centroidPacked || n > 0 && isNotCentroid(center, n, minmax, false))
          bsDelete.or(bs);
      }
      return bsDelete;
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isNotCentroid(P3 center, int n, int[] minmax,
                                boolean centroidPacked) {
    center.scale(1f / n);
    toFractional(center, false);
    // we have to disallow just a tiny slice of atoms due to rounding errors
    // so  -0.000001 is OK, but 0.999991 is not.
    if (centroidPacked)
      return (center.x + 0.000005d <= minmax[0]
          || center.x - 0.000005d > minmax[3]
          || center.y + 0.000005d <= minmax[1]
          || center.y - 0.000005d > minmax[4]
          || center.z + 0.000005d <= minmax[2]
          || center.z - 0.000005d > minmax[5]);

    return (center.x + 0.000005d <= minmax[0] || center.x + 0.00005d > minmax[3]
        || center.y + 0.000005d <= minmax[1] || center.y + 0.00005d > minmax[4]
        || center.z + 0.000005d <= minmax[2]
        || center.z + 0.00005d > minmax[5]);
  }

  // info

  private SymmetryDesc desc;
  private static SymmetryDesc nullDesc;

  private SymmetryDesc getDesc(ModelSet modelSet) {
    if (modelSet == null) {
      return (nullDesc == null
          ? (nullDesc = ((SymmetryDesc) Interface.getInterface(
              "org.jmol.symmetry.SymmetryDesc", null, "modelkit")))
          : nullDesc);
    }
    return (desc == null
        ? (desc = ((SymmetryDesc) Interface.getInterface(
            "org.jmol.symmetry.SymmetryDesc", modelSet.vwr, "eval")))
        : desc).set(modelSet);
  }

  @Override
  public Object getSymmetryInfoAtom(ModelSet modelSet, int iatom, String xyz,
                                    int op, P3 translation, P3 pt, P3 pt2,
                                    String id, int type, float scaleFactor,
                                    int nth, int options, int[] opList) {
    return getDesc(modelSet).getSymopInfo(iatom, xyz, op, translation, pt, pt2,
        id, type, scaleFactor, nth, options, opList);
  }

  @Override
  public Map<String, Object> getSpaceGroupInfo(ModelSet modelSet, String sgName,
                                               int modelIndex, boolean isFull,
                                               float[] cellParams) {
    
    
    
    
    boolean isForModel = (sgName == null);
    if (sgName == null) {
      Map<String, Object> info = modelSet
          .getModelAuxiliaryInfo(modelSet.vwr.am.cmi);
      if (info != null)
        sgName = (String) info.get("spaceGroup");
    }
    SymmetryInterface cellInfo = null;
    if (cellParams != null) {
      cellInfo = new Symmetry().setUnitCell(cellParams, false);
    }
    return getDesc(modelSet).getSpaceGroupInfo(this, modelIndex, sgName, 0,
        null, null, null, 0, -1, isFull, isForModel, 0, cellInfo, null);
  }

  @Override
  public String fcoord(T3 p) {
    return SymmetryOperation.fcoord(p);
  }

  @Override
  public T3[] getV0abc(Object def, M4 retMatrix) {
    return (unitCell == null ? null : unitCell.getV0abc(def, retMatrix));
  }

  @Override
  public Quat getQuaternionRotation(String abc) {
    return (unitCell == null ? null : unitCell.getQuaternionRotation(abc));
  }

  @Override
  public P3 getFractionalOrigin() {
    return unitCell.getFractionalOrigin();
  }

  @Override
  public boolean getState(ModelSet ms, int modelIndex, SB commands) {
    T3 pt = getFractionalOffset();
    boolean loadUC = false;
    if (pt != null && (pt.x != 0 || pt.y != 0 || pt.z != 0)) {
      commands.append("; set unitcell ").append(Escape.eP(pt));
      loadUC = true;
    }
    T3 ptm = getUnitCellMultiplier();
    if (ptm != null) {
      commands.append("; set unitcell ")
          .append(SimpleUnitCell.escapeMultiplier(ptm));
      loadUC = true;
    }
    boolean isAssigned = (ms.getInfo(modelIndex, "spaceGroupAssigned") != null);
    String sg = (String) ms.getInfo(modelIndex, "spaceGroup");
    if (isAssigned && sg != null) {
      commands.append("\n UNITCELL " + Escape.e(ms.getUnitCell(modelIndex).getUnitCellVectors()));
      commands.append("\n MODELKIT SPACEGROUP " + PT.esc(sg));
      loadUC = true;
    }
    return loadUC;
  }

  @Override
  public AtomIndexIterator getIterator(Viewer vwr, Atom atom, BS bsAtoms,
                                       float radius) {
    return ((UnitCellIterator) Interface
        .getInterface("org.jmol.symmetry.UnitCellIterator", vwr, "script"))
            .set(this, atom, vwr.ms.at, bsAtoms, radius);
  }

  @Override
  public boolean toFromPrimitive(boolean toPrimitive, char type, T3[] oabc,
                                 M3 primitiveToCrystal) {
    if (unitCell == null)
      unitCell = UnitCell.fromOABC(oabc, false);
    return unitCell.toFromPrimitive(toPrimitive, type, oabc,
        primitiveToCrystal);
  }

  @Override
  public Lst<P3> generateCrystalClass(P3 pt00) {
    M4[] ops = getSymmetryOperations();
    Lst<P3> lst = new Lst<P3>();
    boolean isRandom = (pt00 == null);
    float rand1 = 0, rand2 = 0, rand3 = 0;
    P3 pt0;
    if (isRandom) {
      rand1 = (float) Math.E;
      rand2 = (float) Math.PI;
      rand3 = (float) Math.log10(2000);
      pt0 = P3.new3(rand1 + 1, rand2 + 2, rand3 + 3);
    } else {
      pt0 = P3.newP(pt00);
    }
    if (ops == null || unitCell == null) {
      lst.addLast(pt0);
    } else {
      unitCell.toFractional(pt0, true); // ignoreOffset
      P3 pt1 = null;
      P3 pt2 = null;
      if (isRandom) {
        pt1 = P3.new3(rand2 + 4, rand3 + 5, rand1 + 6);
        unitCell.toFractional(pt1, true); // ignoreOffset
        pt2 = P3.new3(rand3 + 7, rand1 + 8, rand2 + 9);
        unitCell.toFractional(pt2, true); // ignoreOffset
      }
      Bspt bspt = new Bspt(3, 0);
      CubeIterator iter = bspt.allocateCubeIterator();
      P3 pt = new P3();
      out: for (int i = ops.length; --i >= 0;) {
        ops[i].rotate2(pt0, pt);
        iter.initialize(pt, 0.001f, false);
        if (iter.hasMoreElements())
          continue out;
        P3 ptNew = P3.newP(pt);
        lst.addLast(ptNew);
        bspt.addTuple(ptNew);
        if (isRandom) {
          if (pt2 != null) {
            ops[i].rotate2(pt2, pt);
            lst.addLast(P3.newP(pt));
          }
          if (pt1 != null) {
            // pt2 is necessary to distinguish between Cs, Ci, and C1
            ops[i].rotate2(pt1, pt);
            lst.addLast(P3.newP(pt));
          }
        }
      }
      for (int j = lst.size(); --j >= 0;) {
        pt = lst.get(j);
        if (isRandom)
          pt.scale(0.5f);
        unitCell.toCartesian(pt, true); // ignoreOffset
      }
    }
    return lst;
  }

  @Override
  public void calculateCIPChiralityForAtoms(Viewer vwr, BS bsAtoms) {
    vwr.setCursor(GenericPlatform.CURSOR_WAIT);
    CIPChirality cip = getCIPChirality(vwr);
    String dataClass = (vwr.getBoolean(T.testflag1) ? "CIPData"
        : "CIPDataTracker");
    CIPData data = ((CIPData) Interface
        .getInterface("org.jmol.symmetry." + dataClass, vwr, "script")).set(vwr,
            bsAtoms);
    data.setRule6Full(vwr.getBoolean(T.ciprule6full));
    cip.getChiralityForAtoms(data);
    vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
  }

  @Override
  public String[] calculateCIPChiralityForSmiles(Viewer vwr, String smiles)
      throws Exception {
    vwr.setCursor(GenericPlatform.CURSOR_WAIT);
    CIPChirality cip = getCIPChirality(vwr);
    CIPDataSmiles data = ((CIPDataSmiles) Interface
        .getInterface("org.jmol.symmetry.CIPDataSmiles", vwr, "script"))
            .setAtomsForSmiles(vwr, smiles);
    cip.getChiralityForAtoms(data);
    vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
    return data.getSmilesChiralityArray();
  }

  private CIPChirality getCIPChirality(Viewer vwr) {
    return (cip == null
        ? (cip = ((CIPChirality) Interface
            .getInterface("org.jmol.symmetry.CIPChirality", vwr, "script")))
        : cip);
  }

  /**
   * return a conventional lattice from a primitive
   * 
   * @param latticeType
   *        "A" "B" "C" "R" etc.
   * @return [origin va vb vc]
   */
  @Override
  public T3[] getConventionalUnitCell(String latticeType,
                                      M3 primitiveToCrystal) {
    return (unitCell == null || latticeType == null ? null
        : unitCell.getConventionalUnitCell(latticeType, primitiveToCrystal));
  }

  @Override
  public Map<String, Object> getUnitCellInfoMap() {
    return (unitCell == null ? null : unitCell.getInfo());
  }

  @Override
  public void setUnitCell(SymmetryInterface uc) {
    unitCell = UnitCell.cloneUnitCell(((Symmetry) uc).unitCell);
  }

  @Override
  public Object findSpaceGroup(Viewer vwr, BS atoms, String xyzList, float[] unitCell,
                               boolean asString, boolean isAssign) {
    return ((SpaceGroupFinder) Interface
        .getInterface("org.jmol.symmetry.SpaceGroupFinder", vwr, "eval"))
            .findSpaceGroup(vwr, atoms, xyzList, unitCell, this, asString, isAssign);
  }

  @Override
  public void setSpaceGroupTo(Object sg) {
    symmetryInfo = null;
    if (sg instanceof SpaceGroup) {
      spaceGroup = (SpaceGroup) sg;
    } else {
      spaceGroup = SpaceGroup.getSpaceGroupFromITAName(sg.toString());
    }
  }

  @Override
  public BS removeDuplicates(ModelSet ms, BS bs, boolean highPrec) {
    UnitCell uc = this.unitCell;
    Atom[] atoms = ms.at;
    float[] occs = ms.occupancies;
    boolean haveOccupancies = (occs != null);
    P3[] unitized = new P3[bs.length()];
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      P3 pt = unitized[i] =  P3.newP(atoms[i]);
      uc.toFractional(pt, false);
      if (highPrec)
        uc.unitizeRnd(pt); 
      else
        uc.unitize(pt);
    }
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      P3 pt = unitized[i];
      int type = a.getAtomicAndIsotopeNumber();
      float occ = (haveOccupancies ? occs[i] : 0);
      for (int j = bs.nextSetBit(i + 1); j >= 0; j = bs.nextSetBit(j + 1)) {
        Atom b = atoms[j];
        if (type != b.getAtomicAndIsotopeNumber()
            || (haveOccupancies && occ != occs[j]))
          continue;
        P3 pt2 = unitized[j];
        if (pt.distanceSquared(pt2) < JC.UC_TOLERANCE2) {
          bs.clear(j);
        } 
      }
    }
    return bs;
  }

  @Override
  public Lst<P3> getEquivPoints(Lst<P3> pts, P3 pt, String flags) {
    M4[] ops = getSymmetryOperations();
    return (ops == null || unitCell == null ? null
        : unitCell.getEquivPoints(pt, flags, ops,
            pts == null ? new Lst<P3>() : pts, 0, 0));
  }

  @Override
  public void getEquivPointList(Lst<P3> pts, int nIgnored, String flags) {
    M4[] ops = getSymmetryOperations();
    boolean newPt = (flags.indexOf("newpt") >= 0);
    boolean zapped = (flags.indexOf("zapped") >= 0);
    // we will preserve the points temporarily, then remove them at the end
    int n = pts.size();
    boolean tofractional = (flags.indexOf("tofractional") >= 0);
    // fractionalize all points if necessary
    if (flags.indexOf("fromfractional") < 0) {
      for (int i = 0; i < pts.size(); i++) {
        toFractional(pts.get(i), true);
      }
    }
    // signal to make no changes in points
    flags += ",fromfractional,tofractional";
    int check0 = (nIgnored > 0 ? 0 : n);
    boolean allPoints = (nIgnored == n);
    int n0 = (nIgnored > 0 ? nIgnored : n);
    if (allPoints) {
      nIgnored--;
      n0--;
    }
    if (zapped)
      n0 = 0;
    P3 p0 = (nIgnored > 0 ? pts.get(nIgnored) : null);
    if (ops != null || unitCell != null) {
      for (int i = nIgnored; i < n; i++) {
        unitCell.getEquivPoints(pts.get(i), flags, ops, pts, check0, n0);
      }
    }
    // now remove the starting points, checking to see if perhaps our
    // test point itself has been removed.
    if (!zapped && (pts.size() == nIgnored || pts.get(nIgnored) != p0
        || allPoints || newPt))
      n--;
    for (int i = n - nIgnored; --i >= 0;)
      pts.removeItemAt(nIgnored);
    // final check for removing duplicates
    //    if (nIgnored > 0)
    //      UnitCell.checkDuplicate(pts, 0, nIgnored - 1, nIgnored);

    // and turn these to Cartesians if desired
    if (!tofractional) {
      for (int i = pts.size(); --i >= nIgnored;)
        toCartesian(pts.get(i), true);
    }
  }

  @Override
  public int[] getInvariantSymops(P3 pt, int[] v0) {
    M4[] ops = getSymmetryOperations();
    if (ops == null)
      return new int[0];
    BS bs = new BS();
    P3 p = new P3();
    P3 p0 = new P3();
    int nops = ops.length;
    for (int i = 1; i < nops; i++) {
      p.setT(pt);
      unitCell.toFractional(p, true);
      // unitize here should take care of all Wyckoff positions
      unitCell.unitize(p);
      p0.setT(p);
      ops[i].rotTrans(p);
      unitCell.unitize(p);
      if (p0.distanceSquared(p) < JC.UC_TOLERANCE2) {
        bs.set(i);
      }
    }
    int[] ret = new int[bs.cardinality()];
    if (v0 != null && ret.length != v0.length)
      return null;
    for (int k = 0, i = 1; i < nops; i++) {
      boolean isOK = bs.get(i);
      if (isOK) {
        if (v0 != null && v0[k] != i + 1)
          return null;
        ret[k++] = i + 1;
      }
    }
    return ret;
  }

  @Override
  public Object getWyckoffPosition(Viewer vwr, P3 p, String letter) {
    if (unitCell == null)
      return "";
    SpaceGroup sg = spaceGroup;
    if (sg == null && symmetryInfo != null) {
      sg = SpaceGroup.determineSpaceGroupN(symmetryInfo.sgName);
      if (sg == null)
        sg = SpaceGroup.getSpaceGroupFromITAName(symmetryInfo.intlTableNoFull);
      if (sg == null)
        return "?";
    }
    if (p == null) {
      p = P3.new3(2.3f/5, 2.3f/7, 2.3f/9);      
    } else {
      p = P3.newP(p);
      unitCell.toFractional(p, true);
    }
    if (wyckoffFinder == null) {
      wyckoffFinder = (WyckoffFinder) Interface
          .getInterface("org.jmol.symmetry.WyckoffFinder", null, "symmetry");
    }
    try {
      WyckoffFinder w = wyckoffFinder.getWyckoffFinder(vwr,
          sg.intlTableNumberFull);
      if (letter == null) {
        return w.getWyckoffPosition(p);
      }
      if (w.findPositionFor(p, letter) == null)
        return null;
      unitCell.toCartesian(p, true);
      return p;
    } catch (Exception e) {
      e.printStackTrace();
      return (letter == null ? "?" : null);
    }
  }

  /**
   * @param fracA
   * @param fracB
   * @return matrix
   */
  @Override
  public M4 getTransform(P3 fracA, P3 fracB, boolean best) {
    return getDesc(null).getTransform(unitCell, getSymmetryOperations(), fracA,
        fracB, best);
  }

  static void newPoint(M4 m, P3 atom1, int x, int y, int z, P3 atom2) {
    m.rotTrans2(atom1, atom2);
    atom2.add3(x, y, z);
  }

  @Override
  public boolean isWithinUnitCell(P3 pt, float x, float y, float z) {
    return unitCell.isWithinUnitCell(x, y, z, pt);
  }

  @Override
  public boolean checkPeriodic(P3 pt) {
    return unitCell.checkPeriodic(pt);
  }

  @Override
  public Object convertOperation(String xyz, M4 matrix) {
    if (matrix == null) {
      // does allow for "a,b,c ;1/2,1/2,0" notation
      float[] a = new float[16];
      SymmetryOperation.getMatrixFromString(null, xyz, a, true, false);
      a[3]/= 12; a[7] /= 12; a[11] /= 12;
      return M4.newA16(a);
    }
    return SymmetryOperation.getXYZFromMatrixFrac(matrix, false, false, false, true);
  }

  private static Map<String, Object> aflowStructures;
  private static Map<String, Object>[] itaData;

  private static Lst<Object> allDataITA;
  private static Lst<Object> listDataITA;
  
  @SuppressWarnings("unchecked")
  @Override
  public Object getSpaceGroupJSON(Viewer vwr, String name, String sgname,
                                  int index) {
    String s0 = sgname;
    try {
      // tm 
      int pt = sgname.indexOf("(");
      boolean isTM = (sgname.endsWith(")") && pt > 0);
      String tm = null;
      if (isTM) {
        tm = sgname.substring(pt + 1, sgname.length() - 1);
        sgname = sgname.substring(0, pt);
      }
      int itno = (sgname.equalsIgnoreCase("ALL") ? 0 : PT.parseInt(sgname));
      boolean isInt = (itno != Integer.MIN_VALUE);
      pt = sgname.indexOf('.');
      if (!isTM && isInt && index == 0 && pt > 0) {
        index = PT.parseInt(sgname.substring(pt + 1));
        sgname = sgname.substring(0, pt);
      }
      if (isInt && (itno > 230 || itno < 0))
        throw new ArrayIndexOutOfBoundsException(itno);
      if (name.equalsIgnoreCase("ITA")) {
        if (itno == 0) {
          if (allDataITA == null)
            allDataITA = (Lst<Object>) getResource(vwr, "sg/json/ita_all.json");
          return allDataITA;
        }
        if (itno == Integer.MIN_VALUE) {
          if (listDataITA == null)
            listDataITA = (Lst<Object>) getResource(vwr, "sg/json/ita_list.json");
          if (itno == 0)
            return allDataITA;
        }
        if (itaData == null)
          itaData = new Map[230];
        Map<String, Object> resource = itaData[itno - 1];
        if (resource == null)
          itaData[itno - 1] = resource = (Map<String, Object>) getResource(vwr,
              "sg/json/ita_" + itno + ".json");
        if (resource != null) {
          if (index == 0)
            return resource;
          Lst<Object> its = (Lst<Object>) resource.get("its");
          if (its != null) {
            for (int i = (isInt ? index : its.size()); --i >= 0;) {
              Map<String, Object> map = (Map<String, Object>) its.get(i);
              if (i == index - 1 || sgname.equals(map.get("itaFull"))
                  || tm != null && tm.equals(map.get("tm"))) {
                return map;
              }
            }
            if (tm != null) {
              // todo: allow 134:(a-b,b-c,a) that is not a standard ITA setting 
            }
          }
        }
      } else if (name.equalsIgnoreCase("AFLOW") && tm == null) {
        if (aflowStructures == null)
          aflowStructures = (Map<String, Object>) getResource(vwr, "sg/json/aflow_structures.json");
        if (itno == 0)
          return aflowStructures;
        System.out.println(sgname + " ? " + index);
        Lst<Object> data = (Lst<Object>) aflowStructures.get("" + sgname);
        if (index <= data.size()) {
          return (index == 0 ? data : data.get(index - 1));
        }
      }
      throw new IllegalArgumentException(s0);
    } catch (Exception e) {
      return e.getMessage();
    }
  }
  
  @SuppressWarnings("unchecked")
  private Object getResource(Viewer vwr, String resource) {
    try {
      BufferedReader r = FileManager.getBufferedReaderForResource(vwr, this,
          "org/jmol/symmetry/", resource);
      String[] data = new String[1];
      if (Rdr.readAllAsString(r, Integer.MAX_VALUE, false, data, 0)) {
        return new JSJSONParser().parse(data[0], true);
      }
    } catch (Throwable e) {
      System.err.println(e.getMessage());
    }
    return null;
  }

}
