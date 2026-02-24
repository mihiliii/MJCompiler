package rs.ac.bg.etf.pp1;

// import java.util.ArrayList;

import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class Declaration {

    private Struct type;
    private Obj lastInserted;

    public Declaration() {
        this.type = Tab.noType;
        this.lastInserted = null;
    }

    public Declaration(Struct type) {
        this.type = type;
        this.lastInserted = null;
    }

    public Struct getType() {
        return type;
    }

    public Obj getLastInsertedObj() {
        return lastInserted;
    }

    public Declaration setType(Struct type) {
        this.type = type;
        return this;
    }

    public boolean initializeVar(String ident) {
        if (this.type == Tab.noType) {
            return false;
        }
        lastInserted = Tab.insert(Obj.Var, ident, this.type);
        lastInserted.setAdr(Tab.currentScope.getnVars());
        return true;
    }

    public boolean initializeVarArray(String ident) {
        if (this.type == Tab.noType) {
            return false;
        }
        lastInserted = Tab.insert(Obj.Var, ident, new Struct(Struct.Array, this.type));
        lastInserted.setAdr(Tab.currentScope.getnVars());
        return true;
    }

    public boolean initializeCon(String ident, Literal literal) {
        if (this.type == Tab.noType) {
            return false;
        }
        lastInserted = Tab.insert(Obj.Con, ident, literal.getType());
        lastInserted.setAdr(literal.getValue());
        return true;
    }

    public boolean initializeEnum(String ident) {
        if (this.type == Tab.noType) {
            return false;
        }
        Tab.insert(Obj.Type, ident, type);
        Tab.openScope();
        return true;
    }

    public boolean initializeEnumConst(String ident, int value) {
        if (this.type == Tab.noType || lastInserted == null) {
            return false;
        }
        lastInserted = Tab.insert(Obj.Con, ident, Tab.intType);
        lastInserted.setAdr(value);
        lastInserted.setLevel(1);
        return true;
    }

    public boolean initializeEnumConst(String ident) {
        if (this.type == Tab.noType || lastInserted == null) {
            return false;
        }
        int value = (lastInserted.getKind() == Obj.Con) ? lastInserted.getAdr() + 1 : 0;
        lastInserted = Tab.insert(Obj.Con, ident, Tab.intType);
        lastInserted.setAdr(value);
        return true;
    }

}
