package app.TFGWordle.service;

import app.TFGWordle.model.Participation;
import app.TFGWordle.repository.ParticipationRepository;
import app.TFGWordle.security.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParticipationService {

    @Autowired
    private ParticipationRepository participationRepository;

    public List<User> findStudentsByCompetition(String competitionName) {
        return participationRepository.findByCompetitionName(competitionName);
    }

    public List<Participation> findParticipationsByStudent(Long studentId) {
        return participationRepository.findByUserId(studentId);
    }

    public void save(Participation participation) {
        participationRepository.save(participation);
    }
}
