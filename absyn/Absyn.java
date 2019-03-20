/*
 * Absyn.java -- abstract syntax for SPL
 */

package absyn;

public abstract class Absyn {

	public int row;
	public int col;

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public void setCol(int col) {
		this.col = col;
	}

	public abstract void accept(Visitor v);
	
	public void show(int indentation){
		this.accept(new PrintVisitor(indentation));
	}
}
