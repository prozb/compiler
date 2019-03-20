/*
 * Codegen.java -- Puck code generator
 */

package codegen;

import java.io.*;
import java.util.Iterator;

import absyn.*;
import table.*;
import types.*;

public class Codegenerator {

	private PrintWriter output;
	private int nextLabel = 0;
	private static final int firstReg = 8;
	private static final int lastReg  = 23;

	public Codegenerator(FileWriter writer) {
		output = new PrintWriter(writer);
	}

	public void genCode(Absyn node, Table t, String filename) {
		String[] filenameWithoutEnd = filename.split("\\.");
		filenameWithoutEnd = filenameWithoutEnd[0].split("\\/");
		assemblerProlog(filenameWithoutEnd[filenameWithoutEnd.length - 1]);
		
		node.accept(new CodegenVisitor(t, firstReg));
	}

	private void checkRegister(int reg) {
		if (reg > lastReg){
			throw new RuntimeException
			("expression too complicated, running out of registers");
		}
	}

	private String newLabel() {
		return "L" + nextLabel++;
	}

	private void assemblerProlog(String filename) {
		emitObject(filename);
		emitImport("printi");
		emitImport("printc");
		emitImport("readi");
		emitImport("readc");
		emitImport("exit");
		emitImport("time");
		emitImport("clearAll");
		emitImport("setPixel");
		emitImport("drawLine");
		emitImport("drawCircle");
		emitImport("indexError");
		//emit("\n\t.code");
		//emit("\t.align\t4");
	}

	private void emitObject(String file){
		output.format(".object %s\n\n", file);
	}

	private void emitImport(String id) {
		output.format(".import spllib %s\n", id);
	}
	
	private void emit(String line) {
		output.print(line + "\n");
	}

	private void emitR(String opcode, int reg, String comment) {
		output.format("%s $%d; %s\n", opcode, reg, comment);
	}

	private void emitRI(String opcode, int reg, int value) {
		output.format("%s $%d %d\n", opcode, reg, value);
	}

	private void emitRR(String opcode, int reg1, int reg2){
		output.format("%s $%d $%d\n", opcode, reg1, reg2);
	}

	private void emitRRI(String opcode, int reg1, int reg2, int value) {
		output.format("%s $%d $%d %d\n", opcode, reg1, reg2, value);
	}

	private void emitRRR(String opcode, int reg1, int reg2, int reg3) {
		output.format("%s $%d $%d $%d\n", opcode, reg1, reg2, reg3);
	}

	private void emitRS(String opcode, int reg, String label){
		output.format("%s $%d %s \n", opcode, reg, label);
	}

	private void emitL(String opcode, String label){
		output.format("%s %s\n", opcode, label);
	}

	private void emitMain(){
		output.print("\n.executable main");
	}

	private int extractSizeFromType(Type type){
		return type.byteSize;
	}


	public class CodegenVisitor extends DoNothingVisitor {
		Table globalTable;
		Table currentLocalTable;
		ProcEntry currentProcEntry;
		int currentReg;
		int currentVal;
		String currentLabel;
		Type currentArrayType;
		Type firstArrayType;
		int currentArrayVarCounter;

		public CodegenVisitor(Table t, int reg){
			this.globalTable = t;
			this.currentReg = reg;	
			this.currentVal = 0;
		}

		public void checkAndGenerate(Exp exp){
			if(exp instanceof VarExp){
				// wert der adresse der variable
				emitRR("LDW", this.currentReg, this.currentReg);
			}
		}

		public void visit(StmList node) {	
			
			Iterator<Absyn> iterator = node.iterator();
			Absyn currentNode = null;


			// Do we need to count up the currentReg???
			while(iterator.hasNext()){
				currentNode = iterator.next();
				currentNode.accept(this);
			}
		}

		public void visit(CompStm node) {
			// go recursiv into stmList
			node.stms.accept(this);
		}

		public void visit(IntExp node) {
			checkRegister(this.currentReg + 1);
			this.currentReg += 1;
			this.currentVal = node.val;
			emitRI("SETW", this.currentReg, node.val);
		}


		public void visit(VarExp node) {

			node.var.accept(this);
		}


		public void visit(OpExp node) {

			// code for the left operand
			node.left.accept(this);
			checkAndGenerate(node.left);

			//this.currentReg += 1;
	
			// code for the right operand
			node.right.accept(this);
			checkAndGenerate(node.right);

			switch(node.op){
				case 0:
						emitRRR("EQ", this.currentReg - 1, this.currentReg - 1, this.currentReg);
						break;
				case 1:	
						emitRRR("NE", this.currentReg - 1, this.currentReg - 1, this.currentReg);
						break;
				case 2:	
						emitRRR("LTI", this.currentReg - 1, this.currentReg - 1, this.currentReg);
						break;
				case 3:
						emitRRR("LEI", this.currentReg - 1, this.currentReg - 1, this.currentReg);
						break;
				case 4:
						emitRRR("GTI", this.currentReg - 1, this.currentReg - 1, this.currentReg);
						break;
				case 5:
						emitRRR("GEI", this.currentReg - 1, this.currentReg - 1, this.currentReg);
						break;
				case 6:
						emitRRR("ADD", this.currentReg - 1, this.currentReg - 1, this.currentReg);
						break;
				case 7:
						emitRRR("SUB", this.currentReg - 1, this.currentReg - 1, this.currentReg);
						break;
				case 8:
						emitRRR("MULI", this.currentReg - 1, this.currentReg - 1, this.currentReg);
						break;
				case 9:
						emitRRR("DIVI", this.currentReg - 1, this.currentReg - 1, this.currentReg);
						break;
				default:
						break;
			}

			this.currentReg -= 1;
		}

		public void visit(AssignStm node) {			
			node.var.accept(this);

			node.exp.accept(this);
			checkAndGenerate(node.exp);

			emitRR("STW", this.currentReg, this.currentReg - 1);

			this.currentReg -= 2;
		}

		// Need to check for IndexOutofBounds! How do we implement that?
		public void visit(ArrayVar node) {

			node.var.accept(this);

			// write index in current register
			node.index.accept(this);
			checkAndGenerate(node.index);
			checkRegister(this.currentReg + 1);
			this.currentReg += 1;

			emitRI("SETW", this.currentReg, ((ArrayType)this.currentArrayType).size);
			// is index greater or equal of actual array size
			emitRRR("GEI", this.currentReg, this.currentReg - 1, this.currentReg);
			emitRS("BRT", this.currentReg, "indexError");
			
			// calculating
			emitRI("SETW", this.currentReg, ((ArrayType)this.currentArrayType).baseType.byteSize);
			this.currentArrayType = ((ArrayType)this.currentArrayType).baseType;
			
			emitRRR("MULI", this.currentReg - 1, this.currentReg - 1, this.currentReg);
            this.currentReg = this.currentReg - 1;
            emitRRR("ADD", this.currentReg - 1, this.currentReg - 1, this.currentReg);
			this.currentReg -= 1;
		}

		// SimpleVar: In currentRegister is now the adress
		public void visit(SimpleVar node) {
			VarEntry entry = (VarEntry) this.currentLocalTable.lookup(node.name); 
			checkRegister(this.currentReg + 1);
			this.currentReg += 1;

			if(entry.isRef){
				// zieladresse der referenz in currentRegister speichern
				emitRRI("ADDC", this.currentReg, 29, entry.offset);

				// adresse der variablen auf die die referenz verweist
				emitRR("LDW", this.currentReg, this.currentReg);
			}else{
				// zieladresse der variable in currentRegister speichern
				emitRRI("ADDC", this.currentReg, 29, entry.offset);
			}

			// für arrayType iteration
			if(entry.type instanceof ArrayType){
				this.currentArrayType = entry.type;
				this.firstArrayType = entry.type;
				this.currentArrayVarCounter = 4;
			}
		}

		public void visit(IfStm node) {

			String skipThenLabel = newLabel();

			// assembler code for the exp
			node.test.accept(this);

			emitRS("BRF", this.currentReg, skipThenLabel);
			this.currentReg -= 1;



			if(node.elsePart instanceof EmptyStm){
				// then part
				node.thenPart.accept(this);

				// after then part set label
				emit(skipThenLabel + ":");
			}else{
				String skipElseLabel = newLabel();
				
				//then part 
				node.thenPart.accept(this);

				// go behind else part
				emitL("JMP", skipElseLabel);

				// after then part set label
				emit(skipThenLabel + ":");

				node.elsePart.accept(this);

				// after else part set label
				emit(skipElseLabel + ":");
			}
		}

		public void visit(WhileStm node) {

			// label für WhileLoop
			String loopLabel = newLabel();
			String endLabel = newLabel();

			// before the loop
			emit(loopLabel + ":");

			// assembler code for the exp
			node.test.accept(this);

			emitRS("BRF", this.currentReg, endLabel);

			this.currentReg -= 1;
			node.body.accept(this);

			
			// go Back to head of while loop
			emitL("JMP", loopLabel);

			// after the loop
			emit(endLabel + ":");
		}

		public void visit(DecList node) {
			Iterator<Absyn> iterator = node.iterator();
			Absyn currentNode = null;

			while(iterator.hasNext()){
				currentNode = iterator.next();
				currentNode.accept(this);
			}
		}

		public void visit(ProcDec node) {

			/* prolog */
			if(node.name.toString().equals("main")){
				emitMain();
			}
			emit("");
			emit(node.name.toString() + ":");

			/* get symbol table entry for this procedure */
			ProcEntry procEntry = (ProcEntry) this.globalTable.lookup(node.name);
			this.currentProcEntry = procEntry;
			this.currentLocalTable = procEntry.localTable;
			
			// allocate space for local area
			if(procEntry.localvarAreaSize != 0){
				emitRRI("SUBC", 31, 31, procEntry.localvarAreaSize);
			}

			// save old framepointer in stackframe
			emitRRI("SUBC", 31, 31, 4);
            emitRR("STW", 29, 31);

			// set new framepointer
			emitRRI("ADDC", 29, 31, (procEntry.localvarAreaSize + 4));
			
			// if outgoing area is empty we will not save the return value and the parameters
			if (procEntry.outgoingAreaSize != -1) {
                emitRRI("SUBC", 31, 31, 4);
                emitRR("STW", 30, 31);
			   
				emitRRI("SUBC", 31, 31, procEntry.outgoingAreaSize);
			}
			
			this.currentReg -= 1;

			node.body.accept(this);

			/* epilog */
			
			if (procEntry.outgoingAreaSize != -1) {
                emitRRI("ADDC", 31, 31, procEntry.outgoingAreaSize);
                emitRR("LDW", 30, 31);
				emitRRI("ADDC", 31, 31, 4);
			}
			
			// old framepointer
		    emitRR("LDW", 29, 31);
			
			emitRRI("ADDC", 31, 31, 4 + procEntry.localvarAreaSize);
			emitR("JMPR", 30, "Jump back");
		}

		public void visit(CallStm node) {
			int counter = 0;
			String methodName = node.name.toString();

            ParamTypeList paramList = ((ProcEntry) globalTable.lookup(node.name)).paramTypes;
            for (Absyn elem : node.args) {
                if (paramList.get(counter).isRef) {
					
					Var var = ((VarExp)elem).var;
					while(var instanceof ArrayVar){
						var = ((ArrayVar)var).var;
					}

					VarEntry entry = (VarEntry)currentLocalTable.lookup(((SimpleVar)var).name);
					if(entry.isRef){
						checkRegister(this.currentReg + 1);
						this.currentReg += 1;
						
						emitRRI("SUBC", this.currentReg, 29, (entry.offset * -1));
                        emitRR("LDW", this.currentReg, this.currentReg);
                    } else {
						checkRegister(this.currentReg + 1);
						this.currentReg += 1;
                    
						emitRRI("SUBC", this.currentReg, 29, (entry.offset * -1));
					}
					checkRegister(this.currentReg + 1);
					this.currentReg += 1;
                    emitRRI("SUBC", this.currentReg, 29, 4 + 4 + this.currentProcEntry.localvarAreaSize + this.currentProcEntry.outgoingAreaSize - (counter * 4));
                    emitRR("STW", this.currentReg - 1, this.currentReg);
                    this.currentReg = this.currentReg - 2;
                    counter++;
                } else {
                    elem.accept(this);
					checkAndGenerate((Exp)elem);

					checkRegister(this.currentReg + 1);
					this.currentReg += 1;
                    emitRRI("SUBC", this.currentReg, 29, 4 + 4 + this.currentProcEntry.localvarAreaSize + this.currentProcEntry.outgoingAreaSize - (counter * 4));
                    emitRR("STW", this.currentReg - 1, this.currentReg);
                    this.currentReg = this.currentReg - 2;
                    counter++;
                }
            }
            emitRS("CALL", 30, methodName);
			
		}

		public void visit(ExpList node) {}
		public void visit(ArrayTy t) {}
		public void visit(EmptyStm node) {}
		public void visit(NameTy t) {}
		public void visit(ParDec d) {}
		public void visit(TypeDec d) {}
		public void visit(VarDec d) {}
	}
}