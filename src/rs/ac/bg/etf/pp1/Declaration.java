package rs.ac.bg.etf.pp1;

public class Declaration {
    private String ident;
    private RValue rvalue;

    public Declaration(String ident, RValue rvalue) {
        this.ident = ident;
        this.rvalue = rvalue;
    }

    public String getIdent() {
        return ident;
    }

    public RValue getRvalue() {
        return rvalue;
    }

    public Declaration setIdent(String ident) {
        this.ident = ident;
        return this;
    }

    public Declaration setRvalue(RValue rvalue) {
        this.rvalue = rvalue;
        return this;
    }
}
