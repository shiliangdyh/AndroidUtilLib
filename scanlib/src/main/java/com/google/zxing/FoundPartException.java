package com.google.zxing;


import java.util.ArrayList;
import java.util.List;

public class FoundPartException extends ReaderException {

    private static final FoundPartException INSTANCE = new FoundPartException();

    static {
        INSTANCE.setStackTrace(NO_TRACE); // since it's meaningless
    }

    private List<ResultPoint> foundPoints;

    private FoundPartException() {
        // do nothing
        foundPoints = new ArrayList<>();
    }

    public void clear(){
        foundPoints.clear();
    }

    public void addPattern(ResultPoint point) {
        foundPoints.add(point);
    }

    public void addPatterns(List<ResultPoint> points){
        foundPoints.addAll(points);
    }

    public List<ResultPoint> getFoundPoints() {
        return foundPoints == null ? new ArrayList<ResultPoint>() : foundPoints;
    }

    public static FoundPartException getFoundPartInstance() {
        return INSTANCE;
    }
}
