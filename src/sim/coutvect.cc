//========================================================================
//
//  COUTVECT.CC - part of
//                         OMNeT++
//              Discrete System Simulation in C++
//
//   Member functions of
//    cOutVector         : represents a vector in the output file
//
//   Original version:  Gabor Lencse
//   Rewrite, bugfixes: Andras Varga
//========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2004 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <assert.h>
#include <string.h>   // strlen
#include "coutvect.h"
#include "cmodule.h"
#include "csimul.h"
#include "cenvir.h"
#include "cexception.h"

#ifdef WITH_PARSIM
#include "parsim/ccommbuffer.h"
#endif


cOutVector::cOutVector(const char *nam, int tuple) : cObject(nam)
{
    enabled = true;
    handle = NULL;
    num_received = 0;
    num_stored = 0;
    record_in_inspector = NULL;
    tupl = tuple;

    if (tupl!=1 && tupl!=2)
    {
        tupl = 0;
        throw new cException(this,"constructor: invalid value (%d) for tuple; supported values are 1 and 2", tupl);
    }

    // register early if possible (only needed for Akaroa...)
    if (nam)
        handle = ev.registerOutputVector(simulation.contextModule()->fullPath(), name(), tupl);
}

cOutVector::~cOutVector()
{
    if (handle)
        ev.deregisterOutputVector(handle);
}

void cOutVector::setName(const char *nam)
{
    if (handle)
        throw new cException(this,"setName(): changing name of an output vector after record() calls is not allowed");
    cObject::setName(nam);

    // register early (only needed for Akaroa...)
    if (nam)
        handle = ev.registerOutputVector(simulation.contextModule()->fullPath(), name(), tupl);
}

void cOutVector::info(char *buf)
{
    if (handle)
        sprintf(buf, "(received %ld values, stored %ld)", num_received, num_stored);
    else
        sprintf(buf, "(no values recorded yet)");
}

void cOutVector::netPack(cCommBuffer *buffer)
{
    throw new cException(this, "netPack() not supported");
}

void cOutVector::netUnpack(cCommBuffer *buffer)
{
    throw new cException(this, "netUnpack(): not supported");
}

bool cOutVector::record(double value)
{
    // check tuple
    if (tupl!=1)
        throw new cException(this,eNUMARGS,1);

    num_received++;

    // pass data to inspector
    if (record_in_inspector)
        record_in_inspector(data_for_inspector,value,0.0);

    if (!enabled)
        return false;

    // initialize if not yet done
    if (!handle)
        handle = ev.registerOutputVector(simulation.contextModule()->fullPath(), name(), tupl);

    // pass data to envir for storage
    bool stored = ev.recordInOutputVector(handle, simulation.simTime(), value);
    if (stored) num_stored++;
    return stored;
}

bool cOutVector::record(double value1, double value2)
{
    // check tuple
    if (tupl!=2)
        throw new cException(this,eNUMARGS,2);

    num_received++;

    // pass data to inspector
    if (record_in_inspector)
        record_in_inspector(data_for_inspector,value1,value2);

    if (!enabled)
        return false;

    // initialize if not yet done
    if (!handle)
        handle = ev.registerOutputVector(simulation.contextModule()->fullPath(), name(), tupl);

    // pass data to envir for storage
    bool stored = ev.recordInOutputVector(handle, simulation.simTime(), value1, value2);
    if (stored) num_stored++;
    return stored;
}




