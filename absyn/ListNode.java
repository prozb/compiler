package absyn;

public abstract class ListNode extends Absyn implements Iterable<Absyn> {

    protected boolean isEmpty;
    protected Absyn head;
    protected ListNode tail;
    protected int length;
    
    public Absyn head()      { return head; }
    public boolean isEmpty() { return isEmpty;	}
    public ListNode tail()   { return tail; }
    public int length()      { return length; }
    
    public ListNode () {
	row = -1;
	col = -1;
	isEmpty = true;
	length = 0;
    }

    public ListNode (Absyn head, ListNode tail) {
	row = -1;
	col = -1;
	isEmpty = false;
	this.head = head;
	this.tail = tail;
	length = 1+tail.length;
    }

    public ListNodeIterator iterator() {
	return new ListNodeIterator(this);
    }
}
