package fi.nls.oskari.work;

public enum MapLayerJobType {
        NORMAL ("normal"),
        HIGHLIGHT ("highlight"),
        MAP_CLICK ("mapClick"),
        GEOJSON("geoJSON");

        private final String name;

        private MapLayerJobType(String name) {
            this.name = name;
        }

        @Override
        public String toString(){
            return name;
        }
}