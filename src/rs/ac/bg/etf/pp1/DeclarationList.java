package rs.ac.bg.etf.pp1;

import java.util.ArrayList;

import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class DeclarationList {

    private Struct type;
    private ArrayList<Declaration> declarationList;

    public DeclarationList() {
        this.type = Tab.noType;
        declarationList = new ArrayList<>();
    }

    public Struct getType() {
        return type;
    }

    public ArrayList<Declaration> getDeclarationList() {
        return declarationList;
    }

    public DeclarationList setType(Struct type) {
        this.type = type;
        return this;
    }

    public DeclarationList append(Declaration declaration) {
        this.declarationList.add(declaration);
        return this;
    }

}
