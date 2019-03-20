package semant;

import sym.Sym;
import table.*;
import types.*;
import varalloc.VarAllocator;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import absyn.*;

class TableBuilder {

	private boolean showTables;
	private Absyn program;
	private Table tableLevel1;
	private Map<Sym, Table> localTableMap = new HashMap<Sym, Table>();

	public TableBuilder(Absyn program, boolean showTables){
		this.showTables = showTables;
		this.program = program;
	}

	Table buildSymbolTables() {
		// just building first level table
		TableInitializer tableInit = new TableInitializer();
		//initializing first level table with predefined values 
		this.tableLevel1 = new Table();
		tableInit.intializeSymbolTable(tableLevel1);

		// creating new visitor
		TableBuilderVisitor visitor = new TableBuilderVisitor();

		//traversal AST program and create new table in ProcDec
		program.accept(visitor); //going into recursion

		//show all the tables
		if(showTables){
			Iterator it = localTableMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry)it.next();

				System.out.println("symbol table at end of procedure '" + pair.getKey() + "':");
				Table localTable = (Table) pair.getValue();
				localTable.show();

				it.remove();
			}
		}
		return tableLevel1; // why return just first level table?
	}
    
	private class TableBuilderVisitor extends DoNothingVisitor {

		private ParamTypeList currentParamTypeList;
		private boolean currentIsRef;
		private Sym currentSym;
		private Type resultType;
		private Table currentVisitorTable;
		private Map<Sym, Integer> parameterProcDecIsDouble = new HashMap<Sym, Integer>();

		public void visit(DecList list) {
			// iterating all decList elements and figuring type out 
			for(Absyn elem : list){
				if(elem instanceof TypeDec){
					elem.accept(this);
				}
			}

			for(Absyn elem : list){
				if(elem instanceof ProcDec){
					elem.accept(this);
				}
			}
		}

		public void visit(ProcDec node) {
			parameterProcDecIsDouble.clear();
			// check for redeclaration of ProcDec
			if(tableLevel1.lookup(node.name) != null){
				System.err.println("Error: redeclaration of '" + node.id() + "' as procedur in line " + node.row);
				System.exit(1);
			}

			// creating a new local table
			currentVisitorTable = new Table(tableLevel1);

			// creating new ParamTypeList and using it in ProcEntry constructor
			ParamTypeList paramTypeList = new ParamTypeList();
			

			this.currentParamTypeList = paramTypeList;
			
			DecList params = node.params;
			for (Absyn var : params) {
				// going via recursion into visit(ParDec)

				var.accept(this);
			}

			// traversal procedure declarations
			DecList decList = node.decls;
			for(Absyn dec : decList){
				dec.accept(this);	
			}

			// adding this procDec to the table first level 
			ProcEntry procEntry = new ProcEntry(paramTypeList, currentVisitorTable);
			tableLevel1.enter(node.name, procEntry); 

			StmList stmList = node.body;
			for(Absyn var : stmList){
				var.accept(this);
			}
			
			for(ParamType param : paramTypeList){
				if(calculateTypeSize(param.type) != 0 && !param.isRef){
					System.err.println("Error: parameter '" + (paramTypeList.indexOf(param) + 1)  + "' must be a reference parameter in line " + node.row);
					System.exit(1);
				}
			}

			//save local table and name in Hashmap
			localTableMap.put(node.name, currentVisitorTable);
		}

		public void visit(ParDec node) { 

			// check for redeclaration of parameters
			if(parameterProcDecIsDouble.get(node.name) == null){
				parameterProcDecIsDouble.put(node.name, 1);
			}else{
				System.err.println("Error: redeclaration of '" + node.id() + "' as parameter in line " + node.row);
				System.exit(1);
			}

			node.ty.accept(this);
			this.currentParamTypeList.add(resultType, node.isRef);
			currentVisitorTable.enter(node.name, new VarEntry(resultType, node.isRef));
		}

		public void visit(NameTy node) { 

			Entry entry = tableLevel1.lookup(node.name);

			if(entry instanceof VarEntry){
				this.resultType = ((VarEntry) entry).type;
			}else if(entry instanceof TypeEntry){
				this.resultType = ((TypeEntry)entry).type;
			}else{
				//throw exception if type ist nicht definiert
				System.err.println("Error: undefined type '" + node.id() + "' in line " + node.row);
				System.exit(1);
			}
		}

		public void visit(TypeDec node) {			
			if(node.id() == "main"){
				System.err.println("Error: 'main' is not a procedure");
				System.exit(1);
			}
			if(tableLevel1.lookup(node.name) != null){
				System.err.println("Error: redeclaration of " + node.id() + " as type in line " + node.row);
				System.exit(1);
			}

			node.ty.accept(this);
			tableLevel1.enter(node.name, new TypeEntry(resultType));
		}

		public void visit(ArrayTy node) {
			node.baseTy.accept(this);
			this.resultType = new ArrayType(node.size, resultType);
		}
		
		public void visit(VarDec node) { //OK

			if(tableLevel1.lookup(node.name) != null){
				System.err.println("Error: redeclaration of " + node.id() + " as variable in line " + node.row);
				System.exit(1);
			}		
			node.ty.accept(this);
			enterDeclaration(node.name, new VarEntry(resultType, false));
		}	
		
		private void enterDeclaration(Sym name, Entry entry){
			currentVisitorTable.enter(name, entry);
		}

		private int calculateTypeSize(Type type){
			int size = 1;
			
			if(type instanceof PrimitiveType){
				return 0;
			}else if(type instanceof ArrayType){
				Type arrayType = type;
				while(!(((ArrayType)arrayType).baseType instanceof PrimitiveType)){
					arrayType = ((ArrayType)arrayType).baseType;
					size++;
				}
			}
			return size;
		}
	}
}


