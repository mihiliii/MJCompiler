package rs.ac.bg.etf.pp1;

import java.util.Collection;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.util.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

    private final Logger log = Logger.getLogger(getClass());
    private final Struct boolType = new Struct(Struct.Bool);

    private boolean errorDetected = false;

    private Obj boolObj = null;
    private Obj programObj = null;

    private Declaration declaration = null;
    private Obj currentMethod = null;
    private boolean hasMainMethod = false;

    private Obj currentEnum = null;

    SemanticAnalyzer() {
        boolObj = Tab.insert(Obj.Type, "bool", boolType);
        boolObj.setAdr(-1);
        boolObj.setLevel(-1);
    }

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

    /* SEM PASS CODE */

    @Override
    public void visit(ProgramName programName) {
        if (programObj != null) {
            report_error("Program name already defined", programName);
        }
        else {
            programObj = Tab.insert(Obj.Prog, programName.getIdent(), Tab.noType);
            Tab.openScope();
        }
    }

    @Override
    public void visit(Program program) {
        Tab.chainLocalSymbols(programObj);
        Tab.closeScope();

        if (hasMainMethod == false) {
            report_error("Main method is needed to run program", null);
        }

        programObj = null;
    }

    @Override
    public void visit(Type type) {
        Obj typeObject = Tab.find(type.getName());

        if (typeObject == Tab.noObj) {
            report_error("Unknown type [" + type.getName() + "]", type);
            type.struct = Tab.noType;
        }
        else if (typeObject.getKind() != Obj.Type) {
            report_error("Name [" + type.getName() + "] is not a type", type);
            type.struct = Tab.noType;
        }
        else {
            type.struct = typeObject.getType();
        }

        declaration = new Declaration(type.struct);
    }

    @Override
    public void visit(ConstDeclList constDeclList) {
        declaration = null;
    }

    @Override
    public void visit(ConstDecl constDecl) {
        Literal literal = constDecl.getConst().literal;

        if (literal.getType().assignableTo(declaration.getType())) {
            declaration.initializeCon(constDecl.getIdent(), literal);
        }
        else {
            report_error("Cannot initialize constant of type [" + Stringify.toString(declaration.getType())
                    + "] with value of type [" + Stringify.toString(literal.getType()) + "]", constDecl);
        }
    }

    @Override
    public void visit(ConstNumber constNumber) {
        constNumber.literal = new Literal(Tab.intType, constNumber.getValue());
    }

    @Override
    public void visit(ConstChar constChar) {
        constChar.literal = new Literal(Tab.charType, constChar.getValue());
    }

    @Override
    public void visit(ConstBool constBool) {
        constBool.literal = new Literal(boolType, constBool.getValue());
    }

    @Override
    public void visit(VarDeclList varDeclList) {
        declaration = null;
    }

    @Override
    public void visit(VarDeclSingle varDeclSingle) {
        Obj object = null;

        if (currentMethod == null) {
            object = Tab.find(varDeclSingle.getIdent());
        }
        else {
            object = Tab.currentScope.findSymbol(varDeclSingle.getIdent());
        }

        if (object != null && object != Tab.noObj) {
            report_error("Redefinition of variable '" + varDeclSingle.getIdent() + "'", varDeclSingle);
        }
        else {
            declaration.initializeVar(varDeclSingle.getIdent());
        }

    }

    @Override
    public void visit(VarDeclArray varDeclArray) {
        Obj object = null;

        if (currentMethod == null) {
            object = Tab.find(varDeclArray.getIdent());
        }
        else {
            object = Tab.currentScope().findSymbol(varDeclArray.getIdent());
        }

        if (object != null && object != Tab.noObj) {
            report_error("Redefinition of variable '" + varDeclArray.getIdent() + "'", varDeclArray);
        }
        else {
            declaration.initializeVarArray(varDeclArray.getIdent());
        }
    }

    @Override
    public void visit(EnumDeclList enumDeclList) {
        Tab.chainLocalSymbols(declaration.getType());
        Tab.closeScope();

        declaration = null;
    }

    @Override
    public void visit(EnumName enumName) {
        Struct type = Tab.noType;
        Obj object = Tab.find(enumName.getName());

        if (currentMethod != null) {
            report_error("Enum '" + enumName.getName() + "' must be declared in global scope", enumName);
        }
        else if (object != Tab.noObj) {
            report_error("Redefinition of enum '" + enumName.getName() + "'", enumName);
        }
        else {
            type = new Struct(Struct.Enum);
        }

        declaration = new Declaration(type);
        declaration.initializeEnum(enumName.getName());
        Tab.openScope();
    }

    @Override
    public void visit(EnumDeclAssign enumDeclAssign) {
        Obj object = Tab.currentScope.findSymbol(enumDeclAssign.getEnumField().getName());

        if (object != null) {
            report_error("Multiple enum fields with same name '" + object.getName() + "'", enumDeclAssign);
        }
        else {
            declaration.initializeEnumConst(enumDeclAssign.getEnumField().getName(), enumDeclAssign.getValue());
        }
    }

    public void visit(EnumDeclNonAssign enumDeclNonAssign) {
        Obj object = Tab.currentScope.findSymbol(enumDeclNonAssign.getEnumField().getName());

        if (object != null) {
            report_error("Multiple enum fields with same name '" + object.getName() + "'", enumDeclNonAssign);
        }
        else {
            declaration.initializeEnumConst(enumDeclNonAssign.getEnumField().getName());
        }
    }

    @Override
    public void visit(MethodName methodName) {
        if (methodName.getMethodName().equals("main") || methodName.getMethodName().equals("Main")) {
            hasMainMethod = true;
        }
        currentMethod = Tab.insert(Obj.Meth, methodName.getMethodName(), Tab.noType);
        Tab.openScope();
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        Tab.chainLocalSymbols(currentMethod);
        Tab.closeScope();
        currentMethod = null;
    }

    @Override
    public void visit(ExprSingle exprSingle) {
        exprSingle.struct = exprSingle.getExpression().struct;
    }

    @Override
    public void visit(ExprTernary exprTernary) {
        Struct condType = exprTernary.getCondFact().struct;
        Struct leftType = exprTernary.getExpression().struct;
        Struct rightType = exprTernary.getExpression1().struct;
        currentEnum = null;

        if (condType == Tab.noType || leftType == Tab.noType || rightType == Tab.noType) {
            exprTernary.struct = Tab.noType;
        }
        else if (!(leftType.getKind() == rightType.getKind())) {
            report_error("Ternary operator expressions are not the same type: [" + Stringify.toString(leftType)
                    + "] and [" + Stringify.toString(rightType) + "]", exprTernary);
            exprTernary.struct = Tab.noType;
        }
        else if (!exprTernary.getCondFact().struct.equals(boolType)) {
            report_error("Condition in ternary operator must be of type [bool]", exprTernary);
            exprTernary.struct = Tab.noType;
        }
        else {
            exprTernary.struct = leftType;
        }
    }

    @Override
    public void visit(CondFactExpression condFactExpression) {
        condFactExpression.struct = condFactExpression.getExpression().struct;
    }

    public void visit(CondFactRelOp condFactRelOp) {
        Struct leftType = condFactRelOp.getExpression().struct;
        String operator = Stringify.toString(condFactRelOp.getRelOp());
        Struct rightType = condFactRelOp.getExpression1().struct;

        if (!leftType.compatibleWith(rightType)) {
            report_error("Types [" + Stringify.toString(leftType) + "] and [" + Stringify.toString(rightType)
                    + "] are not compatible for operator '" + operator + "'", condFactRelOp);
            condFactRelOp.struct = Tab.noType;
        }
        else if (leftType.getKind() == Struct.Array && rightType.getKind() == Struct.Array) {

            if (operator.equals("==") || operator.equals("!=")) {
                condFactRelOp.struct = boolType;
            }
            else {
                report_error("Operator '" + operator + "' cannot be applied to [array] type", condFactRelOp);
                condFactRelOp.struct = Tab.noType;
            }

        }
        else {
            condFactRelOp.struct = boolType;
        }
    }

    @Override
    public void visit(ExpressionTerm expressionTerm) {
        expressionTerm.struct = expressionTerm.getTerm().struct;
    }

    @Override
    public void visit(ExpressionAddOpTerm expressionAddOpTerm) {
        String operator = Stringify.toString(expressionAddOpTerm.getAddOp());
        Struct expressionStruct = expressionAddOpTerm.getExpression().struct;
        Struct termStruct = expressionAddOpTerm.getTerm().struct;
        currentEnum = null;

        if (termStruct.getKind() != Struct.Int && termStruct.getKind() != Struct.Enum) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + Stringify.toString(termStruct) + "]", expressionAddOpTerm);
            expressionAddOpTerm.struct = Tab.noType;
        }
        else if (expressionStruct.getKind() != Struct.Int && expressionStruct.getKind() != Struct.Enum) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + Stringify.toString(expressionStruct) + "]", expressionAddOpTerm);
            expressionAddOpTerm.struct = Tab.noType;
        }
        else {
            expressionAddOpTerm.struct = Tab.intType;
        }
    }

    @Override
    public void visit(ExpressionMinusTerm expressionMinusTerm) {
        Struct termStruct = expressionMinusTerm.getTerm().struct;

        if (termStruct.getKind() != Struct.Int && termStruct.getKind() != Struct.Enum) {
            report_error("Operator '-' cannot be used on operand of type [" + Stringify.toString(termStruct) + "]",
                    expressionMinusTerm);
            expressionMinusTerm.struct = Tab.noType;
        }
        else {
            expressionMinusTerm.struct = Tab.intType;
        }
    }

    @Override
    public void visit(TermFactor termFactor) {
        termFactor.struct = termFactor.getFactor().struct;
    }

    @Override
    public void visit(TermMulOpFactor termMulOpFactor) {
        String operator = Stringify.toString(termMulOpFactor.getMulOp());
        Struct termStruct = termMulOpFactor.getTerm().struct;
        Struct factorStruct = termMulOpFactor.getFactor().struct;
        currentEnum = null;

        if (factorStruct.getKind() != Struct.Int && factorStruct.getKind() != Struct.Enum) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + Stringify.toString(factorStruct) + "]", termMulOpFactor);
            termMulOpFactor.struct = Tab.noType;
        }
        else if (termStruct.getKind() != Struct.Int && termStruct.getKind() != Struct.Enum) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + Stringify.toString(termStruct) + "]", termMulOpFactor);
            termMulOpFactor.struct = Tab.noType;
        }
        else {
            termMulOpFactor.struct = Tab.intType;
        }
    }

    @Override
    public void visit(FactorChar factorChar) {
        factorChar.struct = Tab.charType;
    }

    @Override
    public void visit(FactorNumber factorNumber) {
        factorNumber.struct = Tab.intType;
    }

    @Override
    public void visit(FactorBool factorBool) {
        factorBool.struct = boolType;
    }

    @Override
    public void visit(FactorNewType factorNewType) {
        Struct indexType = factorNewType.getExpr().struct;
        Struct newType = factorNewType.getType().struct;

        if (indexType.getKind() != Struct.Int && indexType.getKind() != Struct.Enum) {
            report_error("Array index is not of type [int] nor [enum] ([" + Stringify.toString(indexType) + "])",
                    factorNewType);
            factorNewType.struct = Tab.noType;
        }
        else {
            factorNewType.struct = new Struct(Struct.Array, newType);
        }
    }

    @Override
    public void visit(FactorExpr factorExpr) {
        factorExpr.struct = factorExpr.getExpr().struct;
    }

    @Override
    public void visit(FactorDesignator factorDesignator) {
        factorDesignator.struct = factorDesignator.getDesignator().obj.getType();
    }

    @Override
    public void visit(DesignatorIdent designatorIdent) {
        designatorIdent.obj = Tab.find(designatorIdent.getName());

        if (designatorIdent.obj == Tab.noObj) {
            report_error("Undeclared identifier '" + designatorIdent.getName() + "'", designatorIdent);
        }
    }

    @Override
    public void visit(DesignatorField designatorField) {
        Obj object = designatorField.getDesignator().obj;

        if (object == Tab.noObj) {
            designatorField.obj = Tab.noObj;
        }
        else if (object.getType().getKind() != Struct.Enum) {
            report_error("Name '" + object.getName() + "' is not an enum", designatorField);
            designatorField.obj = Tab.noObj;
        }
        else {
            Collection<Obj> enumFields = object.getType().getMembers();

            for (Obj objectField : enumFields) {
                if (objectField.getName().equals(designatorField.getIdent()) && objectField.getKind() == Obj.Con) {
                    designatorField.obj = objectField;
                    currentEnum = objectField;
                    return;
                }
            }

            report_error(
                    "Enum '" + object.getName() + "' does not have a field named '" + designatorField.getIdent() + "'",
                    designatorField);
            designatorField.obj = Tab.noObj;
        }

        currentEnum = null;
    }

    @Override
    public void visit(DesignatorArray designatorArray) {
        Obj object = designatorArray.getDesignator().obj;
        Struct indexType = designatorArray.getExpr().struct;

        if (object == Tab.noObj) {
            designatorArray.obj = Tab.noObj;
        }
        else if (object.getType().getKind() != Struct.Array) {
            report_error("Name '" + object.getName() + "' is not an array", designatorArray);
            designatorArray.obj = Tab.noObj;
        }
        else if (indexType.getKind() != Struct.Int && indexType.getKind() != Struct.Enum) {
            report_error("Array '" + object.getName() + "' index is not of type [int] nor [enum] (["
                    + Stringify.toString(indexType) + "])", designatorArray);
            designatorArray.obj = Tab.noObj;
        }
        else {
            designatorArray.obj = new Obj(Obj.Elem, object.getName() + "[$]", object.getType().getElemType());
        }
    }

    @Override
    public void visit(DesignatorLength designatorLength) {
        Obj object = designatorLength.getDesignator().obj;

        if (object == Tab.noObj) {
            designatorLength.obj = Tab.noObj;
        }
        else if (object.getType().getKind() != Struct.Array) {
            report_error("Name '" + object.getName() + "' is not an array", designatorLength);
            designatorLength.obj = Tab.noObj;
        }
        else {
            designatorLength.obj = Tab.find("len");
        }
    }

    @Override
    public void visit(DesignatorStatementAssign designatorStatementAssign) {
        Obj designator = designatorStatementAssign.getDesignator().obj;
        Struct exprType = designatorStatementAssign.getExpr().struct;

        if (designator == Tab.noObj || exprType == Tab.noType) {
            currentEnum = null;
            return;
        }
        else if (designator.getKind() != Obj.Var && designator.getKind() != Obj.Elem) {
            report_error("Designator '" + designator.getName() + "' is not a variable", designatorStatementAssign);
        }
        else if (designator.getType().getKind() == Struct.Enum) {
            Collection<Obj> enumFields = designator.getType().getMembers();
            boolean found = false;

            for (Obj objectField : enumFields) {
                if (objectField == currentEnum) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                report_error("Enum value '" + currentEnum.getName() + "' is not the same enum type as variable '"
                        + designator.getName() + "'", designatorStatementAssign);
            }
        }
        else if (!exprType.assignableTo(designator.getType())) {
            report_error("Cannot assign value of type [" + Stringify.toString(exprType) + "] to variable of type ["
                    + Stringify.toString(designator.getType()) + "]", designatorStatementAssign);
        }

        currentEnum = null;
    }

}
