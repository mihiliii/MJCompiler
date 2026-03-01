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
            return "unknown";
        }
    }

    public static String toString(Object operator) {
        if (operator instanceof RelOp) {
            RelOp relOp = (RelOp) operator;
            if (relOp instanceof RelOpEQ) {
                return "==";
            }
            if (relOp instanceof RelOpNEQ) {
                return "!=";
            }
            if (relOp instanceof RelOpLT) {
                return "<";
            }
            if (relOp instanceof RelOpGT) {
                return ">";
            }
            if (relOp instanceof RelOpLTE) {
                return "<=";
            }
            if (relOp instanceof RelOpGTE) {
                return ">=";
            }
        }
        else if (operator instanceof AddOp) {
            AddOp addOp = (AddOp) operator;
            if (addOp instanceof AddOpPLUS) {
                return "+";
            }
            if (addOp instanceof AddOpMINUS) {
                return "-";
            }
        }
        else if (operator instanceof MulOp) {
            MulOp mulOp = (MulOp) operator;
            if (mulOp instanceof MulOpMUL) {
                return "*";
            }
            if (mulOp instanceof MulOpDIV) {
                return "/";
            }
            if (mulOp instanceof MulOpMOD) {
                return "%";
            }
        }
        throw new IllegalArgumentException("Unknown operator '" + operator.toString()
                + "' --- hint: argument must be instance of RelOp, AddOp or MulOp");
    }
}
