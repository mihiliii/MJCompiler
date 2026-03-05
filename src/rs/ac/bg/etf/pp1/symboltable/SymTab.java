package rs.ac.bg.etf.pp1.symboltable;

import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SymTab extends Tab {

    public static final Struct boolType = new Struct(Struct.Bool);

    public static void init() {
        Tab.init();
        currentScope().addToLocals(new Obj(Obj.Type, "bool", boolType));
    }

}
