package org.tech_solutions.application.importedtransactions.model;

import jakarta.persistence.*;
import org.tech_solutions.application.imports.model.ImportFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "imported_transactions")
public class ImportedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id", nullable = false)
    private ImportFile importFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private org.tech_solutions.application.accounts.model.Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private org.tech_solutions.application.categories.model.Category category;

    @Column(name = "raw_description", length = 255)
    private String rawDescription;

    public org.tech_solutions.application.categories.model.Category getCategory() {
        return category;
    }

    public void setCategory(org.tech_solutions.application.categories.model.Category category) {
        this.category = category;
    }

    public org.tech_solutions.application.accounts.model.Account getAccount() {
        return account;
    }

    public void setAccount(org.tech_solutions.application.accounts.model.Account account) {
        this.account = account;
    }

    @Column(name = "raw_amount")
    private BigDecimal rawAmount;

    @Column(name = "raw_date")
    private LocalDate rawDate;

    @Column(nullable = false)
    private Boolean processed;

    public ImportedTransaction() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ImportFile getImportFile() {
        return importFile;
    }

    public void setImportFile(ImportFile importFile) {
        this.importFile = importFile;
    }

    public String getRawDescription() {
        return rawDescription;
    }

    public void setRawDescription(String rawDescription) {
        this.rawDescription = rawDescription;
    }

    public BigDecimal getRawAmount() {
        return rawAmount;
    }

    public void setRawAmount(BigDecimal rawAmount) {
        this.rawAmount = rawAmount;
    }

    public LocalDate getRawDate() {
        return rawDate;
    }

    public void setRawDate(LocalDate rawDate) {
        this.rawDate = rawDate;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }
}
