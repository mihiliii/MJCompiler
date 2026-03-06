package rs.ac.bg.etf.pp1.structs;

import rs.etf.pp1.symboltable.concepts.*;

public class Literal {

    private Struct type;
    private int value;

    public Literal(Struct type, int value) {
        this.type = type;
        this.value = value;
    }

    public Struct getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public Literal setType(Struct type) {
        this.type = type;
        return this;
    }

    public Literal setValue(int value) {
        this.value = value;
        return this;
    }

}
