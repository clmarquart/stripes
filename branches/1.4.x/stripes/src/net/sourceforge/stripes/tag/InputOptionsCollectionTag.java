/* Copyright 2005-2006 Tim Fennell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.stripes.tag;

import net.sourceforge.stripes.exception.StripesJspException;
import net.sourceforge.stripes.localization.LocalizationUtility;
import net.sourceforge.stripes.util.bean.BeanUtil;
import net.sourceforge.stripes.util.bean.ExpressionException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import java.util.Collection;
import java.util.Locale;

/**
 * <p>Writes a set of {@literal <option value="foo">bar</option>} tags to the page based on the
 * contents of a Collection.  Each element in the collection is represented by a single option
 * tag on the page.  Uses the label and value attributes on the tag to name the properties of the
 * objects in the Collection that should be used to generate the body of the HTML option tag and
 * the value attribute of the HTML option tag respectively.</p>
 *
 * <p>E.g. a tag declaration that looks like:</p>
 *   <pre>{@literal <stripes:options-collection collection="${cats} value="catId" label="name"/>}</pre>
 *
 * <p>would cause the container to look for a Collection called "cats" across the various JSP
 * scopes and set it on the tag.  The tag would then proceed to iterate through that collection
 * calling getCatId() and getName() on each cat to produce HTML option tags.</p>
 *
 * <p>The tag will attempt to localize the labels attributes of the option tags that are
 * generated. To do this it will look up labels in the field resource bundle using:</p>
 *
 * <ul>
 *   <li>{className}.{labelPropertyValue}</li>
 *   <li>{packageName}.{className}.{labelPropertyValue}</li>
 *   <li>{className}.{valuePropertyValue}</li>
 *   <li>{packageName}.{className}.{valuePropertyValue}</li>
 * </ul>
 *
 * <p>For example for a class com.myco.Gender supplied to the options-collection tag with
 * label="key" and value="description", when rendering for an instance
 * Gender[key="M", description="Male"] the following localized properites will be looked for:
 *
 * <ul>
 *   <li>Gender.Male</li>
 *   <li>com.myco.Gender.Male</li>
 *   <li>Gender.M</li>
 *   <li>com.myco.Gender.M</li>
 * </ul>
 *
 * <p>If no localized label can be found then the value of the label property will be used.</p>
 *
 * <p>All other attributes on the tag (other than collection, value and label) are passed directly
 * through to the InputOptionTag which is used to generate the individual HTML options tags. As a
 * result the InputOptionsCollectionTag will exhibit the same re-population/selection behaviour
 * as the regular options tag.</p>
 *
 * <p>Since the tag has no use for one it does not allow a body.</p>
 *
 * @author Tim Fennell
 */
public class InputOptionsCollectionTag extends HtmlTagSupport implements Tag {
    private Collection collection;
    private String value;
    private String label;

    /** Sets the collection that will be used to generate options. */
    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    /** Returns the value set with setCollection(). */
    public Collection getCollection() {
        return this.collection;
    }

    /**
     * Sets the name of the property that will be fetched on each bean in the collection in
     * order to generate the value attribute of each option.
     *
     * @param value the name of the attribute
     */
    public void setValue(String value) {
        this.value = value;
    }

    /** Returns the property name set with setValue(). */
    public String getValue() {
        return value;
    }

    /**
     * Sets the name of the property that will be fetched on each bean in the collection in
     * order to generate the body of each option (i.e. what is seen by the user).
     *
     * @param label the name of the attribute
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /** Gets the property name set with setLabel(). */
    public String getLabel() {
        return label;
    }


    /**
     * Iterates through the collection and uses an instance of InputOptionTag to generate each
     * individual option with the correct state.  It is assumed that each element in the collection
     * has non-null values for the properties specified for generating the label and value.
     *
     * @return SKIP_BODY in all cases
     * @throws JspException if either the label or value attributes specify properties that are
     *         not present on the beans in the collection, or output cannot be written.
     */
    public int doStartTag() throws JspException {
        String labelProperty = getLabel();
        String valueProperty = getValue();

        Locale locale = getPageContext().getRequest().getLocale();
        InputOptionTag tag = new InputOptionTag();
        tag.setParent(this);
        tag.setPageContext(getPageContext());
        tag.getAttributes().putAll(getAttributes());

        try {
            for (Object item : this.collection) {
                Class clazz = item.getClass();

                // Lookup the bean properties for the label and value
                Object label = (labelProperty == null) ? item : BeanUtil.getPropertyValue(labelProperty, item);
                Object value = (valueProperty == null) ? item : BeanUtil.getPropertyValue(valueProperty, item);

                // Try to localize the label
                String localizedLabel = null;
                if (label != null) {
                    localizedLabel = LocalizationUtility.getLocalizedFieldName
                        (clazz.getSimpleName() + "."  + label, clazz.getPackage().getName(), locale);
                }
                if (localizedLabel == null && value != null) {
                    localizedLabel = LocalizationUtility.getLocalizedFieldName
                        (clazz.getSimpleName() + "."  + value, clazz.getPackage().getName(), locale);
                }
                if (localizedLabel != null) label = localizedLabel;

                tag.setLabel(label.toString());
                tag.setValue(value);
				try {
	                tag.doStartTag();
	                tag.doInitBody();
	                tag.doAfterBody();
	                tag.doEndTag();
				}
				catch (Throwable t) {
	                /** Catch whatever comes back out of the doCatch() method and deal with it */
	                try { tag.doCatch(t); }
	                catch (Throwable t2) {
	                    if (t2 instanceof JspException) throw (JspException) t2;
	                    if (t2 instanceof RuntimeException) throw (RuntimeException) t2;
	                    else throw new StripesJspException(t2);
	                }
				}
				finally {
					tag.doFinally();
				}
            }
        }
        catch (ExpressionException ee) {
            throw new StripesJspException("A problem occurred generating an options-collection. " +
                "Most likely either [" + labelProperty + "] or ["+ valueProperty + "] is not a " +
                "valid property of the beans in the collection: " + this.collection, ee);
        }

        return SKIP_BODY;
    }

    /**
     * Does nothing.
     *
     * @return EVAL_PAGE in all cases.
     */
    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }
}
