/* Semant.java -- semantic checks */

package semant;

import absyn.*;
import sym.Sym;
import table.*;
import types.*;
import varalloc.*;
import java.lang.Class;

import javax.management.RuntimeErrorException;

/**
 * A SemanticChecker object defines a method "check" for semantic 
 * analysis and symbol table construction for SPL
 * <br>
 * SemanticChecker is a singleton class
 * <br>
 * author: Michael Jäger
 */

public class SemanticChecker {

	static final Type intType = new PrimitiveType("int", VarAllocator.INTBYTESIZE);
	static final Type boolType = new PrimitiveType("boolean", VarAllocator.BOOLBYTESIZE);

	public Table check(Absyn program, boolean showTables) {
				
		TableBuilder tablebuilder = new TableBuilder(program, showTables);
		Table table = tablebuilder.buildSymbolTables();
		checkMainProcedure(table);

		ProcedureBodyChecker bodyChecker = new ProcedureBodyChecker();		
		bodyChecker.check(program, table);

		return table;	
	}

	static void checkClass (Object object, Class<?> expectedClass, String errorMessage, int lineNo)  {
		checkClass(object, expectedClass, errorMessage + " in line " + lineNo);
	}

	static void checkClass (Object object, Class<?> expectedClass, String errorMessage)  {
		if (object.getClass()!=expectedClass)
			throw new RuntimeException(errorMessage);
	}

	private void checkMainProcedure(Table globalTable) {
		Entry entry = globalTable.lookup(Sym.newSym("main"));

		if(entry == null){
			System.err.println("Error: procedure 'main' is missing");
			System.exit(1);
		}else if(entry instanceof ProcEntry && ((ProcEntry) entry).paramTypes.size() > 0){
			System.err.println("Error: procedure 'main' must not have any parameters");
			System.exit(1);
		}
	}
}
