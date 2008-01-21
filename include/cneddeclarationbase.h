//==========================================================================
// CNEDDECLARATIONBASE.H -
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/


#ifndef __CNEDDECLARATIONBASE_H
#define __CNEDDECLARATIONBASE_H

#include <string>
#include <vector>
#include <map>
#include "simkerneldefs.h"
#include "globals.h"
#include "cownedobject.h"
#include "cparvalue.h"
#include "cgate.h"

NAMESPACE_BEGIN

class cProperties;


/**
 * Represents NED declarations of modules, module interfaces,
 * channels and channel interfaces.
 *
 * The name() method returns the unqualified name (without namespace, e.g.
 * "Queue"), and fullName() returns the qualified name (with namespace,
 * e.g. "inet.network.Queue").
 *
 * This is a passive, data-only class.
 *
 * @ingroup FIXME
 */
//FIXME rename to cAbstractNEDDeclaration
class SIM_API cNEDDeclarationBase : public cNamedObject, noncopyable
{
  protected:
    std::string fullname;

  public:
    /**
     * NED declaration as text, if available
     */
    virtual std::string nedSource() const = 0;

    /**
     * For simple modules and channels, it returns the name of the C++ class that
     * has to be instantiated; otherwise it returns NULL.
     */
    virtual const char *implementationClassName() const = 0;

    /** @name Properties */
//FIXME should be marked as internal or something
    //@{
    /**
     * Returns the properties for this component.
     */
    virtual cProperties *properties() const = 0;

    /**
     * Returns the properties of parameter
     */
    virtual cProperties *paramProperties(const char *paramName) const = 0;

    /**
     * Returns the properties of gate
     */
    virtual cProperties *gateProperties(const char *gateName) const = 0;

    /**
     * Returns the properties of a submodule or a contained channel.
     * (Subcomponent type is needed because with the NED "like" syntax,
     * we need the runtime type not the NED type of the submodule.)
     */
    virtual cProperties *subcomponentProperties(const char *subcomponentName, const char *subcomponentType) const = 0;

    /**
     * Returns the properties of a parameter of a submodule or a contained channel.
     * (Subcomponent type is needed because with the NED "like" syntax,
     * we need the runtime type not the NED type of the submodule.)
     */
    virtual cProperties *subcomponentParamProperties(const char *subcomponentName, const char *subcomponentType, const char *paramName) const = 0;

    /**
     * Returns the properties of a submodule gate.
     * (Subcomponent type is needed because with the NED "like" syntax,
     * we need the runtime type not the NED type of the submodule.)
     */
    virtual cProperties *subcomponentGateProperties(const char *subcomponentName, const char *subcomponentType, const char *gateName) const = 0;
    //@}
};

NAMESPACE_END


#endif


