package bvl.domain.input;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Daily {
    private String id;
    private LocalDate date;
    private Double amountSoles;
    private Double amountDollars;
    private Long numberOperations;

    private Double porcentual;

    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getAmountSoles() {
        return amountSoles;
    }

    public void setAmountSoles(Double amountSoles) {
        this.amountSoles = amountSoles;
    }

    public Double getAmountDollars() {
        return amountDollars;
    }

    public void setAmountDollars(Double amountDollars) {
        this.amountDollars = amountDollars;
    }

    public Long getNumberOperations() {
        return numberOperations;
    }

    public void setNumberOperations(Long numberOperations) {
        this.numberOperations = numberOperations;
    }

    public Double getPorcentual() {
        return porcentual;
    }

    public void setPorcentual(Double porcentual) {
        this.porcentual = porcentual;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }
}
