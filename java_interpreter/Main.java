import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.FileInputStream;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        String inputFile = args.length > 0 ? args[0] : "test1.pas";
        InputStream is = new FileInputStream(inputFile);

        delphiLexer lexer = new delphiLexer(CharStreams.fromStream(is));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        delphiParser parser = new delphiParser(tokens);

        ParseTree tree = parser.program(); // start
        Interpreter interpreter = new Interpreter();
        interpreter.visit(tree);
    }
}
