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
    private boolean hasMainMethod;

    private String printType(Struct type) {
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
        default:
            return "unknown";
        }
    }

    SemanticAnalyzer() {
        // Since the symbol table doesnt have a boolean type obj it must be initialized
        // in the constructor of the semantic analyzer.
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
        programObj = Tab.insert(Obj.Prog, programName.getName(), Tab.noType);
        Tab.openScope();
    }

    @Override
    public void visit(Program program) {
        Tab.chainLocalSymbols(programObj);
        Tab.closeScope();
        programObj = null;

        if (hasMainMethod == false) {
            report_error("Main method is needed to run program", null);
        }
    }

    @Override
    public void visit(Type type) {
        Struct typeStruct = Tab.noType;
        Obj typeObject = Tab.find(type.getName());

        if (typeObject == Tab.noObj) {
            report_error("Unknown type [" + type.getName() + "]", type);
        }
        else if (typeObject.getKind() != Obj.Type) {
            report_error("Name [" + type.getName() + "] is not a type", type);
        }
        else {
            typeStruct = typeObject.getType();
        }

        declaration = new Declaration(typeStruct);
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
            report_error("Cannot initialize constant of type [" + printType(declaration.getType())
                    + "] with value of type [" + printType(literal.getType()) + "]", constDecl);
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
        Obj object = Tab.find(enumName.getName());

        if (currentMethod != null) {
            report_error("Enum '" + enumName.getName() + "' must be declared in global scope", enumName);
        }
        else if (object != Tab.noObj) {
            report_error("Redefinition of enum '" + enumName.getName() + "'", enumName);
        }
        else {
            declaration = new Declaration(new Struct(Struct.Enum));
            declaration.initializeEnum(enumName.getName());
        }
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
        else {
            designatorArray.obj = object;
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

}
