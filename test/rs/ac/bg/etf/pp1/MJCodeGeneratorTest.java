package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.symboltable.DumpSymTabVisitor;
import rs.ac.bg.etf.pp1.symboltable.SymTab;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;

public class MJCodeGeneratorTest {

    static {
        DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
        Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
    }

    public static void main(String[] args) throws Exception {

        Logger log = Logger.getLogger(MJCodeGeneratorTest.class);

        Reader bufferedReader = null;
        try {
            File sourceCode = new File("test/semantic_errors.mj");
            log.info("Compiling source file: " + sourceCode.getAbsolutePath());

            bufferedReader = new BufferedReader(new FileReader(sourceCode));
            Yylex lexer = new Yylex(bufferedReader);

            MJParser parser = new MJParser(lexer);
            Symbol parseTreeRoot = parser.parse();

            Program program = (Program) (parseTreeRoot.value);

            log.info(program.toString(""));
            log.info("===================================");

            // Inicijalizacija tabele simbola
            SymTab.init();

            // Semanticka analiza
            SemanticAnalyzer semAnalyzer = new SemanticAnalyzer();
            program.traverseBottomUp(semAnalyzer);

            DumpSymTabVisitor dumpVisitor = new DumpSymTabVisitor();

            SymTab.dump(dumpVisitor);

            if (!parser.is_error_detected() && semAnalyzer.passed()) {
                log.info("Parsiranje uspesno zavrseno!");

                File objFile = new File("test/program.obj");
                if (objFile.exists()) {
                    objFile.delete();
                }

                CodeGenerator codeGenerator = new CodeGenerator();
                program.traverseBottomUp(codeGenerator);

                Code.dataSize = semAnalyzer.getNumLocalVars();
                Code.mainPc = codeGenerator.getMainPc();
                Code.write(new FileOutputStream(objFile));

                log.info("Generisranje koda uspesno zavrseno!");
            }
            else {
                log.error("Parsiranje NIJE uspesno zavrseno!");
            }

        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                }
                catch (IOException e1) {
                    log.error(e1.getMessage(), e1);
                }
            }
        }

    }

}
