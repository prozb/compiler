/*
 * CallStm.java -- abstract syntax for call statement
 */


package absyn;

import sym.Sym;


public class CallStm extends Stm {

	public Sym name;
	public ExpList args;

	public String id(){
		return name.toString();
	}

	public CallStm(int r, int c, Sym n, ExpList a) {
		row = r;
		col = c;
		name = n;
		args = a;
	}

	public void accept(Visitor v){
		v.visit(this);
	}
}
