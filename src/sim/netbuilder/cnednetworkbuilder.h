//==========================================================================
//   CNEDNETWORKBUILDER.H -
//            part of OMNeT++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2004 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `terms' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CNETWORKBUILDER_H
#define __CNETWORKBUILDER_H

#include "nedelements.h"
#include "cpar.h"

class cModule;
class cGate;
class cChannel;
class cPar;


#define MAX_LOOP_NESTING 32


/**
 * Builds a network from the NED file.
 * Assumes object tree has already passed all validation stages (DTD, basic, semantic).
 *
 * @ingroup NetworkBuilder
 */
class cNEDNetworkBuilder
{
  protected:
    // stack of loop variables
    struct {const char *varname; int value;} loopVarStack[MAX_LOOP_NESTING];
    int loopVarSP;

  protected:
    virtual void addSubmodule(cModule *modp, SubmoduleNode *submod);
    virtual void setDisplayString(cModule *submodp, SubmoduleNode *submod);
    virtual void assignSubmoduleParams(cModule *submodp, SubstparamsNode *substparams);
    virtual void setupGateVectors(cModule *submodp, GatesizesNode *gatesizes);
    virtual void addLoopConnection(cModule *modp, ForLoopNode *forloop);
    virtual void doLoopVar(cModule *modp, LoopVarNode *loopvar);
    virtual void addConnection(cModule *modp, ConnectionNode *conn);
    virtual cGate *resolveGate(cModule *modp, const char *modname, ExpressionNode *modindex,
                               const char *gatename, ExpressionNode *gateindex);
    virtual cChannel *createChannel(ConnectionNode *conn);
    virtual ExpressionNode *getExpr(NEDElement *node, const char *exprname);

    virtual double evaluate(cModule *modp, ExpressionNode *expr, cModule *submodp=NULL);

    virtual double evaluateNode(NEDElement *node, cModule *parentmodp, cModule *submodp);
    virtual double evalOperator(OperatorNode *node, cModule *parentmodp, cModule *submodp);
    virtual double evalFunction(FunctionNode *node, cModule *parentmodp, cModule *submodp);
    virtual double evalParamref(ParamRefNode *node, cModule *parentmodp, cModule *submodp);
    virtual double evalIdent(IdentNode *node, cModule *parentmodp, cModule *submodp);
    virtual double evalConst(ConstNode *node, cModule *parentmodp, cModule *submodp);

    virtual void assignParamValue(cPar& p, ExpressionNode *expr, cModule *parentmodp);

    bool needsDynamicExpression(ExpressionNode *expr);
    void addXElems(NEDElement *node, cPar::ExprElem *xelems, int& pos, cModule *submodp);
    void addXElemsOperator(OperatorNode *node, cPar::ExprElem *xelems, int& pos, cModule *submodp);
    void addXElemsFunction(FunctionNode *node, cPar::ExprElem *xelems, int& pos, cModule *submodp);
    void addXElemsParamref(ParamRefNode *node, cPar::ExprElem *xelems, int& pos, cModule *submodp);
    void addXElemsIdent(IdentNode *node, cPar::ExprElem *xelems, int& pos, cModule *submodp);
    void addXElemsConst(ConstNode *node, cPar::ExprElem *xelems, int& pos, cModule *submodp);

  public:

    cNEDNetworkBuilder();
    virtual ~cNEDNetworkBuilder();

    /**
     * Creates the toplevel module, based on the info in the passed NEDElement tree.
     */
    virtual void setupNetwork(NetworkNode *networknode);

    /**
     * Sets up gates and parameters of the module, based on the info in the
     * passed NEDElement tree.
     */
    virtual void addParametersGatesTo(cModule *module, CompoundModuleNode *modulenode);

    /**
     * Builds submodules and internal connections, based on the info in the
     * passed NEDElement tree.
     */
    virtual void buildInside(cModule *module, CompoundModuleNode *modulenode);

};

#endif





