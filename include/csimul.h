//==========================================================================
//   CSIMUL.H  -  header for
//                             OMNeT++
//            Discrete System Simulation in C++
//
//
//  Declaration of the following classes:
//    cSimulation  : simulation management class; only one instance
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2001 Andras Varga
  Technical University of Budapest, Dept. of Telecommunications,
  Stoczek u.2, H-1111 Budapest, Hungary.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CSIMUL_H
#define __CSIMUL_H

#include <time.h>     // time_t, clock_t in cSimulation
#include "defs.h"
#include "util.h"
#include "errmsg.h"
#include "chead.h"
#include "cmsgheap.h"
#include "coutvect.h"
#include "ccor.h"

//=== classes mentioned:
class  cMessage;
class  cGate;
class  cModulePar;
class  cModule;
class  cSimpleModule;
class  cCompoundModule;
class  cNetMod;
class  cSimulation;
class  cNetworkType;
class  TModInspector;
class  cStatistic;

/**
 * Global simulation instance.
 *
 * @ingroup Internals
 */
SIM_API extern cSimulation simulation;

//==========================================================================

/**
 * Simulation manager class.  cSimulation is the central class in OMNeT++, and
 * it has only one instance, the global variable <tt>simulation</tt>.
 * It holds the modules, it manages setting up a simulation, running and
 * finishing it, etc.
 *
 * cSimulation should normally be of little interest to the simulation
 * programmer. Most of its methods are called by the user interface libraries
 * (Envir, Cmdenv, Tkenv) to set up and run simulations.
 *
 * @ingroup Internals
 */
class SIM_API cSimulation : public cObject
{
    friend class cSimpleModule;

  private:
    // variables of the module vector
    int size;                 // size of vector
    int delta;                // if needed, grows by delta
    cModule **vect;           // vector of modules, vect[0] is not used
    int last_id;              // index of last used pos. in vect[]

    // simulation global vars
    cModule *systemmodp;      // pointer to system module
    cSimpleModule *runningmodp; // the currently executing module (NULL if in main)
    cModule *contextmodp;     // module in context (or NULL)
    cHead *locallistp;        // owner of newly created objects
    cHead locals;             // "global" local objects list

    cNetMod *netmodp;         // if runs distributed; communications
                              //      (network interface) module

    int err;                  // error code, 0 (== eOK) if no error
    bool warn;                // if true, overrides individual warn flags

    simtime_t sim_time;       // simulation time (time of current event)
    long event_num;           // sequence number of current event

    int netif_check_freq;     // (distributed execution:) frequency of processing
    int netif_check_cntr;     // msgs from other segments

    cNetworkType *networktype; // ptr to network creator object
    int run_number;            // which simulation run
    time_t simbegtime;         // real time when sim. started
    time_t simendtime;         // real time when sim. ended
    time_t laststarted;        // real time from where sim. was last cont'd
    time_t elapsedtime;        // in seconds
    time_t tmlimit;            // real time limit in seconds
    simtime_t simulatedtime;   // sim.time at after finishing simulation
    simtime_t simtmlimit;      // simulation time limit
    cSimpleModule *backtomod;  // used in cSimpleModule::wait/sendmsg
    cCoroutine runningmod_deleter; // used when a simple module deletes itself

  public:
    cMessageHeap msgQueue;        // future messages (FES)

    cOutFileMgr outvectfilemgr;   // output vector file manager
    cOutFileMgr scalarfilemgr;    // output scalar file manager
    cOutFileMgr parchangefilemgr; // parameter change log file manager
    cOutFileMgr snapshotfilemgr;  // snapshot file manager
    bool logparchanges;           // module parameter change logging on/off
    bool scalarfile_header_written;

  public:
    /** @name Constructor, destructor. */
    //@{

    /**
     * Constructor.
     */
    explicit cSimulation(const char *name, cHead *h=NULL);

    /**
     * Destructor.
     */
    virtual ~cSimulation();
    //@}

    /** @name Redefined cObject member functions. */
    //@{

    /**
     * Returns pointer to a string containing the class name, "cSimulation".
     */
    virtual const char *className() const {return "cSimulation";}

    /**
     * Returns the name of the inspector factory class associated with this class.
     * See cObject for more details.
     */
    virtual const char *inspectorFactoryName() const {return "cSimulationIFC";}

    /**
     * Call the passed function for each contained object.
     * See cObject for more details.
     */
    virtual void forEach(ForeachFunc f);

    /**
     * Writes textual information about this object to the stream.
     * See cObject for more details.
     */
    virtual void writeContents(ostream& os);

    /**
     * Returns the name of the simulation object, "simulation".
     */
    virtual const char *fullPath() const {return name();}
    // FIXME: no dup(), op=!
    //@}

    // Internal: things that cannot be done from the constructor
    // (because it is called before main()).
    void setup();

    /** @name Accessing modules. */
    //@{

    /**
     * Adds a new module and return its ID.
     */
    int add(cModule *mod);

    /**
     * Deletes a module identified by its ID.
     */
    void del(int id);

    /**
     * Returns highest used module ID.
     */
    int lastModuleIndex() _CONST    {return last_id;}

    /**
     * Finds a module by its path.
     */
    cModule *moduleByPath(const char *modulepath) _CONST;

    /**
     * Looks up a module by ID.
     */
    cModule *module(int id) _CONST
         {return id>=0 && id<size ? vect[id] : NO(cModule);}

    /**
     * Same as module(int), only this returns reference instead of pointer.
     */
    cModule& operator[](int id) _CONST
         {return id>=0 && id<size ? *vect[id] : *NO(cModule);}

    /**
     * Sets the system module.
     */
    void setSystemModule(cModule *p)
         {systemmodp = p;}

    /**
     * Returns pointer to the system module.
     */
    cModule *systemModule() _CONST
         {return systemmodp;}
    //@}

    /** @name Setting up and finishing a simulation run. */
    //@{

    /**
     * Sets network interface module.
     */
    void setNetInterface(cNetMod *netif);

    /**
     * Returns network interface module.
     */
    cNetMod *netInterface() _CONST     {return netmodp;}

    /**
     * Resets the clock measuring the elapsed (real) time spent in this
     * simulation run.
     */
    void resetClock();

    /**
     * Start measuring elapsed (real) time spent in this simulation run.
     */
    void startClock();

    /**
     * Stop measuring elapsed (real) time spent in this simulation run.
     */
    void stopClock();

    /**
     * Builds a new network.
     */
    bool setupNetwork(cNetworkType *net,int run_num);

    /**
     * Initializes network.
     */
    void startRun();

    /**
     * Recursively calls finish() on the modules of the network.
     * This method simply invokes callfinish() on the system module.
     */
    void callFinish();

    /**
     * Should be called at the end of a simulation run. Closes open files, etc.
     */
    void endRun();

    /**
     * Cleans up the simulation network currently set up. Deletes modules, message queue etc.
     */
    void deleteNetwork();

    /**
     * Sets an execution time limit for the current simulation run.
     * The value is understood in seconds.
     */
    // FIXME: do time limits really belong to the simulation kernel??? why not in Envir?
    void setTimeLimit(long t)         {tmlimit=(time_t)t;}

    /**
     * Sets a simulation time limit for the current simulation run.
     */
    void setSimTimeLimit(simtime_t t) {simtmlimit=t;}

    /**
     * Used with distributed simulation, sets the frequency of checking
     * messages arriving from other segments. This setting is mostly
     * useful for performance tuning. The meaning of the value is:
     * "check the network interface after every f local events."
     */
    void setNetIfCheckFreq(int f)     {netif_check_freq=f;}
    //@}

    /** @name Information about the current simulation run. */
    //@{

    /**
     * Returns the cNetworkType object that was used to set up
     * the current simulation model.
     */
    cNetworkType *networkType() _CONST     {return networktype;}

    /**
     * Returns the current run number. A run is the execution of a
     * model with a given set of parameter settings. Runs can be defined
     * in omnetpp.ini.
     */
    int  runNumber() _CONST           {return run_number;}

    /**
     * Returns CPU time limit for the current simulation run.
     */
    long timeLimit() _CONST                {return (long)tmlimit;}

    /**
     * Returns simulation time limit for the current simulation run.
     */
    simtime_t simTimeLimit() _CONST   {return simtmlimit;}

    /**
     * Returns current simulation time.
     */
    simtime_t simTime() _CONST       {return sim_time;}

    /**
     * Returns sequence number of current event.
     */
    long eventNumber() _CONST         {return event_num;}
    //@}

    /** @name Scheduling and context switching during simulation. */
    //@{

    /**
     * The scheduler function. Returns the module to which the
     * next event (lowest timestamp event in the FES) belongs.
     */
    cSimpleModule *selectNextModule();

    /**
     * Executes one event. The argument should be the module
     * returned by selectNextModule(); that is, the module
     * to which the next event (lowest timestamp event in
     * the FES) belongs.
     */
    void doOneEvent(cSimpleModule *m);

    /**
     * Increments the event number.
     */
    void incEventNumber()             {event_num++;}

    /**
     * Checks if the current simulation has reached the simulation
     * or real time limits, and if so, stops the simulation with the
     * appropriate message.
     */
    void checkTimes();

    /**
     * Switches to simple module's coroutine. This method is invoked
     * from doOneEvent() for activity()-based modules.
     */
    int transferTo(cSimpleModule *p);

    /**
     * Switches to main coroutine.
     */
    int transferToMain();

    /**
     * Sets the module in context. Used internally.
     */
    void setContextModule(cModule *p);

    /**
     * Sets global context. Used internally.
     */
    void setGlobalContext()  {contextmodp=NULL;locallistp=&locals;}

    /**
     * Sets the 'locals' object for the current context.
     */
    void setLocalList(cHead *p)  {locallistp=p;}

    /**
     * Returns the currently executing simple module.
     */
    cSimpleModule *runningModule() _CONST {return runningmodp;}

    /**
     * Returns the module currently in context.
     */
    cModule *contextModule() _CONST {return contextmodp;}

    /**
     * Returns the module currently in context as a simple module.
     * If the module in context is not a simple module, returns NULL.
     * This is a convenience function which simply calls contextModule().
     */
     // FIXME: implementation should check isSimple() and return NULL if not OK!!!
    cSimpleModule *contextSimpleModule() _CONST; // cannot make inline! would require cmodule.h because of dynamic cast

    /**
     * Returns the currently active 'locals' object. This object is usually the
     * <tt>locals</tt> data member of the module in context, or the global
     * <tt>locals</tt> object if we are in global context. This facility
     * is used internally to manage ownership of user objects created within
     * simple modules.
     */
    cHead *localList() {return locallistp==NULL?(&locals):locallistp;}
    //@}

    /** @name Statistics, snapshots. */
    //@{

    /**
     * Writes a snapshot of the given object and its children to the
     * textual snapshot file.
     * This method is called internally from cSimpleModule's snapshot().
     */
    bool snapshot(cObject *obj, const char *label);

    /**
     * Records a double value into the scalar result file.
     */
    void recordScalar(const char *name, double value);

    /**
     * Records a string value into the scalar result file.
     */
    void recordScalar(const char *name, const char *text);

    /**
     * Records a statistics object into the scalar result file.
     */
    void recordStats(const char *name, cStatistic *stats);
    //@}

    /** @name Errors and warnings. */
    //@{

    /**
     * Return true if warnings are globally enabled.
     */
    bool warnings() _CONST  {return warn;}

    /**
     * Globally disable/enable warnings.
     */
    void setWarnings(bool w) {warn=w;}

    /**
     * Prints a termination message and sets the error number.
     */
    void terminate(int errcode,const char *message);

    /**
     * Issue error message.
     */
    void error(int errcode,const char *message);

    /**
     * Issues simulation warning. message + question:continue/abort?
     */
    void warning(int errcode,const char *message);

    /**
     * Returns true if no errors happened (i.e. error code is zero, eOK).
     */
    bool ok() _CONST        {return err==eOK;}

    /**
     * Returns current error code.
     */
    int errorCode() _CONST  {return err;}

    /**
     * Sets error code without giving error message.
     */
    void setErrorCode(int e) {err=e;}

    /**
     * Examines error code and returns true if simulation terminated normally.
     */
    bool normalTermination() _CONST;

    /**
     * Reset error code.
     */
    void resetError() {err=eOK;}
    //@}
};

//==========================================================================
//=== operator new used by the NEW() macro:
class ___nosuchclass;
void *operator new(size_t m,___nosuchclass *);

#endif
