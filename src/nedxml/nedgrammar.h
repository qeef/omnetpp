/***************************************************/
/*  File: nedgrammar.h                             */
/*                                                 */
/*  Part of OMNeT++                                */
/*                                                 */
/*  Contents:                                      */
/*    declarations shared by ned.lex and ned.y     */
/*                                                 */
/***************************************************/

/*--------------------------------------------------------------*
  Copyright (C) 2002-2004 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __NEDGRAMMAR_H
#define __NEDGRAMMAR_H

class NEDElement;
class NedFileNode;
class NEDParser;
#define YYSTYPE   NEDElement*

#ifndef YYLTYPE
struct my_yyltype {
   int dumy;
   int first_line, first_column;
   int last_line, last_column;
   char *text;
};
#define YYLTYPE  struct my_yyltype
#else
#error 'YYLTYPE defined before ebnf.h -- type clash?'
#endif

typedef struct {int li; int co;} LineColumn;

extern LineColumn pos,prevpos;

extern FILE *yyin;
extern FILE *yyout;

struct yy_buffer_state;

struct yy_buffer_state *yy_scan_string(const char *str);
void yy_delete_buffer(struct yy_buffer_state *);

int runparse (NEDParser *p,NedFileNode *nf,bool parseexpr,bool storesrc, const char *sourcefname);

extern int use_chanattrname_token;

#endif



