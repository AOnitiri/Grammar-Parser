
package COSC455.ParserExample_Java11;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;

import java.net.URL;
import java.net.URLEncoder;


/**
 * COSC 455 Programming Languages: Implementation and Design.
 *
 * DESIGN NOTE: It's generally bad to have a bunch of "top level classes" in one
 * giant file. However, this was done here only to keep the example down to only
 * files... One for the parser and one for everything else!
 *
 * This syntax analyzer implements a top-down, left-to-right, recursive-descent
 * parser based on the production rules for a simple English language provided
 * by Weber in "Modern Programming Languages".
 */
public class MAIN_Compiler {

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!! toggle to display grapviz prompt. !!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    static final boolean PROMPT_FOR_GRAPHVIZ = true;

    public static void main(String[] args) throws IOException {
        // Check for an input file argument
        if (args.length == 0) {
            final File file = new File("COSC455/ParserExample_Java11/input.txt");
            System.out.println(file.getCanonicalPath());
            if (file.exists()) {
                String compiledCode = compile(file);

                // Display the graphviz test page if desired.
                if (PROMPT_FOR_GRAPHVIZ) {
                    try {
                        CodeGenerator.openWebGraphViz(compiledCode);
                    } catch (UnsupportedEncodingException ex) {
                        final String msg = "Could not create a valid URL String!!!";
                        Logger.getLogger("MAIN_Compiler").log(Level.SEVERE, msg, ex);
                    }
                }
            } else {
                System.err.printf("Input file not found: %s%n", file.toPath());
            }

        } else {
            System.err.println("Must Provide an input filename!!");
        }
    }

    /**
     * Start the lexing/parsing/compiling process.
     *
     * @param inputFile the File to read for input.
     */
    static String compile(File inputFile) {
        final CodeGenerator codeGenerator = new CodeGenerator();
        final LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer(inputFile);

        // Compile the program from the input supplied by the lexical analyzer.
        final Parser parser = new Parser(lexicalAnalyzer, codeGenerator);
        parser.analyze();

        return codeGenerator.generatedCodeBuffer.toString();
    }

}

/**
 * *****************************************************************************
 * This is a *FAKE* (Overly simplified) Lexical Analyzer...
 *
 * NOTE: This DOES NOT "lex" the input in the traditional manner on a DFA based
 * "state machine" or even host language supported regular expressions (which
 * are compiled into "state machine" anyhow in most implementations.)
 *
 * Instead of using "state transitions", this is merely a quick hack to create a
 * something that BEHAVES like a traditional lexer in it's FUNCTIONALITY, but it
 * ONLY knows how to separate (tokenize) lexemes delimited by SPACES. A Real
 * Lexer would tokenize based upon far more sophisticated lexical rules.
 *
 * AGAIN: ALL TOKENS MUST BE WHITESPACE DELIMITED.
 */
class LexicalAnalyzer {

    // TOKENIZED input.
    private Queue<TokenString> tokenList;

    // Simple Wrapper around current token.
    boolean isCurrentToken(TOKEN token) {
        return token == getCurrentToken();
    }

    /**
     * Construct a lexer over an input string.
     *
     * @param inputString
     */
    LexicalAnalyzer(String inputString) {
        tokenize(inputString);
    }

    /**
     * Construct a Lexer over the contents of a file. Filters out lines starting
     * with a '#' Symbol. Removes EOL markers; otherwise, our grammar would have
     * to deal with them).
     *
     * @param inputFile
     */
    LexicalAnalyzer(File inputFile) {
        try {
            Path filePath = inputFile.toPath();

            tokenize(Files.lines(filePath) // read lines
                    .map(String::trim) // map to trimmed strings
                    .filter(x -> !x.startsWith("#")) // filter out lines starting with #
                    .collect(Collectors.joining(" "))); // join lines together with spaces in between. 
        } catch (IOException ex) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Error Reading File: {0}", ex);
        }
    }

    /*
     * Convert the line to a series of tokens.
     */
    private void tokenize(final String line) {
        // Using Java 8's "Function Streams"
        this.tokenList = Arrays
                .stream(line.trim().split("\\s+")) // split string on spaces
                .map(TokenString::new) // map to a new Token
                .collect(Collectors.toCollection(LinkedList::new)); // collect into a new list
    }

    /**
     * Get just the lexeme part the head of the token list.
     *
     * @return the Lexeme as an Optional string since an empty list has no
     * tokens.
     */
    public String getCurrentLexeme() {
        return (this.tokenList.isEmpty() || getCurrentToken() == TOKEN.EOF)
                ? "EOF"
                : this.tokenList.peek().lexeme;
    }

    /*
     * get just the token
     */
    public TOKEN getCurrentToken() {
        return this.tokenList.isEmpty()
                ? TOKEN.EOF
                : this.tokenList.peek().token;
    }

    /*
     * Advance to next token, making it current.
     */
    public void advanceToken() {
        if (!this.tokenList.isEmpty()) {
            this.tokenList.remove();
        }
    }

    @Override
    public String toString() {
        return this.tokenList.toString();
    }

    /**
     * Nested class: a "Pair Tuple/Struct" for the token type and original
     * string.
     *
     */
    private class TokenString {

        private final String lexeme;
        private final TOKEN token;

        TokenString(String lexeme) {
            this.lexeme = lexeme;
            this.token = TOKEN.fromLexeme(lexeme);
        }

        @Override
        public String toString() {
            var msg = String.format("{lexeme=%s, token=%s}", lexeme, token);
            return msg;
        }
    }
}

/**
 * *****************************************************************************
 * A "3-Tuple" for the node name and id number.
 * 
 * NOTE: In JAVA 14+, "Java Records" will save us from this kind of thing.
 */
class ParseNode {
    private final String nodeName;
    private final Integer nodeId;
    private static Integer currentNodeID = 0;

    ParseNode(String nodeName) {
        this.nodeName = nodeName;
        this.nodeId = currentNodeID++;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return String.format("%s-%s", this.getNodeName(), this.getNodeId());
    }
}

/**
 * *****************************************************************************
 * This is a ***SIMULATION*** of a "code generator" that simply generates
 * GraphViz output. Technically, this would represent be the "Intermediate Code
 * Generation" step.
 *
 * Also, Instead of building an entire tree in memory followed by a traversal
 * the tree at the end, here were are just adding "code" as we go.
 *
 * (This simulates a single-pass compiler; keep in mind that most modern
 * compilers work in several passes... eg. scan for all top level identifiers,
 * build sub-trees for each class/method/etc., generate an internal intermediate
 * code representation, and so on).
 *
 * DESIGN NOTE: From an OOP design perspective, creating instances of "utility
 * classes" (classes with no internal state) is generally bad. However, in a
 * more elaborate example, the code generator would most certainly maintain some
 * internal state information. (Memory address offsets, etc.)
 */
class CodeGenerator {

    // Buffer for generated code
    StringBuffer generatedCodeBuffer;

    // Constructor
    CodeGenerator() {
        this.generatedCodeBuffer = new StringBuffer();
    }

    // Write generated code to both the screen AND the buffer.  
    private void outputGeneratedCode(String msg) {
        System.out.print(msg);
        this.generatedCodeBuffer.append(msg);
    }

    // Show the terminals as ovals...
    public void addTerminalToTree(ParseNode fromNode, String lexeme) {
        var node = new ParseNode(lexeme);
        var msg = String.format("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=oval]};%n", fromNode, node, lexeme);
        outputGeneratedCode(msg);
    }

    /**
     * Add an "inner node" to the parse tree.
     *
     * @param newNodeName the name of the node to insert into the tree
     * @param parentNode the parent of the node being added to the tree
     * @return the newly added node as ParseNode object.
     */
    public ParseNode addNonTerminalToTree(String newNodeName, ParseNode parentNode) {
        final var toNode = buildNode(newNodeName);
        addNonTerminalToTree(parentNode, toNode);
        return toNode;
    }

    /**
     * Add an "inner node" to the parse tree.
     *
     * The following code employs a bit of a trick to automatically build the
     * calling method name. The "getStackTrace()" method returns information
     * about the entire active stack. Element[0] is the actual "getStackTrace()"
     * method (it doesn't eliminate itself from the array), element[1] is THIS
     * method (since we called "getStackTrace()") and element[2] is the method
     * that called us, etc.
     *
     * @param parentNode the parent of the node being added to the tree
     * @return the newly added node as ParseNode object.
     */
    public ParseNode addNonTerminalToTree(ParseNode parentNode) {
        // This uses a Java "Trick" to return the name of the function that
        // called this method.
        final String fromMethodName
                = Thread
                        .currentThread()
                        .getStackTrace()[2] // See note in method comments.
                        .getMethodName()
                        .toUpperCase();

        final var toNode = buildNode("<" + fromMethodName + ">");

        addNonTerminalToTree(parentNode, toNode);
        return toNode;
    }

    // Show the non-terminals as boxes...
    public ParseNode addNonTerminalToTree(ParseNode fromNode, ParseNode toNode) {
        var msg = String.format("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=rect]};%n", fromNode, toNode, toNode.getNodeName());

        outputGeneratedCode(msg);
        return toNode;
    }

    public ParseNode addNonTerminalToTree(ParseNode fromNode, String toNodeString) {
        final var toNode = buildNode(toNodeString);
        return addNonTerminalToTree(fromNode, toNode);
    }

    // Show the terminals as ovals...
    public void addEmptyToTree(ParseNode fromNode) {
        var node = new ParseNode("ENPTY");
        var msg = String.format("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=none]};%n", fromNode, node, "&epsilon;");

        outputGeneratedCode(msg);
    }
    public void addNothingToTree(ParseNode fromNode) {
        //var node = new ParseNode("ENPTY");
        var msg = String.format("\t\"sa\" -> {};");

        outputGeneratedCode(msg);
    }

    // Call this if a syntax error occurs...
    public void syntaxError(String err, ParseNode fromNode) throws Parser.ParseException {
        var msg = String.format("\t\"%s\" -> {\"%s\"};%n}%n", fromNode, err);

        outputGeneratedCode(msg);
        throw new Parser.ParseException(err);
    }

    // Build a node name so it can be later "deconstructed" for the output.
    private ParseNode buildNode(String name) {
        return new ParseNode(name);
    }

    // "Real" executable code generally has a header.  See:
    // https://en.wikipedia.org/wiki/Executable_and_Linkable_Format
    // (There are some good diagrams at the link)
    public void writeHeader(ParseNode node) {
        // The header for the "compiled" output
        var msg = String.format("digraph ParseTree {\n"
                + "\t\"%s\" [label=\"%s\", shape=diamond];\n",
                node, node.getNodeName());

        outputGeneratedCode(msg);
    }

    public ParseNode writeHeader(String nodeString) {
        // The header for the "compiled" output
        final var headerNode = buildNode(nodeString);
        writeHeader(headerNode);
        return headerNode;
    }

    // Our output requires a footer as well.
    public void writeFooter() {
        var msg = String.format("}\n");

        outputGeneratedCode(msg);
    }

    /**
     * To open a browser window...
     *
     * FEEL FREE TO IGNORE THIS!!! It's just for opening the default browser, if
     * desired.
     */
    static void openWebGraphViz(String graph) throws UnsupportedEncodingException {
        // final var WEBGRAPHVIZ_HOME = "http://www.webgraphviz.com/";
        // final var WEBGRAPHVIZ_HOME = http://viz-js.com/
        final var WEBGRAPHVIZ_HOME = "https://dreampuf.github.io/GraphvizOnline/";

        final var MSG
                = "To visualize the output you may, Copy/Paste the \n"
                + "parser output into: http://www.webgraphviz.com\n";

        String encoded = URLEncoder.encode(graph, "UTF-8").replace("+", "%20");

        // Open the default browser with the url:
        if (Desktop.isDesktopSupported())
        try {
            final URL webGraphvizURI = new URL(WEBGRAPHVIZ_HOME + "#" + encoded);
            final Desktop desktop = Desktop.getDesktop();

            // Can we launch a browser?
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                System.out.println(MSG);
                var response = JOptionPane.showConfirmDialog(
                        null,
                        MSG + "\nOpen Web Graphviz Page?",
                        "Open Web Graphviz Page",
                        JOptionPane.YES_NO_OPTION);

                //
                if (response == JOptionPane.YES_OPTION) {
                    desktop.browse(webGraphvizURI.toURI());
                }
            }
        } catch (IOException | URISyntaxException ex) {
            java.util.logging.Logger.getAnonymousLogger().log(java.util.logging.Level.WARNING, "Could not open browser", ex);
        }
    }

}