package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

    private Logger log = Logger.getLogger(getClass());
    private boolean errorDetected = false;

    private final Struct boolType = new Struct(Struct.Bool);
    private DeclarationList declarationList;

    private Obj programObj;
    private Obj boolObj;
    private Obj currentMethod;
    private boolean mainIncluded;

    private String printType(Struct type) {
        switch (type.getKind()) {
        case Struct.Int:
            return "int";
        case Struct.Char:
            return "char";
        case Struct.Bool:
            return "bool";
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
        if (!mainIncluded) {
            report_error("Main method is needed to run program", null);
        }
    }

    @Override
    public void visit(Type type) {
        declarationList = new DeclarationList();
        Obj typeObject = Tab.find(type.getName());

        if (typeObject == Tab.noObj) {
            report_error("Unknown type [" + type.getName() + "]", type);
            declarationList.setType(Tab.noType);
        }
        else if (typeObject.getKind() != Obj.Type) {
            report_error("Name [" + type.getName() + "] is not a type", type);
            declarationList.setType(Tab.noType);
        }
        else {
            declarationList.setType(typeObject.getType());
        }
    }

    @Override
    public void visit(ConstDeclList constDeclList) {
        for (Declaration declaration : declarationList.getDeclarationList()) {
            Obj object = Tab.find(declaration.getIdent());
            if (object != Tab.noObj) {
                report_error("Redefinition of constant '" + declaration.getIdent() + "'", constDeclList);
            }
            else {
                object = Tab.insert(Obj.Con, declaration.getIdent(), declaration.getRvalue().getType());
                object.setAdr(declaration.getRvalue().getValue());
            }
        }
        declarationList = null;
    }

    @Override
    public void visit(ConstDecl constDecl) {
        RValue rvalue = constDecl.getConst().rvalue;

        if (rvalue.getType().assignableTo(declarationList.getType())) {
            declarationList.append(new Declaration(constDecl.getIdent(), rvalue));
        }
        else {
            report_error("Cannot initialize constant of type [" + printType(declarationList.getType())
                    + "] with value of type [" + printType(rvalue.getType()) + "]", constDecl);
        }
    }

    @Override
    public void visit(ConstNumber constNumber) {
        constNumber.rvalue = new RValue(Tab.intType, constNumber.getValue());
    }

    @Override
    public void visit(ConstChar constChar) {
        constChar.rvalue = new RValue(Tab.charType, constChar.getValue());
    }

    @Override
    public void visit(ConstBool constBool) {
        constBool.rvalue = new RValue(boolType, constBool.getValue());
    }

    @Override
    public void visit(VarDeclList varDeclList) {
        for (Declaration declaration : declarationList.getDeclarationList()) {
            Obj object = null;

            if (currentMethod == null) {
                object = Tab.find(declaration.getIdent());
            }
            else {
                object = Tab.currentScope.findSymbol(declaration.getIdent());
            }

            if (object != null && object != Tab.noObj) {
                report_error("Redefinition of variable '" + declaration.getIdent() + "'", varDeclList);
            }
            else {
                object = Tab.insert(Obj.Var, declaration.getIdent(), declaration.getRvalue().getType());
                object.setAdr(declaration.getRvalue().getValue());
            }
        }
        declarationList = null;
    }

    @Override
    public void visit(VarDeclSingle varDeclSingle) {
        declarationList.append(new Declaration(varDeclSingle.getIdent(), new RValue(declarationList.getType(), 0)));
    }

    @Override
    public void visit(VarDeclArray varDeclArray) {
        declarationList.append(new Declaration(varDeclArray.getIdent(), new RValue(declarationList.getType(), 0)));
    }

    @Override
    public void visit(MethodName methodName) {
        if (methodName.getMethodName().equals("main") || methodName.getMethodName().equals("Main")) {
            mainIncluded = true;
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

}
