/**
 * 
 */
package jme;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

/**
 * @author bruno
 *
 */
public class AtomDisplayLabel {
	public double smallAtomWidthLabel = 0; // atom element only, e.g. N, Cl
	public double fullAtomWidthLabel; // atom element + extra e.g NH3+
	public int alignment;
	public boolean noLabelAtom;

	public Rectangle2D.Double atomLabelBoundingBox;
	public double labelX;
	public double labelY;

	public String str;

	int boundingBoxpadding = 2; // number of pixel of white space surrounding the atom labels

	public double atomMapY; // BB: move the map symbol higher
	public double atomMapX;
	public String mapString;

	public int subscripts[][];
	public int superscripts[][];

	public AtomDisplayLabel(double x, double y, String z, 
	                        int an, int nv, int sbo, int nh, int q, int iso, 
	                        int map, int alignment, FontMetrics fm, double h, boolean showHs) {
		if (z == null || z.length() < 1) {
			z = "*";
			System.err.println("Z error!");
		}
		this.alignment = alignment;

		//boolean isCumuleneSP = (nv == 2 && sbo == 4);
		int padding = 2; // number of pixel of white space surrounding the atom labels
		noLabelAtom = (an == JME.AN_C && q == 0 && iso == 0 && nv > 0 && (nv != 2 || sbo != 4));

		// BB better display for OH or o- : the bond will point at the atom symbol "O"
		// and not at the center of the "OH"
		String hydrogenSymbols = "";
		if (showHs && !this.noLabelAtom) {
			if (nh > 0) {
				hydrogenSymbols += "H";
				if (nh > 1) {
					hydrogenSymbols += nh;
				}
			}
		}

		String isoSymbol = (iso == 0 ? "" : "[" + iso + "]");

		String chargeSymbols = (q == 0 ? "" : (Math.abs(q) > 1 ? "" + Math.abs(q) : "") + (q > 0 ? "+" : "-"));

		String stringForWidth = z;
		if (alignment == JMEUtil.ALIGN_RIGHT) {
			z = chargeSymbols + hydrogenSymbols + isoSymbol + z; // H2N +H3N
		} else {
			z = isoSymbol + z + hydrogenSymbols + chargeSymbols; // NH2 , NH3+
		}
		this.str = z;

		if (alignment == JMEUtil.ALIGN_CENTER) {
			stringForWidth = z;
		}

		// used to position / center the atom label
		double smallWidth = fm.stringWidth(stringForWidth);
		// used to compute the bounding box of the atom label
		double fullWidth = fm.stringWidth(z);

		this.smallAtomWidthLabel = smallWidth;
		this.fullAtomWidthLabel = fullWidth;

		int lineThickness = 1;

		// small width is used to compute the position xstart, that is the x position of
		// the label
		double xstart = x - smallWidth / 2.;
		if (alignment == JMEUtil.ALIGN_RIGHT) {
			xstart -= (fullWidth - smallWidth); // move the xstart further left
		}
		double ystart = y - h / 2; // o 1 vyssie

		// to take into account the line thickness
		xstart -= lineThickness;
		fullWidth += lineThickness;
		Rectangle2D.Double box = this.atomLabelBoundingBox = new Rectangle2D.Double(xstart - padding, ystart - padding, fullWidth + 2 * padding,
				h + 2 * padding);
		this.mapString = null;
		this.labelX = this.atomLabelBoundingBox.x + padding + 1; // see
		this.labelY = this.atomLabelBoundingBox.y + h + padding; // o 1 vyssie

		// place hydrogen count symbols subscript
		if (hydrogenSymbols.length() > 1) {
			int pos = z.indexOf(hydrogenSymbols);
			// H2 -> 2 subscript
			int[] styleIndices = { pos + 1, hydrogenSymbols.length() - 1 };

			this.subscripts = new int[][] { styleIndices };
		}

		// place charge symbols superscript
		if (chargeSymbols.length() > 0) {
			int pos = z.indexOf(chargeSymbols);
			int[] styleIndices = { pos, chargeSymbols.length() };
			this.superscripts = new int[][] { styleIndices };

		}

		// place isotope symbols superscript
		if (isoSymbol.length() > 0) {
			int pos = z.indexOf(isoSymbol);
			int[] styleIndices = { pos, isoSymbol.length() };

			if (this.superscripts == null) {
				this.superscripts = new int[][] { styleIndices };
			} else {
				this.superscripts = new int[][] { this.superscripts[0], styleIndices };
			}
		}

		if (map < 0)
			return;

		// to extend the size of the atomLabelBoundingBox

		this.mapString = " " + map;

		if (noLabelAtom) {
			atomMapX = x + smallWidth / 4; // no atom symbol: put the map label closer, on the right
			atomMapY = y - h * 0.1; // BB: move the map symbol higher
		} else {
			double atomMapStringWidth = fm.stringWidth(mapString);
			if (alignment == JMEUtil.ALIGN_LEFT) {
				atomMapX = x - smallWidth / 2. + fullWidth;
			} else {
				box.x -= atomMapStringWidth;
				atomMapX = x + smallWidth / 2 - fullWidth - atomMapStringWidth;
			}

			// remember: y points down
			double superscriptMove = h * 0.3; // BB: move the map symbol higher
			atomMapY = y - superscriptMove;
			box.y -= superscriptMove;
			box.height += superscriptMove;
			box.width += atomMapStringWidth;
		}
	}

  public void draw(Graphics g) {
    g.drawString(str, (int) labelX, (int) labelY);
  }

  public void drawRect(Graphics g) {
    java.awt.geom.Rectangle2D.Double box = this.atomLabelBoundingBox;
    g.drawRect((int) box.x, (int) box.y, (int) box.width, (int) box.height);
  }

  public void fillRect(Graphics g) {
    java.awt.geom.Rectangle2D.Double box = this.atomLabelBoundingBox;
    g.fillRect((int) box.x, (int) box.y, (int) box.width, (int) box.height);
  }

}
