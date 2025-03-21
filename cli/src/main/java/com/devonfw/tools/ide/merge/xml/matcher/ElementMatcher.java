package com.devonfw.tools.ide.merge.xml.matcher;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xml.XmlMergeSupport;

/**
 * The ElementMatcher class is responsible for matching XML elements in a target document based on the provided update elements.
 */
public class ElementMatcher {

  private final IdeContext context;

  private final Map<QName, String> qName2IdMap;

  private final Map<String, IdComputer> id2ComputerMap;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public ElementMatcher(IdeContext context) {

    this.context = context;
    this.qName2IdMap = new HashMap<>();
    this.id2ComputerMap = new HashMap<>();
  }

  private IdComputer getIdComputer(Element element) {

    QName qName = XmlMergeSupport.getQualifiedName(element);
    String id = XmlMergeSupport.getMergeId(element);
    if ((id == null) || id.isEmpty()) {
      id = this.qName2IdMap.get(qName);
      if (id == null) {
        throw new IllegalStateException(
            "Attribute merge:id is required for XML element " + XmlMergeSupport.getXPath(element, true));
      }
    } else {
      this.qName2IdMap.putIfAbsent(qName, id);
    }
    return this.id2ComputerMap.computeIfAbsent(id, i -> new IdComputer(i, context));
  }

  /**
   * Looks for an element matching the source element inside the target element.
   *
   * @param templateElement the template {@link Element} to be matched.
   * @param workspaceElement the workspace {@link Element} in which to match the template {@link Element}.
   * @return the matched {@link Element} if found, or {@code null} if not found
   */
  public Element matchElement(Element templateElement, Element workspaceElement) {

    IdComputer idComputer = getIdComputer(templateElement);
    return idComputer.evaluateExpression(templateElement, workspaceElement);
  }
}
