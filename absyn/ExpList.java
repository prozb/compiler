/*
 * ExpList.java -- abstract syntax for a list of expressions
 */
package absyn;

public class ExpList extends ListNode {

	public void accept(Visitor v){
		v.visit(this);
	}

	public ExpList() {
		super();
	}
	
	public ExpList(Exp head, ExpList tail) {
		super(head, tail);
	}
}
