/*
 * Varalloc.java -- variable allocation
 */


package varalloc;

import java.util.Arrays;
import java.util.Iterator;

import absyn.*;
import table.*;
import types.*;
import sym.*;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class VarAllocator {

	public static final int INTBYTESIZE = 4;
	public static final int BOOLBYTESIZE = 4;
	public static final int REFBYTESIZE = 4;

	private Table globalTable;
	private boolean showVarAlloc;
	private int resultSize;
	private ProcEntry currentProcEntry;
	private ArrayList<String> libraryMethods;
	
	public VarAllocator(Table table, boolean bool){
		this.globalTable = table;	
		this.showVarAlloc = bool;
		libraryMethods = new ArrayList<>();
		Collections.addAll(libraryMethods, 
							"drawLine",
							"printc", 
							"setPixel",
							"readi", 
							"drawCircle", 
							"time", 
							"clearAll", 
							"readc", 
							"printi", 
							"exit" );

	}

	public void allocVars(Absyn program) {		
		Visitor visitor = new CheckVisitor(this.globalTable);
		program.accept(visitor);

		visitor = new CheckVisitorSecondRound(this.globalTable);
		program.accept(visitor);

		if(showVarAlloc){
			showVarTables(program);
		}
	}

	public void showVarTables(Absyn program){
		Visitor visitor = new PrettyPrint(this.globalTable);
		program.accept(visitor);
	}

	private class CheckVisitor extends DoNothingVisitor {
		private Table globalTable;
		private Table currentlocalTable;
		private ArrayList<Integer> valueOffsetList;
		private int valueFromList;
		
		public CheckVisitor(Table table){
			this.globalTable = table;
			this.valueOffsetList = new ArrayList<>();
		}

		public void visit(DecList node) {
			Iterator<Absyn> iterator = node.iterator();
			Absyn currentNode = null;

			while(iterator.hasNext()){
				currentNode = iterator.next();
				currentNode.accept(this);
			}
		}

		public void visit(StmList node) {
			Iterator<Absyn> iterator = node.iterator();
			Absyn currentNode = null;

			while(iterator.hasNext()){
				currentNode = iterator.next();
				currentNode.accept(this);	
			}
		}

		public void visit(IfStm node) {
			node.elsePart.accept(this);
			node.thenPart.accept(this);
		}

		public void visit(WhileStm node) {
			node.body.accept(this);
		}

		public void visit(CallStm node) {
			Entry entry = globalTable.lookup(node.name);

				// actual params
			ParamTypeList pars = ((ProcEntry)entry).paramTypes;
		}

		public void visit(ProcDec node) {
			if(!node.params.isEmpty()){

				// clear value list
				valueOffsetList.clear();

				ProcEntry entry = (ProcEntry)(globalTable.lookup(node.name));
				int returnSize = 0;
	
				ParamTypeList pars = ((ProcEntry)entry).paramTypes;
				for(ParamType par : pars){
					par.offset = entry.argumentAreaSize;
					valueOffsetList.add(par.offset);

					if(par.isRef){
						returnSize = 4;
					}else{
						returnSize = extractSizeFromParamType(par.type);
					}
					
					entry.argumentAreaSize += returnSize;
				}

				// save localTable in currentlocalTable
				this.currentlocalTable = entry.localTable;
				
				// iterate through paramList
				Iterator<Absyn> iterator = node.params.iterator();
				Absyn currentNode = null;
				valueFromList = 0;
				while(iterator.hasNext()){
					currentNode = iterator.next();
					currentNode.accept(this);

					// to get next value from list
					valueFromList += 1;
				}
				
			}

			if(!node.decls.isEmpty()){
				currentProcEntry = (ProcEntry)(globalTable.lookup(node.name));
				resultSize = 0;
				for(Absyn dec : node.decls){
					dec.accept(this);
				}
				resultSize *= (-1);
				currentProcEntry.localvarAreaSize = resultSize;
			}
		}

		public void visit(VarDec node) {
			VarEntry entry = (VarEntry)(currentProcEntry.localTable.lookup(node.name));
			resultSize -= extractSizeFromParamType(entry.type);
			entry.offset = resultSize; 
		}

		public void visit(ParDec node) {
			VarEntry entry = (VarEntry)(this.currentlocalTable.lookup(node.name));
			
			entry.offset = valueOffsetList.get(this.valueFromList); 
		}

		private int extractSizeFromParamType(Type type){
			return type.byteSize;
		}

	}

	private class CheckVisitorSecondRound extends DoNothingVisitor {
		private Table globalTable;
		private int CallStmSize;
		private int counter;
		
		public CheckVisitorSecondRound(Table table){
			this.globalTable = table;
			this.CallStmSize = 0;
			this.counter = 0;
		}

		public void visit(DecList node) {
			Iterator<Absyn> iterator = node.iterator();
			Absyn currentNode = null;

			while(iterator.hasNext()){
				currentNode = iterator.next();
				currentNode.accept(this);
			}
		}

		

		public void visit(StmList node) {
			Iterator<Absyn> iterator = node.iterator();
			Absyn currentNode = null;

			while(iterator.hasNext()){
				currentNode = iterator.next();
				currentNode.accept(this);	
			}
		}

		public void visit(CallStm node) {
			this.counter += 1;
			
			Entry entry = globalTable.lookup(node.name);

			int areaSize = ((ProcEntry) entry).argumentAreaSize;

			if(this.CallStmSize < areaSize && areaSize != 0){
				this.CallStmSize = areaSize;
			}
		}

		public void visit(IfStm node){
			node.elsePart.accept(this);
			node.thenPart.accept(this);
		}

		public void visit(WhileStm node){
			node.body.accept(this);
		}

		public void visit(CompStm node){
			for(Absyn elem : node.stms){
				if(elem != null){
					elem.accept(this);
				}
			}
		}

		public void visit(ProcDec node) {
			ProcEntry procEntry = (ProcEntry) globalTable.lookup(node.name);
			this.CallStmSize = 0;
			this.counter = 0;

			for(Absyn ele : node.body){
				ele.accept(this);
			}

			if(this.counter != 0){
				procEntry.outgoingAreaSize = this.CallStmSize;
			}else{
				procEntry.outgoingAreaSize = -1;
			}
		}

		private int extractSizeFromParamType(Type type){
			return type.byteSize;
		}
	}

	private class PrettyPrint extends DoNothingVisitor{

		Table globalTable;
		Table currentlocalTable;
		Visitor visitor;
		Absyn currentNode;

		public PrettyPrint(Table table){
			this.globalTable = table;
		}

		public void visit(DecList node) {
			Iterator<Absyn> iterator = node.iterator();
			Absyn currentNode = null;

			while(iterator.hasNext()){
				currentNode = iterator.next();
				currentNode.accept(this);
			}
		}

		public void visit(StmList node) {
			Iterator<Absyn> iterator = node.iterator();
			Absyn currentNode = null;

			while(iterator.hasNext()){
				currentNode = iterator.next();
				currentNode.accept(this);	
			}
		}

		public void visit(ProcDec node) {
			ProcEntry procEntry = (ProcEntry) this.globalTable.lookup(node.name);
			
			System.out.format("Variable allocation for procedure '%s' \n", node.id());
			
			int counter = 1;
			for(ParamType para : procEntry.paramTypes){
				System.out.println("arg " + counter + ": sp + " + para.offset);
				counter += 1;
			}

			System.out.println("size of argument area = " + procEntry.argumentAreaSize);
			
			// save localTable in currentlocalTable
			this.currentlocalTable = procEntry.localTable;
					
			// iterate through paramList
			currentNode = null;
			Iterator<Absyn> iterator = node.params.iterator();
			while(iterator.hasNext()){
				currentNode = iterator.next();
				currentNode.accept(this);
			}
			
			// iterate through decList
			currentNode = null;
			iterator = node.decls.iterator();
			while(iterator.hasNext()){
				currentNode = iterator.next();
				currentNode.accept(this);
			}

			System.out.println("size of localvar area = " + procEntry.localvarAreaSize);
			System.out.println("size of outgoing area = " + procEntry.outgoingAreaSize);
			System.out.println("");
		}

		public void visit(VarDec node) {
			VarEntry varEntry = (VarEntry) this.currentlocalTable.lookup(node.name);

			// set offset number to "-number" or "+number"
			String offsetString = "";
			if(varEntry.offset < 0){
				offsetString = "- " + varEntry.offset * (-1);
			}else{
				offsetString = "+ " + varEntry.offset;
			}

			System.out.format("var '%s': fp " + offsetString + "\n", node.id());
		}

		public void visit(ParDec node){
			VarEntry varEntry = (VarEntry) this.currentlocalTable.lookup(node.name);

			// set offset number to "-number" or "+number"
			String offsetString = "";
			if(varEntry.offset < 0){
				offsetString = "- " + varEntry.offset * (-1);
			}else{
				offsetString = "+ " + varEntry.offset;
			}

			System.out.format("param '%s': fp " + offsetString + "\n", node.id());		
		}
	}
}
