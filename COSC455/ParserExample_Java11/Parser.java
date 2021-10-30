
package COSC455.ParserExample_Java11;

import java.util.*;
import java.util.logging.Logger;

import javax.management.relation.Relation;
import javax.print.attribute.standard.MediaSize.Other;

import static COSC455.ParserExample_Java11.TOKEN.*;

class Parser {

    // The lexer which will provide the tokens
    private final LexicalAnalyzer lexer;

    // the actual "code generator"
    private final CodeGenerator codeGenerator;

    /**
     * The constructor initializes the terminal literals in their vectors.
     *
     * @param lexer The Lexer Object
     */
    public Parser(LexicalAnalyzer lexer, CodeGenerator codeGenerator) {
        this.lexer = lexer;
        this.codeGenerator = codeGenerator;
    }

    
    public void analyze() {
        try {
            // Generate header for our output
            var startNode = codeGenerator.writeHeader("PARSE TREE");

            // Start the actual parsing.
            program(startNode);

            // generate footer for our output
            codeGenerator.writeFooter();

            // For graphically displaying the output.
            // CodeGenerator.openWebGraphViz();
        } catch (ParseException ex) {
            final String msg = String.format("%s\n", ex.getMessage());
            Logger.getAnonymousLogger().severe(msg);
        }
    }

    void program(ParseNode fromNode) throws ParseException {
        final var nodeName = codeGenerator.addNonTerminalToTree(fromNode);

        stmt_list(nodeName);
    }

    void stmt_list(ParseNode fromNode) throws ParseException {
        final var nodeName = codeGenerator.addNonTerminalToTree(fromNode);
        // OP("("), CL(")"), ADD_OP("+", "-"), READ("read"), WRITE("write"), MULT_OP("*", "/"), IF("if"), THEN("then"),
    // ENDIF("fi"), ELSE("else"), WHILE("while"), ENDWHILE("do"), DONE("od"), ASGN(":="),
    // RELATION("<", ">", "<=", ">=", "=", "!="),
        if (lexer.isCurrentToken(OTHER) 
            || lexer.isCurrentToken(READ)
            || lexer.isCurrentToken(WRITE) 
            || lexer.isCurrentToken(IF)
            || lexer.isCurrentToken(THEN)
            || lexer.isCurrentToken(WHILE)||lexer.isCurrentToken(OP)||lexer.isCurrentToken(OP)){

            stmt(nodeName);
            stmt_list(nodeName);
        }


    }
    void Condition(ParseNode fromNode){
        final var treeNode = codeGenerator.addNonTerminalToTree(fromNode);

        expr(treeNode);
        RELATION(treeNode);
        expr(treeNode);


    }
    void stmt(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree(fromNode);
        //System.out.print("******" + lexer.getCurrentLexeme());

        if (lexer.isCurrentToken(TOKEN.IF)) {
            IF(treeNode);
            Condition(treeNode);
            THEN(treeNode);
            stmt_list(treeNode); 
            ENDIF(treeNode);

        } else if (lexer.isCurrentToken(TOKEN.OTHER)) {
            OTHER(treeNode);
            ASGN(treeNode);
            expr(treeNode);
        } else if (lexer.isCurrentToken(TOKEN.READ)) {
            READ(treeNode);
            OTHER(treeNode);
        } else if (lexer.isCurrentToken(TOKEN.WRITE)) {
            WRITE(treeNode);
            expr(treeNode);
        }else if (lexer.isCurrentToken(TOKEN.WHILE)) {
            WHILE(treeNode);
            Condition(treeNode);
            DO(treeNode);
            stmt_list(treeNode);
            ENDWHILE(treeNode);

        }else if (lexer.isCurrentToken(TOKEN.ENDWHILE)) {
            ENDWHILE(treeNode);
        } else {
            raiseException("Statement", fromNode);
        }

    }

    void expr(ParseNode fromNode) throws ParseException {
        final var nodeName = codeGenerator.addNonTerminalToTree(fromNode);

        term(nodeName);
        term_tail(nodeName);
    }

    void term_tail(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree(fromNode);
        if (lexer.isCurrentToken(TOKEN.ADD_OP)) {
            ADD_OP(treeNode);
            term(treeNode);
            term_tail(treeNode);
        } else {
            EMPTY(treeNode);
        }
    }

    void term(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree(fromNode);

        factor(treeNode);
        factor_tail(treeNode);
    }

    void factor_tail(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree(fromNode);
        if (lexer.isCurrentToken(TOKEN.MULT_OP)) {
            MULT_OP(treeNode);
            factor(treeNode);
            factor_tail(treeNode);
        } else {

            EMPTY(treeNode);

        }
    }

    private void EMPTY(ParseNode fromNode) throws ParseException {
        codeGenerator.addEmptyToTree(fromNode);
    }

    void factor(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree(fromNode);

        if (lexer.isCurrentToken(TOKEN.OP)) {
            OP(treeNode);
            expr(treeNode);
            CL(treeNode);
        } else if (lexer.isCurrentToken(TOKEN.OTHER)) {
            OTHER(treeNode);
        } else if (lexer.isCurrentToken(TOKEN.NUMBER)) {
            NUMBER(treeNode);
        }
    }

    void ADD_OP(ParseNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(TOKEN.ENDIF)) {
            addTerminalAndAdvanceToken();
        }
        if (!lexer.isCurrentToken(TOKEN.ADD_OP)) {
            raiseException("ADD_OP expected", fromNode);
        }

        else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void MULT_OP(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.MULT_OP)) {
            raiseException("MULT_OP expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void OP(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.OP)) {
            raiseException("OP expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void CL(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.CL)) {
            raiseException("CL expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void READ(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.READ)) {
            raiseException("READ expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void WRITE(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.WRITE)) {
            raiseException("WRITE expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void IF(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.IF)) {
            raiseException("IF expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void THEN(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.THEN)) {
            raiseException("THEN expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void ENDIF(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.ENDIF)) {
            raiseException("ENDIF expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
            
        }
    }
    void ENDWHILE(ParseNode fromNode) throws ParseException{
        if (!lexer.isCurrentToken(TOKEN.ENDWHILE)) {
            addTerminalAndAdvanceToken(fromNode); 
        } else {
            addTerminalAndAdvanceToken(fromNode); 
        }
    }

    void ELSE(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.ELSE)) {
            raiseException("ELSE expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void WHILE(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.WHILE)) {
            raiseException("WHILE expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void DO(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.ENDWHILE)) {
            raiseException("DO expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void DONE(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.DONE)) {
            raiseException("DONE expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void ASGN(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.ASGN)) {
            raiseException("ASGN expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void RELATION(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.RELATION)) {
            raiseException("RELATION expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void OTHER(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.OTHER)) {
            raiseException("OTHER expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    void NUMBER(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.NUMBER)) {
            raiseException("NUMBER expected", fromNode);
        } else {
            addTerminalAndAdvanceToken(fromNode);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Terminal:
    // Test it's type and continue if we really have a terminal node, syntax error
    //////////////////////////////////////////////////////////////////////////// if
    //////////////////////////////////////////////////////////////////////////// fails.
    void addTerminalAndAdvanceToken(ParseNode fromNode) throws ParseException {
        final var currentTerminal = lexer.getCurrentToken();
        final var terminalNode = codeGenerator.addNonTerminalToTree(fromNode, String.format("<%s>", currentTerminal));

        codeGenerator.addTerminalToTree(terminalNode, lexer.getCurrentLexeme());
        lexer.advanceToken();
    }

    void addTerminalAndAdvanceToken() {
        lexer.advanceToken();
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    // The code below this point is just a bunch of "helper functions" to keep
    // the parser code (above) a bit cleaner.
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    //
    //
    // Handle all of the errors in one place for cleaner parser code.
    private void raiseException(String expected, ParseNode fromNode) throws ParseException {
        final var template = "SYNTAX ERROR: '%s' was expected but '%s' was found.";
        final var err = String.format(template, expected, lexer.getCurrentLexeme());
        codeGenerator.syntaxError(err, fromNode);
    }

    /**
     * An exception to be raised if parsing fails due to a "syntax error" in the
     * input file.
     */
    static class ParseException extends RuntimeException {

        public ParseException(String errMsg) {
            super(errMsg);
        }
    }

}

/**
 * All Of the Tokens/Terminals Used by the parser. The purpose of the enum type
 * here is eliminate the need for direct string comparisons which is generally
 * slow, as being difficult to maintain. (We want Java's "static type checking"
 * to do as much work for us as it can!)
 *
 * !!!!! IMPORTANT !!!!
 * -----------------------------------------------------------------------------
 * IN MOST REAL "PROGRAMMING LANGUAGE" CASES, THERE WILL BE ONLY ONE LEXEME PER
 * TOKEN. !!!
 * -----------------------------------------------------------------------------
 * !!!!! IMPORTANT !!!!
 *
 * The fact that several lexemes exist per token in this example is because this
 * is to parse simple English sentences where most of the token types have many
 * words (lexemes) that could fit. This is generally NOT the case in most
 * programming languages!!!
 */
enum TOKEN {

    OP("("), CL(")"), ADD_OP("+", "-"), READ("read"), WRITE("write"), MULT_OP("*", "/"), IF("if"), THEN("then"),
    ENDIF("fi"), ELSE("else"), WHILE("while"), ENDWHILE("do"), DONE("od"), ASGN(":="),
    RELATION("<", ">", "<=", ">=", "=", "!="),

    
    // THESE ARE NOT USED IN THE GRAMMAR
    EOF, // End of file
    OTHER, // Could be "ID" in a "real programming langauge"
    NUMBER; // A sequence of digits.

    /**
     * A list of all lexemes for each token.
     */
    private final List<String> lexemeList;

    private TOKEN(String... tokenStrings) {
        lexemeList = new ArrayList<>(tokenStrings.length);
        lexemeList.addAll(Arrays.asList(tokenStrings));
    }

    /**
     * Get a TOKEN object from the Lexeme.
     *
     * @param string
     * @return
     */
    public static TOKEN fromLexeme(final String string) {
        // Just to be safe...
        var lexeme = string.trim();

        // An empty string should mean no more tokens to process.
        if (lexeme.isEmpty()) {
            return EOF;
        }

        // digits only (doesn't handle "-", "+", ".", etc., only digits)
        if (lexeme.matches("\\d+")) {
            return NUMBER;
        }

        // Search through ALL lexemes looking for a match with early bailout.
        for (var t : TOKEN.values()) {
            if (t.lexemeList.contains(lexeme)) {
                // early bailout.
                return t;
            }
        }

        // NOTE: Other could represent a number, for example.
        return OTHER;
    }
}
