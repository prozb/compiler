package semant;

import sym.Sym;
import table.*;
import types.*;

class TableInitializer {
	static Type intType = SemanticChecker.intType;
	static Type boolType = SemanticChecker.boolType;

	void intializeSymbolTable(Table globalTable) {
		enterPredefinedTypes(globalTable);
		enterPredefinedProcedures(globalTable);
	}

	
	private void enterPredefinedTypes(Table table) {
		table.enter(Sym.newSym("int"), new TypeEntry(intType));
	}

	@SuppressWarnings("serial")
	private void enterPredefinedProcedures(Table table) {
		ParamTypeList p;

		// we need to fill the argument area size of the lib methods
		ProcEntry procEntry;

		/* printi(i: int) */
		p = new ParamTypeList(){{
			add(intType, false);
		}};
		procEntry = new ProcEntry(p, table);
		procEntry.argumentAreaSize = 4;

		table.enter(Sym.newSym("printi"), procEntry);

		/* printc(i: int) */
		p = new ParamTypeList(){{
			add(intType, false);
		}};
		procEntry = new ProcEntry(p, table);
		procEntry.argumentAreaSize = 4;

		table.enter(Sym.newSym("printc"), procEntry);

		/* readi(ref i: int) */
		p = new ParamTypeList(){{
			add(intType, true);
		}};
		procEntry = new ProcEntry(p, table);
		procEntry.argumentAreaSize = 4;

		table.enter(Sym.newSym("readi"), procEntry);

		/* readc(ref i: int) */
		p = new ParamTypeList(){{
			add(intType, true);
		}};
		procEntry = new ProcEntry(p, table);
		procEntry.argumentAreaSize = 4;

		table.enter(Sym.newSym("readc"), procEntry);

		/* exit() */
		p = new ParamTypeList();
		procEntry = new ProcEntry(p, table);
		table.enter(Sym.newSym("exit"), procEntry);

		/* time(ref i: int) */
		p = new ParamTypeList(){{
			add(intType, true);
		}};
		procEntry = new ProcEntry(p, table);
		procEntry.argumentAreaSize = 4;

		table.enter(Sym.newSym("time"), procEntry);

		/* clearAll(color: int) */
		p = new ParamTypeList(){{
			add(intType, false);
		}};
		procEntry = new ProcEntry(p, table);
		procEntry.argumentAreaSize = 4;

		table.enter(Sym.newSym("clearAll"), procEntry);

		/* setPixel(x: int, y: int, color: int) */
		p = new ParamTypeList(){{
			add(intType, false);
			add(intType, false);
			add(intType, false);
		}};
		procEntry = new ProcEntry(p, table);
		procEntry.argumentAreaSize = 12;

		table.enter(Sym.newSym("setPixel"), procEntry);

		/* drawLine(x1: int, y1: int, x2: int, y2: int, color: int) */
		p = new ParamTypeList(){{
			add(intType, false);
			add(intType, false);
			add(intType, false);
			add(intType, false);
			add(intType, false);
		}};
		procEntry = new ProcEntry(p, table);
		procEntry.argumentAreaSize = 20;

		table.enter(Sym.newSym("drawLine"), procEntry);

		/* drawCircle(x0: int, y0: int, radius: int, color: int) */
		p = new ParamTypeList(){{
			add(intType, false);
			add(intType, false);
			add(intType, false);
			add(intType, false);
		}};
		procEntry = new ProcEntry(p, table);
		procEntry.argumentAreaSize = 16;

		table.enter(Sym.newSym("drawCircle"), procEntry);
	}

}

