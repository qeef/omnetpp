//==========================================================================
//  MSGTYPETABLE.H - part of
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_NEDXML_MSGTYPETABLE_H
#define __OMNETPP_NEDXML_MSGTYPETABLE_H

#include <iostream>
#include <sstream>
#include <string>
#include <vector>
#include <map>
#include <set>

#include "nedelements.h"
#include "nederror.h"

namespace omnetpp {
namespace nedxml {

/**
 * Holds types from msg files.
 *
 * @ingroup CppGenerator
 */
class NEDXML_API MsgTypeTable
{
  public:
    typedef std::vector<std::string> StringVector;

    enum class ClassType {
        UNKNOWN = 0,
        STRUCT,       // struct
        NONCOBJECT,   // class not derived from cObject
        COBJECT,      // class derived from cObject
        COWNEDOBJECT, // class derived from cOwnedObject
        CNAMEDOBJECT  // class derived from cNamedObject
    };

    struct TypeDesc
    {
        const char *nedTypeName;
        const char *cppTypeName;
        const char *fromstring;
        const char *tostring;
        const char *emptyValue;
    };

    typedef std::map<std::string, std::string> Properties;  //FIXME kell egy reszletesebb modell...

    class FieldInfo {
      public:
        NEDElement *nedElement;     // pointer to NED element

        std::string fname;      // field name in MSG
        std::string ftype;      // field type in MSG
        std::string ftypeqname; // fully qualified C++ name of type //TODO should not be needed
        std::string fval;       // value (or empty)
        bool fisabstract;
        bool fispointer;
        bool fisownedpointer;   // true, when should use dup()/delete/take()/drop()/dropAndDelete(); read from @owned, default value is true for cOwnedObject, otherwise false
                                // "T *removeFoo()" also generated for owned pointer members
        bool fisarray;
        bool byvalue;           // @byvalue, default value is false        // TODO: @byvalue should rather be the attribute of the field's type, not the field itself
        std::string farraysize; // array size in MSG, maybe empty for dynamic arrays
        Properties fprops;      // field properties (name, first value of default key)

        // data needed for code generation
        bool fisprimitivetype;  // whether primitive or compound type (TODO merge into ClassType?)
        ClassType classtype;    // cobject/cnamedobject/cownedobject/...
        std::string datatype;   // member C++ datatype
        std::string argtype;    // setter C++ argument type
        std::string rettype;    // getter C++ return type
        std::string var;        // name of data member variable
        std::string argname;    // setter argument name
        std::string varsize;    // data member to store array size | value of farraysize
        std::string fsizetype;  // type for storing array size
        std::string getter;     // getter function name:  "T getter() const;" "const T& getter() const"  default value is getFoo
        std::string mGetter;    // const getter function name:  "T& mGetter();" default value is the value of getter, @mgetter
        std::string remover;    // remover function name (for owned pointers)
        std::string setter;     // Setter function name
        std::string alloc;      // setArraySize() function name
        std::string getsize;    // array size getter function name
        std::string tostring;   // function to convert data to string, defined in property @tostring, default values for primitive types found in MsgCppGenerator::TypeMap MsgCppGenerator::PRIMITIVE_TYPES
                                // if tostring begins a dot, then uses as member function, parentheses needed in property; otherwise tostring implemented as name of a normal function, do not use parentheses
                                // std::string <function>(<datatype>);           // @tostring(function)
                                // std::string <function>(const <datatype>&);    // @tostring(function)
                                // const char * <function>(<datatype>);          // @tostring(function)
                                // const char * <function>(const <datatype>&);   // @tostring(function)
                                // std::string <datatype>::<function>(...);      // @tostring(.function(...))
                                // const char * <datatype>::<function>(...);     // @tostring(.function(...))
        std::string fromstring; // function to convert string to data member, defined in property @fromstring, default values for primitive types found in MsgCppGenerator::TypeMap MsgCppGenerator::PRIMITIVE_TYPES
                                // <datatype> <function>(const char *);          // @fromstring(function)
        std::string maybe_c_str;       // uses ".c_str()"
        std::string enumname;
        std::string enumqname;
        bool fnopack;           // @nopack(true)
        bool feditable;         // @editable(true)
        bool editNotDisabled;   // true when field doesn't have property "@editable(false)"
        bool fopaque;         // @opaque(true)        // TODO: @opaque should rather be the attribute of the field's type, not the field itself
        bool overrideGetter;   // @overridegetter|@override, uses when field getter function overrides a function in base class
        bool overrideSetter;   // @overridesetter|@override, uses when field setter function overrides a function in base class

      public:
        FieldInfo() : nedElement(nullptr), fisabstract(false), fispointer(false), fisarray(false), classtype(ClassType::UNKNOWN), fnopack(false), feditable(false),fopaque(false) {}
    };

    class ClassInfo {
      public:
        typedef std::vector<FieldInfo> Fieldlist;

        NEDElement *nedElement = nullptr;
        std::string keyword;        // struct/class/packet from MSG
        std::string msgname;        // class name from MSG
        std::string msgqname;
        Properties props;           // class properties

        bool classInfoComplete = false;  // whether following fields are filled in
        bool fieldsComplete = false;  // whether fieldlist / baseclassFieldlist are filled in

        std::string msgbaseqname;
        std::string msgbase;        // base class name from MSG
        bool gap = false;                   // true if @customize
        bool omitgetverb = false;
        ClassType classtype = ClassType::UNKNOWN;
        std::string namespacename;
        std::string msgclass;
        std::string realmsgclass;
        std::string msgbaseclass;
        std::string msgdescclass;
        std::string fieldnamesuffix; // e.g. "_var"
        Fieldlist fieldlist;        // list of fields
        Fieldlist baseclassFieldlist;   //modified baseclass fields, e.g. baseclass.basefield = value

        bool generate_class = true;
        bool generate_descriptor = true;
        bool generate_setters_in_descriptor = true;


        StringVector implements;    //value vector from @implements

        std::string defaultvalue;       // value (or empty)
        bool isopaque = false;         // @opaque(true)        // TODO: @opaque should rather be the attribute of the field's type, not the field itself
        bool byvalue = false;           // @byvalue, default value is false        // TODO: @byvalue should rather be the attribute of the field's type, not the field itself
        bool isprimitivetype = false;  // whether primitive or compound type (TODO merge into ClassType?)
        std::string datatype;   // member C++ datatype
        std::string argtype;    // setter C++ argument type
        std::string rettype;    // getter C++ return type
        std::string tostring;   // function to convert data to string, defined in property @tostring
        std::string fromstring; // function to convert string to data member, defined in property @fromstring
        std::string maybe_c_str;       // uses ".c_str()"
//??        bool feditable;         // @editable(true)
    };

    class EnumItem
    {
      public:
        NEDElement *nedElement;
        std::string name;
        std::string value;
        std::string comment;
      public:
        EnumItem() : nedElement(nullptr) {}
    };

    class EnumInfo
    {
      public:
        NEDElement *nedElement;
        std::string enumName;
        std::string enumQName;
        typedef std::vector<EnumItem> FieldList;
        bool isDeclaration = false; // i.e. not a definition
        FieldList fieldlist;
      public:
        EnumInfo() : nedElement(nullptr) {}
    };

  private:
    std::map<std::string,ClassInfo> definedClasses;
    std::map<std::string,EnumInfo> definedEnums;
    std::vector<NEDElement*> importedMsgFiles;

  protected:
    void initDescriptors();
    std::string prefixWithNamespace(const std::string& namespaceName, const std::string& name) {
        return !namespaceName.empty() ? namespaceName + "::" + name : name;
    }

  public:
    MsgTypeTable() {initDescriptors();}
    ~MsgTypeTable();
    StringVector lookupExistingClassName(const std::string& name, const std::string& contextNamespace);
    StringVector lookupExistingEnumName(const std::string& name, const std::string& contextNamespace);
    bool isClassDefined(const std::string& classqname) { return definedClasses.find(classqname) != definedClasses.end(); }
    bool isEnumDefined(const std::string& enumqname) { return definedEnums.find(enumqname) != definedEnums.end(); }
    ClassInfo& getClassInfo(const std::string& classqname);
    const EnumInfo& getEnumInfo(const std::string& qname);
    ClassType getClassType(const std::string& qname);
    void storeMsgFile(NEDElement *tree) {importedMsgFiles.push_back(tree);}
    void addClass(const ClassInfo& classInfo) {definedClasses[classInfo.msgqname] = classInfo;} // TODO assert not already there
    void addEnum(const EnumInfo& enumInfo) {definedEnums[enumInfo.enumQName] = enumInfo;}  //TODO assert not already there
};

} // namespace nedxml
}  // namespace omnetpp


#endif


