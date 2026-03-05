package rs.ac.bg.etf.pp1;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.symboltable.*;
import rs.ac.bg.etf.pp1.util.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

    private final Logger log = Logger.getLogger(getClass());
    private boolean errorDetected = false;

    private Obj programObj = null;
    private int numLocalVars = 0;

    private Obj currentMethod = null;
    private boolean hasMainMethod = false;

    private Declarations declaration = new Declarations();
    private ArrayList<Struct> actParsList = new ArrayList<>();

    public void report_error(String message, SyntaxNode info) {
        errorDetected = true;
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0) {
            msg.append(" --- line: ").append(line);
        }
        log.error(msg.toString());
    }

    public void report_info(String message, SyntaxNode info) {
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0) {
            msg.append(" --- line: ").append(line);
        }
        log.info(msg.toString());
    }

    public boolean passed() {
        return !errorDetected;
    }

    public int getNumLocalVars() {
        return numLocalVars;
    }

    /* Program definition */

    @Override
    public void visit(ProgramName programName) {
        if (programObj != null) {
            report_error("Program name already defined", programName);
        }
        else {
            programObj = SymTab.insert(Obj.Prog, programName.getName(), SymTab.noType);
            SymTab.openScope();
        }
    }

    @Override
    public void visit(Program program) {
        numLocalVars = SymTab.currentScope.getnVars();

        SymTab.chainLocalSymbols(programObj);
        SymTab.closeScope();

        if (hasMainMethod == false) {
            report_error("Main method is needed to run program", null);
        }

        programObj = null;
    }

    /* Type */

    @Override
    public void visit(Type type) {
        Obj typeObj = SymTab.find(type.getName());

        if (typeObj == SymTab.noObj) {
            report_error("Unknown type [" + type.getName() + "]", type);
            type.struct = SymTab.noType;
        }
        else if (typeObj.getKind() != Obj.Type) {
            report_error("Name [" + type.getName() + "] is not a type", type);
            type.struct = SymTab.noType;
        }
        else {
            type.struct = typeObj.getType();
        }

        declaration.setCurrentTypeObj(typeObj);
    }

    /* Const declarations */

    @Override
    public void visit(ConstDeclList constDeclList) {
        declaration.setCurrentTypeObj(SymTab.noObj);
    }

    @Override
    public void visit(ConstDecl constDecl) {
        Literal literal = constDecl.getConst().literal;

        if (currentMethod != null) {
            report_error("Constants must be declared in global scope", constDecl);
        }
        else if (declaration.getCurrentTypeObj().getType().assignableTo(literal.getType())) {
            declaration.initConst(constDecl.getIdent(), literal);
        }
        else {
            report_error("Cannot initialize constant of type ["
                    + Stringify.toString(declaration.getCurrentTypeObj().getType()) + "] with value of type ["
                    + Stringify.toString(literal.getType()) + "]", constDecl);
        }
    }

    @Override
    public void visit(ConstNumber constNumber) {
        constNumber.literal = new Literal(SymTab.intType, constNumber.getValue());
    }

    @Override
    public void visit(ConstChar constChar) {
        constChar.literal = new Literal(SymTab.charType, constChar.getValue());
    }

    @Override
    public void visit(ConstBool constBool) {
        constBool.literal = new Literal(SymTab.boolType, constBool.getValue());
    }

    /* Var declarations */

    @Override
    public void visit(VarDeclList varDeclList) {
        declaration.setCurrentTypeObj(SymTab.noObj);
    }

    @Override
    public void visit(VarDeclSingle varDeclSingle) {
        Obj object = null;

        if (currentMethod == null) {
            object = SymTab.find(varDeclSingle.getIdent());
        }
        else {
            object = SymTab.currentScope.findSymbol(varDeclSingle.getIdent());
        }

        if (object != null && object != SymTab.noObj) {
            report_error("Redefinition of variable '" + varDeclSingle.getIdent() + "'", varDeclSingle);
        }
        else {
            declaration.initVar(varDeclSingle.getIdent());
        }

    }

    @Override
    public void visit(VarDeclArray varDeclArray) {
        Obj object = null;

        if (currentMethod == null) {
            object = SymTab.find(varDeclArray.getIdent());
        }
        else {
            object = SymTab.currentScope().findSymbol(varDeclArray.getIdent());
        }

        if (object != null && object != SymTab.noObj) {
            report_error("Redefinition of variable '" + varDeclArray.getIdent() + "'", varDeclArray);
        }
        else {
            declaration.initVarArray(varDeclArray.getIdent());
        }
    }

    /* Enum declarations */

    @Override
    public void visit(EnumDeclList enumDeclList) {
        SymTab.chainLocalSymbols(declaration.getCurrentTypeObj().getType());
        SymTab.closeScope();

        declaration.setCurrentTypeObj(SymTab.noObj);
    }

    @Override
    public void visit(EnumName enumName) {
        Obj object = SymTab.find(enumName.getName());

        if (currentMethod != null) {
            report_error("Enum '" + enumName.getName() + "' must be declared in global scope", enumName);
            declaration.setCurrentTypeObj(SymTab.noObj);
        }
        else if (object != SymTab.noObj) {
            report_error("Redefinition of enum '" + enumName.getName() + "'", enumName);
            declaration.setCurrentTypeObj(SymTab.noObj);
        }
        else {
            declaration.setCurrentTypeObj(SymTab.insert(Obj.Type, enumName.getName(), new Struct(Struct.Enum)));
            SymTab.openScope();
        }
    }

    @Override
    public void visit(EnumDeclAssign enumDeclAssign) {
        Obj object = SymTab.currentScope.findSymbol(enumDeclAssign.getFieldName());

        if (object != null) {
            report_error("Multiple enum fields with same name '" + object.getName() + "'", enumDeclAssign);
            return;
        }
        if (declaration.getCurrentTypeObj().getType().getKind() != Struct.Enum) {
            report_error("Current type is not an enum", enumDeclAssign);
            return;
        }

        declaration.initEnumConst(enumDeclAssign.getFieldName(), enumDeclAssign.getValue());
    }

    public void visit(EnumDeclNonAssign enumDeclNonAssign) {
        Obj object = SymTab.currentScope.findSymbol(enumDeclNonAssign.getFieldName());

        if (object != null) {
            report_error("Multiple enum fields with same name '" + object.getName() + "'", enumDeclNonAssign);
            return;
        }

        if (declaration.getCurrentTypeObj().getType().getKind() != Struct.Enum) {
            report_error("Current type is not an enum", enumDeclNonAssign);
            return;
        }

        declaration.initEnumConst(enumDeclNonAssign.getFieldName());
    }

    /* Method declarations */

    @Override
    public void visit(MethodDecl methodDecl) {
        SymTab.chainLocalSymbols(currentMethod);
        SymTab.closeScope();
        currentMethod = null;
    }

    @Override
    public void visit(MethodName methodName) {
        if (methodName.getMethodName().equals("main") && hasMainMethod == false) {
            hasMainMethod = true;
        }
        else {
            report_error("There can be only one method named main", methodName);
        }

        methodName.obj = currentMethod = SymTab.insert(Obj.Meth, methodName.getMethodName(), SymTab.noType);
        SymTab.openScope();
    }

    /* Statements */

    @Override
    public void visit(StatementRead statementRead) {
        Obj designator = statementRead.getDesignator().obj;

        if (designator == SymTab.noObj) {
            return;
        }

        if (designator.getKind() != Obj.Var && designator.getKind() != Obj.Elem) {
            report_error("Designator '" + designator.getName() + "' is not a variable", statementRead);
        }
        else if (designator.getType().getKind() != Struct.Int && designator.getType().getKind() != Struct.Char
                && designator.getType().getKind() != Struct.Bool) {
            report_error("Cannot read into variable '" + designator.getName() + "' of type ["
                    + Stringify.toString(designator.getType()) + "]", statementRead);
        }

    }

    @Override
    public void visit(StatementPrint statementPrint) {
        Struct exprType = statementPrint.getExpr().struct;

        if (exprType == SymTab.noType) {
            return;
        }

        if (exprType.getKind() != Struct.Int && exprType.getKind() != Struct.Char
                && exprType.getKind() != Struct.Bool) {
            report_error("Cannot print expression of type [" + Stringify.toString(exprType) + "]", statementPrint);
        }
    }

    @Override
    public void visit(StatementPrintWithNumber statementPrintWithNumber) {
        Struct exprType = statementPrintWithNumber.getExpr().struct;

        if (exprType == SymTab.noType) {
            return;
        }

        if (exprType.getKind() != Struct.Int && exprType.getKind() != Struct.Char
                && exprType.getKind() != Struct.Bool) {
            report_error("Cannot print expression of type [" + Stringify.toString(exprType) + "]",
                    statementPrintWithNumber);
        }
    }

    /* Designator Statements */

    @Override
    public void visit(DesignatorStatementAssign designatorStatementAssign) {
        Obj designator = designatorStatementAssign.getDesignator().obj;
        Struct expr = designatorStatementAssign.getExpr().struct;

        if (designator == SymTab.noObj || expr == SymTab.noType) {
            return;
        }

        if (designator.getKind() != Obj.Var && designator.getKind() != Obj.Elem) {
            report_error("'" + designator.getName() + "' is not a variable", designatorStatementAssign);
        }
        else if (designator.getType().getKind() == Struct.Int && expr.getKind() == Struct.Enum) {
            return;
        }
        else if (designator.getType().getKind() == Struct.Enum && designator.getType() != expr) {
            report_error("'" + designator.getName() + "' is not the same Enum type as value being assigned",
                    designatorStatementAssign);
        }
        else if (!expr.assignableTo(designator.getType())) {
            report_error("Cannot assign value of type [" + Stringify.toString(expr) + "] to variable of type ["
                    + Stringify.toString(designator.getType()) + "]", designatorStatementAssign);
        }
    }

    @Override
    public void visit(DesignatorStatementInc designatorStatementInc) {
        Obj designator = designatorStatementInc.getDesignator().obj;

        if (designator == SymTab.noObj) {
            return;
        }

        else if (designator.getKind() != Obj.Var && designator.getKind() != Obj.Elem) {
            report_error("Designator '" + designator.getName() + "' is not a variable", designatorStatementInc);
        }
        else if (designator.getType().getKind() != Struct.Int) {
            report_error("Operator '++' cannot be applied to variable '" + designator.getName() + "' of type ["
                    + Stringify.toString(designator.getType()) + "]", designatorStatementInc);
        }
    }

    @Override
    public void visit(DesignatorStatementDec designatorStatementDec) {
        Obj designator = designatorStatementDec.getDesignator().obj;

        if (designator == SymTab.noObj) {
            return;
        }

        else if (designator.getKind() != Obj.Var && designator.getKind() != Obj.Elem) {
            report_error("Designator '" + designator.getName() + "' is not a variable", designatorStatementDec);
        }
        else if (designator.getType().getKind() != Struct.Int) {
            report_error("Operator '--' cannot be applied to variable '" + designator.getName() + "' of type ["
                    + Stringify.toString(designator.getType()) + "]", designatorStatementDec);
        }
    }

    @Override
    public void visit(DesignatorStatementFuncActPars designatorStatementFuncActPars) {
        Obj designator = designatorStatementFuncActPars.getDesignator().obj;

        if (designator != SymTab.noObj && designator.getKind() != Obj.Meth) {
            report_error("'" + designator.getName() + "' is not a method", designatorStatementFuncActPars);
            actParsList.clear();
            return;
        }

        if (designator.getLevel() != actParsList.size()) {
            report_error("'" + designator.getName() + "' expects " + designator.getAdr() + " arguments, but "
                    + actParsList.size() + " were given", designatorStatementFuncActPars);
            actParsList.clear();
            return;
        }

        ArrayList<Obj> formPars = new ArrayList<Obj>(designator.getLocalSymbols());
        for (int i = 0; i < designator.getLevel(); i++) {
            if (formPars.get(i).getType() != actParsList.get(i)) {
                report_error("Incompatible type of parameter " + (i + 1) + " in method '" + designator.getName()
                        + "': expected [" + Stringify.toString(formPars.get(i).getType()) + "], but found ["
                        + Stringify.toString(actParsList.get(i)) + "]", designatorStatementFuncActPars);
            }
        }

        actParsList.clear();
    }

    @Override
    public void visit(DesignatorStatementFuncNoActPars designatorStatementFuncNoActPars) {
        Obj designator = designatorStatementFuncNoActPars.getDesignator().obj;

        if (designator != SymTab.noObj && designator.getKind() != Obj.Meth) {
            report_error("'" + designator.getName() + "' is not a method", designatorStatementFuncNoActPars);
            return;
        }

        if (designator.getLevel() != 0) {
            report_error("'" + designator.getName() + "' expects " + designator.getAdr() + " arguments",
                    designatorStatementFuncNoActPars);
        }
    }

    /* ActPars */

    @Override
    public void visit(ActParsExpr actParsExpr) {
        actParsList.add(actParsExpr.getExpr().struct);
    }

    /* Expr */

    @Override
    public void visit(ExprSingle exprSingle) {
        exprSingle.struct = exprSingle.getExpression().struct;
    }

    @Override
    public void visit(ExprTernary exprTernary) {
        Struct condType = exprTernary.getCondFact().struct;
        Struct leftType = exprTernary.getExpression().struct;
        Struct rightType = exprTernary.getExpression1().struct;

        if (condType == SymTab.noType || leftType == SymTab.noType || rightType == SymTab.noType) {
            exprTernary.struct = SymTab.noType;
        }
        else if (leftType.getKind() != rightType.getKind()) {
            report_error("Ternary operator expressions are not the same type: [" + Stringify.toString(leftType)
                    + "] and [" + Stringify.toString(rightType) + "]", exprTernary);
            exprTernary.struct = SymTab.noType;
        }
        else if (condType != SymTab.boolType) {
            report_error("Condition in ternary operator must be of type [bool]", exprTernary);
            exprTernary.struct = SymTab.noType;
        }
        else {
            exprTernary.struct = leftType;
        }
    }

    /* CondFact */

    @Override
    public void visit(CondFactExpression condFactExpression) {
        condFactExpression.struct = condFactExpression.getExpression().struct;
    }

    @Override
    public void visit(CondFactRelOp condFactRelOp) {
        Struct leftType = condFactRelOp.getExpression().struct;
        String operator = Stringify.toString(condFactRelOp.getRelOp());
        Struct rightType = condFactRelOp.getExpression1().struct;

        if (!leftType.compatibleWith(rightType)) {
            report_error("Types [" + Stringify.toString(leftType) + "] and [" + Stringify.toString(rightType)
                    + "] are not compatible for operator '" + operator + "'", condFactRelOp);
            condFactRelOp.struct = SymTab.noType;
        }
        else if (leftType.getKind() == Struct.Array && rightType.getKind() == Struct.Array
                && !(operator.equals("==") || operator.equals("!="))) {
            report_error("Operator '" + operator + "' cannot be applied to [array] type", condFactRelOp);
            condFactRelOp.struct = SymTab.noType;
        }
        else {
            condFactRelOp.struct = SymTab.boolType;
        }
    }

    /* Expression */

    @Override
    public void visit(ExpressionTerm expressionTerm) {
        expressionTerm.struct = expressionTerm.getTerm().struct;
    }

    @Override
    public void visit(ExpressionAddOpTerm expressionAddOpTerm) {
        String operator = Stringify.toString(expressionAddOpTerm.getAddOp());
        Struct expressionStruct = expressionAddOpTerm.getExpression().struct;
        Struct termStruct = expressionAddOpTerm.getTerm().struct;

        if (termStruct.getKind() != Struct.Int && termStruct.getKind() != Struct.Enum) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + Stringify.toString(termStruct) + "]", expressionAddOpTerm);
            expressionAddOpTerm.struct = SymTab.noType;
        }
        else if (expressionStruct.getKind() != Struct.Int && expressionStruct.getKind() != Struct.Enum) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + Stringify.toString(expressionStruct) + "]", expressionAddOpTerm);
            expressionAddOpTerm.struct = SymTab.noType;
        }
        else {
            expressionAddOpTerm.struct = SymTab.intType;
        }
    }

    @Override
    public void visit(ExpressionMinusTerm expressionMinusTerm) {
        Struct termStruct = expressionMinusTerm.getTerm().struct;

        if (termStruct.getKind() != Struct.Int && termStruct.getKind() != Struct.Enum) {
            report_error("Operator '-' cannot be used on operand of type [" + Stringify.toString(termStruct) + "]",
                    expressionMinusTerm);
            expressionMinusTerm.struct = SymTab.noType;
        }
        else {
            expressionMinusTerm.struct = SymTab.intType;
        }
    }

    /* Term */

    @Override
    public void visit(TermFactor termFactor) {
        termFactor.struct = termFactor.getFactor().struct;
    }

    @Override
    public void visit(TermMulOpFactor termMulOpFactor) {
        String operator = Stringify.toString(termMulOpFactor.getMulOp());
        Struct termStruct = termMulOpFactor.getTerm().struct;
        Struct factorStruct = termMulOpFactor.getFactor().struct;

        if (factorStruct.getKind() != Struct.Int && factorStruct.getKind() != Struct.Enum) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + Stringify.toString(factorStruct) + "]", termMulOpFactor);
            termMulOpFactor.struct = SymTab.noType;
        }
        else if (termStruct.getKind() != Struct.Int && termStruct.getKind() != Struct.Enum) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + Stringify.toString(termStruct) + "]", termMulOpFactor);
            termMulOpFactor.struct = SymTab.noType;
        }
        else {
            termMulOpFactor.struct = SymTab.intType;
        }
    }

    /* Factor */

    @Override
    public void visit(FactorDesignator factorDesignator) {
        Obj designator = factorDesignator.getDesignator().obj;

        if (declaration.getEnumType(designator) != null) {
            factorDesignator.struct = declaration.getEnumType(designator).getType();
            return;
        }

        factorDesignator.struct = designator.getType();
    }

    @Override
    public void visit(FactorFuncNoActPars factorFuncNoActPars) {
        Obj designator = factorFuncNoActPars.getDesignator().obj;

        if (designator == SymTab.noObj) {
            factorFuncNoActPars.struct = SymTab.noType;
            return;
        }

        if (designator.getKind() != Obj.Meth) {
            report_error("'" + designator.getName() + "' is not a method", factorFuncNoActPars);
            factorFuncNoActPars.struct = SymTab.noType;
            return;
        }

        if (designator.getLevel() != 0) {
            report_error("'" + designator.getName() + "' expects " + designator.getAdr() + " arguments",
                    factorFuncNoActPars);
            factorFuncNoActPars.struct = SymTab.noType;
            return;
        }

        factorFuncNoActPars.struct = designator.getType();
    }

    @Override
    public void visit(FactorFuncActPars factorFuncActPars) {
        Obj designator = factorFuncActPars.getDesignator().obj;

        if (designator == SymTab.noObj) {
            factorFuncActPars.struct = SymTab.noType;
            actParsList.clear();
            return;
        }

        if (designator.getKind() != Obj.Meth) {
            report_error("'" + designator.getName() + "' is not a method", factorFuncActPars);
            factorFuncActPars.struct = SymTab.noType;
            actParsList.clear();
            return;
        }

        ArrayList<Obj> formPars = new ArrayList<Obj>(designator.getLocalSymbols());
        for (int i = 0; i < designator.getLevel(); i++) {
            if (formPars.get(i).getType() != actParsList.get(i)) {
                report_error("Incompatible type of parameter " + (i + 1) + " in method '" + designator.getName()
                        + "': expected [" + Stringify.toString(formPars.get(i).getType()) + "], but found ["
                        + Stringify.toString(actParsList.get(i)) + "]", factorFuncActPars);
                factorFuncActPars.struct = SymTab.noType;
                return;
            }
        }

        factorFuncActPars.struct = designator.getType();
        actParsList.clear();
    }

    @Override
    public void visit(FactorChar factorChar) {
        factorChar.struct = SymTab.charType;
    }

    @Override
    public void visit(FactorNumber factorNumber) {
        factorNumber.struct = SymTab.intType;
    }

    @Override
    public void visit(FactorBool factorBool) {
        factorBool.struct = SymTab.boolType;
    }

    @Override
    public void visit(FactorNewType factorNewType) {
        Struct indexType = factorNewType.getExpr().struct;
        Struct newType = factorNewType.getType().struct;

        if (indexType.getKind() != Struct.Int && indexType.getKind() != Struct.Enum) {
            report_error("Array index is not of type [int] ([" + Stringify.toString(indexType) + "])", factorNewType);
            factorNewType.struct = SymTab.noType;
        }
        else {
            factorNewType.struct = new Struct(Struct.Array, newType);
        }
    }

    @Override
    public void visit(FactorExpr factorExpr) {
        factorExpr.struct = factorExpr.getExpr().struct;
    }

    /* Designator */

    @Override
    public void visit(DesignatorIdent designatorIdent) {
        designatorIdent.obj = SymTab.find(designatorIdent.getName());

        if (designatorIdent.obj == SymTab.noObj) {
            report_error("Undeclared identifier '" + designatorIdent.getName() + "'", designatorIdent);
        }
    }

    @Override
    public void visit(DesignatorField designatorField) {
        Obj enumType = designatorField.getDesignator().obj;

        if (enumType == SymTab.noObj) {
            designatorField.obj = SymTab.noObj;
            return;
        }

        if (enumType.getType().getKind() != Struct.Enum) {
            report_error("Name '" + enumType.getName() + "' is not an enum", designatorField);
            designatorField.obj = SymTab.noObj;
            return;
        }

        for (Obj enumConst : new ArrayList<Obj>(enumType.getType().getMembers())) {
            if (enumConst.getName().equals(designatorField.getFieldName())) {
                designatorField.obj = enumConst;
                return;
            }
        }

        report_error("Enum type for enum constant '" + designatorField.getFieldName() + "' not found", designatorField);
        designatorField.obj = SymTab.noObj;
    }

    @Override
    public void visit(DesignatorArray designatorArray) {
        Obj object = designatorArray.getDesignator().obj;
        Struct indexType = designatorArray.getExpr().struct;

        if (object == SymTab.noObj) {
            designatorArray.obj = SymTab.noObj;
            return;
        }

        if (object.getType().getKind() != Struct.Array) {
            report_error("Name '" + object.getName() + "' is not an array", designatorArray);
            designatorArray.obj = SymTab.noObj;
        }
        else if (indexType.getKind() != Struct.Int && indexType.getKind() != Struct.Enum) {
            report_error("Array '" + object.getName() + "' index is not of type [int] nor [enum] (["
                    + Stringify.toString(indexType) + "])", designatorArray);
            designatorArray.obj = SymTab.noObj;
        }
        else {
            designatorArray.obj = new Obj(Obj.Elem, object.getName() + "[$]", object.getType().getElemType());
        }
    }

    @Override
    public void visit(DesignatorLength designatorLength) {
        Obj object = designatorLength.getDesignator().obj;

        if (object == SymTab.noObj) {
            designatorLength.obj = SymTab.noObj;
            return;
        }

        if (object.getType().getKind() != Struct.Array) {
            report_error("Name '" + object.getName() + "' is not an array", designatorLength);
            designatorLength.obj = SymTab.noObj;
        }
        else {
            designatorLength.obj = SymTab.find("len");
        }
    }

}
