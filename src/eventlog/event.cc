//=========================================================================
//  EVENT.CC - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2006 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "linetokenizer.h"
#include "filereader.h"
#include "event.h"
#include "eventlogentry.h"
#include "eventlogentries.h"
#include "eventlogentryfactory.h"

Event::Event()
{
    eventEntry = NULL;
    numLogMessages = 0;
}

void parse()
{
}

long Event::eventNumber()
{
    return eventEntry->eventNumber;
}

long Event::parse(FileReader *reader, long offset)
{
    EventLogEntryFactory factory;
    LineTokenizer tokenizer;

    eventEntry = NULL;
    eventLogEntries.clear();
    reader->seekTo(offset);

    while (true)
    {
        char *line = reader->readLine();

        if (!line)
            return reader->fileSize();

        if (*line == '-')
        {
            EventLogMessage *eventLogMessage = new EventLogMessage();
            eventLogMessage->parse(line);
            eventLogEntries.push_back(eventLogMessage);
            continue;
        }

        tokenizer.tokenize(line);

        EventLogEntry *eventLogEntry = factory.parseEntry(tokenizer.tokens(), tokenizer.numTokens());
        EventEntry *eventEntry = dynamic_cast<EventEntry *>(eventLogEntry);

        if (eventEntry)
        {
            if (this->eventEntry == NULL)
                this->eventEntry = eventEntry;
            else
                break;
        }

        if (eventLogEntry != NULL)
            eventLogEntries.push_back(eventLogEntry);
    }

    return reader->lineStartOffset();
}

void Event::print(FILE *file)
{
    for (EventLogEntryList::iterator it = eventLogEntries.begin(); it != eventLogEntries.end(); it++)
    {
        EventLogEntry *eventLogEntry = *it;
        eventLogEntry->print(file);
    }
}
