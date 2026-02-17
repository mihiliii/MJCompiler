package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class MJSemanticTest {

    static {
        DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
        Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
    }

    public static void main(String[] args) throws Exception {

        Logger log = Logger.getLogger(MJSemanticTest.class);

        Reader bufferedReader = null;
        try {
            File sourceCode = new File("test/program1.mj");
            log.info("Compiling source file: " + sourceCode.getAbsolutePath());

            bufferedReader = new BufferedReader(new FileReader(sourceCode));
            Yylex lexer = new Yylex(bufferedReader);

            MJParser parser = new MJParser(lexer);
            Symbol parseTreeRoot = parser.parse();

            Program program = (Program) (parseTreeRoot.value);

            log.info(program.toString(""));
            log.info("===================================");

            // Inicijalizacija tabele simbola
            Tab.init();

            Struct boolType = new Struct(Struct.Bool);
            Obj boolObj = Tab.insert(Obj.Type, "bool", boolType);
            boolObj.setAdr(-1);
            boolObj.setLevel(-1);

            // Semanticka analiza
            SemanticAnalyzer semAnalyzer = new SemanticAnalyzer();
            program.traverseBottomUp(semAnalyzer);

            Tab.dump();

            // log.info(!parser.is_error_detected());
            // log.info(semAnalyzer.passed());

            if (!parser.is_error_detected() && semAnalyzer.passed()) {
                log.info("Parsiranje uspesno zavrseno!");
            } else {
                log.error("Parsiranje NIJE uspesno zavrseno!");
            }

        } finally {
            if (bufferedReader != null)
                try {
                    bufferedReader.close();
                } catch (IOException e1) {
                    log.error(e1.getMessage(), e1);
                }
        }

    }

}
