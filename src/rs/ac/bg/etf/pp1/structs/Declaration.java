package rs.ac.bg.etf.pp1.structs;

import java.util.ArrayList;

import rs.ac.bg.etf.pp1.symboltable.SymTab;
import rs.etf.pp1.symboltable.concepts.*;
import rs.etf.pp1.symboltable.structure.SymbolDataStructure;

public class Declaration {

    private Obj typeObj = SymTab.noObj;

    public Obj getTypeObj() {
        return typeObj;
    }

    public Declaration setTypeObj(Obj type) {
        this.typeObj = type;
        return this;
    }

    public Obj initVar(String ident) {
        return SymTab.insert(Obj.Var, ident, this.typeObj.getType());
    }

    public Obj initVarArray(String ident) {
        return SymTab.insert(Obj.Var, ident, new Struct(Struct.Array, this.typeObj.getType()));
    }

    public Obj initConst(String ident, Literal literal) {
        Obj constObj = SymTab.insert(Obj.Con, ident, this.typeObj.getType());
        constObj.setAdr(literal.getValue());

        return constObj;
    }

    public Obj initEnumConst(String ident, int value) {
        Obj enumConstObj = SymTab.insert(Obj.Con, ident, SymTab.intType);
        enumConstObj.setAdr(value);

        return enumConstObj;
    }

    public Obj initEnumConst(String ident) {
        int value;

        SymbolDataStructure locals = SymTab.currentScope().getLocals();
        if (locals != null) {
            ArrayList<Obj> symbols = new ArrayList<Obj>(locals.symbols());
            value = symbols.get(symbols.size() - 1).getAdr() + 1;
        }
        else {
            value = 0;
        }

        return initEnumConst(ident, value);
    }

}
