package semant;

import table.*;
import types.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.PrimitiveIterator;

import types.PrimitiveType;

import absyn.*;
import sym.Sym;

class ProcedureBodyChecker {

	void check(Absyn program, Table globalTable) {
		Visitor visitor = new CheckVisitor(globalTable);
		
		program.accept(visitor);
	}

	private class CheckVisitor extends DoNothingVisitor {
		private ProcEntry currentProcEntry;
		private int currentArrayType;
		private int prevArrayType;
		private int assignLeftType;
		private int leftOp;
		private int rightOp;
		private boolean currentOpExpIntType;
		private boolean currentLeftOpExpIntType;
		private boolean currentRightOpExpIntType;

		private boolean assignStmFlag;
		private boolean assignLeftArray;
		private boolean rightSideOfAssign;
		private boolean leftArrayWithoutIndex;
		private int leftTypeOfAssign; 
		private int typeOfAssign;
		private int leftArraySize;

		private boolean previousOpExpIntType;
		private int currentRow;
		private SimpleVar currentSimpleVar;
		private SimpleVar currentArraySimpleVar;

		private Table globalTable;
		private Table currentLocalTable;

		private Type currentType;
		private ArrayVar currentArrayVar;

		public CheckVisitor(Table table){
			this.globalTable = table;
			this.currentProcEntry = null;
			this.currentArrayType = 0;
			this.leftOp = 0;
			this.rightOp = 0;
			this.currentRow = 0;
		}

		public void visit(DecList node){
			for(Absyn elem : node){
				elem.accept(this);
			}
		}

		public void visit(ProcDec procDec) {
			Entry entry = globalTable.lookup(procDec.name);
			
			this.currentLocalTable = ((ProcEntry) entry).localTable;


			StmList stms = procDec.body;
			for(Absyn var : stms){
				if(var != null){
					var.accept(this);
				}
			}
		}

		public void visit(CallStm node){
			Entry entry = globalTable.lookup(node.name);
			if(entry != null && entry instanceof ProcEntry){
				toFewtoMuchArgumentsInLineError(node);
			}else if(entry != null && entry instanceof VarEntry){ 
				callOfNonProcedureError(node.id(), node.row);
			}else{
				callOfNonProcedureError(node.id(), node.row);
			}
			mustBeVariableInLineError(node);
		}


		public void visit(CompStm node) { 
			StmList stms = node.stms;
			for(Absyn var : stms){
				if(var != null){
					var.accept(this);
				}
			}
		}

		public void visit(AssignStm node){
			currentArrayType = 0;
			assignStmFlag = true;
			node.var.accept(this);	

			if(currentArrayType == 0 && node.var instanceof ArrayVar){
				leftArrayWithoutIndex = true;
			}else if(node.var instanceof ArrayVar && currentArraySimpleVar != null){
				VarEntry entry = (VarEntry)(currentLocalTable.lookup(currentArraySimpleVar.name));
				int size = calculateTypeSize(entry.type);
				if(currentArrayType > size){
					illegalIndexingNonArrayError(node.row);
				}
			}

			if(node.var instanceof ArrayVar){
				VarEntry entry = (VarEntry)(currentLocalTable.lookup(currentArraySimpleVar.name));
				int size = calculateTypeSize(entry.type);
				if(currentArrayType > size && assignStmFlag 
				|| currentArraySimpleVar == null 
				|| size != currentArrayType  && !assignStmFlag){
					illegalIndexingNonArrayError(node.row);
				}

				currentOpExpIntType = true;
				leftTypeOfAssign = currentArrayType - 1;
			}else{
				if(assignLeftArray){
					VarEntry entry = (VarEntry)(currentLocalTable.lookup(currentArraySimpleVar.name));
					int size = calculateTypeSize(entry.type);
					leftArraySize = size;
					leftTypeOfAssign = size;
				}else{
					leftTypeOfAssign = 0;
				}
			}

			rightSideOfAssign = true;
			node.exp.accept(this);
			rightSideOfAssign = false;

			assignStmFlag = false;
			assignLeftArray = false;
			leftArrayWithoutIndex = false;
			leftTypeOfAssign = 0;
			currentArrayType = 0;
		}

		public void visit(SimpleVar node){	
			undefinedVariableError(node);	
			
			VarEntry entry = (VarEntry)(currentLocalTable.lookup(node.name));
			if(calculateTypeSize(entry.type) == 0){
				this.currentSimpleVar = node;
			}else{
				if(rightSideOfAssign && assignLeftArray && currentArrayType == 0){

					illegalIndexingNonIntegerError(node.row);
				}else if(currentArrayType > calculateTypeSize(entry.type)){
					illegalIndexingNonArrayError(node.row);
				}

				assignLeftArray = true;
				this.currentArraySimpleVar = node;
			}

			if(entry != null && calculateTypeSize(entry.type) == 0){
				currentOpExpIntType = true;		
			}else{
				currentOpExpIntType = false;
			}
		}

		public void visit(ArrayVar node){
			currentArrayType++;
			
			node.var.accept(this); 
			node.index.accept(this);
		}

		public void visit(VarExp node){ 
			if(node.var instanceof ArrayVar){
				prevArrayType = currentArrayType;
				currentArrayType = 0;
			}

			node.var.accept(this);	

			if(node.var instanceof ArrayVar){
				VarEntry entry = (VarEntry)(currentLocalTable.lookup(currentArraySimpleVar.name));
				
				int size = calculateTypeSize(entry.type);
				if(size !=  currentArrayType){
					System.err.println("Error: illegal indexing a non-array in line: " + node.row);
					System.exit(1);
				}
				currentOpExpIntType = true;
			}

			if(assignStmFlag){
				if(node.var instanceof ArrayVar && !assignLeftArray){
					VarEntry entry = (VarEntry)(currentLocalTable.lookup(currentArraySimpleVar.name));
					int currentType = calculateTypeSize(entry.type);
					
					if(leftTypeOfAssign != (currentArrayType - currentType)){
						assignmentHasDifferentTypeError(node.row);
					}
				}else if(leftTypeOfAssign != 0 && !assignLeftArray){
					assignmentHasDifferentTypeError(node.row);
				}else if(assignLeftArray && node.var instanceof ArrayVar){
					int currentType = 0;
					VarEntry entry = (VarEntry)(currentLocalTable.lookup(currentArraySimpleVar.name));
					currentType = calculateTypeSize(entry.type);			
				}if(leftArrayWithoutIndex && rightSideOfAssign){
					assignmentRequiresIntegerError(node.row);
				}
			}
		}
		
		public void visit(IntExp node){
			currentOpExpIntType = true;
		}

		public void visit(OpExp node){	
			node.left.accept(this);	
			currentLeftOpExpIntType = currentOpExpIntType;	

			node.right.accept(this);
			currentRightOpExpIntType = currentOpExpIntType;	

			expCombinesDifferentTypeError(node);
			currentOpExpIntType = currentLeftOpExpIntType;
			checkArithmeticComparisonError(node);
			currentOpExpIntType = currentRightOpExpIntType;
			checkArithmeticComparisonError(node);
		}

		public void visit(IfStm node){ 
			if(!(node.test instanceof OpExp)){
				throw new RuntimeException("\'if\' expression must be of type boolean in line " + node.row);
			}
			this.currentRow = node.row;

			node.test.accept(this);
			node.elsePart.accept(this);
			node.thenPart.accept(this);
		}

		public void visit(WhileStm node){
			if(!(node.test instanceof OpExp)){
				throw new RuntimeException("\'while\' expression must be of type boolean in line " + node.row);
			}
			this.currentRow = node.row;

			node.test.accept(this);
			node.body.accept(this);
		}

		//======================ERROR MESSAGES==========================
		private void illegalIndexingNonArrayError(int row){
			System.err.println("Error: illegal indexing a non-array in line " + row);
			System.exit(1);
		}
		
		private void illegalIndexingNonIntegerError(SimpleVar node){
			
		}
		private void illegalIndexingNonIntegerError(int row){
			System.err.println("Error: illegal indexing with a non-integer in line " + row);
			System.exit(1);
		}

		private void assignmentRequiresIntegerError(int row){
			System.err.println("Error: assignment requires integer variable in line " + row);
			System.exit(1);
		}

		private void assignmentHasDifferentTypeError(int row){
			System.err.println("Error: assignment has different types in line " + row);
			System.exit(1);
		}

		private void expCombinesDifferentTypeError(OpExp node){
			if(currentLeftOpExpIntType != currentRightOpExpIntType){
				System.err.println("Error: expression combines different types in line " + node.row);
				System.exit(1);
			}	
		}

		private void callOfNonProcedureError(String id, int row){
			System.err.println("Error: call of non-procedure '" + id + "' in line " + row);
			System.exit(1);
		}

		private void undefinedProcedureError(String id, int row){
			System.err.println("Error: undefined procedure '" + id + "' in line " + row);
			System.exit(1);
		}

		private void toFewtoMuchArgumentsInLineError(CallStm node){
			int argsInCallStm = node.args.length();

			ProcEntry procEntry = (ProcEntry)(globalTable.lookup(node.name));
			this.currentProcEntry = procEntry;

			int actualArgumentSize = procEntry.paramTypes.size();

			if(argsInCallStm < actualArgumentSize){
				System.err.println("Error: procedure '" + node.id() +  "' called with too few arguments in line " + node.row);
				System.exit(1);
			}else if(argsInCallStm > actualArgumentSize){
				System.err.println("Error: procedure '" + node.id() +  "' called with too many arguments in line " + node.row);
				System.exit(1);
			}
		}

		private void argumentTypeMismatchError(int row, int pos, String procName){
			System.err.println("Error: procedure '" + procName + "' argument " + pos + " type mismatch in line " + row);
			System.exit(1);
		}
		private void mustBeVariableInLineError(CallStm node){
			int pos = 0;
			Absyn elem = null;

			for(ParamType param : currentProcEntry.paramTypes){ //iterating actual params
				elem = getFromIterable(pos, node.args);
				
				currentRow = node.row;
				if(param.isRef){
					if(elem instanceof IntExp || elem instanceof OpExp){
						System.err.println("Error: procedure '" + node.id() + "' argument " + (pos + 1) + " must be a variable in line " + node.row);
						System.exit(1);
					}else if(elem instanceof VarExp){
						VarExp varExp = (VarExp)elem;
						
						if(varExp.var instanceof ArrayVar){
							int paramSize = getTypeOfParam(param);
							varExp.var.accept(this);
							VarEntry entry = (VarEntry)(currentLocalTable.lookup(currentArraySimpleVar.name));
							int entrySize = calculateTypeSize(entry.type);
							if(entrySize - currentArrayType != paramSize){
								argumentTypeMismatchError(node.row, (pos + 1), node.id());
							}
						}
					}
				}
				pos++;	
			}
		}	

		private void undefinedVariableError(SimpleVar node){
			Entry entry = currentLocalTable.lookup(Sym.newSym(node.id()));

			if(entry == null){
				System.err.println("Error: undefined variable '" + 	node.id() + "' in line " + node.row);
				System.exit(1);
			}
		}
		private void checkArithmeticComparisonError(OpExp node){
			if(!currentOpExpIntType){
				if(node.op > OpExp.ADD && node.op < OpExp.DIV){
					arithmeticOpRequiresIntError(node.row);
				}else{
					comparisonRequiresIntError(node.row);
				}
			}
		}
		private void arithmeticOpRequiresIntError(int row){
			System.err.println("Error: arithmetic operation requires integer operands in line " + row);
			System.exit(1);
		}		

		private void comparisonRequiresIntError(int row){
			System.err.println("Error: comparison requires integer operands in line " + row);
			System.exit(1);
		}
		//======================ERROR MESSAGES==========================

		//======================HELPER METHODS==========================
		private Absyn getFromIterable(int index, Iterable<Absyn> list){
			int pos = 0;
			Iterator<Absyn> iterator = list.iterator();
			Absyn elem = null;

			while(iterator.hasNext()){
				elem = iterator.next();
				if(pos == index){
					break;
				}
				pos++;
			}
			return elem;
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

		private int getTypeOfParam(ParamType param){
			int actualSize = calculateTypeSize(param.type);
			return actualSize;
		}
		//======================HELPER METHODS==========================
	}
}
