//=========================================================================
//  FILEREADERCONSUMER.CC - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include "exception.h"
#include "platmisc.h"
#include "filereader.h"
#include "timeutil.h"


void runConsumer(const char *filename, double duration)
{
    timeval start,end, now;
    gettimeofday(&start,NULL);
    end = timeval_add(start, duration);

    while (true)
    {
        if (duration > 0) {
            gettimeofday(&now, NULL);
            if (timeval_greater(now, end))
                break;
        }

        FileReader reader(filename);
        char *line;
        while ((line=reader.getNextLineBufferPointer())!=NULL)
        {
            int len=reader.getCurrentLineLength();
            if (len < 0)
                throw opp_runtime_error("Line length is negative: %s\n", len);
        }

    }
}

void usage(const char *message)
{
    if (message)
        fprintf(stderr, "Error: %s\n\n", message);

    fprintf(stderr, ""
"Usage:\n"
"   filereaderconsumer <input-file-name> <duration>\n"
);
}

int main(int argc, char **argv)
{
    try {
        if (argc < 3) {
            usage("Not enough arguments specified");

            return -1;
        }
        else {
            const char *filename = argv[1];
            char *e;
            double duration = strtod(argv[2],&e);
            if (*e || duration < 0) {
                usage("<duration> should be a non-negative number");
                return -1;
            }

            usleep(500000);
            runConsumer(filename, duration);
            printf("PASS\n");

            return 0;
        }
    }
    catch (std::exception& e) {
        printf("FAIL: %s", e.what());

        return -2;
    }
}
