package io.github.smolcan.aggrid.jpa.adapter.filter.simple.params;

public abstract class SimpleFilterParams {
    
    private int maxNumConditions = 2;

    public int getMaxNumConditions() {
        return maxNumConditions;
    }

    public void setMaxNumConditions(int maxNumConditions) {
        this.maxNumConditions = maxNumConditions;
    }
}
