#
# opp_makemake_vc (formerly jar_mkmk)
#
#  Creates an MSVC makefile for a given OMNeT++ model.
#  Assumes that .ned, .cc and .h files are in one directory.
#  The name of the program defaults to the name of the directory ('myproject').
#
#  --VA
#

#
# process command line args
#
use Cwd;

# input parameters
$outfile = cwd;
$outfile  =~ s/[\/\\]$//;
$outfile  =~ s/.*[\/\\]//;

$userif = "TKENV";
$type = "exe";
$includes = "";
$libpath = "";
$libs = "";
$dirs = "";
$xobjs = "";
@fragments = ();
$doxyconf = "doxy.cfg";
@nedfiles = ();
$ccext = "cpp";
$cfgdir="../..";

# process arg vector
while (@ARGV)
{
    $arg = shift @ARGV;
    if ($arg eq '-h' || $arg eq '--help')
    {
        print '
opp_makemake: write OMNeT++ Makefile for MSVC, based on source files in current directory

opp_makemake [-h] [-f] [-e ext] [-o make-target] [-n] [-s] [-u user-interface]
         [-p] [-Idir] [-Llibrary-path] [-llibrary] [-c configdir] [-i makefile-fragment-file]
         [directories, library and object files]...
    -h, --help            This help text
    -f, --force           Force overwriting existing Makefile
    -e, --ext             Assumed extension of C++ sources (default is "cpp")
    -o, --outputfile      Name of simulation executable/library
    -n, --nolink          Produce object files but do not create executable or
                          library. Useful for models with parts in several dirs.
    -s, --make-so         Make shared library. Useful if you want to load the
                          model dynamically (Cmdenv/Tkenv -l so-file switch).
    -u, --userinterface   Use Cmdenv or Tkenv. Defaults to Tkenv
    -M, --mpi             Link executable with MPI
    -p, --pvm             Link executable with PVM
    -Idir                 Additional NED and C++ include directory
    -Llibrary-path        Additional library path
    -llibrary             Additional library to link against
    -c, --configdir       Directory of configuser.vc (default is "../..")
    -i, --includefragment Append file to near end of Makefile. The file makefrag.vc
                          is appended implicitly if no -i options are given.
    directory             Recursive make in that dir. Makemake will need
                          to call make there to see what object files to
                          include in the Makefile.
    library or object     Link with that file

With the -n and -s options, -u, -p and -l have no effect.
If you have a source tree instead of a single directory, run opp_makemake -n
in the subdirs and then opp_makemake dir1 dir2 etc. in your top-level directory.
The -i option is useful if a source file (.ned or .cc) is to be generated
from other files.
';
        exit(1);
    }
    elsif ($arg eq '-f' || $arg eq '--force')
    {
        $force = 'y';
    }
    elsif ($arg eq '-e' || $arg eq '--ext')
    {
        $ccext = shift @ARGV;
    }
    elsif ($arg eq '-o' || $arg eq '--outputfile')
    {
        $outfile = shift @ARGV;
    }
    elsif ($arg eq '-n' || $arg eq '--nolink')
    {
        $type = "o";
    }
    elsif ($arg eq '-s' || $arg eq '--make-so')
    {
        $type = "so";
    }
    elsif ($arg eq '-u' || $arg eq '--userinterface')
    {
        $userif = shift @ARGV;
        $userif = uc($userif);
        if ($userif ne "CMDENV" && $userif ne "TKENV")
        {
            print STDERR "opp_makemake: -u: valid user interface names are Cmdenv or Tkenv";
            exit;
        }
    }
    elsif ($arg eq '-M' || $arg eq '--mpi')
    {
        $mpi = "y";
    }
    elsif ($arg eq '-p' || $arg eq '--pvm')
    {
        $pvm = "y";
    }
    elsif ($arg eq '-i' || $arg eq '--includefragment')
    {
        push(@fragments, shift @ARGV);
    }
    elsif ($arg =~ /^-I/)
    {
        $includes .= " ".$arg;
    }
    elsif ($arg =~ /^-L/)
    {
        $libpath .= " ".$arg;
    }
    elsif ($arg =~ /^-l/)
    {
        $libs .= " ".$arg;
    }
    else
    {
        if (-d $ARGV[1])
        {
            push(@dirs, $ARGV[1]);
        }
        else
        {
            if (! -f $ARGV[1])
            {
                print STDERR"opp_makemake: $ARGV[1] is neither an existing file/dir nor a valid option";
                exit;
            }
            push(@xobjs, $ARGV[1]);
        }
    }
}

$makefile = "Makefile.vc";

if (-f $makefile && $force ne "y")
{
    print stderr "opp_makemake: use -f to force overwriting existing $makefile\n";
    exit;
}

@sobjs = ();

$makecommand = 'nmake -f Makefile.vc';

foreach $i (@dirs)
{
    print "*** Running make in $i to see object and ned file names\n";
    if (chdir($i) && system($makecommand)==0)
    {
        @o = glob("$i/*.obj");
        if (@o == ())
        {
            print STDERR "opp_makemake: warning: no object files in $i\n";
        }
        else
        {
            push(@sobjs, @o);
        }

        # collect ned files of subdirectory for target neddoc.html.
        @nedf = glob("$i/*.ned");
        push(@nedfiles, @nedf);
    }
    else
    {
        print STDERR "opp_makemake: make in $i failed, exiting\n";
        exit;
    }
}

@objs = glob("*.ned *.$ccext");
foreach $i (@objs)
{

    $i =~ s/\*[^ ]*//g;
    $i =~ s/[^ ]*_n\.$ccext//g;
    $i =~ s/\.ned/_n.obj/g;
    $i =~ s/\.$ccext/.obj/g;
}

#
# now the Makefile creation
#
open(OUT, ">$makefile");

print OUT "#
#  Makefile for $outfile
#
#  ** This file was automatically generated by the command:
#  opp_makemake $args
#
#  By perl version of opp_makemake
#
";

$suffix = '.exe';
if ($type eq "so")
{
    $suffix = '.dll';
}
if ($type eq "o")
{
    $suffix = '';
}

if ($userif eq 'CMDENV')
{
    $c_cmd = '';
    $c_tk = '# ';
}
elsif ($userif eq 'TKENV')
{
    $c_cmd = '# ';
    $c_tk = '';
}


$c_std = '';
$c_pvm = '# ';
$c_mpi = '# ';

if ($pvm eq "y")
{
    $c_std = '# ';
    $c_pvm = '';
    $c_mpi = '#';
}

if ($mpi eq "y")
{
    $c_std = '# ';
    $c_pvm = '#';
    $c_mpi = '';
}

print OUT "

# Name of target to be created (-o option)
TARGET = $outfile$suffix

# User interface (uncomment one) (-u option)
${c_cmd}USERIF_LIBS=\$(CMDENV_LIBS)
${c_tk}USERIF_LIBS=\$(TKENV_LIBS)

# uncomment 1 of the 3 lines to support either serial or parallel operation
${c_std}KERNEL_LIBS=\$(STD_KERNEL_LIBS)
${c_pvm}KERNEL_LIBS=\$(PVM_KERNEL_LIBS)
${c_mpi}KERNEL_LIBS=\$(MPI_KERNEL_LIBS)

# .ned or .h include paths with -I
INCLUDE_PATH=$includes

# misc additional object and library files to link
EXRA_OBJS=$xobjs

# object files in subdirectories
SUBDIR_OBJS=$sobjs

# Additional libraries (-L option -l option)
LIBS=$libpath$libs

#------------------------------------------------------------------------------
";

# Makefile
print OUT "

!include \"$cfgdir/configuser.vc\"

# User interface libs
CMDENV_LIBS=envir.lib cmdenv.lib
TKENV_LIBS=envir.lib tkenv.lib \$(TK_LIBS)

# Simulation kernel
STD_KERNEL_LIBS=sim_std.lib
MPI_KERNEL_LIBS=sim_mpi.lib \$(MPI_LIBS)
PVM_KERNEL_LIBS=sim_pvm.lib \$(PVM_LIBS)

# Simulation kernel and user interface libraries
OMNETPP_LIBS=/libpath:\$(OMNETPP_LIB_DIR) \$(USERIF_LIBS) \$(KERNEL_LIBS) \$(SYS_LIBS)

COPTS=\$(CFLAGS) \$(INCLUDE_PATH) -I\$(OMNETPP_INCL_DIR)
NEDCOPTS=\$(CFLAGS) \$(NEDCFLAGS) \$(INCLUDE_PATH) -I\$(OMNETPP_INCL_DIR)

#------------------------------------------------------------------------------
";

if ($dirs ne "" )
{
    $subdirsphony = "subdirs-phony";
}
else
{
    $subdirsphony = "";
}

# rules for $(TARGET)
$objs = join(" ", @objs);
print OUT "# Object files from this directory to link\n";
print OUT "OBJS= $objs\n";
print OUT "\n";

if ($type eq 'exe')
{
    print OUT "\$(TARGET): \$(OBJS) \$(EXRA_OBJS) $makefile $subdirsphony\n";
    print OUT "\t\$(LINK) \$(LDFLAGS) \$(OBJS) \$(EXRA_OBJS) \$(SUBDIR_OBJS) \$(LIBS) \$(OMNETPP_LIBS) /out:\$(TARGET)\n";
}
elsif ($type eq 'so')
{
    print OUT "\$(TARGET): \$(OBJS) \$(EXRA_OBJS) $subdirsphony $makefile\n";
    print OUT "\t\$(SHLIB_LD) -o \$(TARGET) \$(OBJS) \$(EXRA_OBJS) \$(SUBDIR_OBJS)\n";
}
elsif ($type eq 'o')
{
    print OUT "\$(TARGET): \$(OBJS) $makefile\n";
}
print OUT "\n";

# rule for Purify
print OUT "# purify: \$(OBJS) \$(EXRA_OBJS) $subdirsphony $makefile\n";
print OUT "# \tpurify \$(CXX) \$(LDFLAGS) \$(OBJS) \$(EXRA_OBJS) \$(SUBDIR_OBJS) \$(LIBS) -L\$(OMNETPP_LIB_DIR) \$(KERNEL_LIBS) \$(USERIF_LIBS) \$(SYS_LIBS_PURE) -o \$(TARGET).pure\n";
print OUT "\n";

if ($type ne "o")
{
    if ($dirs ne "")
    {
        print OUT 'subdirs-phony:';
        foreach $i (@dirs)
        {
           print OUT "\t(cd $i && make)\n";
        }
        print OUT "\n";
    }
}

# rules for NED files
foreach $i (glob("*.ned"))
{
    # extend list for target neddoc.html
    push(@nedfiles, $i);

    $obj = $i;
    $obj =~ s/\.ned/_n.obj/g;
    $c = $i;
    $c =~ s/\.ned/_n.$ccext/g;

    print OUT "$obj: $c\n";
    print OUT "\t\$(CXX) -c \$(NEDCOPTS) /Tp $c\n";
    print OUT "\n";

    print OUT "$c: $i\n";
    print OUT "\t\$(NEDC) -s _n.$ccext \$(INCLUDE_PATH) $i\n";
    print OUT "\n";
}

# rules for non-NED C++ files
foreach $i (glob("*.$ccext"))
{
    if ($i =~ /_n\.$ccext/)
    {
        next;
    }

    $obj = $i;
    $obj =~ s/\.$ccext/.obj/g;
    print OUT "$obj: $i\n";
    print OUT "\t\$(CXX) -c \$(COPTS) /Tp $i\n";
    print OUT "\n";
}

# documentation targets
print OUT "
doc: neddoc.html htmldocs

neddoc.html: $nedfiles
\t\@opp_neddoc $nedfiles > neddoc.html
\t\@echo File neddoc.html generated.

htmldocs:
\t\@doxygen -g- | sed \"s/^PROJECT_NAME.*/PROJECT_NAME = $outfile/;\\
\ts|^INPUT *=.*|INPUT = . $dirs|;\\
\ts/^EXTRACT_ALL *=.*/EXTRACT_ALL = yes/;\\
\ts/^EXTRACT_PRIVATE *=.*/EXTRACT_PRIVATE = yes/;\\
\ts/^EXCLUDE_PATTERNS *=.*/EXCLUDE_PATTERNS = *_n.$ccext *_n.h/;\\
\ts/^ALPHABETICAL_INDEX *=.*/ALPHABETICAL_INDEX = yes/;\\
\ts/^HTML_OUTPUT *=.*/HTML_OUTPUT = htmldoc/;\\
\ts/^GENERATE_LATEX *=.*/GENERATE_LATEX = no/;\\
\ts/^GENERATE_TREEVIEW *=.*/GENERATE_TREEVIEW = yes/;\\
\ts/^HIDE_UNDOC_RELATIONS *=.*/HIDE_UNDOC_RELATIONS = no/;\\
\ts|^TAGFILES *=.*|TAGFILES = \$(OMNETPP_ROOT)/doc/api/opptags.xml=\$(OMNETPP_ROOT)/doc/api|;\\
\ts|^GENERATE_TAGFILE *=.*|GENERATE_TAGFILE = htmldoc/tags.xml|;\\
\ts/^QUIET *=.*/QUIET = yes/\" > $doxyconf
\t\@doxygen $doxyconf
\t\@echo Code documentation generated. Now, point your web browser to ./htmldoc/index.html.
";

# include external Makefile fragments
if (@fragments != ())
{
    foreach $frag (@fragments)
    {
        print OUT "# inserted from file '$frag':\n";
        open(FRAG,$frag);
        while(<FRAG>)  {print OUT;}
        close FRAG;
        print OUT "\n";
    }
}
else
{
    if (-f "makefrag.vc")
    {
        print OUT "# inserted from file 'makefrag.vc':\n";
        open(FRAG,"makefrag.vc");
        while(<FRAG>)  {print OUT;}
        close FRAG;
        print OUT "\n";
    }
}

# rule for 'clean'
print OUT "
clean:
\trm -f *.obj *_n.$ccext *_n.h \$(TARGET)\$(EXE_SUFFIX)
\trm -f *.vec *.sca
\trm -rf neddoc.html htmldoc
";

# makedepend, re-makemake
@args1 = @ARGV;
foreach $i (@args1)
{
    $i =~ s/ / /g;
    $i =~ s/ -f / /g;
    $i =~ s/ -m / /g;
}
$args1 = join(" ",@args1);

print OUT "
depend:
\t\$(MAKEDEPEND) \$(INCLUDE_PATH) -- *.$ccext

re-makemake:
\topp_makemake_vc -f $args1  #recreate Makefile

# DO NOT DELETE THIS LINE -- make depend depends on it.

";

close OUT;

print "$makefile created -- type 'nmake -f $makefile depend' to add dependencies to it\n";



