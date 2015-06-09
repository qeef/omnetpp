//==========================================================================
//  PACKING.H - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2015 Andras Varga
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_PACKING_H
#define __OMNETPP_PACKING_H

#include "ccommbuffer.h"

NAMESPACE_BEGIN

//
// pack/unpack functions for primitive types
//
#define DOPACKING(T,R) \
          inline void doParsimPacking(OPP::cCommBuffer *b, const T R a) {b->pack(a);}  \
          inline void doParsimPacking(OPP::cCommBuffer *b, const T *a, int n) {b->pack(a,n);}  \
          inline void doParsimUnpacking(OPP::cCommBuffer *b, T& a) {b->unpack(a);}  \
          inline void doParsimUnpacking(OPP::cCommBuffer *b, T *a, int n) {b->unpack(a,n);}
#define _
DOPACKING(char,_)
DOPACKING(unsigned char,_)
DOPACKING(bool,_)
DOPACKING(short,_)
DOPACKING(unsigned short,_)
DOPACKING(int,_)
DOPACKING(unsigned int,_)
DOPACKING(long,_)
DOPACKING(unsigned long,_)
DOPACKING(float,_)
DOPACKING(double,_)
DOPACKING(long double,_)
DOPACKING(char *,_)
DOPACKING(SimTime,&)
DOPACKING(opp_string,&)
#undef _
#undef DOPACKING

NAMESPACE_END

#endif

