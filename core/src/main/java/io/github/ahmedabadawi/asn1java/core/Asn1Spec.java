package io.github.ahmedabadawi.asn1java.core;

import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.exception.Asn1SyntaxException;
import io.github.ahmedabadawi.asn1java.core.validation.Asn1SemanticValidator;
import org.antlr.v4.runtime.*;

public class Asn1Spec {

    public static ModuleNode parse(String source) {
        var lexer  = new ASN1Lexer(CharStreams.fromString(source));
        var tokens = new CommonTokenStream(lexer);
        var parser = new ASN1Parser(tokens);

        var throwingListener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                throw new Asn1SyntaxException(line, charPositionInLine, msg);
            }
        };
        lexer.removeErrorListeners();
        lexer.addErrorListener(throwingListener);
        parser.removeErrorListeners();
        parser.addErrorListener(throwingListener);

        var visitor = new Asn1ModuleVisitor();
        var module = (ModuleNode) visitor.visit(parser.moduleDefinition());
        new Asn1SemanticValidator().validate(module);
        return module;
    }
}
