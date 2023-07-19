package cn.lili.controller.goods;

import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.NestedMetaData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class inner<T> {
    public String index;
    public String id;
    public float score;
    public List<Object> sortValues;
    public T content;
    public Map<String, List<String>> highlightFields = new LinkedHashMap<>();
    public Map<String, SearchHits<?>> innerHits = new LinkedHashMap<>();
    public NestedMetaData nestedMetaData;
    public String flag;


    public void setFlag(String flag) {
        this.flag = flag;
    }
    
    public void setIndex(String index) {
        this.index = index;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public void setSortValues(List<Object> sortValues) {
        this.sortValues = sortValues;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public void setHighlightFields(Map<String, List<String>> highlightFields) {
        this.highlightFields = highlightFields;
    }

    public void setInnerHits(Map<String, SearchHits<?>> innerHits) {
        this.innerHits = innerHits;
    }

    public void setNestedMetaData(NestedMetaData nestedMetaData) {
        this.nestedMetaData = nestedMetaData;
    }
}
