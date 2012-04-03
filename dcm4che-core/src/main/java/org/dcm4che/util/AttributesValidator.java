/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4che.util;

import java.util.Arrays;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Sequence;
import org.dcm4che.data.Value;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 *
 */
public class AttributesValidator {

    private final Attributes attrs;
    private int[] missingAttributes = {};
    private Attributes missingAttributeValues = new Attributes();
    private Attributes invalidAttributeValues = new Attributes();
    private String errorComment;

    public AttributesValidator(Attributes attrs) {
        if (attrs == null)
            throw new NullPointerException();
        this.attrs = attrs;
    }

    public final Attributes getAttributes() {
        return attrs;
    }

    public String getType1String(int tag, int index, int maxvm,
            String... enumvals) {
        String[] ss = attrs.getStrings(tag);
        if (ss == null) {
            addMissingAttribute(tag);
            return null;
        }
        if (ss.length <= index) {
            addMissingAttributeValue(tag);
            return null;
        }
        if (ss.length > maxvm)
            addInvalidAttribueValue(tag);
        else
            checkValue(tag, ss[index], enumvals);
        return ss[index];
    }

    public Sequence getType1Sequence(int tag, int maxSize) {
        return getSequence(tag, 1, maxSize);
    }

    public Sequence getType2Sequence(int tag, int maxSize) {
        return getSequence(tag, 0, maxSize);
    }

    public Sequence getType3Sequence(int tag, int maxSize) {
        return getSequence(tag, -1, maxSize);
    }

    private Sequence getSequence(int tag, int minSize, int maxSize) {
        Object value = attrs.getValue(tag);
        if (value == null) {
            if (minSize >= 0) 
                addMissingAttribute(tag);
            return null;
        }
        if (value instanceof Value) {
            if (((Value) value).isEmpty()) {
                if (minSize >= 0) 
                    addMissingAttributeValue(tag);
                return null;
            }
            if (value instanceof Sequence) {
                Sequence seq = (Sequence) value;
                int seqSize = seq.size();
                if (seqSize >= minSize && seqSize <= maxSize)
                    return seq;
            }
        }
        addInvalidAttribueValue(tag);
        return null;
    }

    public String getType2String(int tag, int index, int maxvm, String defval,
            String... enumvals) {
        String[] ss = attrs.getStrings(tag);
        if (ss == null) {
            addMissingAttribute(tag);
            return defval;
        }
        if (ss.length <= index)
            return defval;
        if (ss.length > maxvm)
            addInvalidAttribueValue(tag);
        else
            checkValue(tag, ss[index], enumvals);
        return ss[index];
    }

    public String getType3String(int tag, int index, int maxvm, String defval,
            String... enumvals) {
        String[] ss = attrs.getStrings(tag);
        if (ss == null || ss.length <= index)
            return defval;
        if (ss.length > maxvm)
            addInvalidAttribueValue(tag);
        else
            checkValue(tag, ss[index], enumvals);
        return ss[index];
    }

    private void checkValue(int tag, String s, String[] enumvals) {
        if (enumvals.length > 0) {
            for (String val : enumvals)
                if (s.equals(val))
                    return;
            addInvalidAttribueValue(tag);
        }
    }

    private int[] addTo(int[] tags, int tag) {
        int[] newTags = Arrays.copyOf(tags, tags.length + 1);
        newTags[tags.length] = tag;
        return newTags;
    }

    public void addMissingAttribute(int tag) {
        missingAttributes = addTo(missingAttributes, tag);
        setErrorComment("Missing Attribute ", tag);
    }

    public void addMissingAttributeValue(int tag) {
        missingAttributeValues.addSelected(attrs, null, tag);
        setErrorComment("Missing Attribute Value of ", tag);
    }

    public void addInvalidAttribueValue(int tag) {
        invalidAttributeValues.addSelected(attrs, null, tag);
        setErrorComment("Invalid Attribute Value of ", tag);
    }

    public void setErrorComment(String prompt, int tag) {
        errorComment = prompt + TagUtils.toString(tag);
    }

    public final int[] getMissingAttributes() {
        return missingAttributes;
    }

    public final Attributes getMissingAttributeValues() {
        return missingAttributeValues;
    }

    public final Attributes getInvalidAttributeValues() {
        return invalidAttributeValues;
    }

    public final boolean hasMissingAttributes() {
        return missingAttributes.length > 0;
    }

    public final boolean hasMissingAttributeValues() {
        return !missingAttributeValues.isEmpty();
    }

    public final boolean hasInvalidAttributeValues() {
        return !invalidAttributeValues.isEmpty();
    }

    public final boolean hasOffendingElements() {
        return hasMissingAttributes()
                || hasMissingAttributeValues()
                || hasInvalidAttributeValues();
    }

    public final int[] getOffendingElements() {
        int[] invalid = new int[missingAttributes.length
                              + missingAttributeValues.size()
                              + invalidAttributeValues.size()];
        int i = 0;
        for (int tag : missingAttributes)
            invalid[i++] = tag;
        for (int tag : missingAttributeValues.tags())
            invalid[i++] = tag;
        for (int tag : invalidAttributeValues.tags())
            invalid[i++] = tag;

        return invalid;
    }

    public final String getErrorComment() {
        return errorComment;
    }
}