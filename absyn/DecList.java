/*
 * DecList.java -- abstract syntax for a list of declarations
 */

package absyn;

public class DecList extends ListNode {

	public DecList(Dec s1, DecList s2) {
		super (s1,s2);
	}

	public DecList() {
		super ();
	}

	public void accept(Visitor v){
		v.visit(this);
	}
}
