/****************************************************************************
 *
 * @file FrameDecoder_I420.java
 * @brief
 *
 * Contains the FrameDecoder_I420 class.
 *
 * @author Abalta Technologies, Inc.
 * @date Jan, 2014
 *
 * @cond Copyright
 *
 * COPYRIGHT 2014 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
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
 *
 * @endcond
 *****************************************************************************/
package com.abaltatech.weblinkclientapp;

import android.os.Build;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;

/**
 * Utility class that overrides the InputConnection imlementation in order to provide
 * better support for extracting keyboard events in a text edit.
 *
 * This was created for a specific native keyboard implementation, in order to capture all key
 * inputs.
 *
 */
public class DummyInputConnection extends BaseInputConnection {
    static final String DUMMY = "DUMMYTEXTDUMMYTEXTDUMMYTEXTDUMMYTEXTDUMMYTEXTDUMMYTEXTDUMMYTEXTDUMMYTEXTDUMMYTEXTDUMMYTEXTDUMMYTEXTDUMMYTEXTDUMMYTEXT";
    private DummyEditable mEditable;

    public DummyInputConnection(View targetView, boolean fullEditor) {
        super(targetView, fullEditor);
    }

    private class DummyEditable extends SpannableStringBuilder {
        DummyEditable(CharSequence source) {
            super(source);
        }

        @Override
        public SpannableStringBuilder replace(final int start, final int end, CharSequence tb, int tbstart, int tbend) {
            if (tbend > tbstart) {
                super.replace(0, length(), "", 0, 0);
                return super.replace(0, 0, tb, tbstart, tbend);
            } else if (end > start) {
                super.replace(0, length(), "", 0, 0);
                return super.replace(0, 0, DUMMY, 0, DUMMY.length());
            }
            return super.replace(start, end, tb, tbstart, tbend);
        }
    }

    @Override
    public Editable getEditable() {
        if (Build.VERSION.SDK_INT < 14) {
            return super.getEditable();
        }
        if (mEditable == null) {
            mEditable = new DummyEditable(DUMMY);
            Selection.setSelection(mEditable, DUMMY.length());
        } else if (mEditable.length() == 0) {
            mEditable.append(DUMMY);
            Selection.setSelection(mEditable, DUMMY.length());
        }
        return mEditable;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        // Not called in latest Android version...
        return super.deleteSurroundingText(beforeLength, afterLength);
    }
}