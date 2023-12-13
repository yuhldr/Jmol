package org.jmol.api;

import org.jmol.viewer.Viewer;

import javajs.util.BS;

public interface JmolInChI {

  String getInchi(Viewer vwr, BS atoms, Object molData, String options);

}
