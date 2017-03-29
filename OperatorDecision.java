
package ptolemy.actor.lib.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import mapss.graph.hierarchy.Port;
import ptolemy.actor.IOPort;
import ptolemy.actor.NoRoomException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.ASTPtRootNode;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.ModelScope;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.ParseTreeEvaluator;
import ptolemy.data.expr.ParseTreeFreeVariableCollector;
import ptolemy.data.expr.ParseTreeTypeInference;
import ptolemy.data.expr.PtParser;
import ptolemy.data.expr.Variable;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.MonotonicFunction;
import ptolemy.data.type.Type;
import ptolemy.data.type.TypeConstant;
import ptolemy.graph.InequalityTerm;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;
import ptolemy.data.expr.StringParameter;


public class OperatorDecision extends TypedAtomicActor {


    public OperatorDecision(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
        super(container, name);
        // TODO Auto-generated constructor stub
        //output = new TypedIOPort(this,"output",false,true);
        //output.setTypeEquals(BaseType.STRING);
        file = new FileParameter(this,"file");
    }

    //public TypedIOPort output;
    public FileParameter file;

    
    @Override
    public void attributeChanged(Attribute attribute)
            throws IllegalActionException {
   /*     if (attribute == rule) {
            _parseTree = null;
        }*/
    }

 
    @Override
    public Object clone(Workspace workspace) throws CloneNotSupportedException {
        OperatorDecision newObject = (OperatorDecision) super.clone(workspace);
        newObject._iterationCount = 1;
        newObject._parseTree = null;
        newObject._parseTreeEvaluator = null;
        newObject._scope = null;
        newObject._setOutputTypeConstraint();
        newObject._tokenMap = null;
        return newObject;
    }
    
    public class ConditonExpression{
        public ConditonExpression(int conditionIndex, int conditionId, String condition){
            this.conditionId = conditionId;
            this.condition = condition; 
            this.conditionIndex = conditionIndex;

        }
        public int conditionIndex;
        public int conditionId;
        public String condition;
        List<OutputVariable> output = new ArrayList<OutputVariable>();
        public void addOutput(String variable, String trueAction, String falseAction, String select){
            output.add(new OutputVariable(variable,trueAction,falseAction,select));
        }
        
    }
    public static class OutputVariable{
        OutputVariable(String variable, String trueAction, String falseAction, String select){
            this.variable = variable;
            this.trueAction = trueAction;
            this.falseAction = falseAction;
            this.select = select;
        }
        String variable;
        String trueAction;
        String falseAction;
        String select;
    }
    
    
    public Token getExpressionApi( String str) throws IllegalActionException{
        Token answer;
        PtParser parser = new PtParser();
        _parseTree = parser.generateParseTree(str);
        _parseTreeEvaluator = new ParseTreeEvaluator();
         _scope = new VariableScope();
         answer = _parseTreeEvaluator.evaluateParseTree(_parseTree, _scope);
         return answer;
    }
    List<ConditonExpression> conditionList = new ArrayList<ConditonExpression>();
    public void SetConditionsInformation(HSSFSheet readSheet){
        conditionList.clear();
        int count = 0;
        int nRows = readSheet.getPhysicalNumberOfRows();
        for(int i = 1 ; i<=nRows ; i++){
            HSSFRow row = readSheet.getRow(i);
            try{
                if(row.getCell(0)!=null){
                        count++;
                        ConditonExpression c = new ConditonExpression(i,count,row.getCell(0).toString());
                        conditionList.add(c);
                }
            }
             catch(Exception exception){
                System.out.println(exception);
                continue;
            }
        }
    }
    public String[] ChangeNullCellToEmptyString(HSSFRow row){
       String [] str = new String[4];
       
       for(int i = 1 ; i <= 4 ; i++){
         if(row.getCell(i)!=null)
             str[i-1] = row.getCell(i).toString();
         else{
               str[i-1] =  "\"\"";
             }
       }
       return str;
        
    }
    public void SetOutputInformation(HSSFSheet readSheet){
        int NumberOfOutput = 0;
        for(int i = 0 ; i < conditionList.size()   ; i++){
            if(i!=conditionList.size()-1)
                NumberOfOutput = conditionList.get(i+1).conditionIndex - conditionList.get(i).conditionIndex;
            else
                NumberOfOutput = readSheet.getPhysicalNumberOfRows() - conditionList.get(i).conditionIndex;
            for(int j = 0 ; j < NumberOfOutput ; j++){
                HSSFRow row = readSheet.getRow(conditionList.get(i).conditionIndex+j);
                String []str = ChangeNullCellToEmptyString(row);
                
                 /*conditionList.get(i).addOutput(row.getCell(1).toString(),
                                               row.getCell(2).toString(),
                                               row.getCell(3).toString());
                                               */
                //conditionList.get(i).addOutput(str[0],str[1],str[2], row.getCell(4).toString());
                conditionList.get(i).addOutput(str[0],str[1],str[2], str[3]);
            }
        }
    }
    public void WhatToSend(int i, Map<String,Integer>outputList, boolean decision) throws NoRoomException, IllegalActionException{
        for(int j = 0 ; j < conditionList.get(i).output.size() ; j++){
            OutputVariable variablePair = conditionList.get(i).output.get(j);
            if(outputList.containsKey(variablePair.variable)){
                int getIndex = outputList.get(variablePair.variable);
                this.outputPortList().get(getIndex).setTypeEquals(BaseType.STRING);
                if(decision && variablePair.select.toString()=="TRUE")
                {    
                   if(variablePair.trueAction!="\"\"")
                   {   
                       
                       this.outputPortList().get(getIndex).send(channelConflict[getIndex], getExpressionApi(variablePair.trueAction));
                       channelConflict[getIndex]++;
                   }
                }
                else if(!decision && variablePair.select.toString()=="TRUE")
                {   
                    if(variablePair.falseAction!="\"\"")
                    {    
                        this.outputPortList().get(getIndex).send(channelConflict[getIndex], getExpressionApi(variablePair.falseAction));
                        channelConflict[getIndex]++;
                    }
                }
                else{
                    continue;
                }
            }
            
        }
    }
    
   
    @Override
    public void fire() throws IllegalActionException {
        super.fire();

        Iterator inputPorts = inputPortList().iterator();
        while (inputPorts.hasNext()) {
            IOPort port = (IOPort) inputPorts.next();
            // FIXME: Handle multiports
            if (port.isOutsideConnected()) {
                if (port.hasToken(0)) {
                    Token inputToken = port.get(0);
                    _tokenMap.put(port.getName(), inputToken);
                } else {
                    throw new IllegalActionException(this, "Input port "
                            + port.getName() + " has no data.");
                }
            }
        }
        try{
            FileInputStream ExcelFileToRead = new FileInputStream(file.getExpression());
            HSSFWorkbook readWorkbook = new HSSFWorkbook(ExcelFileToRead);
            HSSFSheet readSheet = readWorkbook.getSheetAt(0);
            channelConflict = new int[readSheet.getPhysicalNumberOfRows()];

            SetConditionsInformation(readSheet);
            SetOutputInformation(readSheet);
            Map<String,Integer> outputList = new HashMap<>();
            
            for(int i = 0 ; i < this.outputPortList().size(); i++)
            {
                outputList.put(this.outputPortList().get(i).getDisplayName(), i);
            }
            for(int i = 0 ; i < conditionList.size() ; i++){

                if(getExpressionApi(conditionList.get(i).condition).toString()=="true")
                    WhatToSend(i,outputList, true); 
                else
                    WhatToSend(i,outputList, false);
            }
                    
        }catch(IOException ioexception){
                throw new IllegalActionException(this,ioexception,"File not found");
        }
    }

    @Override
    public void initialize() throws IllegalActionException {
        super.initialize();
        
        if (getPort("time") != null) {
            throw new IllegalActionException(
                    this,
                    "This actor has a port named \"time\", "
                            + "which will not be read, instead the "
                            + "reserved system variable \"time\" will be read. "
                            + "Delete the \"time\" port to avoid this message.");
        }
        if (getPort("iteration") != null) {
            throw new IllegalActionException(
                    this,
                    "This actor has a port named \"iteration\", "
                            + "which will not be read, instead the "
                            + "reserved system variable \"iteration\" will be read. "
                            + "Delete the \"iteration\" port to avoid this message.");
        }
        _iterationCount = 1;
    }

    /** Increment the iteration count.
     *  @exception IllegalActionException If the superclass throws it.
     */
    @Override
    public boolean postfire() throws IllegalActionException {
        _iterationCount++;

        return super.postfire();
    }

    @Override
    public boolean prefire() throws IllegalActionException {
        Iterator inputPorts = inputPortList().iterator();

        while (inputPorts.hasNext()) {
            IOPort port = (IOPort) inputPorts.next();

            // FIXME: Handle multiports
            if (port.isOutsideConnected()) {
                if (!port.hasToken(0)) {
                    return false;
                }
            }
        }

        return super.prefire();
    }

    /** Preinitialize this actor.
     */
    @Override
    public void preinitialize() throws IllegalActionException {
        super.preinitialize();
        _tokenMap = new HashMap<String, Token>();
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected variables               ////

    /** Variable storing the result of the expression evaluation so that
     *  subclasses can access it in an overridden fire() method.
     */
    public Token _result;

    /** Map from input port name to input value.
     *  The fire() method populates this map.
     *  This is protected so that if a subclass overrides fire(), it
     *  can determine the values of the inputs.
     */
    protected Map<String, Token> _tokenMap;

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////
    // Add a constraint to the type output port of this object.
    private void _setOutputTypeConstraint() {
        //output.setTypeAtLeast(new OutputTypeFunction());
    }

    public class VariableScope extends ModelScope {
        /** Look up and return the attribute with the specified name in the
         *  scope. Return null if such an attribute does not exist.
         *  @return The attribute with the specified name in the scope.
         */
        @Override
        public Token get(String name) throws IllegalActionException {
            if (name.equals("time")) {
                return new DoubleToken(getDirector().getModelTime()
                        .getDoubleValue());
            } else if (name.equals("iteration")) {
                return new IntToken(_iterationCount);
            }

            Token token = _tokenMap.get(name);

            if (token != null) {
                return token;
            }

            Variable result = getScopedVariable(null, OperatorDecision.this, name);

            if (result != null) {
                return result.getToken();
            }

            return null;
        }

        /** Look up and return the type of the attribute with the
         *  specified name in the scope. Return null if such an
         *  attribute does not exist.
         *  @return The attribute with the specified name in the scope.
         */
        @Override
        public Type getType(String name) throws IllegalActionException {
            if (name.equals("time")) {
                return BaseType.DOUBLE;
            } else if (name.equals("iteration")) {
                return BaseType.INT;
            }

            // Check the port names.
            TypedIOPort port = (TypedIOPort) getPort(name);

            if (port != null) {
                return port.getType();
            }

            Variable result = getScopedVariable(null, OperatorDecision.this, name);

            if (result != null) {
                return (Type) result.getTypeTerm().getValue();
            }

            return null;
        }

        /** Look up and return the type term for the specified name
         *  in the scope. Return null if the name is not defined in this
         *  scope, or is a constant type.
         *  @return The InequalityTerm associated with the given name in
         *  the scope.
         *  @exception IllegalActionException If a value in the scope
         *  exists with the given name, but cannot be evaluated.
         */
        @Override
        public ptolemy.graph.InequalityTerm getTypeTerm(String name)
                throws IllegalActionException {
            if (name.equals("time")) {
                return new TypeConstant(BaseType.DOUBLE);
            } else if (name.equals("iteration")) {
                return new TypeConstant(BaseType.INT);
            }

            // Check the port names.
            TypedIOPort port = (TypedIOPort) getPort(name);

            if (port != null) {
                return port.getTypeTerm();
            }

            Variable result = getScopedVariable(null, OperatorDecision.this, name);

            if (result != null) {
                return result.getTypeTerm();
            }

            return null;
        }

        /** Return the list of identifiers within the scope.
         *  @return The list of identifiers within the scope.
         */
        @Override
        public Set identifierSet() {
            return getAllScopedVariableNames(null, OperatorDecision.this);
        }
    }

    // This class implements a monotonic function of the type of
    // the output port.
    // The function value is determined by type inference on the
    // expression, in the scope of this Expression actor.
    private class OutputTypeFunction extends MonotonicFunction {
        ///////////////////////////////////////////////////////////////
        ////                       public inner methods            ////

        /** Return the function result.
         *  @return A Type.
         *  @exception IllegalActionException If inferring types for the
         *  expression fails.
         */
        @Override
        public Object getValue() throws IllegalActionException {
            try {
                // Deal with the singularity at UNKNOWN..  Assume that if
                // any variable that the expression depends on is UNKNOWN,
                // then the type of the whole expression is unknown..
                // This allows us to properly find functions that do exist
                // (but not for UNKNOWN arguments), and to give good error
                // messages when functions are not found.
                InequalityTerm[] terms = getVariables();

                for (InequalityTerm term : terms) {
                    if (term != this && term.getValue() == BaseType.UNKNOWN) {
                        return BaseType.UNKNOWN;
                    }
                }

                // Note: This code is similar to the token evaluation
                // code above.
             /*   if (_parseTree == null) {
                    // Note that the parser is NOT retained, since in most
                    // cases the expression doesn't change, and the parser
                    // requires a large amount of memory.
                    PtParser parser = new PtParser();
                    _parseTree = parser.generateParseTree(rule
                            .getExpression());
                }
*/
                if (_scope == null) {
                    _scope = new VariableScope();
                }

                Type type = _typeInference.inferTypes(_parseTree, _scope);
                return type;
            } catch (Exception ex) {
                throw new IllegalActionException(OperatorDecision.this, ex,
                        "An error occurred during expression type inference of \""
                               /* + rule.getExpression()*/ + "\".");
            }
        }

        /** Return the type variable in this inequality term. If the type
         *  of input ports are not declared, return an one element array
         *  containing the inequality term representing the type of the port;
         *  otherwise, return an empty array.
         *  @return An array of InequalityTerm.
         */
        @Override
        public InequalityTerm[] getVariables() {
            // Return an array that contains type terms for all of the
            // inputs and all of the parameters that are free variables for
            // the expression.
            try {
               /* if (_parseTree == null) {
                    PtParser parser = new PtParser();
                    _parseTree = parser.generateParseTree(rule
                            .getExpression());
                }*/

                if (_scope == null) {
                    _scope = new VariableScope();
                }

                Set set = _variableCollector.collectFreeVariables(_parseTree,
                        _scope);
                List termList = new LinkedList();

                for (Iterator elements = set.iterator(); elements.hasNext();) {
                    String name = (String) elements.next();
                    InequalityTerm term = _scope.getTypeTerm(name);

                    if (term != null && term.isSettable()) {
                        termList.add(term);
                    }
                }

                return (InequalityTerm[]) termList
                        .toArray(new InequalityTerm[termList.size()]);
            } catch (IllegalActionException ex) {
                return new InequalityTerm[0];
            }
        }

        /** Override the base class to give a description of this term.
         *  @return A description of this term.
         */
        /*@Override
        public String getVerboseString() {
            return rule.getExpression();
        }*/

        ///////////////////////////////////////////////////////////////
        ////                       private inner variable          ////
        private ParseTreeTypeInference _typeInference = new ParseTreeTypeInference();

        private ParseTreeFreeVariableCollector _variableCollector = new ParseTreeFreeVariableCollector();
    }

    ///////////////////////////////////////////////////////////////////
    ////                                       ////
    public int _iterationCount = 1;

    public ASTPtRootNode _parseTree = null;

    public ParseTreeEvaluator _parseTreeEvaluator = null;

    public VariableScope _scope = null;
    
    public int []channelConflict;
    
}
