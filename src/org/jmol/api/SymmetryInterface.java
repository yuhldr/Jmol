package org.jmol.api;

import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.util.Tensor;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

public interface SymmetryInterface {

  int addSpaceGroupOperation(String xyz, int opId);

  String addSubSystemOp(String code, Matrix rs, Matrix vs, Matrix sigma);

  void calculateCIPChiralityForAtoms(Viewer vwr, BS bsAtoms);

  String[] calculateCIPChiralityForSmiles(Viewer vwr, String smiles)
      throws Exception;

  int addBioMoleculeOperation(M4 mat, boolean isReverse);

  boolean addLatticeVectors(Lst<float[]> lattvecs);

  boolean checkDistance(P3 f1, P3 f2, float distance, 
                                        float dx, int iRange, int jRange, int kRange, P3 ptOffset);

  boolean createSpaceGroup(int desiredSpaceGroupIndex,
                                           String name,
                                           Object data, int modDim);

  Object findSpaceGroup(Viewer vwr, BS atoms, String xyzList, float[] unitCellParams, boolean asString, boolean isAssign);

  int[] getCellRange();

  T3[] getConventionalUnitCell(String latticeType, M3 primitiveToCryst);

  boolean getCoordinatesAreFractional();

  void getEquivPointList(Lst<P3> pts, int nIgnore, String flags);

  P3 getFractionalOffset();

  String getIntTableNumber();

  String getIntTableNumberFull();

  int getLatticeOp();

  char getLatticeType();

  String getMatrixFromString(String xyz, float[] temp, boolean allowScaling, int modDim);

  Lst<String> getMoreInfo();

  Matrix getOperationRsVs(int op);

  String getPointGroupName();

  Quat getQuaternionRotation(String abc);

  int getSiteMultiplicity(P3 a);

  Object getSpaceGroup();

  Map<String, Object> getSpaceGroupInfo(ModelSet modelSet, String spaceGroup, int modelIndex, boolean isFull, float[] cellParams);

  Object getSpaceGroupInfoObj(String name, float[] params,
                              boolean isFull, boolean addNonstandard);

  String getSpaceGroupName();

  /**
   * 
   * @param type "Hall" or "HM" or "ITA"
   * @return type or null
   */
  String getSpaceGroupNameType(String type);

  M4 getSpaceGroupOperation(int i);
  
  String getSpaceGroupOperationCode(int op);

  int getSpaceGroupOperationCount();
  
  String getSpaceGroupXyz(int i, boolean doNormalize);

  int getSpinOp(int op);

  boolean getState(ModelSet ms, int modelIndex, SB commands);

  String getSymmetryInfoStr();

  M4[] getSymmetryOperations();

  Tensor getTensor(Viewer vwr, float[] anisoBorU);

  M4 getTransform(P3 fracA, P3 fracB, boolean debug);

  SymmetryInterface getUnitCelld(T3[] points, boolean setRelative, String name);

  SymmetryInterface getUnitCell(T3[] points, boolean setRelative, String name);

  float[] getUnitCellAsArray(boolean vectorsOnly);

  String getUnitCellInfo(boolean scaled);

  Map<String, Object> getUnitCellInfoMap();

  float getUnitCellInfoType(int infoType);

  SymmetryInterface getUnitCellMultiplied();

  float[] getUnitCellParams();

  String getUnitCellState();

  P3[] getUnitCellVectors();

  P3[] getUnitCellVerticesNoOffset();

  T3[] getV0abc(Object def, M4 m);

  boolean haveUnitCell();

  boolean isBio();

  boolean isPolymer();

  boolean isSimple();

  boolean isSlab();

  boolean isSupercell();

  void newSpaceGroupPoint(P3 pt, int i, M4 o,
                                          int transX, int transY, int transZ, P3 retPoint);

  BS notInCentroid(ModelSet modelSet, BS bsAtoms,
                          int[] minmax);

  BS removeDuplicates(ModelSet ms, BS bs, boolean highPrec);

  V3[] rotateAxes(int iop, V3[] axes, P3 ptTemp, M3 mTemp);

  void setFinalOperations(int dim, String name, P3[] atoms,
                                          int iAtomFirst,
                                          int noSymmetryCount, boolean doNormalize, String filterSymop);

  /**
   * set symmetry lattice type using Hall rotations
   * 
   * @param latt SHELX index or character lattice character P I R F A B C S T or \0
   * 
   */
  void setLattice(int latt);

  void setOffset(int nnn);

  void setOffsetPt(T3 pt);

  void setSpaceGroup(boolean doNormalize);

  void setSpaceGroupName(String name);

  /**
   * 
   * @param spaceGroup ITA number, ITA full name ("48:1")
   */
  void setSpaceGroupTo(Object spaceGroup);

  SymmetryInterface setSymmetryInfo(int modelIndex, Map<String, Object> modelAuxiliaryInfo, float[] notionalCell);

  void setTimeReversal(int op, int val);

  SymmetryInterface setUnitCell(float[] params, boolean setRelative);

  void setUnitCell(SymmetryInterface uc);

  void toCartesian(T3 pt, boolean ignoreOffset);

  void toFractional(T3 pt, boolean ignoreOffset);
  
  void toFractionalM(M4 m);

  boolean toFromPrimitive(boolean toPrimitive, char type, T3[] oabc,
                          M3 primitiveToCrystal);

  void toUnitCell(T3 pt, T3 offset);

  void toUnitCellRnd(T3 pt, T3 offset);

  boolean unitCellEquals(SymmetryInterface uc2);

  void unitize(T3 ptFrac);

  void initializeOrientation(M3 matUnitCellOrientation);

  String fcoord(T3 p);

  // floats
  
  /**
   * 
   * @param ms
   * @param iatom
   * @param xyz
   * @param op
   * @param translation TODO
   * @param pt
   * @param pt2 a second point or an offset
   * @param id
   * @param type  T.point, T.lattice, or T.draw, T.matrix4f, T.label, T.list, T.info, T.translation, T.axis, T.plane, T.angle, T.center
   * @param scaleFactor
   * @param nth TODO
   * @param options could be T.offset
   * @param oplist 
   * @return a variety of object types
   */
  Object getSymmetryInfoAtom(ModelSet ms, int iatom, String xyz, int op,
                                    P3 translation, P3 pt, P3 pt2, String id, int type, float scaleFactor, int nth, int options, int[] oplist);

  P3 toSupercell(P3 fpt);


  T3 getUnitCellMultiplier();

  P3 getCartesianOffset();

  P3[] getCanonicalCopy(float scale, boolean withOffset);

  Lst<P3> getLatticeCentering();

  Object getLatticeDesignation();

  Object getPointGroupInfo(int modelIndex, String drawID,
                           boolean asInfo, String type,
                           int index, float scale);

  SymmetryInterface setPointGroup(
                                  SymmetryInterface pointGroupPrevious,
                                  T3 center, T3[] atomset,
                                  BS bsAtoms,
                                  boolean haveVibration,
                                  float distanceTolerance, float linearTolerance, int maxAtoms, boolean localEnvOnly);

  int[] getInvariantSymops(P3 p3, int[] v0);

  Lst<P3> getEquivPoints(Lst<P3> pts, P3 pt, String flags);
  
  Lst<P3> generateCrystalClass(P3 pt0);

  P3 getFractionalOrigin();

  AtomIndexIterator getIterator(Viewer vwr, Atom atom, BS bstoms, float radius);

  boolean isWithinUnitCell(P3 pt, float x, float y, float z);

  boolean checkPeriodic(P3 pt);

  Object convertOperation(String string, M4 matrix);

  int getAdditionalOperationsCount();

  M4[] getAdditionalOperations();
  
  Object getWyckoffPosition(Viewer vwr, P3 pt, String letter);

  Object getSpaceGroupJSON(Viewer vwr, String name, String sgname, int index);
}
