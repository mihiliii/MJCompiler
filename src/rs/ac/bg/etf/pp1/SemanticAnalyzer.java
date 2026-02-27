package rs.ac.bg.etf.pp1;

import java.util.Collection;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;
import rs.ac.bg.etf.pp1.ast.DesignatorArray;
import rs.ac.bg.etf.pp1.ast.FactorDesignator;
import rs.ac.bg.etf.pp1.ast.FactorNumber;

public class SemanticAnalyzer extends VisitorAdaptor {

    private Logger log = Logger.getLogger(getClass());
    private boolean errorDetected = false;

    private Obj boolObj;
    private Obj programObj;
    private final Struct boolType = new Struct(Struct.Bool);

    private Declaration declaration;
    private Obj currentMethod;

    private boolean hasMainMethod = false;

    private String toString(Struct type) {
        switch (type.getKind()) {
        case Struct.Int:
            return "int";
        case Struct.Char:
            return "char";
        case Struct.Bool:
            return "bool";
        case Struct.Array:
            return "array";
        case Struct.Enum:
            return "enum";
        case Struct.Class:
            return "class";
        case Struct.None:
            return "notype";
        default:
            return "unknown";
        }
    }

    private String toString(Object operator) {
        if (operator instanceof RelOp) {
            RelOp relOp = (RelOp) operator;
            if (relOp instanceof RelOpEQ) {
                return "==";
            }
            if (relOp instanceof RelOpNEQ) {
                return "!=";
            }
            if (relOp instanceof RelOpLT) {
                return "<";
            }
            if (relOp instanceof RelOpGT) {
                return ">";
            }
            if (relOp instanceof RelOpLTE) {
                return "<=";
            }
            if (relOp instanceof RelOpGTE) {
                return ">=";
            }
        }
        else if (operator instanceof AddOp) {
            AddOp addOp = (AddOp) operator;
            if (addOp instanceof AddOpPLUS) {
                return "+";
            }
            if (addOp instanceof AddOpMINUS) {
                return "-";
            }
        }
        else if (operator instanceof MulOp) {
            MulOp mulOp = (MulOp) operator;
            if (mulOp instanceof MulOpMUL) {
                return "*";
            }
            if (mulOp instanceof MulOpDIV) {
                return "/";
            }
            if (mulOp instanceof MulOpMOD) {
                return "%";
            }
        }
        throw new IllegalArgumentException("Unknown operator '" + operator.toString()
                + "' --- hint: argument must be instance of RelOp, AddOp or MulOp");
    }

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
        programObj = Tab.insert(Obj.Prog, programName.getIdent(), Tab.noType);
        Tab.openScope();
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
            report_error("Cannot initialize constant of type [" + toString(declaration.getType())
                    + "] with value of type [" + toString(literal.getType()) + "]", constDecl);
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

        if (condType == Tab.noType || leftType == Tab.noType || rightType == Tab.noType) {
            exprTernary.struct = Tab.noType;
        }
        else if (!(leftType.getKind() == rightType.getKind())) {
            report_error("Ternary operator expressions are not the same type: [" + toString(leftType) + "] and ["
                    + toString(rightType) + "]", exprTernary);
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
        String operator = toString(condFactRelOp.getRelOp());
        Struct rightType = condFactRelOp.getExpression1().struct;

        if (!leftType.compatibleWith(rightType)) {
            report_error("Types [" + toString(leftType) + "] and [" + toString(rightType)
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
        String operator = toString(expressionAddOpTerm.getAddOp());
        if (!expressionAddOpTerm.getTerm().struct.equals(Tab.intType)) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + toString(expressionAddOpTerm.getTerm().struct) + "]", expressionAddOpTerm);
            expressionAddOpTerm.struct = Tab.noType;
        }
        else if (!expressionAddOpTerm.getExpression().struct.equals(Tab.intType)) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + toString(expressionAddOpTerm.getExpression().struct) + "]", expressionAddOpTerm);
            expressionAddOpTerm.struct = Tab.noType;
        }
        else {
            expressionAddOpTerm.struct = Tab.intType;
        }
    }

    @Override
    public void visit(ExpressionMinusTerm expressionMinusTerm) {
        if (!expressionMinusTerm.getTerm().struct.equals(Tab.intType)) {
            report_error("Operator '-' cannot be used on operand of type ["
                    + toString(expressionMinusTerm.getTerm().struct) + "]", expressionMinusTerm);
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
        String operator = toString(termMulOpFactor.getMulOp());

        if (!termMulOpFactor.getFactor().struct.equals(Tab.intType)) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + toString(termMulOpFactor.getFactor().struct) + "]", termMulOpFactor);

            termMulOpFactor.struct = Tab.noType;
        }
        else if (!termMulOpFactor.getTerm().struct.equals(Tab.intType)) {
            report_error("Operator '" + operator + "' cannot be used on operand of type ["
                    + toString(termMulOpFactor.getTerm().struct) + "]", termMulOpFactor);

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
        if (!factorNewType.getExpr().struct.equals(Tab.intType)) {
            report_error("New array must be of type [int]", factorNewType);
            factorNewType.struct = Tab.noType;
        }
        else {
            factorNewType.struct = new Struct(Struct.Array, factorNewType.getType().struct);
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
                    return;
                }
            }

            report_error(
                    "Enum '" + object.getName() + "' does not have a field named '" + designatorField.getIdent() + "'",
                    designatorField);
            designatorField.obj = Tab.noObj;
        }

    }

    @Override
    public void visit(DesignatorArray designatorArray) {
        Obj object = designatorArray.getDesignator().obj;

        if (object == Tab.noObj) {
            designatorArray.obj = Tab.noObj;
        }
        else if (object.getType().getKind() != Struct.Array) {
            report_error("Name '" + object.getName() + "' is not an array", designatorArray);
            designatorArray.obj = Tab.noObj;
        }
        else if (!designatorArray.getExpr().struct.equals(Tab.intType)) {
            report_error("Array index is not of type [int]", designatorArray);
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
            designatorStatementAssign.obj = Tab.noObj;
        }
        else if (designator.getKind() != Obj.Var && designator.getKind() != Obj.Elem) {
            report_error("Designator '" + designator.getName() + "' is not a variable", designatorStatementAssign);
            designatorStatementAssign.obj = Tab.noObj;
        }
        // else if (exprType.equals(Tab.intType)) {
        // designatorStatementAssign.obj = designator;
        // }
        else if (!exprType.assignableTo(designator.getType())) {
            report_error("Cannot assign value of type [" + toString(exprType) + "] to variable of type ["
                    + toString(designator.getType()) + "]", designatorStatementAssign);
            designatorStatementAssign.obj = Tab.noObj;
        }
    }

}
