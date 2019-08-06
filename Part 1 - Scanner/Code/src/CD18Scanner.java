import java.io.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;

/** CD18Scanner.java
 *
 * Author: Greg Choice c9311718@uon.edu.au
 *
 * Created: 03/08/2018
 * Updated: 21/08/2018
 *
 * Description:
 * CD18Scanner class is a lexical analyser for the CD18 language
 *
 */
public class CD18Scanner
{
    private int lineNum;
    private int colNum;
    private BufferedReader srcFile;
    private boolean eof;
    private String error;
    private LinkedList<SimpleEntry<Token, String>> errorList;

    /** Constructor
     *
     * @param fileName - String, The name of the source file
     */
    public CD18Scanner(String fileName)
    {
        lineNum = 1;
        colNum = 1;
        eof = false;
        errorList = new LinkedList<>();
        try
        {
            srcFile = new BufferedReader(new FileReader(fileName));
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }

    /** hasErrors()
     *
     * Returns true if the source has lexical errors
     *
     * @return - boolean, true if source contains lexical errors, false if no errors
     */
    public boolean hasErrors()
    {
        return !errorList.isEmpty();
    }

    /** nextError()
     *
     * Returns the next error in the list of lexical errors
     *
     * @return - AbstractMap.SimpleEntry<Token, String>, a Token-String pair describing the error
     */
    public SimpleEntry nextError()
    {
        return errorList.pop();
    }

    /** getError()
     *
     * Returns the error message of the current token being processed
     *
     * @return - String, an error message describing the error that occured
     */
    public String getError()
    {
        return error;
    }

    /** eof()
     *
     * Checks for end of source file
     *
     * @return - boolean, returns true if end of file reached, false otherwise
     */
    public boolean eof()
    {
        return eof;
    }

    /** getToken()
     *
     * Checks the next character of the source file and returns a Token based on the input
     *
     * @return - Token, returns a token with required parameters based on a prescribed character sequence
     * @throws IOException - BufferedReader can throw an IOException
     */
    public Token getToken() throws IOException
    {
        StringBuilder lexeme = new StringBuilder();

        int startCol;

        int nextChar;

        srcFile.mark(2);

        nextChar = srcFile.read();

        while (nextChar != -1)
        {
            // First character scanned is a letter and initially marked as an identifier
            if(Character.isLetter(nextChar))
            {
                startCol = colNum;

                while(Character.isLetterOrDigit(nextChar))
                {
                    lexeme.append((char)nextChar);
                    srcFile.mark(1);
                    nextChar = srcFile.read();
                    colNum++;
                }
                srcFile.reset();

                // Identifier is valid
                return new Token(Token.ID.TIDEN, lineNum, startCol, lexeme.toString());
            }
            // First character scanned is a number
            else if(Character.isDigit(nextChar))
            {
                //srcFile.mark(1);
                startCol = colNum;
                while(Character.isDigit(nextChar))
                {
                    lexeme.append((char)nextChar);
                    srcFile.mark(1);
                    nextChar = srcFile.read();
                    colNum++;
                }
                srcFile.reset();

                srcFile.mark(2);

                // Decide if integer or float
                if(nextChar == '.')
                {
                    srcFile.skip(1);
                    nextChar = srcFile.read();

                    if(Character.isDigit(nextChar))
                    {
                        srcFile.mark(1);
                        lexeme.append('.');
                        //lexeme.append((char)nextChar);

                        while(Character.isDigit(nextChar))
                        {
                            lexeme.append((char)nextChar);
                            srcFile.mark(1);
                            nextChar = srcFile.read();
                            colNum++;
                        }
                        srcFile.reset();

                        // Number is a float
                        return new Token(Token.ID.TFLIT, lineNum, startCol, lexeme.toString());
                    }
                }
                srcFile.reset();

                //colNum++;

                // Number is integer
                return new Token(Token.ID.TILIT, lineNum, startCol, lexeme.toString());
            }
            // Start of string literal
            else if(nextChar == '\"')
            {
                startCol = colNum++;

                srcFile.mark(1);
                nextChar = srcFile.read();

                while((nextChar) != '\"')
                {
                    colNum++;
                    // String literal missing closing quotes
                    if(nextChar == '\r' || nextChar == -1)
                    {
                        colNum = startCol--;
                        Token undefined = new Token(Token.ID.TUNDF, lineNum, startCol, '\"' + lexeme.toString());
                        error = "Lexical Error (" + lineNum + ", " + colNum + "): unclosed string literal : \"" + lexeme.toString();
                        errorList.add(new SimpleEntry<>(undefined, error));
                        return undefined;
                    }
                    lexeme.append((char)nextChar);
                    nextChar = srcFile.read();
                }
                // String literal is complete
                return new Token(Token.ID.TSTRG, lineNum, startCol, lexeme.toString());
            }
            // All other characters
            else
            {
                switch(nextChar)
                {
                    case '\n':
                        lineNum++;
                        colNum = 1;
                        break;

                    case '/':
                        startCol = colNum++;

                        srcFile.mark(3);
                        nextChar = srcFile.read();
                        switch(nextChar)
                        {
                            // Token is '/='
                            case '=':
                                srcFile.mark(1);
                                return new Token(Token.ID.TDVEQ, lineNum, startCol, null);

                            // Decide if a comment follows
                            case '-':
                                nextChar = srcFile.read();

                                if(nextChar == '-')
                                {
                                    srcFile.mark(2);

                                    // Ignore until the end of line or end of file
                                    while((nextChar = srcFile.read()) != '\r' && nextChar != -1)
                                    {
                                        colNum++;
                                    }
                                }
                                // Token is '/'
                                else
                                {
                                    colNum++;
                                    srcFile.reset();
                                    return new Token(Token.ID.TDIVD, lineNum, startCol, null);
                                }
                                break;

                            // Token is '/'
                            default:
                                srcFile.reset();
                                return new Token(Token.ID.TDIVD, lineNum, startCol, null);
                        }
                        colNum++;
                        break;

                    case '+':
                        startCol = colNum++;
                        srcFile.mark(1);

                        // Token is '+='
                        if(srcFile.read() == '=')
                        {
                            colNum++;
                            return new Token(Token.ID.TPLEQ, lineNum, startCol, null);
                        }
                        // Token is '+'
                        else
                        {
                            srcFile.reset();
                            return new Token(Token.ID.TPLUS, lineNum, startCol, null);
                        }

                    case '-':
                        startCol = colNum++;
                        srcFile.mark(1);

                        // Token is '-='
                        if(srcFile.read() == '=')
                        {
                            colNum++;
                            return new Token(Token.ID.TMNEQ, lineNum, startCol, null);
                        }
                        // Token is '-'
                        else
                        {
                            srcFile.reset();
                            return new Token(Token.ID.TMINS, lineNum, startCol, null);
                        }

                    case '*':
                        startCol = colNum++;
                        srcFile.mark(1);

                        // Token is '*='
                        if(srcFile.read() == '=')
                        {
                            colNum++;
                            return new Token(Token.ID.TSTEQ, lineNum, startCol, null);
                        }
                        // Token is '*'
                        else
                        {
                            srcFile.reset();
                            return new Token(Token.ID.TSTAR, lineNum, startCol, null);
                        }

                    case '<':
                        startCol = colNum++;
                        srcFile.mark(1);

                        // Token is '<='
                        if(srcFile.read() == '=')
                        {
                            colNum++;
                            return new Token(Token.ID.TLEQL, lineNum, startCol, null);
                        }
                        // Token is '<'
                        else
                        {
                            srcFile.reset();
                            return new Token(Token.ID.TLESS, lineNum, startCol, null);
                        }

                    case '>':
                        startCol = colNum++;
                        srcFile.mark(1);

                        // Token is '>='
                        if(srcFile.read() == '=')
                        {
                            colNum++;
                            return new Token(Token.ID.TGEQL, lineNum, startCol, null);
                        }
                        // Token is '>'
                        else
                        {
                            srcFile.reset();
                            return new Token(Token.ID.TGRTR, lineNum, startCol, null);
                        }

                    case '=':
                        startCol = colNum++;
                        srcFile.mark(1);

                        // Token is '=='
                        if(srcFile.read() == '=')
                        {
                            colNum++;
                            return new Token(Token.ID.TEQEQ, lineNum, startCol, null);
                        }
                        // Token is '='
                        else
                        {
                            srcFile.reset();
                            return new Token(Token.ID.TEQUL, lineNum, startCol, null);
                        }

                        // Token is one of the following '% ^ . , [ ] ( ) : ;'
                    case '%':
                        startCol = colNum++;
                        return new Token(Token.ID.TPERC, lineNum, startCol, null);

                    case '^':
                        startCol = colNum++;
                        return new Token(Token.ID.TCART, lineNum, startCol, null);

                    case '.':
                        return new Token(Token.ID.TDOT, lineNum, colNum++, null);

                    case ',':
                        return new Token(Token.ID.TCOMA, lineNum, colNum++, null);

                    case '[':
                        return new Token(Token.ID.TLBRK, lineNum, colNum++, null);

                    case ']':
                        return new Token(Token.ID.TRBRK, lineNum, colNum++, null);

                    case '(':
                        return new Token(Token.ID.TLPAR, lineNum, colNum++, null);

                    case ')':
                        return new Token(Token.ID.TRPAR, lineNum, colNum++, null);

                    case ':':
                        return new Token(Token.ID.TCOLN, lineNum, colNum++, null);

                    case ';':
                        return new Token(Token.ID.TSEMI, lineNum, colNum++, null);

                    case '!':
                        startCol = colNum;

                        // Token is '!='
                        if(srcFile.read() == '=')
                        {
                            return new Token(Token.ID.TNEQL, lineNum, startCol, null);
                        }
                        srcFile.reset();

                    // All other characters
                    default:
                        if(!Character.isWhitespace(nextChar))
                        {
                            startCol = colNum;
                            String validChar = "[]()=+-*/%^;:,.<>\"";

                            while(!validChar.contains(Character.toString((char)nextChar)) && !Character.isLetterOrDigit(nextChar) && !Character.isWhitespace(nextChar))
                            {
                                switch(nextChar)
                                {
                                    case '!':
                                        if(srcFile.read() != '=')
                                        {
                                            srcFile.reset();

                                            lexeme.append((char)srcFile.read());

                                            srcFile.mark(1);
                                            nextChar = srcFile.read();
                                            colNum++;
                                        }
                                        else
                                        {
                                            srcFile.reset();
                                            Token undefined = new Token(Token.ID.TUNDF, lineNum, startCol, lexeme.toString());
                                            error = "Lexical Error (" + lineNum + ", " + startCol + "): invalid character sequence : " + lexeme.toString();
                                            errorList.add(new SimpleEntry<>(undefined, error));
                                            return undefined;
                                        }

                                    default:
                                        if(nextChar == '!')
                                            break;
                                        if(!validChar.contains(Character.toString((char)nextChar)) && !Character.isLetterOrDigit(nextChar) && !Character.isWhitespace(nextChar))
                                        {
                                            lexeme.append((char)nextChar);
                                            srcFile.mark(2);
                                            nextChar = srcFile.read();
                                        }
                                }
                                colNum++;
                            }
                            srcFile.reset();
                            Token undefined = new Token(Token.ID.TUNDF, lineNum, startCol, lexeme.toString());
                            error = "Lexical Error (" + lineNum + ", " + startCol + "): invalid character sequence : " + lexeme.toString();
                            errorList.add(new SimpleEntry<>(undefined, error));
                            return undefined;
                        }
                        if(nextChar == '\t')
                            colNum += 4;
                }
                srcFile.mark(2);
                nextChar = srcFile.read();
            }
        }
        // End of source file has been reached
        eof = true;
        return new Token(Token.ID.TEOF, lineNum, colNum, null);
    }
}
