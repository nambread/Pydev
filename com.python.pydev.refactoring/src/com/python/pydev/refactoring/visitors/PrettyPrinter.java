/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.Writer;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Assign;
import org.python.parser.ast.BinOp;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.If;
import org.python.parser.ast.Name;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.Num;
import org.python.parser.ast.Pass;
import org.python.parser.ast.UnaryOp;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.exprType;

public class PrettyPrinter extends VisitorBase{

    protected PrettyPrinterPrefs prefs;
    private Writer writer;
    private WriteState state;
    private AuxSpecials auxComment;

    public PrettyPrinter(PrettyPrinterPrefs prefs, Writer writer){
        this.prefs = prefs;
        this.writer = writer;
        state = new WriteState(writer, prefs);
        auxComment = new AuxSpecials(state, writer, prefs);
    }
    
    @Override
    public Object visitAssign(Assign node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        for (SimpleNode target : node.targets) {
            target.accept(this);
        }
        writer.write(" = ");
        auxComment.startRecord();
        node.value.accept(this);
        auxComment.writeSpecialsAfter(node);
        
        if(!auxComment.endRecord().writtenComment){
            state.writeNewLine();
        }
        return null;
    }
    
    @Override
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        writer.write(node.operand.toString());
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    
    public static final String[] operatorMapping = new String[] {
        "<undef>",
        " + ",
        " - ",
        " * ",
        " / ",
        " % ",
        " ** ",
        " << ",
        " >> ",
        " | ",
        " ^ ",
        " & ",
        " // ",
    };

    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        node.left.accept(this);
        writer.write(operatorMapping[node.op]);
        node.right.accept(this);
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    @Override
    public Object visitNum(Num node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        writer.write(node.n.toString());
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    @Override
    public Object visitIf(If node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        node.test.accept(this);
        state.indent();
        state.writeNewLine();
        state.writeIndent();
        for (SimpleNode n : node.body){
            auxComment.writeSpecialsBefore(n);
            n.accept(this);
            auxComment.writeSpecialsAfter(n);
        }
        state.dedent();
        
        
        if(node.orelse != null && node.orelse.length > 0){
            auxComment.writeSpecialsAfter(node);
            if(node.specialsAfter.contains("else:")){
                state.indent();
                state.writeNewLine();
                state.writeIndent();
            }
            for (SimpleNode n : node.orelse) {
                auxComment.writeSpecialsBefore(n);
                n.accept(this);
//                auxComment.writeSpecialsAfter(n); // same as the initial
            }
        }
        
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        auxComment.writeSpecialsBefore(node.name);
        auxComment.writeSpecialsBefore(node);
        writer.write("class ");
        
        
        NameTok name = (NameTok) node.name;

        auxComment.startRecord();
        writer.write(name.id);
        //we want the comments to be indented too
        state.indent();
        {
            auxComment.writeSpecialsAfter(name);
    
            if(node.bases.length > 0){
                writer.write("(");
                for (exprType expr : node.bases) {
                    expr.accept(this);
                }
            }
            if(!auxComment.endRecord().writtenComment){
                state.writeNewLine();
            }
            for(SimpleNode n: node.body){
                n.accept(this);
            }
        
            state.dedent();
        }   
        auxComment.writeSpecialsAfter(node);
        return null;
    }

    

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        auxComment.writeSpecialsBefore(node.name);
        state.writeIndent();
        writer.write("def ");
        
        
        NameTok name = (NameTok) node.name;
        writer.write(name.id);
        writer.write("(");

        //arguments
        boolean writtenNewLine = makeArgs(node.args.args);
        //end arguments
        
        if(!writtenNewLine){
            state.writeNewLine();
        }
        
        state.indent();
        {
            for(SimpleNode n: node.body){
                n.accept(this);
            }
        
            state.dedent();
        }
        auxComment.writeSpecialsAfter(node.name);
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    private boolean makeArgs(exprType[] args) throws Exception {
        exprType prev = null;
        boolean written = false;
        for (exprType type : args) {
            auxComment.startRecord();
            if(prev != null && prev.specialsAfter.size() > 1){
                //has some comment (not only ',')
                state.writeIndent(1);
            }
            type.accept(this);
            prev = type;
            written = auxComment.endRecord().writtenComment;
        }
        return written;
    }

    @Override
    public Object visitPass(Pass node) throws Exception {
        state.writeIndent();
        auxComment.writeSpecialsBefore(node);
        writer.write("pass");
        auxComment.writeSpecialsAfter(node);
        state.writeNewLine();
        return null;
    }
    
    @Override
    public Object visitName(Name node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        writer.write(node.id);
        auxComment.writeStringsAfter(node);
        auxComment.writeCommentsAfter(node);
        return null;
    }
    
    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        writer.write(node.id);
        auxComment.writeSpecialsAfter(node);
        return null;
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

}
