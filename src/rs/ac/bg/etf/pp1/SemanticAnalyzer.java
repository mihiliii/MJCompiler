package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

    private Logger log = Logger.getLogger(getClass());
    private boolean errorDetected = false;
    private Obj currentProgram;
    private Struct currentType;
    private Struct boolStruct;
    private Const constant;
    private Obj currentMethod;
    private boolean mainHappened;

    SemanticAnalyzer() {
        boolStruct = Tab.find("bool").getType();
    }

    class Const {
        public int value;
        public Struct type;

        public Const() {
        }

        public Const(int value, Struct type) {
            this.value = value;
            this.type = type;
        }
    }

    public void report_error(String message, SyntaxNode info) {
        errorDetected = true;
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0) {
            msg.append(" na liniji ").append(line);
        }
        log.error(msg.toString());
    }

    public void report_info(String message, SyntaxNode info) {
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0) {
            msg.append(" na liniji ").append(line);
        }
        log.info(msg.toString());
    }

    public boolean passed() {
        return !errorDetected;
    }

    /* SEM PASS CODE */

    @Override
    public void visit(ProgramName programName) {
        currentProgram = Tab.insert(Obj.Prog, programName.getName(), Tab.noType);
        Tab.openScope();
    }

    @Override
    public void visit(Program program) {
        Tab.chainLocalSymbols(currentProgram);
        Tab.closeScope();
        currentProgram = null;
        if (!mainHappened) {
            report_error("Ne postoji main metoda!", program);
        }
    }

    @Override
    public void visit(Type type) {
        Obj typeObj = Tab.find(type.getType());

        if (typeObj == Tab.noObj) {
            report_error("Ne postoji tip: " + type.getType(), type);
            currentType = Tab.noType;
        } else if (typeObj.getKind() != Obj.Type) {
            report_error("Pogresan tip podatka: " + type.getType(), type);
        }

        currentType = typeObj.getType();
    }

    @Override
    public void visit(ConstDecl constDecl) {
        Obj constObj = Tab.find(constDecl.getName());

        if (constObj != Tab.noObj) {
            report_error("Dvostruka definicija konstante: " + constDecl.getName(), constDecl);
        } else if (constant.type.assignableTo(currentType)) {
            constObj = Tab.insert(Obj.Con, constDecl.getName(), currentType);
            constObj.setAdr(constant.value);
        } else {
            report_error("Neadekvatna dodela konstanti: " + constDecl.getName(), constDecl);
        }

    }

    @Override
    public void visit(ConstNumber constNumber) {
        constant = new Const(constNumber.getValue(), Tab.intType);
    }

    @Override
    public void visit(ConstChar constChar) {
        constant = new Const(constChar.getValue(), Tab.charType);
    }

    @Override
    public void visit(ConstBool constBool) {
        constant = new Const(constBool.getValue(), boolStruct);
    }

    @Override
    public void visit(VarDeclNonArray varDecl) {
        Obj varObj = null;

        if (currentMethod == null) {
            varObj = Tab.find(varDecl.getName());
        } else {
            varObj = Tab.currentScope.findSymbol(varDecl.getName());
        }

        if (varObj == null || varObj == Tab.noObj) {
            varObj = Tab.insert(Obj.Var, varDecl.getName(), currentType);
            return;
        }

        report_error("Dvostruka definicija promenljive: " + varDecl.getName(), varDecl);
    }

    @Override
    public void visit(VarDeclArray varDeclArray) {
        Obj varObj = null;

        if (currentMethod == null) {
            varObj = Tab.find(varDeclArray.getName());
        } else {
            varObj = Tab.currentScope.findSymbol(varDeclArray.getName());
        }

        if (varObj == null || varObj == Tab.noObj) {
            varObj = Tab.insert(Obj.Var, varDeclArray.getName(), currentType);
            return;
        }

        report_error("Dvostruka definicija promenljive: " + varDeclArray.getName(), varDeclArray);
    }

    @Override
    public void visit(MethodName methodName) {
        if (methodName.getName().toLowerCase().equals("main")) {
            mainHappened = true;
        }
        currentMethod = Tab.insert(Obj.Meth, methodName.getName(), currentType);
        Tab.openScope();
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        Tab.chainLocalSymbols(currentMethod);
        Tab.closeScope();
        currentMethod = null;
    }

}
