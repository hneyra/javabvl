package bvl.domain.input;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BvlItem {
    private String companyCode;
    private String companyName;
    private String shortName;
    private String nemonico;
    private String sectorCode;
    private String sectorDescription;
    private LocalDateTime lastDate;

    private Double buy;

    private Double sell;

    private LocalDate previousDate;
    private Double last;
    private Double minimun;
    private Double maximun;
    private Double opening;
    private Double previous;
    private Long negotiatedQuantity;
    private Long negotiatedAmount;
    private Long negotiatedNationalAmount;
    private Long operationsNumber;
    private Double exderecho;
    private Double percentageChange;
    private String currency;
    private Integer unity;
    private String segment;
    private LocalDateTime createdDate;
    private Long numNeg;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getNemonico() {
        return nemonico;
    }

    public void setNemonico(String nemonico) {
        this.nemonico = nemonico;
    }

    public String getSectorCode() {
        return sectorCode;
    }

    public void setSectorCode(String sectorCode) {
        this.sectorCode = sectorCode;
    }

    public String getSectorDescription() {
        return sectorDescription;
    }

    public void setSectorDescription(String sectorDescription) {
        this.sectorDescription = sectorDescription;
    }

    public LocalDateTime getLastDate() {
        return lastDate;
    }

    public void setLastDate(LocalDateTime lastDate) {
        this.lastDate = lastDate;
    }

    public Double getBuy() {
        return buy;
    }

    public void setBuy(Double buy) {
        this.buy = buy;
    }

    public Double getSell() {
        return sell;
    }

    public void setSell(Double sell) {
        this.sell = sell;
    }

    public LocalDate getPreviousDate() {
        return previousDate;
    }

    public void setPreviousDate(LocalDate previousDate) {
        this.previousDate = previousDate;
    }

    public Double getLast() {
        return last;
    }

    public void setLast(Double last) {
        this.last = last;
    }

    public Double getMinimun() {
        return minimun;
    }

    public void setMinimun(Double minimun) {
        this.minimun = minimun;
    }

    public Double getMaximun() {
        return maximun;
    }

    public void setMaximun(Double maximun) {
        this.maximun = maximun;
    }

    public Double getOpening() {
        return opening;
    }

    public void setOpening(Double opening) {
        this.opening = opening;
    }

    public Double getPrevious() {
        return previous;
    }

    public void setPrevious(Double previous) {
        this.previous = previous;
    }

    public Long getNegotiatedQuantity() {
        return negotiatedQuantity;
    }

    public void setNegotiatedQuantity(Long negotiatedQuantity) {
        this.negotiatedQuantity = negotiatedQuantity;
    }

    public Long getNegotiatedAmount() {
        return negotiatedAmount;
    }

    public void setNegotiatedAmount(Long negotiatedAmount) {
        this.negotiatedAmount = negotiatedAmount;
    }

    public Long getNegotiatedNationalAmount() {
        return negotiatedNationalAmount;
    }

    public void setNegotiatedNationalAmount(Long negotiatedNationalAmount) {
        this.negotiatedNationalAmount = negotiatedNationalAmount;
    }

    public Long getOperationsNumber() {
        return operationsNumber;
    }

    public void setOperationsNumber(Long operationsNumber) {
        this.operationsNumber = operationsNumber;
    }

    public Double getExderecho() {
        return exderecho;
    }

    public void setExderecho(Double exderecho) {
        this.exderecho = exderecho;
    }

    public Double getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(Double percentageChange) {
        this.percentageChange = percentageChange;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getUnity() {
        return unity;
    }

    public void setUnity(Integer unity) {
        this.unity = unity;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getNumNeg() {
        return numNeg;
    }

    public void setNumNeg(Long numNeg) {
        this.numNeg = numNeg;
    }

    @Override
    public String toString() {
        return "{id: " + companyName + "-" + companyCode + "}";
    }
}
