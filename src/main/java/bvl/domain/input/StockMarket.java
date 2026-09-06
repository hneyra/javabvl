package bvl.domain.input;

import java.util.List;

public class StockMarket {
    private Integer up;
    private Integer down;
    private Integer equal;

    private List<BvlItem> content;

    public Integer getUp() {
        return up;
    }

    public void setUp(Integer up) {
        this.up = up;
    }

    public Integer getDown() {
        return down;
    }

    public void setDown(Integer down) {
        this.down = down;
    }

    public Integer getEqual() {
        return equal;
    }

    public void setEqual(Integer equal) {
        this.equal = equal;
    }

    public List<BvlItem> getContent() {
        return content;
    }

    public void setContent(List<BvlItem> content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "StockMarket{" +
                "up=" + up +
                ", down=" + down +
                ", equal=" + equal +
                ", content#size=" + content.size() +
                '}';
    }
}
