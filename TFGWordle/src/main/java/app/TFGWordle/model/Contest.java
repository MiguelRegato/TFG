package app.TFGWordle.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Contest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Date startDate;
    private Date endDate;
    private Boolean useDictionary;

    @ManyToOne
    @JoinColumn(name = "competition_id")
    private Competition competition;

    @OneToMany(mappedBy = "contest_id", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wordle> wordles = new ArrayList<>();

    public Contest() {}

    public Contest(String name, Competition competition, Date startDate, Date endDate, Boolean useDictionary) {
        this.name = name;
        this.competition = competition;
        this.startDate = startDate;
        this.endDate = endDate;
        this.useDictionary = useDictionary;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getContestName() {
        return name;
    }

    public void setContestName(String name) {
        this.name = name;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Boolean getUseDictionary() {
        return useDictionary;
    }

    public void setUseDictionary(Boolean useDictionary) {
        this.useDictionary = useDictionary;
    }

    public Competition getCompetition() {
        return competition;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
    }

    public List<Wordle> getWordles() {
        return wordles;
    }

    public void setWordles(List<Wordle> wordles) {
        this.wordles = wordles;
    }
}
