package com.thinkaurelius.faunus.mapreduce.algebra;

import com.thinkaurelius.faunus.io.graph.FaunusVertex;
import com.thinkaurelius.faunus.mapreduce.algebra.util.Counters;
import com.tinkerpop.blueprints.pgm.Edge;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class LabelFilter {

    public static final String LABELS = "faunus.algebra.labelfilter.labels";

    public static class Map extends Mapper<NullWritable, FaunusVertex, NullWritable, FaunusVertex> {

        protected Set<String> legalLabels;

        @Override
        public void setup(final Mapper.Context context) throws IOException, InterruptedException {
            String[] temp = context.getConfiguration().getStrings(LABELS);
            if (temp != null && temp.length > 0)
                this.legalLabels = new HashSet<String>(Arrays.asList(temp));
            else
                this.legalLabels = null;
        }

        @Override
        public void map(final NullWritable key, final FaunusVertex value, final org.apache.hadoop.mapreduce.Mapper<NullWritable, FaunusVertex, NullWritable, FaunusVertex>.Context context) throws IOException, InterruptedException {
            if (null != this.legalLabels) {
                long allowedCounter = 0;
                long filteredCounter = 0;
                final List<Edge> newEdges = new LinkedList<Edge>();
                for (final Edge edge : value.getOutEdges()) {
                    if (this.legalLabels.contains(edge.getLabel())) {
                        newEdges.add(edge);
                        allowedCounter++;
                    } else {
                        filteredCounter++;
                    }
                }
                value.setOutEdges(newEdges);

                if (allowedCounter > 0)
                    context.getCounter(Counters.EDGES_ALLOWED_BY_LABEL).increment(allowedCounter);

                if (filteredCounter > 0)
                    context.getCounter(Counters.EDGES_FILTERED_BY_LABEL).increment(filteredCounter);
            } else {
                context.getCounter(Counters.EDGES_ALLOWED_BY_LABEL).increment(((List) value.getOutEdges()).size());
            }
            context.write(NullWritable.get(), value);

        }
    }

}