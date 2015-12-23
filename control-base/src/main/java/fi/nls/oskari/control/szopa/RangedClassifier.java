package fi.nls.oskari.control.szopa;

import org.geotools.filter.function.Classifier;

public final class RangedClassifier extends Classifier {

    Comparable<?> min[];
    Comparable<?> max[];
    String[] titles;

    public RangedClassifier(Comparable min[], Comparable max[]) {
        this.min = min;
        this.max = max;
        //initialize titles
        this.titles = new String[min.length];
        for (int i = 0; i < titles.length; i++) {
            titles[i] = generateTitle( min[i], max[i] );
        }
    }
    /**
     * Null safe title generation.
     * 
     * @param min
     * @param max
     * @return generated title
     */
    private String generateTitle(Comparable<?> min, Comparable<?> max) {
        if( min == null && max == null){
            return "Other";
        }
        else if ( min == null ){
            return "Below "+truncateZeros( String.valueOf( max ));
        }
        else if ( max == null ){
            return "Above "+truncateZeros( String.valueOf( min ));
        }
        else {
            return truncateZeros(String.valueOf(min)) + ".." + truncateZeros(String.valueOf(max));
        }
    }
    /**
     * Used to remove trailing zeros; preventing out put like 1.00000.
     * @param str
     * @return origional string with any trailing decimal places removed.
     */
    private String truncateZeros(String str) {
        if (str.indexOf(".") > -1) {
            while(str.endsWith("0")) {
                str = str.substring(0, str.length() - 1);
            }
            if (str.endsWith(".")) {
                str = str.substring(0, str.length() - 1);
            }
        }
        return str;
    }
    
    public int getSize() {
        return Math.min(min.length, max.length);
    }
    
    public Object getMin(int slot) {
        return min[slot];
    }
    
    public Object getMax(int slot) {
        return max[slot];
    }
    
    public int classify(Object value) {
        return classify((Comparable) value); 
    }
    
    @SuppressWarnings("rawtypes")
    private int classify(Comparable<?> val) {
        Comparable<?> value = val;
        if (val instanceof Integer) { //convert to double as java is stupid
            value = new Double(((Integer) val).intValue());
        }
        //check each slot and see if: min <= value <= max
        int last = min.length - 1;
        for (int i = 0; i <= last; i++) {
            Comparable localMin = this.min[i];
            Comparable localMax = this.max[i];
            
            if ((localMin == null || localMin.compareTo(value) < 1 ) &&
                ( localMax == null || localMax.compareTo(value) > -1)) {
                return i;
            }
        }
        if (compareTo(max[last],value) == 0) { //if value = max, put it in the last slot
            return last;
        }
        return -1; // value does not fit into any of the provided categories
    }

    private int compareTo(Comparable compare, Comparable value) {
        if( compare == null && value == null ){
            return 0;
        }
        else if( compare == null ){
            return -1;
        }
        else if( value == null ){
            return +1;
        }
        return value.compareTo(compare);
    }
    
}

