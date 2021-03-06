/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.script;

import static org.omnetpp.scave.engine.ResultItemField.FILE;
import static org.omnetpp.scave.engine.ResultItemField.MODULE;
import static org.omnetpp.scave.engine.ResultItemField.NAME;
import static org.omnetpp.scave.engine.ResultItemField.RUN;
import static org.omnetpp.scave.engine.RunAttribute.EXPERIMENT;
import static org.omnetpp.scave.engine.RunAttribute.MEASUREMENT;
import static org.omnetpp.scave.engine.RunAttribute.REPLICATION;
import static org.omnetpp.scave.engine.RunAttribute.RUNNUMBER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.omnetpp.common.Debug;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.scave.charting.dataset.CompoundXYDataset;
import org.omnetpp.scave.charting.dataset.HistogramDataset;
import org.omnetpp.scave.charting.dataset.IHistogramDataset;
import org.omnetpp.scave.charting.dataset.IXYDataset;
import org.omnetpp.scave.charting.dataset.ScalarDataset;
import org.omnetpp.scave.charting.dataset.ScalarScatterPlotDataset;
import org.omnetpp.scave.charting.dataset.VectorDataset;
import org.omnetpp.scave.charting.dataset.VectorScatterPlotDataset;
import org.omnetpp.scave.charting.dataset.XYVector;
import org.omnetpp.scave.engine.DataSorter;
import org.omnetpp.scave.engine.DataflowManager;
import org.omnetpp.scave.engine.IDList;
import org.omnetpp.scave.engine.Node;
import org.omnetpp.scave.engine.NodeType;
import org.omnetpp.scave.engine.NodeTypeRegistry;
import org.omnetpp.scave.engine.Port;
import org.omnetpp.scave.engine.ResultFile;
import org.omnetpp.scave.engine.ResultFileList;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engine.ResultItem;
import org.omnetpp.scave.engine.ResultItemField;
import org.omnetpp.scave.engine.ResultItemFields;
import org.omnetpp.scave.engine.ScalarResult;
import org.omnetpp.scave.engine.StringMap;
import org.omnetpp.scave.engine.StringVector;
import org.omnetpp.scave.engine.VectorResult;
import org.omnetpp.scave.engine.XYArray;
import org.omnetpp.scave.engine.XYDataset;
import org.omnetpp.scave.engine.XYDatasetVector;
import org.omnetpp.scave.model.BarChart;
import org.omnetpp.scave.model.Chart;
import org.omnetpp.scave.model.HistogramChart;
import org.omnetpp.scave.model.LineChart;
import org.omnetpp.scave.model.ResultType;
import org.omnetpp.scave.model.ScatterChart;
import org.omnetpp.scave.model2.FilterUtil;
import org.omnetpp.scave.model2.IsoLineData;
import org.omnetpp.scave.model2.ResultItemFormatter;
import org.omnetpp.scave.model2.ScaveModelUtil;

/**
 * This class calculates the content of a chart
 * applying the operations described by the chart.
 *
 * @author tomi
 */
//TODO proper error handling for scripts (syntax validation during editing, runtime errors as markers, etc)
public class ScriptEngine {
    private static final boolean debug = false;

    public static class ResultSet {
        public IDList idList = new IDList();
        Map<Long,XYVector> vectorData = new HashMap<>(); // keyed on ID
    }

    public static ResultSet evaluateScript(ScriptCommand[] script, ResultFileManager manager, IProgressMonitor progressMonitor) {
        checkReadLock(manager);

        try {
            ResultSet resultSet = new ResultSet();
            for (ScriptCommand command : script) {
                Debug.println("DOING SCRIPT COMMAND: " + command);
                long startTime = System.currentTimeMillis();
                if (command instanceof AddCommand) {
                    AddCommand addCommand = (AddCommand)command;
                    IDList idlist = ScriptEngine.getFilteredIDList(manager, addCommand.getResultType(), addCommand.getFilterExpression());
                    resultSet.idList.merge(idlist);

                    // load vector data -- TODO maybe defer to the end (or to the first "APPLY" operation, so that the vector file needs to be read only once
                    IDList vectorIDs = idlist.filterByTypes(ResultFileManager.VECTOR);
                    XYVector[] vectors = getDataOfVectors(manager, vectorIDs, progressMonitor);
                    for (int i = 0; i < vectorIDs.size(); i++)
                        resultSet.vectorData.put(vectorIDs.get(i), vectors[i]);
                }
                else {
                    throw new RuntimeException("Unknown command " + command.toString());
                }
                Debug.println(command.getClass().getSimpleName() + " took " + (System.currentTimeMillis()-startTime) + "ms");
            }

            return resultSet;
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    //TODO used from ScatterChartEditForm (???)
    public static IDList getIDListFromDataset(ResultFileManager manager, Chart chart, ResultType type) {
        checkReadLock(manager);

        //TODO rewrite!

        IDList pool;
        switch (type) {
            case SCALAR_LITERAL: pool = manager.getAllScalars(); break;
            case VECTOR_LITERAL: pool = manager.getAllVectors(); break;
            case HISTOGRAM_LITERAL: pool = manager.getAllHistograms(); break;
            default: pool = new IDList();
        }

        IDList result = new IDList();
        String inputString = StringUtils.nullToEmpty(chart.getScript());
        for (String filter : inputString.split("\n")) { // each line is a filter expression (for now)
            if (!StringUtils.isBlank(filter)) {
                IDList filtered = manager.filterIDList(pool, filter);
                result.merge(filtered);
            }
        }

        Debug.println("DatasetManager.getIDListFromDataset(): FILTER MATCHED " + result.size() + " ITEMS OF " + pool.size());
        return result;
    }

    private static IDList getFilteredIDList(ResultFileManager manager, ResultType type, String filter) {
        checkReadLock(manager);
        IDList pool;
        switch (type) {
            case SCALAR_LITERAL: pool = manager.getAllScalars(); break;
            case VECTOR_LITERAL: pool = manager.getAllVectors(); break;
            case HISTOGRAM_LITERAL: pool = manager.getAllHistograms(); break;
            default: pool = new IDList();
        }

        IDList result = StringUtils.isBlank(filter) ? pool : manager.filterIDList(pool, filter);
        Debug.println("DatasetManager.getIDListFromDataset(): FILTER MATCHED " + result.size() + " ITEMS OF " + pool.size());
        return result;
    }

    public static XYVector[] getDataOfVectors(ResultFileManager manager, IDList idlist, IProgressMonitor progressMonitor) {
        long startTime = System.currentTimeMillis();
        DataflowManager dataflowManager = new DataflowManager();
        String readerNodeTypeName = "vectorreaderbyfiletype";
        NodeTypeRegistry registry = NodeTypeRegistry.getInstance();
        NodeType readerNodeType = registry.getNodeType(readerNodeTypeName);
        Assert.isNotNull(readerNodeType, "unknown node type " + readerNodeTypeName);

        ResultFileList filteredVectorFileList = manager.getUniqueFiles(idlist);
        Map<ResultFile, Node> vectorFileReaders = new HashMap<>();
        for (int i = 0; i < (int)filteredVectorFileList.size(); i++) {
            ResultFile resultFile = filteredVectorFileList.get(i);
            StringMap attrs = new StringMap();
            attrs.set("filename", resultFile.getFileSystemFilePath());
            attrs.set("allowindexing", "true");
            Node readerNode = readerNodeType.create(dataflowManager, attrs);
            vectorFileReaders.put(resultFile, readerNode);
        }

        NodeType arrayBuilderNodeType = registry.getNodeType("arraybuilder");
        Assert.isNotNull(arrayBuilderNodeType, "unknown node type arraybuilder");

        List<Node> arrayBuilders = new ArrayList<>();

        for (int i = 0; i < idlist.size(); i++) {
            // create a port for each vector on its reader node
            VectorResult vector = manager.getVector(idlist.get(i));
            String portName = "" + vector.getVectorId();
            Node readerNode = vectorFileReaders.get(vector.getFile());
            Assert.isNotNull(readerNode);
            Port outPort = readerNodeType.getPort(readerNode, portName);

            // create writer node(s) and connect them
            Node arrayBuilderNode = arrayBuilderNodeType.create(dataflowManager, new StringMap());
            dataflowManager.connect(outPort, arrayBuilderNodeType.getPort(arrayBuilderNode, "in"));
            arrayBuilders.add(arrayBuilderNode);
        }

        XYArray[] arrays = null;
        if (dataflowManager != null) {
            if (debug)
                dataflowManager.dump();
            try {
                arrays = executeDataflowNetwork(dataflowManager, arrayBuilders, progressMonitor);
            }
            finally {
                dataflowManager.delete();
                dataflowManager = null;
            }
        }

        // convert native XYArrays to pure Java XYVectors
        XYVector[] vectors = new XYVector[idlist.size()];
        if (arrays != null) { //TODO ??? check how it can be null
            Assert.isTrue(arrays.length == idlist.size());
            for (int i = 0; i < arrays.length; i++)
                vectors[i] = new XYVector(arrays[i]);
        }

        Debug.println("getDataOfVectors() took " + (System.currentTimeMillis()-startTime) + "ms");

        return vectors;
    }

    private static XYArray[] executeDataflowNetwork(DataflowManager manager, List<Node> arrayBuilders, IProgressMonitor monitor) {
        long startTime = System.currentTimeMillis();
        if (arrayBuilders.size() > 0) // XXX DataflowManager crashes when there are no sinks
            manager.execute(monitor);
        if (debug)
            Debug.println("execute dataflow network: "+(System.currentTimeMillis()-startTime)+" ms");

        XYArray[] result = new XYArray[arrayBuilders.size()];
        for (int i = 0; i < result.length; ++i)
            result[i] = arrayBuilders.get(i).getArray();
        return result;
    }

    public static ScalarDataset createScalarDataset(BarChart chart, ResultFileManager manager, IProgressMonitor progressMonitor) {
        checkReadLock(manager);
        try {
            String script = StringUtils.nullToEmpty(chart.getScript());
            ScriptCommand[] commands = ScriptParser.parseScript(script);
            ResultSet resultSet = evaluateScript(commands, manager, progressMonitor);
            if (progressMonitor != null && progressMonitor.isCanceled())
                return null;

            // take the scalars only
            IDList scalarIDs = resultSet.idList.filterByTypes(ResultFileManager.SCALAR);
            ScalarDataset result = new ScalarDataset(scalarIDs, chart.getGroupByFields(), chart.getBarFields(), chart.getAveragedFields(), manager);
            return result;
        }
        catch (Exception e) {
            throw new RuntimeException("Chart '" + chart.getName() + "': " + e.getMessage());
        }
    }

    public static VectorDataset createVectorDataset(LineChart chart, ResultFileManager manager, IProgressMonitor progressMonitor) {
        checkReadLock(manager);
        return createVectorDataset(chart, chart.getLineNameFormat(), true, manager, progressMonitor);
    }

    public static VectorDataset createVectorDataset(LineChart chart, String lineNameFormat, boolean computeData, ResultFileManager manager, IProgressMonitor progressMonitor) { //TODO use computeData
        checkReadLock(manager);

        try {
            String script = StringUtils.nullToEmpty(chart.getScript());
            ScriptCommand[] commands = ScriptParser.parseScript(script);
            ResultSet resultSet = evaluateScript(commands, manager, progressMonitor);

            if (progressMonitor != null && progressMonitor.isCanceled())
                return null;

            // take the vector only
            IDList vectorIDs = resultSet.idList.filterByTypes(ResultFileManager.VECTOR);
            XYVector[] vectors = new XYVector[vectorIDs.size()];
            for (int i = 0; i < vectorIDs.size(); i++)
                vectors[i] = resultSet.vectorData.get(vectorIDs.get(i));

            // make default title (TODO why here?)
            ResultItem[] items = ScaveModelUtil.getResultItems(vectorIDs, manager);
            String title = items.length <= 1 ? null : defaultTitle(items);
            String lineTitleFormat = defaultResultItemTitleFormat(items);

            // create vector dataset
            return new VectorDataset(title, vectorIDs, vectors, lineNameFormat, lineTitleFormat, manager);
        }
        catch (Exception e) {
            throw new RuntimeException("Chart '" + chart.getName() + "': " + e.getMessage(), e);
        }

    }

    public static IXYDataset createScatterPlotDataset(ScatterChart chart, ResultFileManager manager, IProgressMonitor progressMonitor) {
        checkReadLock(manager);

        checkReadLock(manager);
        try {
            String script = StringUtils.nullToEmpty(chart.getScript());
            ScriptCommand[] commands = ScriptParser.parseScript(script);
            ResultSet resultSet = evaluateScript(commands, manager, progressMonitor);

            if (progressMonitor != null && progressMonitor.isCanceled())
                return null;

            IDList scalars = resultSet.idList.filterByTypes(ResultType.SCALAR);
            IDList vectors = resultSet.idList.filterByTypes(ResultType.VECTOR);
            String xDataPattern = chart.getXDataPattern();
            List<String> isoPatterns = chart.getIsoDataPattern();
            boolean averageReplications = chart.isAverageReplications();

            return createScatterPlotDataset(scalars, vectors, xDataPattern, isoPatterns, averageReplications, manager);
        }
        catch (Exception e) {
            throw new RuntimeException("Chart '" + chart.getName() + "': " + e.getMessage());
        }
    }

    private static IXYDataset createScatterPlotDataset(IDList scalars, IDList vectors, String xDataPattern, List<String> isoPatterns, boolean averageReplications, ResultFileManager manager) {
        // process scalars
        XYDatasetVector xyScalars = null;
        List<IsoLineData[]> isoLineIds = null;
        if (!scalars.isEmpty()) {
            IsoLineData xData = IsoLineData.fromFilterPattern(xDataPattern);
            Assert.isLegal(xData != null && xData.getModuleName() != null && xData.getDataName() != null, "X data is not selected.");

            String xModuleName = xData.getModuleName();
            String xScalarName = xData.getDataName();

            StringVector isoModuleNames = new StringVector();
            StringVector isoScalarNames = new StringVector();
            StringVector isoAttrNames = new StringVector();
            collectIsoParameters(isoPatterns, isoModuleNames, isoScalarNames, isoAttrNames);

            DataSorter sorter = new DataSorter(manager);
            ResultItemFields rowFields = new ResultItemFields(MODULE, NAME);
            ResultItemFields columnFields = averageReplications ? new ResultItemFields(EXPERIMENT, MEASUREMENT) :
                new ResultItemFields(RUN, EXPERIMENT, MEASUREMENT, REPLICATION);
            xyScalars = sorter.prepareScatterPlot3(scalars,
                    xModuleName, xScalarName, rowFields, columnFields,
                    isoModuleNames, isoScalarNames, new ResultItemFields(isoAttrNames));
            // XXX isoLineIds should be returned by prepareScatterPlot3
            isoLineIds = collectIsoLineIds(scalars, isoModuleNames, isoScalarNames, isoAttrNames, xyScalars, manager);
            // assertOrdered(xyScalars);
        }

        // process vectors
        XYArray[] xyVectors = null;
        IDList yVectors = null;
        //TODO
        //            if (!vectors.isEmpty()) {
        //                DataflowNetworkBuilder builder = new DataflowNetworkBuilder(manager);
        //                DataflowManager dataflowManager = builder.build(chart, chart, true /*computeData*/); // XXX
        //                yVectors = builder.getDisplayedIDs();
        //
        //                if (dataflowManager != null) {
        //                    List<Node> arrayBuilders = builder.getArrayBuilders();
        //                    if (debug) dataflowManager.dump();
        //                    try {
        //                        xyVectors = executeDataflowNetwork(dataflowManager, arrayBuilders, monitor);
        //                    }
        //                    finally {
        //                        dataflowManager.delete();
        //                        dataflowManager = null;
        //                    }
        //                    if (xyVectors != null) {
        //                        for (int i = 0; i < xyVectors.length; ++i)
        //                            xyVectors[i].sortByX();
        //                    }
        //                }
        //            }

        // compose results
        IXYDataset result;
        if (xyScalars != null && xyVectors == null)
            result = createScatterPlotDataset(xyScalars, isoLineIds, manager);
        else if (xyScalars == null && xyVectors != null)
            result = new VectorScatterPlotDataset(yVectors, xyVectors, manager);
        else if (xyScalars != null && xyVectors != null)
            result = new CompoundXYDataset(
                    createScatterPlotDataset(xyScalars, isoLineIds, manager),
                    new VectorScatterPlotDataset(yVectors, xyVectors, manager));
        else
            result = null; //TODO ???
        return result;
    }

    /**
     * Collect the module/scalar names or attribute names from the filter patterns.
     */
    private static void collectIsoParameters(List<String> isoPatterns,
                            StringVector isoModuleNames, StringVector isoScalarNames, StringVector isoAttrNames) {

        for (String pattern : isoPatterns) {
            IsoLineData filter = IsoLineData.fromFilterPattern(pattern);
            if (filter != null && filter.isValid()) {
                if (filter.getModuleName() != null && filter.getDataName() != null) {
                    isoModuleNames.add(filter.getModuleName());
                    isoScalarNames.add(filter.getDataName());
                }
                else if (filter.getAttributeName() != null) {
                    isoAttrNames.add(filter.getAttributeName());
                }
            }
        }
    }

    private static List<IsoLineData[]> collectIsoLineIds(IDList scalars,
            StringVector isoModuleNames, StringVector isoScalarNames, StringVector isoAttrNames,
            XYDatasetVector xyScalars, ResultFileManager manager) {
        checkReadLock(manager);
        List<IsoLineData[]> isoLineIds = new ArrayList<IsoLineData[]>((int)xyScalars.size());
        for (int i = 0; i < xyScalars.size(); ++i) {
            XYDataset xyData = xyScalars.get(i);
            Assert.isTrue(xyData.getRowCount() > 0);
            String runName = xyData.getRowFieldNoCheck(0, FLD_RUN);
            FilterUtil filter = new FilterUtil();
            filter.setField(RUN, runName);
            List<IsoLineData> isoLineId = new ArrayList<IsoLineData>((int)(isoModuleNames.size()+isoAttrNames.size()));
            for (int j = 0; j < isoModuleNames.size(); ++j) {
                filter.setField(MODULE, isoModuleNames.get(j));
                filter.setField(NAME, isoScalarNames.get(j));
                IDList idlist = manager.filterIDList(scalars, filter.getFilterPattern());
                if (idlist.size() > 0) {
                    ScalarResult scalar = manager.getScalar(idlist.get(0));
                    IsoLineData data = new IsoLineData(isoModuleNames.get(j), isoScalarNames.get(j));
                    data.setValue(String.valueOf(scalar.getValue()));
                    isoLineId.add(data);
                }
            }
            for (int j = 0; j < isoAttrNames.size(); ++j) {
                String attrName = isoAttrNames.get(j);
                String value = xyData.getRowFieldNoCheck(0, new ResultItemField(attrName));
                if (value != null) {
                    IsoLineData data = new IsoLineData(attrName);
                    data.setValue(value);
                    isoLineId.add(data);
                }
            }
            isoLineIds.add(isoLineId.toArray(new IsoLineData[isoLineId.size()]));
        }

        return isoLineIds;
    }

    public static void assertOrdered(XYDatasetVector xyDatasets) {
        for(int i = 0; i < xyDatasets.size(); ++i) {
            XYDataset ds = xyDatasets.get(i);
            double prevX = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < ds.getColumnCount(); ++j) {
                double x = ds.getValue(0, j).getMean();
                if (!Double.isNaN(x)) {
                    if (x < prevX)
                        Assert.isTrue(false, "Not ordered");
                    prevX = x;
                }
            }
        }
    }

    public static IXYDataset createScatterPlotDataset(XYDatasetVector xydatasets, List<IsoLineData[]> isoLineIds, ResultFileManager manager) {
        Assert.isTrue(isoLineIds == null || xydatasets.size() == isoLineIds.size());
        checkReadLock(manager);

        IXYDataset[] datasets = new IXYDataset[(int)xydatasets.size()];
        for (int i = 0; i < datasets.length; ++i)
            datasets[i] = new ScalarScatterPlotDataset(xydatasets.get(i), isoLineIds == null ? null : isoLineIds.get(i));
        return new CompoundXYDataset(datasets);
    }

    public static IXYDataset createXYDataset(Chart chart, boolean computeData, ResultFileManager manager, IProgressMonitor monitor) {
        checkReadLock(manager);

        if (chart instanceof LineChart) {
            LineChart lineChart = (LineChart)chart;
            return createVectorDataset(lineChart, lineChart.getLineNameFormat(), computeData, manager, monitor);
        }
        else if (chart instanceof ScatterChart)
            return createScatterPlotDataset((ScatterChart)chart, manager, monitor);
        else
            return null;
    }

    public static IHistogramDataset createHistogramDataset(HistogramChart chart, ResultFileManager manager, IProgressMonitor progressMonitor) {
        checkReadLock(manager);
        try {
            String script = StringUtils.nullToEmpty(chart.getScript());
            ScriptCommand[] commands = ScriptParser.parseScript(script);
            ResultSet resultSet = evaluateScript(commands, manager, progressMonitor);

            if (progressMonitor != null && progressMonitor.isCanceled())
                return null;

            // take the histograms only
            IDList histogramIDs = resultSet.idList.filterByTypes(ResultFileManager.HISTOGRAM);
            return new HistogramDataset(histogramIDs, manager);
        }
        catch (Exception e) {
            throw new RuntimeException("Chart '" + chart.getName() + "': " + e.getMessage());
        }
    }

    public static String[] getResultItemNames(IDList idlist, String nameFormat, ResultFileManager manager) {
        checkReadLock(manager);
        ResultItem[] items = ScaveModelUtil.getResultItems(idlist, manager);
        String format = StringUtils.defaultIfEmpty(nameFormat, defaultNameFormat(items));
        return ResultItemFormatter.formatResultItems(format, items);
    }

    private static final ResultItemField
        FLD_FILE = new ResultItemField(FILE),
        FLD_RUN = new ResultItemField(RUN),
        FLD_RUNNUMBER = new ResultItemField(RUNNUMBER),
        FLD_MODULE = new ResultItemField(MODULE),
        FLD_NAME = new ResultItemField(NAME),
        FLD_EXPERIMENT = new ResultItemField(EXPERIMENT),
        FLD_MEASUREMENT = new ResultItemField(MEASUREMENT),
        FLD_REPLICATION = new ResultItemField(REPLICATION);

    private static final ResultItemField[] FIELDS_OF_LINENAMES = new ResultItemField[] {
        FLD_FILE, FLD_RUN,
        FLD_RUNNUMBER, FLD_MODULE,
        FLD_NAME, FLD_EXPERIMENT,
        FLD_MEASUREMENT, FLD_REPLICATION
    };

    private static final List<ResultItemField> complement(List<ResultItemField> fields) {
        List<ResultItemField> result = new ArrayList<ResultItemField>(Arrays.asList(FIELDS_OF_LINENAMES));
        result.removeAll(fields);
        return result;
    }

    /**
     * Returns the default format string for names of the {@code items}.
     * It is "${file} ${run} ${run-number} ${module} ${name} ${experiment} ${measurement} ${replication}",
     * but fields that are the same for each item are omitted.
     * If all the fields has the same value in {@code items}, then the "${index}" is used as
     * the format string.
     */
    private static String defaultNameFormat(ResultItem[] items) {

        if (items.length <= 1)
            return "${module} ${name}";

        List<ResultItemField> differentFields = complement(getCommonFields(items, FIELDS_OF_LINENAMES));
        if (differentFields.isEmpty())
            return "${module} ${name} - ${index}";
        else
            return nameFormatUsingFields(differentFields);
    }

    public static String defaultResultItemTitleFormat(ResultItem[] items) {
        String format = defaultNameFormat(items);
        return format.replace("${name}", "${title_or_name}");
    }

    private static final ResultItemField[] titleFields = new ResultItemField[] {
        FLD_NAME, FLD_MODULE, FLD_EXPERIMENT, FLD_MEASUREMENT, FLD_REPLICATION
    };

    public static String defaultTitle(ResultItem[] items) {
        List<ResultItemField> fields = getCommonFields(items, titleFields);
        // remove computed file name
        if (fields.contains(FLD_NAME) || fields.contains(FLD_MODULE)) {
            fields.remove(FLD_EXPERIMENT);
            fields.remove(FLD_MEASUREMENT);
            fields.remove(FLD_REPLICATION);
        }
        return fields.isEmpty() ? null : ResultItemFormatter.formatResultItem(nameFormatUsingFields(fields), items[0]);
    }

    /**
     * Returns the fields from {@code fields} that has the same value
     * in {@code items}.
     */
    private static List<ResultItemField> getCommonFields(ResultItem[] items, ResultItemField[] fields) {
        List<ResultItemField> commonFields = new ArrayList<ResultItemField>();
        int itemCount = items.length;

        if (itemCount > 0) {
            for (ResultItemField field : fields) {
                String firstValue = field.getFieldValue(items[0]);
                int i;
                for (i = 1; i < itemCount; ++i) {
                    String value = field.getFieldValue(items[i]);
                    if (!ObjectUtils.equals(firstValue, value))
                        break;
                }
                if (i == itemCount)
                    commonFields.add(field);
            }
        }

        return commonFields;
    }

    /**
     * Generates a format string from the specified fields
     * for formatting result items.
     */
    private static String nameFormatUsingFields(List<ResultItemField> fields) {
        StringBuilder sbFormat = new StringBuilder();
        char separator = ' ';
        int lastIndex = fields.size() - 1;
        for (int i = 0; i < fields.size(); i++) {
            sbFormat.append("${").append(fields.get(i).getName()).append('}');
            if (i != lastIndex)
                sbFormat.append(separator);
        }
        return sbFormat.toString();
    }

    private static void checkReadLock(ResultFileManager manager) {
        if (manager != null)
            manager.checkReadLock();
    }
}
