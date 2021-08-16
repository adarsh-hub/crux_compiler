//0.22
package crux.backend;

import crux.midend.ir.core.*;
import crux.midend.ir.core.insts.*;
import crux.printing.IRValueFormatter;

import java.util.*;
public final class CodeGen extends InstVisitor {
    private final IRValueFormatter irFormat = new IRValueFormatter();

    private final Program p;
    private final CodePrinter out;
    int VarCounter = 0;
    long AL = 0;
    private HashMap<Instruction, String> mLabelMap;

    private HashMap<Variable, String> registers = null;

    public CodeGen(Program p) {
        this.p = p;
        // Do not change the file name that is outputted or it will
        // break the grader!

        out = new CodePrinter("a.s");
    }

    public void genCode() {
        //This function should generate code for the entire program.
        this.registers = new HashMap<Variable, String>();
        var pGlobVars = p.getGlobals();
        var pFunctions = p.getFunctions();
        //Declare global variables
        while(pGlobVars.hasNext()){
            var glob = pGlobVars.next();
            var dest = glob.getAllocatedAddress();
            String name = dest.getName().substring(1);
            var sz = (IntegerConstant) glob.getNumElement();
            long size = 8*sz.getValue();
            out.bufferCode(".comm " + name + ", " + size + ", 8");
        }
        while(pFunctions.hasNext()) {
            genCode(pFunctions.next());
        }
        out.outputBuffer();
        out.close();
    }

    public void nextInstruction(Function f){
        HashMap<Instruction, String> labelMap = new HashMap<>();
        Stack<Instruction> tovisit = new Stack<>();
        HashSet<Instruction> discovered = new HashSet<>();
        tovisit.push(f.getStart());
        while (!tovisit.isEmpty()) {
            Instruction inst = tovisit.pop();
            inst.accept(this);
            for (int childIdx = 0; childIdx < inst.numNext(); childIdx++) {
                Instruction child = inst.getNext(childIdx);
                if (discovered.contains(child)) {
                    //Found the node for a second time...need a label for merge points
                    if (!labelMap.containsKey(child)) {
                        child.accept(this);
                    }
                } else {
                    discovered.add(child);
                    tovisit.push(child);
                    //Need a label for jump targets also
                    if (childIdx == 1 && !labelMap.containsKey(child)) {
                        child.accept(this);
                    }
                }
            }
        }
    }

    public String getTempReg(){
        String register = "r10";
        var check = registers.getOrDefault("r10", null);
        if(check!=null){register = "r11";}
        return register;
    }

    public long getValue(Value val){
        long value = 0;
        if(val.getClass()==IntegerConstant.class){
            value = ((IntegerConstant) val).getValue();
        }
        if(val.getClass()==BooleanConstant.class){
            var v = ((BooleanConstant) val);
            if(v.getValue()==true){
                value = 1;
            }
            else{value = 0;}
        }
        return value;
    }

    private int labelcount = 1;

    private String getNewLabel() {
        return "L" + (labelcount++);
    }

    private void genCode(Function f) {
        System.out.println("GENCODE START");
        mLabelMap = assignLabels(f);
        var name = f.getName();
        var code = ".globl " + name;

        out.bufferCode(code);
        out.bufferLabel(name+":");
        int numslots = 8;
        out.bufferCode("enter $(8 * " + numslots + "), $0");
        var fStart = f.getStart();
        nextInstruction(f);
        out.bufferCode("leave");
        out.bufferCode("ret");

    }

    /** Assigns Labels to any Instruction that might be the target of a
     * conditional or unconditional jump. */
    private HashMap<Instruction, String> assignLabels(Function f) {
        HashMap<Instruction, String> labelMap = new HashMap<>();
        Stack<Instruction> tovisit = new Stack<>();
        HashSet<Instruction> discovered = new HashSet<>();
        tovisit.push(f.getStart());
        while (!tovisit.isEmpty()) {
            Instruction inst = tovisit.pop();

            for (int childIdx = 0; childIdx < inst.numNext(); childIdx++) {
                Instruction child = inst.getNext(childIdx);
                if (discovered.contains(child)) {
                    //Found the node for a second time...need a label for merge points
                    if (!labelMap.containsKey(child)) {
                        labelMap.put(child, getNewLabel());
                    }
                } else {
                    discovered.add(child);
                    tovisit.push(child);
                    //Need a label for jump targets also
                    if (childIdx == 1 && !labelMap.containsKey(child)) {
                        labelMap.put(child, getNewLabel());
                    }
                }
            }
        }
        return labelMap;
    }

    //Memory Instructions
    public void visit(AddressAt i) {
        System.out.println("codegen visitng AddressAt");
        var iBase = i.getBase();
        var iDest = i.getDst();
        var iOff = i.getOffset();
        out.bufferCode("/* "+ iDest.toString() + " = AddressAt " + iBase.toString() +", "+ iOff.toString() + " */");
        String name = iBase.toString().substring(1);
        out.bufferCode("movq " + AL + "(%rbp), %r11");
        out.bufferCode("movq $8, %r10");
        out.bufferCode("imul %r10, %r11");
        VarCounter++;
        AL= -8*VarCounter;
        out.bufferCode("movq " + name + "@GOTPCREL(%rip) , %r10");
        out.bufferCode("addq %r10, %r11");
        out.bufferCode("movq %r11, " + AL + "(%rbp)");

    }
    public void visit(LoadInst i) {
        System.out.println("codegen visitng LoadInst");
        var dest = i.getDst();
        var addy = i.getSrcAddress();
        out.bufferCode("/* LoadInst: "+ addy.toString() + " into " + dest.toString()+" */");
        out.bufferCode("movq "+AL+"(%rbp), %r10");
        out.bufferCode("movq 0(%r10), %r10");
        VarCounter++;
        AL= -8*VarCounter;
        out.bufferCode("movq %r10, "+AL+"(%rbp)");
    }
    public void visit(StoreInst i) {
        System.out.println("codegen visitng StoreInst");
        var dest = i.getDestAddress();
        var stor = i.getSrcValue();
        out.bufferCode("/* StoreInst: "+ stor.toString() + " into " + dest.toString()+" */");
        registers.put(dest,String.valueOf(AL));
        out.bufferCode("movq "+AL+"(%rbp), %r10");
        long AL_str= -8*(VarCounter-1);
        out.bufferCode("movq "+AL_str+"(%rbp), %r11");
        out.bufferCode("movq %r10, 0(%r11)");
    }
    public void visit(CopyInst i) {
        System.out.println("codegen visitng CopyInst");
        VarCounter++;
        long value = getValue(i.getSrcValue());
        var dest = i.getDstVar();
        out.bufferCode("/* CopyInst: " + dest.toString() + " = " + value +" */");

        String register = "r10";

        out.bufferCode("movq $" + value + ", %"+register);
        AL = -8*VarCounter;
        out.bufferCode("movq %"+register+", "+ AL+"(%rbp)");
        registers.put(dest,String.valueOf(AL));
    }

    //Operations
    public void visit(BinaryOperator i) {
        System.out.println("codegen visitng BinaryOperator");
        var op = i.getOperator();
        var dest = i.getDst();
        var lhs = i.getLeftOperand();
        var rhs = i.getRightOperand();
        if(op == BinaryOperator.Op.Add){
            out.bufferCode("movq " + AL +"(%rbp), %r10");
            AL = AL - 8;
            out.bufferCode("addq " + AL +"(%rbp), %r10");
            if(dest!=lhs&&dest!=rhs){AL = AL - 8;}
            out.bufferCode("movq %r10, " + AL + "(%rbp)");
        }
        if(op == BinaryOperator.Op.Sub){
            out.bufferCode("movq " + AL +"(%rbp), %r10");
            AL = AL - 8;
            out.bufferCode("subq " + AL +"(%rbp), %r10");
            if(dest!=lhs&&dest!=rhs){AL = AL - 8;}
            out.bufferCode("movq %r10, " + AL + "(%rbp)");
        }
        if(op == BinaryOperator.Op.Mul){
            out.bufferCode("movq " + AL +"(%rbp), %r10");
            AL = AL - 8;
            out.bufferCode("imulq " + AL +"(%rbp), %r10");
            if(dest!=lhs&&dest!=rhs){AL = AL - 8;}
            out.bufferCode("movq %r10, " + AL + "(%rbp)");
        }
        if(op == BinaryOperator.Op.Div){
            out.bufferCode("movq " + AL +"(%rbp), %r10");
            AL = AL - 8;
            out.bufferCode("divq " + AL +"(%rbp), %r10");
            if(dest!=lhs&&dest!=rhs){AL = AL - 8;}
            out.bufferCode("movq %r10, " + AL + "(%rbp)");
        }
    }
    public void visit(CompareInst i) {
        System.out.println("codegen visitng CompareInst");
        var pred = i.getPredicate();
        var lhs = i.getLeftOperand();
        var rhs = i.getRightOperand();
        var dest = i.getDst();
        out.bufferCode("/* CompareInst" + dest.toString() + " = " + lhs.toString() + pred.toString() + rhs.toString() + "*/");
        if(pred.equals(CompareInst.Predicate.LT)){
            out.bufferCode("movq $0, %r10");
            out.bufferCode("movq $1, %rax");
            out.bufferCode("movq " +AL+"(%rbp), %r11");
            AL = AL - 8;
            out.bufferCode("cmp " +AL+"(%rbp), %r11");
            out.bufferCode("cmovl %rax, %r10");
            AL = AL - 8;
            out.bufferCode("movq %r10,"+ AL+"(%rbp)");
        }
        if(pred.equals(CompareInst.Predicate.GT)){
            out.bufferCode("movq $0, %r10");
            out.bufferCode("movq $1, %rax");
            out.bufferCode("movq " +AL+"(%rbp), %r11");
            AL = AL - 8;
            out.bufferCode("cmp " +AL+"(%rbp), %r11");
            out.bufferCode("cmovg %rax, %r10");
            AL = AL - 8;
            out.bufferCode("movq %r10,"+ AL+"(%rbp)");
        }
        if(pred.equals(CompareInst.Predicate.LE)){
            out.bufferCode("movq $0, %r10");
            out.bufferCode("movq $1, %rax");
            out.bufferCode("movq " +AL+"(%rbp), %r11");
            AL = AL - 8;
            out.bufferCode("cmp " +AL+"(%rbp), %r11");
            out.bufferCode("cmovle %rax, %r10");
            AL = AL - 8;
            out.bufferCode("movq %r10,"+ AL+"(%rbp)");
        }
        if(pred.equals(CompareInst.Predicate.GE)){
            out.bufferCode("movq $0, %r10");
            out.bufferCode("movq $1, %rax");
            out.bufferCode("movq " +AL+"(%rbp), %r11");
            AL = AL - 8;
            out.bufferCode("cmp " +AL+"(%rbp), %r11");
            out.bufferCode("cmovge %rax, %r10");
            AL = AL - 8;
            out.bufferCode("movq %r10,"+ AL+"(%rbp)");
        }
        if(pred.equals(CompareInst.Predicate.EQ)){
            out.bufferCode("movq $0, %r10");
            out.bufferCode("movq $1, %rax");
            out.bufferCode("movq " +AL+"(%rbp), %r11");
            AL = AL - 8;
            out.bufferCode("cmp " +AL+"(%rbp), %r11");
            out.bufferCode("cmove %rax, %r10");
            AL = AL - 8;
            out.bufferCode("movq %r10,"+ AL+"(%rbp)");
        }
        if(pred.equals(CompareInst.Predicate.NE)){
            out.bufferCode("movq $0, %r10");
            out.bufferCode("movq $1, %rax");
            out.bufferCode("movq " +AL+"(%rbp), %r11");
            AL = AL - 8;
            out.bufferCode("cmp " +AL+"(%rbp), %r11");
            out.bufferCode("cmovne %rax, %r10");
            AL = AL - 8;
            out.bufferCode("movq %r10,"+ AL+"(%rbp)");
        }
    }

    public void visit(JumpInst i) {
        System.out.println("codegen visitng JumpInst");
        var pred = i.getPredicate();
        out.bufferCode("/* JumpInst  " + pred.toString() + "*/");
        String register = getTempReg();
        out.bufferCode("movq " + AL + "(%rbp),%"+register);
        out.bufferCode("cmp $1,%"+register);
        out.bufferCode("je "+mLabelMap.get(i.getNext(1)));

        //L1
        //L2

    }

    public void visit(ReturnInst i) {
        System.out.println("codegen visitng ReturnInst");
        out.bufferCode("movq "+AL+"(%rbp), %rax");
        out.bufferCode("leave");
        out.bufferCode("ret");
}

    public void visit(CallInst i) {
        System.out.println("codegen visitng CallInst");
        String funcname = i.getCallee().getName();
        out.bufferCode("/* CallInst: call " + funcname + " */");
        if(i.getDst()!=null){
        out.bufferCode("movq "+AL+"(%rbp), %rdi");      }
        out.bufferCode("call " + funcname.substring(1));

    }

    public void visit(UnaryNotInst i) {
        System.out.println("codegen visitng UnaryNotInst");
    }

    public void visit(NopInst i) {
        /* Do Nothing */
        System.out.println("codegen visitng NopInst");
        out.bufferCode("/* NopInst */");
    }
}
