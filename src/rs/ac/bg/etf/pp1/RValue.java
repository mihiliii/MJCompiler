package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class RValue {
    private Struct type;
    private int value;

    public RValue(Struct type, int value) {
        this.type = type;
        this.value = value;
    }

    public RValue() {
        this.type = Tab.noType;
        this.value = 0;
    }

    public Struct getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public RValue setType(Struct type) {
        this.type = type;
        return this;
    }

    public RValue setValue(int value) {
        this.value = value;
        return this;
    }

}
