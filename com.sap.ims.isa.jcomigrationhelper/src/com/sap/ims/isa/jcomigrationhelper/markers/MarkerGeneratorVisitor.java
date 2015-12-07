package com.sap.ims.isa.jcomigrationhelper.markers;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;

import com.sap.ims.isa.jcomigrationhelper.internal.utils.JavaEditorUtils;

public class MarkerGeneratorVisitor extends ASTVisitor {

    /**
     * The document the visitor is working with. It is mostly used to get the line of the document.
     */
    private CompilationUnit document;

    /**
     * The resource where to add the marker. I was not able to get this information from the cumpilation unit.
     */
    private IResource res;

    /**
     * The visitor requires for its work the document and the resource. If the values are not set, then a
     * {@link NullPointerException} will be thrown!
     * @param document the document the AST has been created on.
     * @param res The resource which was used to generate the AST.
     * @throws NullPointerException If one of the arguments is null.
     */
    public MarkerGeneratorVisitor(CompilationUnit document, IResource res) {
        Objects.requireNonNull(document, "The document must not be null!");
        Objects.requireNonNull(res, "The resource must not be null!");
        this.document = document;
        this.res = res;
    }

    /**
     * Will be called if the visit of {@link VariableDeclarationStatement} or of the {@link FieldDeclaration} returned <code>true</code>.
     * @return Always <code>false</code>, because no further visits are required.
     */
    @Override
    public boolean visit(VariableDeclarationFragment node) {
        boolean isSupported = false;
        Type parentType = null;

        if(node.getInitializer() instanceof MethodInvocation) {
            MethodInvocation methodUsedToInitialize = (MethodInvocation) node.getInitializer();
            isSupported = JavaEditorUtils.isMethodSupported(methodUsedToInitialize.getName().getFullyQualifiedName());
            if(!isSupported) {
                ASTNode parent = node.getParent();
                if(parent instanceof VariableDeclarationStatement) {
                    parentType = ((VariableDeclarationStatement)parent).getType();
                }
            }
        } else if(node.getParent() instanceof FieldDeclaration) {
            parentType = ((FieldDeclaration)node.getParent()).getType();
        } else if(node.getParent() instanceof VariableDeclarationStatement) {
            parentType = ((VariableDeclarationStatement)node.getParent()).getType();
        }
        if(parentType != null && parentType.isSimpleType()) {
            isSupported = JavaEditorUtils.isJCoTypeSupported(((SimpleType)parentType).getName().getFullyQualifiedName());
        }

        if(isSupported) {
            SourceRange sourceRange = new SourceRange(node.getStartPosition(), node.getName().getFullyQualifiedName().length());
            // check if the variable is used somewhere
            ASTNode root = node.getRoot();
            ASTNode selNode = NodeFinder.perform(root, sourceRange.getOffset(), 1);
            ASTNode[] sameNodes = LinkedNodeFinder.findByNode(root, (SimpleName) selNode);
            if (Arrays.stream(sameNodes).anyMatch(n -> JavaEditorUtils.isValidForProcessing(n))) {
                int lineNum = this.document.getLineNumber(sourceRange.getOffset());
                JCoMarkerFactory.createMarker(this.res, sourceRange, lineNum, null);
            }
        }

        return false;
    }


    /**
     * Will be used if an invocation like invocation could be a call like <code>function.getImportParameterList().setValue("NAME", "VALUE")</code>
     * is used. Then the value should also be marked.
     */
    @Override
    public boolean visit(MethodInvocation node) {
        ASTNode parent = node.getParent();
        int nType = parent.getNodeType();
        if(nType == ASTNode.EXPRESSION_STATEMENT // the value is usually 21
                && node.getName().getFullyQualifiedName().equals("setValue")
                && node.getExpression() instanceof MethodInvocation) {
            // this invocation could be a call like function.getImportParameterList().setValue("NAME", "VALUE")
            MethodInvocation fromMethod = (MethodInvocation)node.getExpression();
            String methodName = fromMethod.getName().getFullyQualifiedName();
            if(JavaEditorUtils.isMethodSupported(methodName)) {
                int start = fromMethod.getName().getStartPosition();
                SourceRange sourceRange = new SourceRange(start, methodName.length());
                int lineNum = this.document.getLineNumber(start);
                JCoMarkerFactory.createMarker(this.res, sourceRange, lineNum, null);
                return true;
            }
        }
        return false;
    }

    /**
     * Marks also variables which ware method parameters.
     */
    @Override
    public boolean visit(MethodDeclaration node) {
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> parameters = node.parameters();
        if(parameters.size() > 0) {
            parameters.forEach(param -> {
                Type type = param.getType();
                if(!param.isVarargs() && param.getType().isSimpleType() && JavaEditorUtils.isJCoTypeSupported(((SimpleType)type).getName().getFullyQualifiedName())) {
                    SimpleName varName = param.getName();
                    SourceRange sourceRange = new SourceRange(varName.getStartPosition(), varName.getFullyQualifiedName().length());
                    int lineNum = this.document.getLineNumber(sourceRange.getOffset());
                    JCoMarkerFactory.createMarker(this.res, sourceRange, lineNum, null);
                }
            });
        }

        return true;
    }

    @Override
    public String toString() {
        return "MarkerGeneratorVisitor [document=" + this.document + ", res=" + this.res + "]";
    };

}
