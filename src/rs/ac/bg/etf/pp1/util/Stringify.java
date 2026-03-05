package rs.ac.bg.etf.pp1.util;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.Struct;

public class Stringify {

    public static String toString(Struct type) {
        switch (type.getKind()) {
        case Struct.Int:
            return "int";
        case Struct.Char:
            return "char";
        case Struct.Bool:
            return "bool";
        case Struct.Array:
            return "array";
        case Struct.Enum:
            return "enum";
        case Struct.Class:
            return "class";
        case Struct.None:
            return "notype";
        default:
            throw new IllegalArgumentException("Unknown type '" + type.toString() + "'");
        }
    }

    public static String toString(RelOp operator) {
        if (operator instanceof RelOpEQ) {
            return "==";
        }
        if (operator instanceof RelOpNEQ) {
            return "!=";
        }
        if (operator instanceof RelOpLT) {
            return "<";
        }
        if (operator instanceof RelOpGT) {
            return ">";
        }
        if (operator instanceof RelOpLTE) {
            return "<=";
        }
        if (operator instanceof RelOpGTE) {
            return ">=";
        }
        throw new IllegalArgumentException("Unknown operator '" + operator.toString() + "'");
    }

    public static String toString(AddOp operator) {
        if (operator instanceof AddOpPLUS) {
            return "+";
        }
        if (operator instanceof AddOpMINUS) {
            return "-";
        }
        throw new IllegalArgumentException("Unknown operator '" + operator.toString() + "'");

    }

    public static String toString(MulOp operator) {
        if (operator instanceof MulOpMUL) {
            return "*";
        }
        if (operator instanceof MulOpDIV) {
            return "/";
        }
        if (operator instanceof MulOpMOD) {
            return "%";
        }
        throw new IllegalArgumentException("Unknown operator '" + operator.toString() + "'");
    }
}
