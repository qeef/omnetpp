//=========================================================================
//  EVENTLOGENTRY.H - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2006 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __EVENTLOGENTRYFACTORY_H_
#define __EVENTLOGENTRYFACTORY_H_

#include "eventlogentries.h"

class EventLogEntryFactory
{
   public:
      EventLogTokenBasedEntry * parseEntry(char **tokens, int numTokens);
};

#endif