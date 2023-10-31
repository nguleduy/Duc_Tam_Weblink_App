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

import android.content.Context;
import android.graphics.Canvas;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/**
 * Utility class for wrapping the native keyboard input in a way that is possible for extracting
 * all key strokes.
 */
public class DummyInputView extends EditText {

    public DummyInputView(Context context) {
        super(context);
    }    
    public DummyInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public DummyInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        //Setup our custom input connection.
        DummyInputConnection ic = new DummyInputConnection(this, false);
        outAttrs.inputType = InputType.TYPE_NULL;
        return ic;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //take up no layout space.
        setMeasuredDimension(0, 0);
    }

    @Override
    public void onDraw(Canvas canvas) {
        //invisible, do not draw anything
    }

}
