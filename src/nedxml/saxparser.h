//==========================================================================
//   SAXPARSER.H -
//            part of OMNeT++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2004 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/



#ifndef __SAXPARSER_H
#define __SAXPARSER_H

#include <stdio.h>

class SAXParser;


/**
 * Base class for SAX event handlers needed by XMLParser.
 * This is a simplified SAX handler interface.
 *
 * All event handlers provided by this class are empty, one must
 * subclass SAXHandler and redefine the event handler to make them
 * do something useful.
 *
 * @ingroup XMLParser
 */
class SAXHandler
{
    friend class SAXParser;
  protected:
    SAXParser *parser;
    virtual void setParser(SAXParser *p) {parser=p;}

  public:
    /**
     * Constructor
     */
    SAXHandler() {parser=0;}

    /**
     * Destructor
     */
    virtual ~SAXHandler() {}

    /**
     * Called by the parser on SAX StartElement events.
     */
    virtual void startElement(const char *name, const char **atts)  {}

    /**
     * Called by the parser on SAX EndElement events.
     */
    virtual void endElement(const char *name)  {}

    /**
     * Called by the parser on SAX CharacterData events.
     */
    virtual void characterData(const char *s, int len)  {}

    /**
     * Called by the parser on SAX ProcessingInstruction events.
     */
    virtual void processingInstruction(const char *target, const char *data)  {}

    /**
     * Called by the parser on SAX Comment events.
     */
    virtual void comment(const char *data)  {}

    /**
     * Called by the parser on SAX CDataStart events.
     */
    virtual void startCdataSection()  {}

    /**
     * Called by the parser on SAX CDataEnd events.
     */
    virtual void endCdataSection()  {}
};


/**
 * Front-end to XML parsers (non-validating, SAX parsers).
 * The current implementation uses Expat.
 * One must provide a SAXHandler for this class to be useful.
 *
 * @ingroup XMLParser
 */
class SAXParser
{
  protected:
    char errortext[512];
    SAXHandler *saxhandler;
    void *currentparser;

  public:
    /**
     * Constructor
     */
    SAXParser();

    /**
     * Install a SAX handler into the parser.
     */
    void setHandler(SAXHandler *sh);

    /**
     * Parse XML input read from the given file. Methods of the SAX handler
     * will be called as the parser processes the file.
     */
    int parse(FILE *f);

    /**
     * Returns the current line number in the input. Can be called from SAX handler code.
     */
    int getCurrentLineNumber();
};

#endif

