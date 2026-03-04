package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {

    private int mainPc;
    private int lengthAdr;

    public int getMainPc() {
        return mainPc;
    }

    Logger log = Logger.getLogger(CodeGenerator.class.getName());

    public CodeGenerator() {
        Obj ord = Tab.find("ord");
        ord.setAdr(Code.pc);
        Code.put(Code.enter);
        Code.put(ord.getLevel());
        Code.put(ord.getLocalSymbols().size());
        Code.put(Code.load_n);
        Code.put(Code.exit);
        Code.put(Code.return_);

        Obj chr = Tab.find("chr");
        chr.setAdr(Code.pc);
        Code.put(Code.enter);
        Code.put(chr.getLevel());
        Code.put(chr.getLocalSymbols().size());
        Code.put(Code.load_n);
        Code.put(Code.exit);
        Code.put(Code.return_);

        Obj len = Tab.find("len");
        len.setAdr(Code.pc);
        lengthAdr = Code.pc;
        Code.put(Code.enter);
        Code.put(len.getLevel());
        Code.put(len.getLocalSymbols().size());
        Code.put(Code.load_n);
        Code.put(Code.arraylength);
        Code.put(Code.exit);
        Code.put(Code.return_);

    }

    @Override
    public void visit(MethodName methodName) {
        if (methodName.getMethodName().equals("main")) {
            mainPc = Code.pc;
        }
        methodName.obj.setAdr(Code.pc);

        Code.put(Code.enter);
        Code.put(methodName.obj.getLevel());
        Code.put(methodName.obj.getLocalSymbols().size());
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        Code.put(Code.exit);
        Code.put(Code.return_);

    }

    @Override
    public void visit(DesignatorIdent designatorIdent) {
        if (designatorIdent.getParent() instanceof DesignatorArray
                || designatorIdent.getParent() instanceof DesignatorLength) {
            Code.load(designatorIdent.obj);
        }
    }

    @Override
    public void visit(DesignatorLength designatorLength) {}

    @Override
    public void visit(ExpressionMinusTerm expressionMinusTerm) {
        Code.put(Code.neg);
    }

    @Override
    public void visit(DesignatorStatementAssign designatorStatementAssign) {
        Code.store(designatorStatementAssign.getDesignator().obj);
    }

    @Override
    public void visit(FactorNumber factorNumber) {
        Code.loadConst(factorNumber.getN1());
    }

    @Override
    public void visit(FactorChar factorChar) {
        Code.loadConst(factorChar.getC1());
    }

    @Override
    public void visit(FactorBool factorBool) {
        Code.loadConst(factorBool.getB1());
    }

    @Override
    public void visit(FactorDesignator factorDesignator) {
        if (factorDesignator.getDesignator().obj.getKind() != Obj.Meth) {
            Code.load(factorDesignator.getDesignator().obj);
        }
        else {
            int offset = lengthAdr - Code.pc;
            Code.put(Code.call);
            Code.put2(offset);
        }
    }

    @Override
    public void visit(FactorNewType factorNewType) {
        Code.put(Code.newarray);
        if (factorNewType.getType().struct.equals(Tab.intType)
                || factorNewType.getType().struct.equals(SemanticAnalyzer.boolType)) {
            Code.put(1);
        }
        else if (factorNewType.getType().struct.equals(Tab.charType)) {
            Code.put(0);
        }
    }

    public void visit(FactorFuncNoActPars factorFuncNoActPars) {
        int offset = factorFuncNoActPars.getDesignator().obj.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(offset);
    }

    public void visit(FactorFuncActPars factorFuncActPars) {
        int offset = factorFuncActPars.getDesignator().obj.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(offset);
    }

    @Override
    public void visit(ExpressionAddOpTerm expressionAddOpTerm) {
        if (expressionAddOpTerm.getAddOp() instanceof AddOpPLUS) {
            Code.put(Code.add);
        }
        else if (expressionAddOpTerm.getAddOp() instanceof AddOpMINUS) {
            Code.put(Code.sub);
        }
    }

    @Override
    public void visit(TermMulOpFactor termMulOpFactor) {
        if (termMulOpFactor.getMulOp() instanceof MulOpMUL) {
            Code.put(Code.mul);
        }
        else if (termMulOpFactor.getMulOp() instanceof MulOpDIV) {
            Code.put(Code.div);
        }
        else if (termMulOpFactor.getMulOp() instanceof MulOpMOD) {
            Code.put(Code.rem);
        }
    }

    /* Statements */

    @Override
    public void visit(DesignatorStatementInc designatorStatementInc) {
        if (designatorStatementInc.getDesignator().obj.getKind() == Obj.Elem) {
            Code.put(Code.dup2);
        }
        Code.load(designatorStatementInc.getDesignator().obj);
        Code.loadConst(1);
        Code.put(Code.add);
        Code.store(designatorStatementInc.getDesignator().obj);
    }

    @Override
    public void visit(DesignatorStatementDec designatorStatementDec) {
        if (designatorStatementDec.getDesignator().obj.getKind() == Obj.Elem) {
            Code.put(Code.dup2);
        }
        Code.load(designatorStatementDec.getDesignator().obj);
        Code.loadConst(1);
        Code.put(Code.sub);
        Code.store(designatorStatementDec.getDesignator().obj);
    }

    @Override
    public void visit(StatementReturn statementReturn) {
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    @Override
    public void visit(StatementRead statementRead) {
        if (statementRead.getDesignator().obj.getType().equals(Tab.charType)) {
            Code.put(Code.bread);
        }
        else if (statementRead.getDesignator().obj.getType().equals(Tab.intType)) {
            Code.put(Code.read);
        }
        else if (statementRead.getDesignator().obj.getType().equals(SemanticAnalyzer.boolType)) {
            Code.put(Code.read);
        }
        Code.store(statementRead.getDesignator().obj);
    }

    @Override
    public void visit(StatementPrint statementPrint) {
        Code.loadConst(0);
        if (statementPrint.getExpr().struct.equals(Tab.charType)) {
            Code.put(Code.bprint);
        }
        else if (statementPrint.getExpr().struct.equals(Tab.intType)) {
            Code.put(Code.print);
        }
        else if (statementPrint.getExpr().struct.equals(SemanticAnalyzer.boolType)) {
            Code.put(Code.print);
        }
    }

    @Override
    public void visit(StatementPrintWithNumber statementPrintWithNumber) {
        Code.loadConst(statementPrintWithNumber.getNumber());
        if (statementPrintWithNumber.getExpr().struct.equals(Tab.charType)) {
            Code.put(Code.bprint);
        }
        else if (statementPrintWithNumber.getExpr().struct.equals(Tab.intType)) {
            Code.put(Code.print);
        }
        else if (statementPrintWithNumber.getExpr().struct.equals(SemanticAnalyzer.boolType)) {
            Code.put(Code.print);
        }
    }

    @Override
    public void visit(DesignatorStatementFuncActPars designatorStatementFuncActPars) {
        Code.put(Code.call);
        Code.put2(designatorStatementFuncActPars.getDesignator().obj.getAdr() - Code.pc);

        if (designatorStatementFuncActPars.getDesignator().obj.getType() != Tab.noType) {
            Code.put(Code.pop);
        }
    }

    @Override
    public void visit(DesignatorStatementFuncNoActPars designatorStatementFuncNoActPars) {
        Code.put(Code.call);
        Code.put2(designatorStatementFuncNoActPars.getDesignator().obj.getAdr() - Code.pc);

        if (designatorStatementFuncNoActPars.getDesignator().obj.getType() != Tab.noType) {
            Code.put(Code.pop);
        }
    }

}
