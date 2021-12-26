package nl.bryanderidder.wicketfolding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.properties.PropertiesImplUtil
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiImmediateClassType
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.util.PsiTreeUtil
import org.codehaus.plexus.util.StringUtils


class CustomFold : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, b: Boolean): Array<FoldingDescriptor> {
        val descriptors = ArrayList<FoldingDescriptor>()

        // only java files
        if (root !is PsiJavaFile)
            return descriptors.toTypedArray()
        // only try this if there is a BigDecimal import statement
        if (PsiTreeUtil.findChildrenOfType(root, PsiImportStatement::class.java).any { "org.apache.wicket.model.StringResourceModel" == it.qualifiedName }) {

            root.accept(object : JavaRecursiveElementWalkingVisitor() {

                override fun visitNewExpression(expression: PsiNewExpression?) {
                    if (expression?.text?.contains("new StringResourceModel") == true)
                        addFoldDescriptor(expression)
                    super.visitNewExpression(expression)
                }

                private fun addFoldDescriptor(expression: PsiNewExpression) {
                    val expressions = expression.argumentList?.expressions
                    if (expressions.isNullOrEmpty())
                        return
                    if (expressions.size == 1)
                        return // get replacement for current file ... resourceKey.containingFile
                    if (expressions.size == 2 && expressions[1] is PsiThisExpression) {
                        val resourceKey = expressions[0].text.replace("\"", "")
                        var placeholder = resourceKey
                        val className = ((expressions[1] as PsiThisExpression).type as PsiImmediateClassType).className
                        val dir = PsiTreeUtil.getParentOfType(expressions[1], PsiClass::class.java)?.containingFile?.containingDirectory?.virtualFile
                        if (dir != null) {
                            val scope = GlobalSearchScopesCore.DirectoryScope(expression.project, dir, false)
                            val files = FilenameIndex.getFilesByName(expression.project, className + ".properties", scope)
                            if (!files.isNullOrEmpty())
                                placeholder = PropertiesImplUtil.getPropertiesFile(files[0])?.namesMap?.get<Any?, String?>(resourceKey) ?: resourceKey
                        }
                        descriptors.add(
                            FoldingDescriptor(
                                expression.node,
                                TextRange(expression.textRange.startOffset, expression.textRange.endOffset),
                                FoldingGroup.newGroup("test"),
                                StringUtils.abbreviate(placeholder, 20)
                            )
                        )
                    }
                }
            })
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(astNode: ASTNode): String {
        return "..."
    }

    override fun isCollapsedByDefault(astNode: ASTNode): Boolean = true
}