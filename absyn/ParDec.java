/*
 * ParDec.java -- abstract syntax for parameter declaration
 */


package absyn;

import sym.Sym;


public class ParDec extends VarDec {
  public boolean isRef;
  
  public ParDec(int row, int col, Sym name, Ty type, boolean isRef) {
    super(row, col, name, type);
    this.isRef = isRef;
  }

    public void accept(Visitor v){
	v.visit(this);
    }
}
