package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;

public class CodeGenerator extends VisitorAdaptor {

    private int mainPc;

    public int getMainPc() {
        return mainPc;
    }

    @Override
    public void visit(MethodName methodName) {
        Code.put(Code.enter);
    }

}
