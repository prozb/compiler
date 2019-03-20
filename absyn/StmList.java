/*
 * StmList.java -- abstract syntax for a list of statements
 */

package absyn;

public class StmList extends ListNode {

	public StmList(Stm head, StmList tail) {
		super(head, tail);
	}
	
    public StmList() {
		super();
	}

	public void accept(Visitor v){
	v.visit(this);
    }
}
