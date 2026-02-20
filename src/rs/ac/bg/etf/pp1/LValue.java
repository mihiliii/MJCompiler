package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class LValue {
    private String name;
    private Struct type;

    public LValue(String name, Struct type) {
        this.name = name;
        this.type = type;
    }

    public LValue() {
        this.name = "";
        this.type = Tab.noType;
    }

    public String getName() {
        return name;
    }

    public Struct getType() {
        return type;
    }

    public LValue setName(String name) {
        this.name = name;
        return this;
    }

    public LValue setType(Struct type) {
        this.type = type;
        return this;
    }

    public String getTypeName() {
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

}
