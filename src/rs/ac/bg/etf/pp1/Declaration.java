package rs.ac.bg.etf.pp1;

// import java.util.ArrayList;

import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;
import rs.etf.pp1.symboltable.structure.SymbolDataStructure;

public class Declaration {

    private Struct type;

    public Declaration(Struct type) {
        this.type = type;
    }

    public Struct getType() {
        return type;
    }

    public boolean initializeVar(String ident) {
        if (this.type == Tab.noType) {
            return false;
        }
        Tab.insert(Obj.Var, ident, this.type).setAdr(Tab.currentScope().getnVars());
        return true;
    }

    public boolean initializeVarArray(String ident) {
        if (this.type == Tab.noType) {
            return false;
        }
        Tab.insert(Obj.Var, ident, new Struct(Struct.Array, this.type)).setAdr(Tab.currentScope().getnVars());
        return true;
    }

    public boolean initializeCon(String ident, Literal literal) {
        if (this.type == Tab.noType) {
            return false;
        }
        Tab.insert(Obj.Con, ident, literal.getType()).setAdr(literal.getValue());
        return true;
    }

    public boolean initializeEnum(String ident) {
        if (this.type == Tab.noType) {
            return false;
        }
        Tab.insert(Obj.Type, ident, type);
        return true;
    }

    public boolean initializeEnumConst(String ident, int value) {
        if (this.type == Tab.noType) {
            return false;
        }
        Obj enumConstObj = Tab.insert(Obj.Con, ident, Tab.intType);
        enumConstObj.setAdr(value);
        enumConstObj.setLevel(1);
        return true;
    }

    public boolean initializeEnumConst(String ident) {
        if (this.type == Tab.noType) {
            return false;
        }
        int value;
        SymbolDataStructure locals = Tab.currentScope().getLocals();
        if (locals != null) {
            Obj[] localsArray = locals.symbols().toArray(new Obj[locals.symbols().size()]);
            Obj lastObj = localsArray[localsArray.length - 1];
            value = lastObj.getAdr() + 1;
        }
        else {
            value = 0;
        }
        Tab.insert(Obj.Con, ident, Tab.intType).setAdr(value);
        return true;
    }

}
