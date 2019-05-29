package org.apache.sling.models.persist.bean;

//import com.adobe.cq.export.json.ExporterConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * every property has a different mapped name, designed to test the @Named annotation is respected.
 * fields are all named "wrong" because if we see that in the stored JCR values then the persist logic was wrong.
 */
//@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class BeanWithMappedNames {
    @Inject
    @Named("prop-1")
    @JsonProperty("prop-1")
    private String wrong1;

    @Inject
    @Named("prop-2")
    @JsonProperty("prop-2")
    private String[] wrong2;

    @Inject
    @Named("child-1")
    @JsonProperty("child-1")
    private ChildBean wrong3;

    @Inject
    @Named("child-2")
    @JsonProperty("child-2")
    private List<ChildBean> wrong4;

    @Inject
    @Named("child-3")
    @JsonProperty("child-3")
    private Map<String,ChildBean> wrong5;

    @Inject
    @Named("prop-3")
    @JsonProperty("prop-3")
    private Boolean wrong6;

    public static class ChildBean {
        @Inject
        private String name;

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * @return the wrong1
     */
    public String getWrong1() {
        return wrong1;
    }

    /**
     * @param wrong1 the wrong1 to set
     */
    public void setWrong1(String wrong1) {
        this.wrong1 = wrong1;
    }

    /**
     * @return the wrong2
     */
    public String[] getWrong2() {
        return wrong2;
    }

    /**
     * @param wrong2 the wrong2 to set
     */
    public void setWrong2(String[] wrong2) {
        this.wrong2 = wrong2;
    }

    /**
     * @return the wrong3
     */
    public ChildBean getWrong3() {
        return wrong3;
    }

    /**
     * @param wrong3 the wrong3 to set
     */
    public void setWrong3(ChildBean wrong3) {
        this.wrong3 = wrong3;
    }

    /**
     * @return the wrong4
     */
    public List<ChildBean> getWrong4() {
        return wrong4;
    }

    /**
     * @param wrong4 the wrong4 to set
     */
    public void setWrong4(List<ChildBean> wrong4) {
        this.wrong4 = wrong4;
    }

    /**
     * @return the wrong5
     */
    public Map<String,ChildBean> getWrong5() {
        return wrong5;
    }

    /**
     * @param wrong5 the wrong5 to set
     */
    public void setWrong5(Map<String,ChildBean> wrong5) {
        this.wrong5 = wrong5;
    }

    /**
     * @return the wrong6
     */
    public Boolean isWrong6() {
        return wrong6;
    }

    /**
     * @param wrong6 the wrong6 to set
     */
    public void setWrong6(Boolean wrong6) {
        this.wrong6 = wrong6;
    }
}
