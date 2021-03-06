package io.pivotal.bds.gemfire.geojson.function;

import java.util.Iterator;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.FunctionException;
import org.apache.geode.cache.execute.ResultSender;

import com.codahale.metrics.MetricRegistry;
import com.vividsolutions.jts.geom.Geometry;

import io.pivotal.bds.gemfire.geojson.comp.ComparisonType;
import io.pivotal.bds.gemfire.geojson.data.Boundary;
import io.pivotal.bds.gemfire.geojson.data.FindFeaturesRequest;
import io.pivotal.bds.metrics.rater.Rater;
import io.pivotal.bds.metrics.timer.Timer;

@SuppressWarnings("serial")
public class FindFeaturesFunction implements Function {

    private Boundary rootBoundary;
    private static final Logger LOG = LoggerFactory.getLogger(FindFeaturesFunction.class);

    private static final Timer timer = new Timer("FindFeaturesFunction-Timer");
    private static final Rater rater = new Rater("FindFeaturesFunction-Rater");

    public FindFeaturesFunction(Boundary rootBoundary, MetricRegistry registry) {
        this.rootBoundary = rootBoundary;
    }

    @Override
    public void execute(FunctionContext context) {
        try {
            FindFeaturesRequest req = (FindFeaturesRequest) context.getArguments();

            Geometry geom = req.getGeometry();
            String typeName = req.getTypeName();
            ComparisonType compType = req.getComparisonType();
            LOG.debug("execute: typeName={}, compType={}, geom={}", typeName, compType, geom);

            timer.start();
            List<SimpleFeature> fl = rootBoundary.findFeatures(geom, compType, typeName);
            timer.end();
            rater.increment();

            ResultSender<String> sender = context.getResultSender();

            if (fl.isEmpty()) {
                sender.lastResult("");
            } else {
                Iterator<SimpleFeature> iter = fl.iterator();

                while (iter.hasNext()) {
                    SimpleFeature sf = iter.next();
                    if (iter.hasNext()) {
                        sender.sendResult(sf.getID());
                    } else {
                        sender.lastResult(sf.getID());
                    }
                }
            }
        } catch (Exception x) {
            LOG.error("execute: x={}", x.toString(), x);
            throw new FunctionException(x.toString(), x);
        }
    }

    @Override
    public String getId() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public boolean isHA() {
        return true;
    }

    @Override
    public boolean optimizeForWrite() {
        return false;
    }

}
