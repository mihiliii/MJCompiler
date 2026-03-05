package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.HashMap;

import rs.ac.bg.etf.pp1.symboltable.SymTab;
import rs.etf.pp1.symboltable.concepts.*;
import rs.etf.pp1.symboltable.structure.SymbolDataStructure;

public class Declarations {

    private Obj currentTypeObj = SymTab.noObj;
    private HashMap<Obj, Obj> enumConstMap = new HashMap<>();

    public Obj getCurrentTypeObj() {
        return currentTypeObj;
    }

    public Declarations setCurrentTypeObj(Obj type) {
        this.currentTypeObj = type;
        return this;
    }

    public Declarations initVar(String ident) {
        SymTab.insert(Obj.Var, ident, this.currentTypeObj.getType());
        return this;
    }

    public Declarations initVarArray(String ident) {
        SymTab.insert(Obj.Var, ident, new Struct(Struct.Array, this.currentTypeObj.getType()));
        return this;
    }

    public Declarations initConst(String ident, Literal literal) {
        SymTab.insert(Obj.Con, ident, this.currentTypeObj.getType()).setAdr(literal.getValue());
        return this;
    }

    public Declarations initEnumConst(String ident, int value) {
        Obj enumConstObj = SymTab.insert(Obj.Con, ident, SymTab.intType);
        enumConstObj.setAdr(value);

        enumConstMap.put(enumConstObj, this.currentTypeObj);
        return this;
    }

    public Declarations initEnumConst(String ident) {
        int value;

        SymbolDataStructure locals = SymTab.currentScope().getLocals();
        if (locals != null) {
            ArrayList<Obj> symbols = new ArrayList<Obj>(locals.symbols());
            value = symbols.get(symbols.size() - 1).getAdr() + 1;
        }
        else {
            value = 0;
        }

        initEnumConst(ident, value);
        return this;
    }

    public Obj getEnumType(Obj enumConst) {
        return enumConstMap.get(enumConst);
    }

    public Obj getEnumConst(Obj enumType) {
        for (Obj enumConst : enumConstMap.keySet()) {
            if (enumConstMap.get(enumConst) == enumType) {
                return enumConst;
            }
        }
        return null;
    }

}
