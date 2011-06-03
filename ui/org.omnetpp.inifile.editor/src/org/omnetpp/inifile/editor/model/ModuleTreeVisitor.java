package org.omnetpp.inifile.editor.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.omnetpp.common.engine.Common;
import org.omnetpp.common.engine.PatternMatcher;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.core.IModuleTreeVisitor;
import org.omnetpp.ned.core.ParamUtil;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;
import org.omnetpp.ned.model.interfaces.ISubmoduleOrConnection;
import org.omnetpp.ned.model.pojo.SubmoduleElement;

/**
 * Module tree visitor for collecting module parameters and properties.
 * It can optionally use an inifile as additional input for parameter values, etc.
 */
class ModuleTreeVisitor  implements IModuleTreeVisitor {
    
    private final IReadonlyInifileDocument doc; // null if running without an inifile
    private final String[] sectionChain; // null if running without an inifile
    private final IProgressMonitor monitor;
    private final boolean collectParameters;
    private final String[] propertiesToCollect;     // names of properties to be collected, null if none
    private final PatternMatcher moduleNamePattern;
    
    private final List<ParamResolution> paramResolutions;
    private final List<PropertyResolution> propertyResolutions;
    private final Map<String,ISubmoduleOrConnection> modules;

    protected Stack<ISubmoduleOrConnection> elementPath = new Stack<ISubmoduleOrConnection>();
    protected Stack<INedTypeInfo> typeInfoPath = new Stack<INedTypeInfo>();
    protected Stack<String> fullPathStack = new Stack<String>();  //XXX performance: use cumulative names, so that StringUtils.join() can be eliminated (like: "Net", "Net.node[*]", "Net.node[*].ip" etc)
    
    /**
     * For traversing a given NED element or NED type.
     */
    public ModuleTreeVisitor(boolean collectParameters, String[] propertiesToCollect, PatternMatcher moduleNamePattern) {
        this.doc = null;
        this.sectionChain = null;
        this.monitor = null;
        this.collectParameters = collectParameters;
        this.propertiesToCollect = propertiesToCollect;
        this.moduleNamePattern = moduleNamePattern;
        
        this.paramResolutions = collectParameters ? new ArrayList<ParamResolution>() : null;
        this.propertyResolutions = propertiesToCollect != null ? new ArrayList<PropertyResolution>() : null;
        this.modules = moduleNamePattern != null ? new HashMap<String,ISubmoduleOrConnection>() : null;
    }
    
    /**
     * For traversing the configured network.
     */
    public ModuleTreeVisitor(IReadonlyInifileDocument doc, String activeSection,
            boolean collectParameters, String[] propertiesToCollect, PatternMatcher moduleNamePattern, IProgressMonitor monitor) {
        Assert.isTrue(collectParameters || propertiesToCollect != null || moduleNamePattern != null);
        this.doc = doc;
        this.sectionChain = doc.getSectionChain(activeSection);
        this.collectParameters = collectParameters;
        this.propertiesToCollect = propertiesToCollect;
        this.moduleNamePattern = moduleNamePattern;
        this.monitor = monitor;

        this.paramResolutions = collectParameters ? new ArrayList<ParamResolution>() : null;
        this.propertyResolutions = propertiesToCollect != null ? new ArrayList<PropertyResolution>() : null;
        this.modules = moduleNamePattern != null ? new HashMap<String,ISubmoduleOrConnection>() : null;
    }
    
    public List<ParamResolution> getParamResolutions() {
        return paramResolutions;
    }
    
    public List<PropertyResolution> getPropertyResolutions() {
        return propertyResolutions;
    }
    
    public Map<String,ISubmoduleOrConnection> getModules() {
        return modules;
    }

    public boolean enter(ISubmoduleOrConnection element, INedTypeInfo typeInfo) {
        if (monitor != null && monitor.isCanceled())
            throw new OperationCanceledException();
        
        elementPath.push(element);
        typeInfoPath.push(typeInfo);

        fullPathStack.push(element == null ? typeInfo.getName() : ParamUtil.getParamPathElementName(element));
        String fullPath = StringUtils.join(fullPathStack, ".");
        
        if (moduleNamePattern == null || moduleNamePattern.matches(fullPath)) {
            if (moduleNamePattern != null)
                modules.put(fullPath,element);
            
            // collect parameters
            if (collectParameters) {
                ParamCollector.resolveModuleParameters(paramResolutions, fullPath, typeInfoPath, elementPath, sectionChain, doc);
            }
            
            // collect properties
            if (propertiesToCollect != null) {
                String activeSection = sectionChain != null ? sectionChain[0] : null;
                for (String propertyName : propertiesToCollect)
                    ParamCollector.resolveModuleProperties(propertyName, propertyResolutions, fullPath, typeInfoPath, elementPath, activeSection);
            }
        }
        
        return true;
    }
    
    public void leave() {
        elementPath.pop();
        typeInfoPath.pop();
        fullPathStack.pop();
    }

    public void unresolvedType(ISubmoduleOrConnection element, String typeName) {
    }

    public void recursiveType(ISubmoduleOrConnection element, INedTypeInfo typeInfo) {
    }

    public String resolveLikeType(ISubmoduleOrConnection element) {
        // Note: we cannot use InifileUtils.resolveLikeExpr(), as that calls
        // resolveLikeExpr() which relies on the data structure we are currently building

        // TODO: we should probably return a string array, because if the submodule is a vector,
        // different indices may have different NED types.
       
        if (!collectParameters)
            return null;

        // get module type expression
        String likeExpr = element.getLikeExpr();

        //XXX this code is a near duplicate of one in InifileUtils
        
        // note: the following lookup order is based on src/sim/netbuilder code, namely cNEDNetworkBuilder::getSubmoduleTypeName()

        // first, try to use expression between angle braces from the NED file
        if (!element.getIsDefault() && StringUtils.isNotEmpty(likeExpr)) {
            return evaluateLikeExpr(likeExpr);
        }

        // then, use **.typename from NED deep param assignments
        // XXX this is not implemented yet

        // then, use **.typename option in the configuration if exists
        if (sectionChain != null) {
            String fullPath = StringUtils.join(fullPathStack, ".");
            String name = "channel";  // unless it's a submodule:
            if (element instanceof SubmoduleElement) {
                name = ((SubmoduleElement)element).getName();
                if (!StringUtils.isEmpty(((SubmoduleElement)element).getVectorSize()))
                    name += "[*]";
            }
            String key = fullPath + "." + name + "." + ConfigRegistry.TYPENAME;
            List<SectionKey> sectionKeys = InifileUtils.lookupParameter(key, false, sectionChain, doc);

            // prefer the one with all asterisk indices, "[*]"
            SectionKey chosenKey = null;
            for (SectionKey sectionKey : sectionKeys)
                if (ParamUtil.isTotalParamAssignment(sectionKey.key))
                    chosenKey = sectionKey;
            if (chosenKey == null && !sectionKeys.isEmpty())
                chosenKey = sectionKeys.get(0);
            if (chosenKey != null) {
                // we only understand if there's a string constant there
                String value = doc.getValue(chosenKey.section, chosenKey.key);
                try {
                    return Common.parseQuotedString(value);
                } 
                catch (RuntimeException e) {
                    return null; // it is something we don't understand
                }
            }
        }
        
        // then, use **.typename=default() expressions from NED deep param assignments
        // XXX this is not implemented yet

        // last, use default(expression) between angle braces from the NED file
        if (StringUtils.isEmpty(likeExpr))
            return null; // cannot happen (<default()> is not an accepted NED syntax)
        return evaluateLikeExpr(likeExpr);
    }

    protected String evaluateLikeExpr(String likeExpr) {
        // understands string literals and references to parent module parameters;
        // return null for anything else (i.e. when we are not sophisticated enough to figure it out) 
        if (likeExpr.charAt(0) == '"') {
            try {
                // looks like a string literal
                return Common.parseQuotedString(likeExpr);
            } 
            catch (RuntimeException e) {
                return null;  // nope
            }
        }
        else if (likeExpr.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            // identifier: it should be parameter of the parent module; we should look up and 
            // return its value (note: we cannot use InifileUtils.resolveLikeParam() here yet)
            String fullPath = StringUtils.join(fullPathStack, ".");
            ParamResolution res = null;
            for (ParamResolution r : paramResolutions)
                if (r.paramDeclaration.getName().equals(likeExpr) && r.fullPath.equals(fullPath))
                {res = r; break;}
            if (res == null)
                return null; // likely no such parameter
            String value = getParamValue(res, doc);
            if (value == null)
                return null; // likely unassigned
            try {
                return Common.parseQuotedString(value);
            } 
            catch (RuntimeException e) {
                return null; // value is not a string literal
            }
        }
        else {
            return null; 
        }
    }
    
    private static String getParamValue(ParamResolution res, IReadonlyInifileDocument doc) {
        return getParamValue(res, doc, true);
    }

    private static String getParamValue(ParamResolution res, IReadonlyInifileDocument doc, boolean allowNull) {
        switch (res.type) {
            case UNASSIGNED:
                if (allowNull)
                    return null;
                else
                    return "(unassigned)";
            case INI_ASK:
                if (allowNull)
                    return null;
                else
                    return "(ask)";
            case NED: case INI_DEFAULT: case IMPLICITDEFAULT:
                return res.paramAssignment.getValue();
            case INI: case INI_OVERRIDE: case INI_NEDDEFAULT:
                return doc.getValue(res.section, res.key);
            default: throw new IllegalArgumentException("invalid param resolution type: "+res.type);
        }
    }
}