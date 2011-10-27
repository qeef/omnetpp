/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.animation.editors;

import org.omnetpp.animation.providers.EventLogAnimationPrimitiveProvider;
import org.omnetpp.animation.providers.IAnimationPrimitiveProvider;
import org.omnetpp.animation.widgets.EventLogAnimationParameters;

public class EventLogAnimationEditor extends AnimationEditor
{
    @Override
    protected IAnimationPrimitiveProvider createAnimationPrimitiveProvider() {
        return new EventLogAnimationPrimitiveProvider(eventLogInput, getAnimationController(), new EventLogAnimationParameters());
    }
}