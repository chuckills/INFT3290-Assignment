import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;

//	COMP3290 CD18 Compiler
//		Syntax Tree Node Class - Builds a syntax tree node
//
//		Check this out as you use it, my parser is well on the way but it isn't working yet.
//
//	Based on Code by MRH (2013) - Updated by DB for CD18	
//

/** STNode.java
 *
 * Modified from supplied COMP3290 tree node
 *
 * Author: Greg Choice c9311718@uon.edu.au
 *
 * Created:
 * Updated: 28/09/2018
 *
 * Description:
 * Syntax Tree node super class provides common functions for all
 * sub-classes of STNode
 *
 */
public class STNode
{
    // SYNTAX TREE NODE VALUES
    public enum NID
    {
        NUNDEF,
        NPROG, NGLOB, NILIST, NINIT, NFUNCS, NMAIN, NSDLST, NTYPEL, NRTYPE, NATYPE,
        NFLIST, NSDECL, NALIST, NARRD, NFUND, NPLIST, NSIMP, NARRP, NARRC, NDLIST,
        NSTATS, NFOR, NREPT, NASGNS, NIFTH, NIFTE, NASGN, NPLEQ, NMNEQ, NSTEQ,
        NDVEQ, NINPUT, NPRINT, NPRLN, NCALL, NRETN, NVLIST, NSIMV, NARRV, NEXPL,
        NBOOL, NNOT, NAND, NOR, NXOR, NEQL, NNEQ, NGRT, NLSS, NLEQ,
        NADD, NSUB, NMUL, NDIV, NMOD, NPOW, NILIT, NFLIT, NTRUE, NFALS,
        NFCALL, NPRLST, NSTRG, NGEQ
    }

	private NID nodeID;
	private STNode leftChild, middleChild, rightChild;
	private TableEntry symbol;

    protected static String error;
    protected static LinkedList<SimpleEntry<Token, String>> errorList = new LinkedList<>();

    //===============================
    // CONSTRUCTORS
    //===============================
	public STNode(NID id)
    {
		nodeID = id;
        leftChild = null;
		middleChild = null;
        rightChild = null;
		symbol = null;
	}

	public STNode(NID id, TableEntry st)
    {
		this(id);
		symbol = st;
	}

	public STNode(NID id, STNode left, STNode right)
    {
		this(id);
        leftChild = left;
        rightChild = right;
	}

	public STNode(NID id, STNode left, STNode middle, STNode right)
    {
		this(id, left, right);
        middleChild = middle;
	}

	//==================================
    // ACCESSORS AND MUTATORS
    //==================================
	public NID getNodeID()
    {
        return nodeID;
    }

	public STNode getLeft()
    {
        return leftChild;
    }

	public STNode getMiddle()
    {
        return middleChild;
    }

	public STNode getRight()
    {
        return rightChild;
    }

	public TableEntry getSymbol()
    {
        return symbol;
    }

	public void setNodeID(NID id)
    {
        nodeID = id;
    }

	public void setLeft(STNode left)
    {
        leftChild = left;
    }

	public void setMiddle(STNode middle)
    {
        middleChild = middle;
    }

	public void setRight(STNode right)
    {
        rightChild = right;
    }

	public void setSymbol(TableEntry entry)
    {
        symbol = entry;
    }

    /** hasErrors()
     *
     * Returns true if the source has syntax errors
     *
     * @return - boolean, true if source contains lexical errors, false if no errors
     */
    public boolean hasErrors()
    {
        return !errorList.isEmpty();
    }

    /** nextError()
     *
     * Returns the next error in the list of syntax errors
     *
     * @return - AbstractMap.SimpleEntry<Token, String>, a Token-String pair describing the error
     */
    public SimpleEntry nextError()
    {
        return errorList.pop();
    }
	
    /** processExpression()
     *
     * Processes an expression starting with the lowest level of precedence
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processExpression(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode term, expression;

        // Process the first term in the expression
        term = processTerm(tokenList, table);

        Token nextToken = tokenList.pop();

        // Select the operation
        switch(nextToken.getTokenID())
        {
            case TPLUS:
                expression = new NAdd(tokenList, table);
                expression.setLeft(term);
                break;

            case TMINS:
                expression = new NSub(tokenList, table);
                expression.setLeft(term);
                break;

            // Expression is unary
            default:
                tokenList.push(nextToken);
                expression = term;
        }

        return expression;
    }

    /** processTerm()
     *
     * Processes the second level of operator precedence in an expression
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processTerm(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode factor, term;

        // Process the first factor of the term
        factor = processFactor(tokenList, table);

        Token nextToken = tokenList.pop();

        // Select the operation
        switch(nextToken.getTokenID())
        {
            case TSTAR:
                term = new NMul(tokenList, table);
                term.setLeft(factor);
                break;

            case TDIVD:
                term = new NDiv(tokenList, table);
                term.setLeft(factor);
                break;

            case TPERC:
                term = new NMod(tokenList, table);
                term.setLeft(factor);
                break;

            // Term is unary
            default:
                tokenList.push(nextToken);
                term = factor;
        }

        return term;
    }

    /** processFactor()
     *
     * Processes the third level of operator precedence in an expression
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processFactor(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode exponent, factor;

        // process the first exponent of the factor
        exponent = processExponent(tokenList, table);

        Token nextToken = tokenList.pop();

        switch(nextToken.getTokenID())
        {
            case TCART:
                factor = new NPow(tokenList, table);
                factor.setLeft(exponent);
                break;

            // Exponent is unary
            default:
                tokenList.push(nextToken);
                factor = exponent;
        }

        return factor;
    }

    /** processExponent()
     *
     * Processes the operands of an expression
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processExponent(LinkedList<Token> tokenList, SymbolTable table)
    {
        Token nextToken = tokenList.pop();

        // Select the type of exponent
        switch(nextToken.getTokenID())
        {
            case TILIT:
                tokenList.push(nextToken);
                return new NIlit(tokenList, table);

            case TFLIT:
                tokenList.push(nextToken);
                return new NFlit(tokenList, table);

            case TTRUE:
                return new NTrue(tokenList, table);

            case TFALS:
                return new NFals(tokenList, table);

            case TLPAR:
                STNode bool = processBool(tokenList, table);
                nextToken = tokenList.pop();
                if(nextToken.getTokenID() != Token.TID.TRPAR)
                    tokenList.push(nextToken);
                return bool;

            case TIDEN:
                tokenList.push(nextToken);
                return processIden(tokenList, table);

            case TNOT:
                tokenList.push(nextToken);
                return processBool(tokenList, table);

            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected operand or expression."));
                int line = nextToken.getLineNum();
                while(nextToken.getTokenID() != Token.TID.TCOMA && nextToken.getTokenID() != Token.TID.TTYPS && nextToken.getTokenID() != Token.TID.TARRS && nextToken.getTokenID() != Token.TID.TFUNC && nextToken.getTokenID() != Token.TID.TMAIN
                        && nextToken.getTokenID() != Token.TID.TRBRK && nextToken.getLineNum() == line)
                {
                    nextToken = tokenList.pop();
                }
                tokenList.push(nextToken);
                return new NUndef(tokenList, table);
        }
    }

    /** processBool()
     *
     * Processes a boolean expression starting with the lowest level of precedence
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processBool(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode rel, bool, logop;

        // Process the first relation
        rel = processRel(tokenList, table);

        Token nextToken = tokenList.pop();

        // Select the operator
        switch(nextToken.getTokenID())
        {
            case TAND:
                bool = new NBool(tokenList, table);
                logop = new NAnd(tokenList, table);
                logop.setLeft(rel);
                bool.setLeft(logop);
                break;

            case TOR:
                bool = new NBool(tokenList, table);
                logop = new NOr(tokenList, table);
                logop.setLeft(rel);
                bool.setLeft(logop);
                break;

            case TXOR:
                bool = new NBool(tokenList, table);
                logop = new NXor(tokenList, table);
                logop.setLeft(rel);
                bool.setLeft(logop);
                break;

            // Relation is unary
            default:
                tokenList.push(nextToken);
                bool = rel;
        }

        return bool;
    }

    /** processRel()
     *
     * Processes the next level of boolean precedence
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processRel(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode rel, expression;

        Token nextToken = tokenList.pop();

        // Select the relation type
        switch(nextToken.getTokenID())
        {
            // Relation is negated
            case TNOT:
                rel = new NNot(tokenList, table);
                rel.setLeft(processRel(tokenList, table));
                return rel;

            // Relation is not negated, process the expression
            default:
                tokenList.push(nextToken);
                expression = processExpression(tokenList, table);
                if(expression.getNodeID() != NID.NTRUE && expression.getNodeID() != NID.NFALS)
                    expression = foldConstants(expression, table);
        }

        nextToken = tokenList.pop();

        Double op1, op2;

        // Attempts made to fold boolean expressions
        // Not sure how successfull it is
        switch(nextToken.getTokenID())
        {
            case TEQEQ:
                rel = new NEql(tokenList, table);
                rel.setLeft(expression);

                op1 = getValue(rel.getLeft(), table);
                op2 = getValue(rel.getRight(), table);

                if(op1 != null && op2 != null)
                {
                    if(op1.equals(op2))
                        rel = new NTrue(new Token(Token.TID.TTRUE, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                    else
                        rel = new NFals(new Token(Token.TID.TFALS, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                }
                break;

            case TNEQL:
                rel = new NNeq(tokenList, table);
                rel.setLeft(expression);

                op1 = getValue(rel.getLeft(), table);
                op2 = getValue(rel.getRight(), table);

                if(op1 != null && op2 != null)
                {
                    if(!op1.equals(op2))
                        rel = new NTrue(new Token(Token.TID.TTRUE, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                    else
                        rel = new NFals(new Token(Token.TID.TFALS, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                }
                break;

            case TGRTR:
                rel = new NGrt(tokenList, table);
                rel.setLeft(expression);

                op1 = getValue(rel.getLeft(), table);
                op2 = getValue(rel.getRight(), table);

                if(op1 != null && op2 != null)
                {
                    if(op1.compareTo(op2) > 0)
                        rel = new NTrue(new Token(Token.TID.TTRUE, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                    else
                        rel = new NFals(new Token(Token.TID.TFALS, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                }
                break;

            case TGEQL:
                rel = new NGeq(tokenList, table);
                rel.setLeft(expression);

                op1 = getValue(rel.getLeft(), table);
                op2 = getValue(rel.getRight(), table);

                if(op1 != null && op2 != null)
                {
                    if(op1.compareTo(op2) >= 0)
                        rel = new NTrue(new Token(Token.TID.TTRUE, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                    else
                        rel = new NFals(new Token(Token.TID.TFALS, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                }
                break;

            case TLESS:
                rel = new NLss(tokenList, table);
                rel.setLeft(expression);

                op1 = getValue(rel.getLeft(), table);
                op2 = getValue(rel.getRight(), table);

                if(op1 != null && op2 != null)
                {
                    if(op1.compareTo(op2) < 0)
                        rel = new NTrue(new Token(Token.TID.TTRUE, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                    else
                        rel = new NFals(new Token(Token.TID.TFALS, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                }
                break;

            case TLEQL:
                rel = new NLeq(tokenList, table);
                rel.setLeft(expression);

                op1 = getValue(rel.getLeft(), table);
                op2 = getValue(rel.getRight(), table);

                if(op1 != null && op2 != null)
                {
                    if(op1.compareTo(op2) <= 0)
                        rel = new NTrue(new Token(Token.TID.TTRUE, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                    else
                        rel = new NFals(new Token(Token.TID.TFALS, rel.getLeft().getSymbol().getLine(), rel.getLeft().getSymbol().getCol(), null), table);
                }
                break;

            default:
                tokenList.push(nextToken);
                rel = expression;
        }

        return rel;
    }

    /** getValue()
     *
     * Convert symbol name to numeric
     *
     */
    private Double getValue(STNode node, SymbolTable table)
    {
        try
        {
            return Double.parseDouble(foldConstants(node, table).getSymbol().getName());
        }
        catch(NumberFormatException e2)
        {
            System.out.println(e2.getMessage());
        }
        return null;
    }


    /** processIden()
     *
     * Processes the structure of an identifier
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processIden(LinkedList<Token> tokenList, SymbolTable table)
    {
        Token nextToken = tokenList.pop();
        Token tempToken = tokenList.pop();

        // Select the structure of identifier
        switch(tempToken.getTokenID())
        {
            // Function call
            case TLPAR:
                tokenList.push(tempToken);
                tokenList.push(nextToken);
                if(!table.hasGlobalID(nextToken.getLexeme()))
                {
                    errorList.add(new SimpleEntry<>(nextToken, "Semantic Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Function not defined."));
                    int line = nextToken.getLineNum();
                    while(nextToken.getLineNum() == line)
                    {
                        nextToken = tokenList.pop();
                    }
                    return new NUndef(tokenList, table);
                }

                return new NFcall(tokenList, table);

            // Array member
            case TLBRK:
                tokenList.push(tempToken);
                tokenList.push(nextToken);
                return new NArrv(tokenList, table);

            // Simple variable
            default:
                tokenList.push(tempToken);
                tokenList.push(nextToken);
                return new NSimv(tokenList, table);
        }
    }

    /** processAssgn()
     *
     * Processes a list of variable assignments
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processAssgn(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode var, assgn;

        // Get the identifier type
        var = processIden(tokenList, table);

        Token nextToken = tokenList.pop();

        // Select assignment operator
        switch(nextToken.getTokenID())
        {
            // Attempts to fold constants made here for semantic checking
            case TEQUL:
                assgn = new NAsgn(tokenList, table);
                if(assgn.getRight().getNodeID() != NID.NTRUE && assgn.getRight().getNodeID() != NID.NFALS)
                {
                    assgn.setLeft(var);

                    if(assgn.getRight().getNodeID() != NID.NFLIT && assgn.getRight().getNodeID() != NID.NILIT)
                    {
                        assgn.setRight(foldConstants(assgn.getRight(), table));
                    }

                    if(var.getNodeID() != NID.NARRV)
                    {
                        if(!assgn.getLeft().getSymbol().getType().equals(assgn.getRight().getSymbol().getType()))
                        {
                            switch(assgn.getLeft().getSymbol().getType())
                            {
                                case "real":
                                    promoteToReal(assgn, table);
                                    break;

                                default:
                                    errorList.add(new SimpleEntry<>(nextToken, "Semantic Error: (" + var.getSymbol().getLine() + ", " + var.getSymbol().getCol() + "): Type mismatch."));
                            }
                        }
                        table.updateValue(var.getSymbol().getName(), assgn.getRight().getSymbol().getName());
                    }
                    else
                    {
                        if(!assgn.getLeft().getRight().getSymbol().getType().equals(assgn.getRight().getSymbol().getType()))
                        {
                            switch(assgn.getLeft().getRight().getSymbol().getType())
                            {
                                case "real":
                                    promoteToReal(assgn, table);
                                    break;

                                default:
                                    errorList.add(new SimpleEntry<>(nextToken, "Semantic Error: (" + var.getSymbol().getLine() + ", " + var.getSymbol().getCol() + "): Type mismatch."));
                            }
                        }
                        table.updateValue(var.getRight().getSymbol().getName(), assgn.getRight().getSymbol().getName());
                    }
                }
                else
                {
                    if(var.getNodeID() != NID.NARRV)
                    {
                        if(!var.getSymbol().getType().equals("boolean"))
                            errorList.add(new SimpleEntry<>(nextToken, "Semantic Error: (" + var.getSymbol().getLine() + ", " + var.getSymbol().getCol() + "): Type mismatch."));
                        else
                        {
                            if(assgn.getRight().getNodeID() == NID.NTRUE)
                                table.updateValue(var.getSymbol().getName(), "true");
                            else
                                table.updateValue(var.getSymbol().getName(), "false");
                        }
                    }
                    else
                    {
                        if(!var.getRight().getSymbol().getType().equals("boolean"))
                            errorList.add(new SimpleEntry<>(nextToken, "Semantic Error: (" + var.getSymbol().getLine() + ", " + var.getSymbol().getCol() + "): Type mismatch."));
                        else
                        {
                            if(assgn.getRight().getNodeID() == NID.NTRUE)
                                table.updateValue(var.getRight().getSymbol().getName(), "true");
                            else
                                table.updateValue(var.getRight().getSymbol().getName(), "false");
                        }
                    }
                }
                break;

            // Semantic checking for the remaining assignments not implemented
            case TPLEQ:
                assgn = new NPleq(tokenList, table);
                assgn.setLeft(var);
                break;

            case TMNEQ:
                assgn = new NMneq(tokenList, table);
                assgn.setLeft(var);
                break;

            case TSTEQ:
                assgn = new NSteq(tokenList, table);
                assgn.setLeft(var);
                break;

            case TDVEQ:
                assgn = new NDveq(tokenList, table);
                assgn.setLeft(var);
                break;

            default:
                assgn = new NUndef(tokenList, table);
        }

        return assgn;
    }

    /** promoteToReal()
     *
     * Promotes an integer to real
     *
     */
    private void promoteToReal(STNode node, SymbolTable table)
    {
        if(node.getRight().getNodeID() == NID.NILIT)
        {
            node.setRight(new NFlit(new Token(Token.TID.TFLIT, node.getRight().getSymbol().getLine(), node.getRight().getSymbol().getCol(), node.getRight().getSymbol().getName()+".0"), table));
        }
        else if(node.getRight().getNodeID() != NID.NFLIT)
        {
            errorList.add(new SimpleEntry<>(node.getRight().getSymbol().getToken(), "Semantic Error: (" + node.getRight().getSymbol().getLine() + ", " + node.getRight().getSymbol().getCol() + "): Type mismatch."));
        }
    }

    /** processIlist()
     *
     * Processes a list of global constant initialisations
     *
     * @param tokenList - LinkedList,
     * @param globalTable - SymbolTable,
     * @return STNode,
     */
    protected STNode processIlist(LinkedList<Token> tokenList, SymbolTable globalTable)
    {
        Token nextToken = tokenList.pop();
        Token tempToken = tokenList.pop();

        STNode init;
        STNode iList;

        // Check for equal operator
        switch(tempToken.getTokenID())
        {
            // Constants folded and type checked for constants section
            case TEQUL:
                STNode expr = processExpression(tokenList, globalTable);

                if(expr.getNodeID() == NID.NFCALL)
                    errorList.add(new SimpleEntry<>(nextToken, "Semantic Error: (" + expr.getSymbol().getLine() + ", " + expr.getSymbol().getCol() + "): Constant assignment must be numeric literal or constant expression."));

                tokenList.push(nextToken);
                init =  new NInit(tokenList, globalTable);

                if(expr.getNodeID() != NID.NILIT && expr.getNodeID() != NID.NFLIT)
                    expr = foldConstants(expr, globalTable);

                init.setLeft(expr);
                if(expr.getNodeID() == NID.NILIT)
                {
                    init.getSymbol().setType("integer");
                }
                else if(expr.getNodeID() == NID.NFLIT)
                {
                    init.getSymbol().setType("real");
                }
                else
                {
                    nextToken = tokenList.pop();
                    init = new NUndef(tokenList, globalTable);
                    while(nextToken.getTokenID() != Token.TID.TCOMA && nextToken.getTokenID() != Token.TID.TIDEN && nextToken.getTokenID() != Token.TID.TTYPS)
                    {
                        nextToken = tokenList.pop();
                    }

                    if(nextToken.getTokenID() == Token.TID.TCOMA)
                        nextToken = tokenList.pop();

                    // Comma is also missing
                    if(nextToken.getTokenID() == Token.TID.TIDEN)
                    {
                        tokenList.push(nextToken);
                        iList =  new NIlist(tokenList, globalTable);
                        iList.setLeft(init);
                        return iList;
                    }
                    if(nextToken.getTokenID() == Token.TID.TTYPS)
                    {
                        break;
                    }
                }

                init.getSymbol().setValue(expr.getSymbol().getName());
                globalTable.addSymbol(init.getSymbol());
                nextToken = tokenList.pop();

                // Return list of initialisers
                switch(nextToken.getTokenID())
                {
                    case TCOMA:
                        iList =  new NIlist(tokenList, globalTable);
                        iList.setLeft(init);
                        return iList;

                    // Missing comma between initialisers, throw error and process identifier as the next part of the list
                    case TIDEN:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected \",\" after initialiser."));
                        tokenList.push(nextToken);
                        iList =  new NIlist(tokenList, globalTable);
                        iList.setLeft(init);
                        return iList;

                    default:
                }
                break;

            // Equal operator missing, throw error and continue to next identifier
            default:
                init = new NUndef(tokenList, globalTable);
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected \"=\" operator."));
                while(nextToken.getTokenID() != Token.TID.TCOMA && nextToken.getTokenID() != Token.TID.TIDEN)
                {
                    nextToken = tokenList.pop();
                }

                if(nextToken.getTokenID() == Token.TID.TCOMA)
                    nextToken = tokenList.pop();

                // Comma is also missing
                if(nextToken.getTokenID() == Token.TID.TIDEN)
                {
                    tokenList.push(nextToken);
                    iList =  new NIlist(tokenList, globalTable);
                    iList.setLeft(init);
                    return iList;
                }
        }

        // Return single constant initialiser
        tokenList.push(nextToken);
        return init;
    }

    /** processSdlst()
     *
     * Process a list of variable declarations for the main function
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processSdlst(LinkedList<Token> tokenList, SymbolTable table)
    {
        Token nextToken = tokenList.pop();
        Token tempToken = tokenList.pop();

        STNode sDecl = null;
        STNode sDlst;

        switch(tempToken.getTokenID())
        {
            case TCOLN:
                tokenList.push(nextToken);
                sDecl =  new NSdecl(tokenList, table);

                nextToken = tokenList.pop();

                switch(nextToken.getTokenID())
                {
                    case TCOMA:
                        sDlst =  new NSdlst(tokenList, table);
                        sDlst.setLeft(sDecl);
                        return sDlst;

                    case TBEGN:
                        break;

                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected comma in declaration list."));
                        tokenList.push(nextToken);
                        while(nextToken.getTokenID() != Token.TID.TIDEN && nextToken.getTokenID() != Token.TID.TBEGN)
                        {
                            tokenList.pop();
                        }

                        if(nextToken.getTokenID() == Token.TID.TIDEN)
                        {
                            sDlst = new NSdlst(tokenList, table);
                            sDlst.setLeft(sDecl);
                            return sDlst;
                        }
                }
                break;

            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \":\" in declaration."));
        }

        tokenList.push(nextToken);
        return sDecl;
    }

    /** processVlist()
     *
     * Processes a list of variables for the input statement
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processVlist(LinkedList<Token> tokenList, SymbolTable table)
    {
        Token nextToken = tokenList.pop();
        Token tempToken = tokenList.pop();

        STNode vItem = null;
        STNode vList;

        // Check the variable starts with an identifier
        switch(nextToken.getTokenID())
        {
            case TIDEN:
                // Variable is a simple type
                if(tempToken.getTokenID() != Token.TID.TLBRK)
                {
                    tokenList.push(tempToken);
                    tokenList.push(nextToken);
                    vItem = new NSimv(tokenList, table);

                }
                // Variable is an array member
                else if(tempToken.getTokenID() == Token.TID.TLBRK)
                {
                    tokenList.push(tempToken);
                    tokenList.push(nextToken);
                    vItem = new NArrv(tokenList, table);
                }

                nextToken = tokenList.pop();
                break;

            // An identifier was not the first token encountered, throw an error and continue at the next identifier
            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected variable or variable list."));
                vItem = new NUndef(tokenList, table);
                tokenList.push(tempToken);

                while(nextToken.getTokenID() != Token.TID.TSEMI && nextToken.getTokenID() != Token.TID.TCOMA)
                {
                    nextToken = tokenList.pop();
                }
        }

        // Check if there are more variables to be added
        switch(nextToken.getTokenID())
        {
            case TCOMA:
                vList = new NVlist(tokenList, table);
                vList.setLeft(vItem);
                return vList;

            case TSEMI:
                break;

            default:
        }

        tokenList.push(nextToken);
        return vItem;
    }

    /** processPrlst()
     *
     *  Processes a list of variables, strings and expressions for the print and printline statements
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processPrlst(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode pItem;
        STNode pList;

        Token nextToken = tokenList.pop();

        // Check the type of item in the print list
        switch(nextToken.getTokenID())
        {
            case TSTRG:
                tokenList.push(nextToken);
                pItem = new NStrg(tokenList, table);
                break;

            case TIDEN:
            case TILIT:
            case TFLIT:
            case TTRUE:
            case TFALS:
            case TLPAR:
                tokenList.push(nextToken);
                pItem = processExpression(tokenList, table);
                break;

            // The first print item was not a variable or expression, throw an error and continue at the next print item
            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected string or expression."));
                pItem = new NUndef(tokenList, table);
                while(nextToken.getTokenID() != Token.TID.TSEMI && nextToken.getTokenID() != Token.TID.TCOMA)
                {
                    nextToken = tokenList.pop();
                }
                tokenList.push(nextToken);
        }

        nextToken = tokenList.pop();

        // Check if there are more print items to be added
        switch(nextToken.getTokenID())
        {
            case TCOMA:
                pList =  new NPrlst(tokenList, table);
                pList.setLeft(pItem);
                return pList;

            default:
                tokenList.push(nextToken);
        }

        return pItem;
    }

    /** processTypes()
     *
     * Processes a list of global type declarations
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processTypes(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode tNode = null;
        STNode tList;

        Token nextToken = tokenList.pop();
        Token tempToken = tokenList.pop();

        switch(tempToken.getTokenID())
        {
            case TIS:
                break;

			// Missing is statement	in type definition
            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected is after identifier."));
                while((nextToken.getTokenID() != Token.TID.TIDEN || tempToken.getTokenID() != Token.TID.TIS) && tempToken.getTokenID() != Token.TID.TARRS && tempToken.getTokenID() != Token.TID.TFUNC && tempToken.getTokenID() != Token.TID.TMAIN)
                {
                    nextToken = tempToken;
                    tempToken = tokenList.pop();
                }
        }

        if(tempToken.getTokenID() != Token.TID.TIS)
            tokenList.push(tempToken);
        else
            tempToken = tokenList.pop();
		
		// Check the structure of the type definition
        switch(tempToken.getTokenID())
        {
			// Type is complex structure
            case TIDEN:
                tokenList.push(tempToken);
                tokenList.push(nextToken);

                tNode = new NRtype(tokenList, table);

                nextToken = tokenList.pop();

                if(nextToken.getTokenID() == Token.TID.TEND)
                {
                    nextToken = tokenList.pop();
                    switch(nextToken.getTokenID())
                    {
                        case TIDEN:
                            tokenList.push(nextToken);
                            tList = new NTypel(tokenList, table);
                            tList.setLeft(tNode);
                            return tList;

                        case TARRS:
                            tokenList.push(nextToken);
                            break;

						// Identifier missing or invalid	
                        default:
                            errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected identifier"));


                    }
                }
                else if(nextToken.getTokenID() != Token.TID.TARRS)
                {
					// No end statement
                    errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected end statement in type definition."));
                    switch(nextToken.getTokenID())
                    {
                        case TIDEN:
                            tokenList.push(nextToken);
                            tList = new NTypel(tokenList, table);
                            tList.setLeft(tNode);
                            return tList;

                        default:
                            tokenList.push(nextToken);
                            tList = new NUndef(tokenList, table);
                            tList.setLeft(tNode);
                            return tList;
                    }
                }
                break;

			// Type is array	
            case TARAY:
                tokenList.push(nextToken);
                tNode = new NAtype(tokenList, table);

                nextToken = tokenList.pop();

                switch(nextToken.getTokenID())
                {
                    case TIDEN:
                        tokenList.push(nextToken);
                        tList =  new NTypel(tokenList, table);
                        tList.setLeft(tNode);
                        return tList;

                    case TMAIN:
                    case TARRS:
                    case TFUNC:
                        tokenList.push(nextToken);
                        break;

                    default:
                        System.out.println("Error: Expected type identifier.");
                }
                break;

            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected identifier"));

        }
        return tNode;
    }

    /** processFlist()
     *
     * Processes a list of fields in a type structure definition
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processFlist(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode sDecl = null;
        STNode fList;

        Token nextToken = tokenList.pop();
        Token tempToken = tokenList.pop();

        // Check for colon in field definition
        switch(tempToken.getTokenID())
        {
            case TCOLN:
                tokenList.push(nextToken);
                sDecl = new NSdecl(tokenList, table);
                break;

            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \")\" in field list."));
                tokenList.push(nextToken);

        }

        nextToken = tokenList.pop();

        // Check for mor field definitions
        switch(nextToken.getTokenID())
        {
            case TCOMA:
                nextToken = tokenList.pop();
                switch(nextToken.getTokenID())
                {
                    case TIDEN:
                        tokenList.push(nextToken);
                        fList = new NFlist(tokenList, table);
                        fList.setLeft(sDecl);
                        return fList;

                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected type list identifier."));
                        while(nextToken.getTokenID() != Token.TID.TCOMA && nextToken.getTokenID() != Token.TID.TEND && nextToken.getTokenID() != Token.TID.TARRS)
                        {
                            nextToken = tokenList.pop();
                        }
                        if(nextToken.getTokenID() == Token.TID.TCOMA)
                        {
                            fList = new NFlist(tokenList, table);
                            fList.setLeft(sDecl);
                            return fList;
                        }
                        else
                            break;
                }
                break;

            case TEND:
                tokenList.push(nextToken);
                break;

            default:
                tokenList.push(nextToken);
                while(nextToken.getTokenID() != Token.TID.TIDEN && nextToken.getTokenID() != Token.TID.TEND)
                {
                    tokenList.pop();
                }

                if(nextToken.getTokenID() == Token.TID.TIDEN)
                {
                    nextToken = tokenList.pop();
                    if(nextToken.getTokenID() == Token.TID.TCOLN)
                    {
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected comma in field declaration list."));

                        tokenList.push(nextToken);
                        fList = new NFlist(tokenList, table);
                        fList.setLeft(sDecl);
                        return fList;
                    }
                    else
                    {
                        tokenList.push(nextToken);
                    }
                }
                break;
        }

        return sDecl;
    }

    /** processAlist()
     *
     * Processes a list of global array declarations
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processAlist(LinkedList<Token> tokenList, SymbolTable table)
    {
        Token nextToken = tokenList.pop();
        Token tempToken = tokenList.pop();

        STNode arrd = null;
        STNode aList;
		
		// Check the array definition has a colon
        switch(tempToken.getTokenID())
        {
            case TCOLN:
                tokenList.push(nextToken);
                arrd =  new NArrd(tokenList, table);

                nextToken = tokenList.pop();

                switch(nextToken.getTokenID())
                {
                    case TCOMA:
                        aList =  new NAlist(tokenList, table);
                        aList.setLeft(arrd);
                        return aList;

                    case TFUNC:
                    case TMAIN:
                        break;

                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected comma in array declaration list."));
                        tokenList.push(nextToken);
                        while(nextToken.getTokenID() != Token.TID.TIDEN && nextToken.getTokenID() != Token.TID.TFUNC && nextToken.getTokenID() != Token.TID.TMAIN)
                        {
                            tokenList.pop();
                        }

                        if(nextToken.getTokenID() == Token.TID.TIDEN)
                        {
                            aList =  new NAlist(tokenList, table);
                            aList.setLeft(arrd);
                            return aList;
                        }
                }
                break;

            case TFUNC:
                tokenList.push(nextToken);
                break;

            case TMAIN:
                tokenList.push(nextToken);
                break;

            default:
                System.out.println("Error: Missing \":\" in declaration.");
        }

        tokenList.push(nextToken);
        return arrd;
    }

    /** processFuncs()
     *
     * Processes a function or several functions
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processFuncs(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode func = null;
        STNode funcs;

        Token nextToken = tokenList.pop();

        // Check the function starts with an identifier
        switch(nextToken.getTokenID())
        {
            case TIDEN:
                tokenList.push(nextToken);
                func =  new NFund(tokenList, table);

                nextToken = tokenList.pop();

                // Process the next function
                if(nextToken.getTokenID() == Token.TID.TFUNC)
                {
                    funcs =  new NFuncs(tokenList, table);
                    funcs.setLeft(func);
                    return funcs;
                }
                break;

            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected function identifier."));
        }

        tokenList.push(nextToken);
        return func;
    }

    /** processStats()
     *
     * Processes a statement or block of statements
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processStats(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode stat;

        Token nextToken = tokenList.pop();

        switch(nextToken.getTokenID())
        {
            // Start of repeat statement
            case TREPT:
                nextToken = tokenList.pop();
                switch(nextToken.getTokenID())
                {
                    case TLPAR:
                        break;

                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \"(\" after repeat statement."));
                        while(nextToken.getTokenID() != Token.TID.TIDEN)
                        {
                            nextToken = tokenList.pop();
                        }
                }

                stat = new NRept(tokenList, table);

                nextToken = tokenList.pop();

				// Check for right parentheses
                switch(nextToken.getTokenID())
                {
                    case TRPAR:
                        break;

                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \")\" after assignment list."));
                        tokenList.push(nextToken);
                }

				// Process repeat statement block
                stat.setMiddle(processStats(tokenList, table));

                nextToken = tokenList.pop();

				// Check for until statement
                switch(nextToken.getTokenID())
                {
                    case TUNTL:
                        stat.setRight(processBool(tokenList, table));
                        nextToken = tokenList.pop();

                        // Check for more statements
                        switch(nextToken.getTokenID())
                        {
                            case TSEMI:
                                nextToken = tokenList.pop();
                                break;

                            // Missing semi-colon after statement
                            default:
                                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \";\" after statement."));
                        }
                        break;

					// No until statement	
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Repeat loop without until."));
                        tokenList.push(nextToken);
                        setRight(new NUndef(tokenList, table));
                        while(nextToken.getTokenID() != Token.TID.TSEMI)
                        {
                            nextToken = tokenList.pop();
                        }
                        tokenList.push(nextToken);
                }

                return nextStatement(nextToken, stat, tokenList, table);

            // Start of function call or assignment statement
            case TIDEN:
                Token tempToken = tokenList.pop();

                switch(tempToken.getTokenID())
                {
                    // Function call
                    case TLPAR:
                        tokenList.push(tempToken);
                        tokenList.push(nextToken);

                        if(!table.hasGlobalID(nextToken.getLexeme()))
                        {
                            errorList.add(new SimpleEntry<>(nextToken, "Semantic Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Function not defined."));
                            int line = nextToken.getLineNum();
                            while(nextToken.getLineNum() == line)
                            {
                                nextToken = tokenList.pop();
                            }
                            stat = new NUndef(tokenList, table);
                        }
                        else
                        {
                            stat = new NCall(tokenList, table);

                            nextToken = tokenList.pop();

                            // Check for more statements
                            switch(nextToken.getTokenID())
                            {
                                case TSEMI:
                                    nextToken = tokenList.pop();
                                    break;

                                // Missing semi-colon after statement
                                default:
                                    errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \";\" after statement."));
                            }
                        }
                        break;

                    // Assignment statement
                    case TLBRK:
                    case TEQUL:
                    case TPLEQ:
                    case TMNEQ:
                    case TSTEQ:
                    case TDVEQ:
                        tokenList.push(tempToken);
                        tokenList.push(nextToken);

                        stat = processAssgn(tokenList, table);

                        nextToken = tokenList.pop();

                        // Check for more statements
                        switch(nextToken.getTokenID())
                        {
                            case TSEMI:
                                nextToken = tokenList.pop();
                                break;

                            // Missing semi-colon after statement
                            default:
                                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \";\" after statement."));
                        }

                        break;

                    // Identifier starts with invalid character
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Invalid character following identifier."));
                        tokenList.push(tempToken);
                        tokenList.push(nextToken);

                        stat = new NUndef(tokenList, table);
                        return stat;
                }

                return nextStatement(nextToken, stat, tokenList, table);
                //break;

            // Start of input statement
            case TINPT:
                stat = new NInput(tokenList, table);
                nextToken = tokenList.pop();

                switch(nextToken.getTokenID())
                {
                    case TSEMI:
                        nextToken = tokenList.pop();
                        break;

                    // Missing semi-colon after statement
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \";\" after statement."));
                }

                return nextStatement(nextToken, stat, tokenList, table);

            // Start of print statement
            case TPRIN:
                stat = new NPrint(tokenList, table);
                nextToken = tokenList.pop();

                switch(nextToken.getTokenID())
                {
                    case TSEMI:
                        nextToken = tokenList.pop();
                        break;

                    // Missing semi-colon after statement
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \";\" after statement."));
                }

                return nextStatement(nextToken, stat, tokenList, table);

            // Start of printline statement
            case TPRLN:
                stat = new NPrln(tokenList, table);

                nextToken = tokenList.pop();

                switch(nextToken.getTokenID())
                {
                    case TSEMI:
                        nextToken = tokenList.pop();
                        break;

                    // Missing semi-colon after statement
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \";\" after statement."));
                }

                return nextStatement(nextToken, stat, tokenList, table);

            // Start of return statement
            case TRETN:
                stat = new NRetn(tokenList, table);

                nextToken = tokenList.pop();

                switch(nextToken.getTokenID())
                {
                    case TSEMI:
                        nextToken = tokenList.pop();
                        break;

                    // Missing semi-colon after statement
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \";\" after statement."));
                }

                return nextStatement(nextToken, stat, tokenList, table);

            // Start of for statement
            case TFOR:
                stat = new NFor(tokenList, table);

                nextToken = tokenList.pop();

                switch(nextToken.getTokenID())
                {
                    case TEND:
                        nextToken = tokenList.pop();
                        break;

                    // Missing end statement
                    case TMAIN:
                    case TFUNC:
                    case TCD18:
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing end after for statement."));
                        tokenList.push(nextToken);
                }

                return nextStatement(nextToken, stat, tokenList, table);

            // Start of if statement
            case TIFTH:
                nextToken = tokenList.pop();
                STNode bool;
                STNode ifStats;
                STNode ifNode = null;
                switch(nextToken.getTokenID())
                {
                    case TLPAR:
                        bool = processBool(tokenList, table);
                        break;

                    // Missing left-par after if statement
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \"(\" after if statement."));
                        tokenList.push(nextToken);
                        bool = processBool(tokenList, table);

                }
                nextToken = tokenList.pop();

                switch(nextToken.getTokenID())
                {
                    case TRPAR:
                        ifStats = processStats(tokenList, table);
                        break;

                    // Missing right-par after expression
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \")\" after boolean expression."));
                        tokenList.push(nextToken);
                        ifStats = processStats(tokenList, table);
                }

                nextToken = tokenList.pop();

                // Check for else statement
                switch(nextToken.getTokenID())
                {
                    case TELSE:
                        ifNode = new NIfte(tokenList, table);
                        ifNode.setLeft(bool);
                        ifNode.setMiddle(ifStats);
                        nextToken = tokenList.pop();
                        break;

                    case TEND:
                        ifNode = new NIfth(tokenList, table);
                        ifNode.setLeft(bool);
                        ifNode.setRight(ifStats);
                        break;
                }

                stat = ifNode;

                // End of if block
                switch(nextToken.getTokenID())
                {
                    case TEND:
                        nextToken = tokenList.pop();

                        // Process next statements
                        return nextStatement(nextToken, stat, tokenList, table);

                    // Missing end statement
                    case TMAIN:
                    case TFUNC:
                    case TCD18:
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing end after if statement."));
                        tokenList.push(nextToken);

                }
                break;

            // Else without if
            case TELSE:
                tokenList.push(nextToken);
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Else without if statement."));
                while(nextToken.getTokenID() != Token.TID.TSEMI && nextToken.getTokenID() != Token.TID.TEND && nextToken.getTokenID() != Token.TID.TUNTL && nextToken.getTokenID() != Token.TID.TELSE)
                {
                    nextToken = tokenList.pop();
                }
                stat = new NUndef(tokenList, table);

                nextToken = tokenList.pop();

                // Process next statements
                return nextStatement(nextToken, stat, tokenList, table);

            case TMAIN:
            case TFUNC:
            case TCD18:
                tokenList.push(nextToken);
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing end statement."));

            // Other invalid statements
            default:
                tokenList.push(nextToken);
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Invalid start of statement."));
                int line = nextToken.getLineNum();
                while(nextToken.getLineNum() == line)
                {
                    nextToken = tokenList.pop();
                }

                stat = new NUndef(tokenList, table);

                // Process next statements
                return nextStatement(nextToken, stat, tokenList, table);
        }
        return stat;
    }

    /** nextStatement()
     *
     * Processes a sub-tree of statements following the node of the argument
     *
     * @param nextToken - Token,
     * @param stat - STNode,
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode nextStatement(Token nextToken, STNode stat, LinkedList<Token> tokenList, SymbolTable table)
    {
        // Check for more statements
        if(nextToken.getTokenID() != Token.TID.TEND && nextToken.getTokenID() != Token.TID.TUNTL && nextToken.getTokenID() != Token.TID.TELSE  && nextToken.getTokenID() != Token.TID.TFUNC  && nextToken.getTokenID() != Token.TID.TMAIN && nextToken.getTokenID() != Token.TID.TCD18)
        {
            STNode stats;
            tokenList.push(nextToken);
            stats = new NStats(tokenList, table);
            stats.setLeft(stat);
            return stats;
        }
        else
        {
            tokenList.push(nextToken);
            return stat;
        }
    }

    /** processDlist()
     *
     * Processes a list of declarations for a function
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processDlist(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode decl;
        STNode dList;

        Token nextToken = tokenList.pop();
        Token tempToken = tokenList.pop();

		// Check for a colon in the definition
        switch(tempToken.getTokenID())
        {
            case TCOLN:
                tempToken = tokenList.pop();
                break;

            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \":\" after identifier."));
        }
		
		// Check the type
        switch(tempToken.getTokenID())
        {
            case TIDEN:
                tokenList.push(tempToken);
                tokenList.push(nextToken);
                decl = new NArrd(tokenList, table);
                break;

            case TINTG:
            case TREAL:
            case TBOOL:
                tokenList.push(tempToken);
                tokenList.push(nextToken);
                decl = new NSdecl(tokenList, table);
                break;

			// Invalid type	
            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected primitive type or type identifier."));
                decl = new NUndef(tokenList, table);
                tokenList.push(tempToken);
                tokenList.push(nextToken);
                while(nextToken.getTokenID() != Token.TID.TBEGN && nextToken.getTokenID() != Token.TID.TCOMA)
                {
                    nextToken = tokenList.pop();
                }
                tokenList.push(nextToken);
        }

        nextToken = tokenList.pop();

		// Check for more definitions
        switch(nextToken.getTokenID())
        {
            case TCOMA:
                nextToken = tokenList.pop();
				
                switch(nextToken.getTokenID())
                {
                    case TIDEN:
                        tokenList.push(nextToken);
                        dList = new NDlist(tokenList, table);
                        dList.setLeft(decl);
                        return dList;

                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected identifier in declaration list."));
                        while(nextToken.getTokenID() != Token.TID.TBEGN && nextToken.getTokenID() != Token.TID.TCOMA)
                        {
                            nextToken = tokenList.pop();
                        }
                        tokenList.push(nextToken);
                        if(nextToken.getTokenID() == Token.TID.TCOMA)
                        {
                            dList = new NDlist(tokenList, table);
                            dList.setLeft(decl);
                            return dList;
                        }
                        else
                            break;
                }
                break;

            case TBEGN:
                tokenList.push(nextToken);
                break;

            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Declarations not separated with \",\"."));
                tokenList.push(nextToken);
                dList = new NDlist(tokenList, table);
                dList.setLeft(decl);
                return dList;
        }

        return decl;
    }

    /** processPlist()
     *
     * Processes a list of parameters in a function definition
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processPlist(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode param;
        STNode pList;

        Token nextToken = tokenList.pop();
        Token tempToken;

        // Check the structure type of the first parameter
        switch(nextToken.getTokenID())
        {
            // Type is const array
            case TCNST:
                nextToken = tokenList.pop();

                // Check for parameter identifier
                switch(nextToken.getTokenID())
                {
                    case TIDEN:
                        tempToken = tokenList.pop();
                        switch(tempToken.getTokenID())
                        {
                            case TCOLN:
                                tokenList.push(nextToken);
                                param = new NArrc(tokenList, table);
                                table.addParamType(param.getLeft().getSymbol().getType());
                                break;

                            // Colon missing after identifier, throw an error and continue
                            default:
                                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \":\" after array identifier."));
                                while(nextToken.getTokenID() != Token.TID.TRPAR && nextToken.getTokenID() != Token.TID.TCOMA)
                                {
                                    nextToken = tokenList.pop();
                                }
                                tokenList.push(nextToken);
                                param = new NUndef(tokenList, table);
                        }
                        break;

                    // Array identifier is missing, throw an error and continue
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected array identifier."));
                        while(nextToken.getTokenID() != Token.TID.TRPAR && nextToken.getTokenID() != Token.TID.TCOMA)
                        {
                            nextToken = tokenList.pop();
                        }
                        tokenList.push(nextToken);
                        param = new NUndef(tokenList, table);
                }
                break;

            // Type is array or simple type
            case TIDEN:
                tempToken = tokenList.pop();

                switch(tempToken.getTokenID())
                {
                    // Check the type definition
                    case TCOLN:
                        tempToken = tokenList.pop();

                        tokenList.push(tempToken);
                        tokenList.push(nextToken);

                        switch(tempToken.getTokenID())
                        {
                            case TIDEN:
                                param = new NArrp(tokenList, table);
                                table.addParamType(param.getLeft().getSymbol().getType());
                                break;

                            case TINTG:
                            case TREAL:
                            case TBOOL:
                                param = new NSimp(tokenList, table);
                                table.addParamType(param.getLeft().getSymbol().getType());
                                break;

                            // Type is missing from parameter declaration, throw an error and continue
                            default:
                                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected primitive or array type identifier."));
                                while(nextToken.getTokenID() != Token.TID.TRPAR && nextToken.getTokenID() != Token.TID.TCOMA)
                                {
                                    nextToken = tokenList.pop();
                                }
                                tokenList.push(nextToken);
                                param = new NUndef(tokenList, table);
                        }
                        break;

                    // Colon is missing from parameter declaration, throw an error and continue
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \":\" after identifier."));
                        while(nextToken.getTokenID() != Token.TID.TRPAR && nextToken.getTokenID() != Token.TID.TCOMA)
                        {
                            nextToken = tokenList.pop();
                        }
                        tokenList.push(nextToken);
                        param = new NUndef(tokenList, table);
                }
                break;

            // Encountered an illegal character in parameter list, throw an error and continue
            default:
                errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected const, primitive or array type identifier."));
                tokenList.push(nextToken);
                while(nextToken.getTokenID() != Token.TID.TCNST && nextToken.getTokenID() != Token.TID.TRPAR && nextToken.getTokenID() != Token.TID.TCOMA)
                {
                    nextToken = tokenList.pop();
                }
                tokenList.push(nextToken);
                param = processPlist(tokenList, table);
        }

        nextToken = tokenList.pop();

        // Check for more parameters
        switch(nextToken.getTokenID())
        {
            // More parameters to process
            case TCOMA:
                nextToken = tokenList.pop();
                switch(nextToken.getTokenID())
                {
                    case TIDEN:
                        tokenList.push(nextToken);
                        pList = new NPlist(tokenList, table);
                        pList.setLeft(param);
                        return pList;

                    // Identifier is missing, throw an error and continue
                    default:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Expected identifier in parameter list."));
                        tokenList.push(nextToken);
                        while(nextToken.getTokenID() != Token.TID.TRPAR && nextToken.getTokenID() != Token.TID.TCOMA)
                        {
                            nextToken = tokenList.pop();
                        }
                }
                break;

            // End of parameter list
            case TRPAR:
                break;

            // Parameters not closed or comma missing
            default:
                while(nextToken.getTokenID() != Token.TID.TRPAR && nextToken.getTokenID() != Token.TID.TIDEN && nextToken.getTokenID() != Token.TID.TCOLN)
                {
                    nextToken = tokenList.pop();
                }

                // Skip to the next parameter or function type definition, throw an error and continue
                switch(nextToken.getTokenID())
                {
                    case TIDEN:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Parameters not separated with \",\"."));
                        tokenList.push(nextToken);
                        pList = new NPlist(tokenList, table);
                        pList.setLeft(param);
                        return pList;

                    case TCOLN:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \")\" in parameter list."));
                        tokenList.push(nextToken);
                        break;

                    default:
                }
        }

        return param;
    }

    /** processElist()
     *
     * Processes a list of arguments in a function call
     *
     * @param tokenList - LinkedList,
     * @param table - SymbolTable,
     * @return STNode,
     */
    protected STNode processElist(LinkedList<Token> tokenList, SymbolTable table)
    {
        STNode bool = processBool(tokenList, table);
        STNode eList;

        Token nextToken = tokenList.pop();
		
		// Check for more expressions
        switch(nextToken.getTokenID())
        {
            case TCOMA:
                eList = new NExpl(tokenList, table);
                eList.setLeft(bool);
                return eList;

            case TRPAR:
                break;

            default:
                while(nextToken.getTokenID() != Token.TID.TRPAR && nextToken.getTokenID() != Token.TID.TIDEN && nextToken.getTokenID() != Token.TID.TSEMI && nextToken.getTokenID() != Token.TID.TEND)
                {
                    nextToken = tokenList.pop();
                }
                tokenList.push(nextToken);
                switch(nextToken.getTokenID())
                {
                    case TIDEN:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Arguments not separated with \",\"."));
                        eList = new NExpl(tokenList, table);
                        eList.setLeft(bool);
                        return eList;

                    case TEND:
                    case TSEMI:
                        errorList.add(new SimpleEntry<>(nextToken, "Syntax Error: (" + nextToken.getLineNum() + ", " + nextToken.getColNum() + "): Missing \")\" in argument list."));
                        break;

                    default:
                }
        }

        return bool;
    }

    /** foldContants()
     *
     * Attempts to fold constant expressions into a single constant
     *
     */
	protected STNode foldConstants(STNode root, SymbolTable table)
    {
        NID rootID = root.getNodeID();

        if(rootID == NID.NSIMV || rootID == NID.NTRUE || rootID == NID.NFALS)
            root = propagateConstants(root, table);

        if(root.getLeft() == null || root.getRight() == null)
            return root;

        STNode leftChild = root.getLeft(), rightChild = root.getRight();

        STNode leftVal = parseNode(leftChild, table);
        STNode rightVal = parseNode(rightChild, table);

        Number result = calcNode(leftVal, rightVal, root);

        if(result instanceof Double)
        {
            root = new NFlit(new Token(Token.TID.TFLIT, leftVal.getSymbol().getLine(), leftVal.getSymbol().getCol(), result.toString()), table);
        }
        else if(result instanceof Integer)
        {
            root = new NIlit(new Token(Token.TID.TILIT, leftVal.getSymbol().getLine(), leftVal.getSymbol().getCol(), result.toString()), table);
        }
        else
        {
            TableEntry undef;
            if(leftVal.getNodeID() != NID.NFLIT && leftVal.getNodeID() != NID.NILIT)
                undef = leftVal.getSymbol();
            else
                undef = rightVal.getSymbol();
            errorList.add(new SimpleEntry<>(undef.getToken(), "Semantic Error: (" + undef.getLine() + ", " + undef.getCol() + "): Identifier not numeric."));

        }
        return root;
    }

    /** parseNode()
     *
     * Decides whether a node needs to be folded or substituted
     *
     */
    private STNode parseNode(STNode node, SymbolTable table)
    {
        if(node != null)
        {
            if(node.getNodeID() == NID.NILIT || node.getNodeID() == NID.NFLIT)
            {
                return node;
            }
            else if(node.getNodeID() == NID.NSIMV || node.getNodeID() == NID.NARRV)
            {
                return propagateConstants(node, table);
            }

            return foldConstants(node, table);
        }
        return null;
    }

    /** calcNode()
     *
     * Calculates a binary expression node and checks it semantically
     *
     */
    private Number calcNode(STNode leftVal, STNode rightVal, STNode root)
    {
        Number op1, op2, result = null;

        op1 = checkNumber(leftVal);
        if(op1 == null)
            return null;

        op2 = checkNumber(rightVal);
        if(op2 == null)
            return null;

        switch(root.getNodeID())
        {
            case NADD:

                if(leftVal.getNodeID() == NID.NFLIT || rightVal.getNodeID() == NID.NFLIT)
                    result = op1.doubleValue() + op2.doubleValue();
                else
                    result = op1.intValue() + op2.intValue();
                break;

            case NSUB:
                if(leftVal.getNodeID() == NID.NFLIT || rightVal.getNodeID() == NID.NFLIT)
                    result = op1.doubleValue() - op2.doubleValue();
                else
                    result = op1.intValue() - op2.intValue();
                break;

            case NDIV:
                if(op2.doubleValue() != 0.0)
                {
                    if(leftVal.getNodeID() == NID.NFLIT || rightVal.getNodeID() == NID.NFLIT)
                        result = op1.doubleValue() / op2.doubleValue();
                    else
                        result = op1.intValue() / op2.intValue();
                }
                else
                {
                    result = Double.NaN;
                    TableEntry symbol = root.getRight().getSymbol();
                    errorList.add(new SimpleEntry<>(symbol.getToken(), "Semantic Error: (" + symbol.getLine() + ", " + symbol.getCol() + "): Divide by zero error."));
                }
                break;

            case NMUL:
                if(leftVal.getNodeID() == NID.NFLIT || rightVal.getNodeID() == NID.NFLIT)
                    result = op1.doubleValue() * op2.doubleValue();
                else
                    result = op1.intValue() * op2.intValue();
                break;

            case NMOD:
                if(op2.doubleValue() != 0.0)
                {
                    if(leftVal.getNodeID() == NID.NILIT)
                    {
                        if(rightVal.getNodeID() != NID.NILIT)
                        {
                            result = Double.NaN;
                            TableEntry symbol = root.getRight().getSymbol();
                            errorList.add(new SimpleEntry<>(symbol.getToken(), "Semantic Error: (" + symbol.getLine() + ", " + symbol.getCol() + "): Illegal float literal in modulus operation."));
                        }
                        else
                        {
                            result = op1.intValue() % op2.intValue();
                        }
                    }
                    else
                    {
                        result = Double.NaN;
                        TableEntry symbol = root.getLeft().getSymbol();
                        errorList.add(new SimpleEntry<>(symbol.getToken(), "Semantic Error: (" + symbol.getLine() + ", " + symbol.getCol() + "): Illegal float literal in modulus operation."));
                    }
                }
                else
                {
                    result = Double.NaN;
                    TableEntry symbol = root.getRight().getSymbol();
                    errorList.add(new SimpleEntry<>(symbol.getToken(), "Semantic Error: (" + symbol.getLine() + ", " + symbol.getCol() + "): Divide by zero error."));
                }
                break;

            case NPOW:
                if(leftVal.getNodeID() == NID.NILIT && rightVal.getNodeID() == NID.NILIT)
                {
                    result = op1.intValue();
                    for(int i = op2.intValue(); i > 1; i--)
                    {
                        result = result.intValue() * op1.intValue();
                    }
                }
                else
                {
                    result = Double.NaN;
                    TableEntry symbol = getLeft().getRight().getSymbol();
                    errorList.add(new SimpleEntry<>(symbol.getToken(), "Semantic Error: (" + symbol.getLine() + ", " + symbol.getCol() + "): Illegal float literal in exponent operation."));
                }
                break;

            default:
        }
        return result;
    }

    /** checkNumber()
     *
     * Returns a symbol name as a number
     *
     */
    private Number checkNumber(STNode node)
    {
        if((node.getNodeID() == NID.NILIT))
        {
            return Integer.parseInt(node.getSymbol().getName());
        }
        else if((node.getNodeID() == NID.NFLIT))
        {
            return Double.parseDouble(node.getSymbol().getName());
        }
        else
            return null;
    }

    /** propagateConstants()
     *
     * Substitutes numeric values into variables
     *
     */
    protected STNode propagateConstants(STNode root, SymbolTable table)
    {
        switch(root.getNodeID())
        {
            case NSIMV:
                TableEntry entry;
                if(table.hasID(root.getSymbol().getName()))
                {
                    entry = table.getIdEntry(root.getSymbol().getName());
                    if(entry.getType().equals("integer"))
                        root = new NIlit(new Token(Token.TID.TILIT, root.getSymbol().getLine(), root.getSymbol().getCol(), entry.getValue()), table);
                    else if(entry.getType().equals("real"))
                        root = new NFlit(new Token(Token.TID.TFLIT, root.getSymbol().getLine(), root.getSymbol().getCol(), entry.getValue()), table);
                }
                break;

            case NTRUE:
            case NFALS:
                errorList.add(new SimpleEntry<>(symbol.getToken(), "Semantic Error: (" + symbol.getLine() + ", " + symbol.getCol() + "): Illegal boolean in numeric expression."));
                break;

            default:

        }
        return root;
    }

    /** getNumArguments()
     *
     * Calculates the number of arguments a function call has
     *
     */
    protected int getNumArguments(STNode eList, int count)
    {
        count++;
        if(eList.getRight() != null)
        {
            return getNumArguments(eList.getRight(), count);
        }
        else
            return count;
    }
}
