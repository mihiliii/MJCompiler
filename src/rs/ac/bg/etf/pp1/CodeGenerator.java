package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {

    private int mainPc;

    public int getMainPc() {
        return mainPc;
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
    public void visit(DesignatorArray designatorArray) {
        if (!(designatorArray.getParent() instanceof DesignatorStatementAssign)) {
            Code.load(designatorArray.obj);
        }
    }

    @Override
    public void visit(DesignatorIdent designatorIdent) {
        if (!(designatorIdent.getParent() instanceof DesignatorStatementAssign)) {
            Code.load(designatorIdent.obj);
        }
    }

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

    // @Override
    // public void visit(FactorDesignator factorDesignator) {
    // Code.load(factorDesignator.getDesignator().obj);
    // }

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

}
