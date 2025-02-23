package app.TFGWordle.controller;

import app.TFGWordle.dto.FolderDTO;
import app.TFGWordle.model.Contest;
import app.TFGWordle.model.Folder;
import app.TFGWordle.model.Wordle;
import app.TFGWordle.security.entity.User;
import app.TFGWordle.security.service.UserService;
import app.TFGWordle.service.ContestService;
import app.TFGWordle.service.ContestStateService;
import app.TFGWordle.service.FolderService;
import app.TFGWordle.service.WordleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wordle")
@CrossOrigin
public class WordleController {

    private final WordleService wordleService;
    private final ContestService contestService;
    private final UserService userService;
    private final FolderService folderService;
    private final ContestStateService contestStateService;

    public WordleController(WordleService wordleService, ContestService contestService, UserService userService, FolderService folderService, ContestStateService contestStateService) {
        this.wordleService = wordleService;
        this.contestService = contestService;
        this.userService = userService;
        this.folderService = folderService;
        this.contestStateService = contestStateService;
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @PostMapping("/newWordles/{contestId}/{professorName}/{folderId}")
    public ResponseEntity<List<Wordle>> newWordles(@RequestBody List<String> wordles, @PathVariable Long contestId, @PathVariable String professorName, @PathVariable Long folderId) {
        if (contestId != 0 & !contestService.existsById(contestId))
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        if (folderId != 0 && !folderService.existsById(folderId))
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        User professor = userService.getByUserName(professorName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Contest> contests = new ArrayList<>();

        if (contestId != 0)
            contests.add(contestService.getById(contestId));

        List<Wordle> toSave = new ArrayList<>();

        for (String wordle : wordles) {
            Wordle newWordle = new Wordle(wordle, contests, professor);
            if (folderId != 0)
                newWordle.setFolder(folderService.getById(folderId));
            toSave.add(newWordle);
        }
        if (!contests.isEmpty())
            contests.get(0).updateWordles(toSave);

        return ResponseEntity.status(HttpStatus.CREATED).body(wordleService.saveAll(toSave));
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @DeleteMapping("/deleteWordles")
    public ResponseEntity<?> deleteWordles(@RequestBody List<String> wordlesName) {

        for (String word : wordlesName) {
            if (!wordleService.existsByWord(word))
                return new ResponseEntity<>("No existe la palabra " + word, HttpStatus.NOT_FOUND);

            Wordle wordle = wordleService.getByWord(word);
            if (!wordle.getContests().isEmpty()) {
                List<Contest> contests = wordle.getContests();
                for (Contest contest : contests) {
                    contest.getWordles().remove(wordle);
                    contestService.save(contest);
                }
            }
            wordleService.delete(wordle);
        }

        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @PostMapping("/updateWordle/{wordInitial}/{wordUpdated}")
    public ResponseEntity<?> updateWordles(@PathVariable String wordInitial, @PathVariable String wordUpdated, @RequestBody List<Contest> contests) {

        if (!wordleService.existsByWord(wordInitial)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Wordle wordle = wordleService.getByWord(wordInitial);
        List<Contest> oldContests = new ArrayList<>(wordle.getContests());

        wordle.setWord(wordUpdated);
        wordle.setContests(contests);

        for (Contest oldContest : oldContests) {
            if (!contests.contains(oldContest)) {
                oldContest.getWordles().remove(wordle);
                contestService.save(oldContest);
            }
        }

        for (Contest contest : contests) {
            if (!contestService.existsById(contest.getId())) {
                return new ResponseEntity<>("El concurso " + contest.getContestName() + " no existe", HttpStatus.NOT_FOUND);
            }

            if (contest.getCompetition() == null) {
                Contest existingContest = contestService.getById(contest.getId());
                contest.setCompetition(existingContest.getCompetition());
            }

            if (!contest.getWordles().contains(wordle)) {
                contest.getWordles().add(wordle);
                contestService.save(contest);
            }
        }

        wordleService.save(wordle);

        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('STUDENT') || hasRole('ADMIN')")
    @GetMapping("/getWordlesByContest/{contestId}")
    public ResponseEntity<List<Wordle>> getWordlesByContest(@PathVariable Long contestId) {
        if (!contestService.existsById(contestId))
            return new ResponseEntity<>(HttpStatus.CONFLICT);

        Contest contest = contestService.getById(contestId);

        return ResponseEntity.status(HttpStatus.OK).body(wordleService.findByContestId(contest.getId()));
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @GetMapping("/getWordlesByProfessor/{professorName}")
    public ResponseEntity<List<Wordle>> getWordlesByProfessor(@PathVariable String professorName) {
        User professor = userService.getByUserName(professorName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Wordle> toReturn = new ArrayList<>();

        for (Wordle w : wordleService.findByProfessorId(professor.getId())) {
            if (w.getFolder() == null)
                toReturn.add(w);
        }

        return ResponseEntity.status(HttpStatus.OK).body(toReturn);
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @GetMapping("/getContestsWhereIsUsed/{word}")
    public ResponseEntity<List<Contest>> getContestsWhereIsUsed(@PathVariable String word) {
        if(!wordleService.existsByWord(word))
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Wordle wordle = wordleService.getByWord(word);
        return ResponseEntity.status(HttpStatus.OK).body(wordle.getContests());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/checkWordleAttempt/{contestId}/{wordleIndex}/{word}/{userEmail}")
    public ResponseEntity<List<Integer>> checkWordleAttempt(@PathVariable Long contestId, @PathVariable Integer wordleIndex, @PathVariable String word, @PathVariable String userEmail) {
        if (!contestService.existsById(contestId))
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        User user = userService.getByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Map<Character, Integer> letterCounts = contestStateService.getContestState(contestId, user.getId()).getLetterCountsList().get(wordleIndex);

        List<Integer> toReturn = new ArrayList<>();
        Contest contest = contestService.getById(contestId);
        String wordle = contest.getWordles().get(wordleIndex).getWord();

        for (int i = 0; i < wordle.length(); i++) {
            char expected = wordle.charAt(i);
            char got = Character.toLowerCase(word.charAt(i));

            if (expected == got && letterCounts.containsKey(got) && letterCounts.get(got) > 0) {
                toReturn.add(2);
                letterCounts.put(got, letterCounts.get(got) - 1);
            }
            else
                toReturn.add(3);
        }

        for (int i = 0; i < wordle.length(); i++) {
            char got = Character.toLowerCase(word.charAt(i));
            if (toReturn.get(i) == 3 && wordle.contains(String.valueOf(got)) && letterCounts.containsKey(got) && letterCounts.get(got) > 0) {
                toReturn.set(i, 1);
                letterCounts.put(got, letterCounts.get(got) - 1);
            }
        }

        for (int i = 0; i < wordle.length(); i++) {
            if (toReturn.get(i) == 3)
                toReturn.set(i, 0);
        }

        return ResponseEntity.status(HttpStatus.OK).body(toReturn);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/getWordleInContest/{contestId}/{wordleIndex}")
    public ResponseEntity<Wordle> getWordleInContest(@PathVariable Long contestId, @PathVariable Integer wordleIndex) {
        if (!contestService.existsById(contestId))
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return ResponseEntity.status(HttpStatus.OK).body(contestService.getById(contestId).getWordles().get(wordleIndex));
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @PostMapping("/newFolder/{folderName}")
    public ResponseEntity<?> createFolder(@PathVariable String folderName, @RequestBody String professorName) {
        if (folderService.existsByName(folderName))
            return new ResponseEntity<>(HttpStatus.CONFLICT);

        User professor = userService.getByUserName(professorName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Folder newFolder = new Folder(folderName, professor);
        newFolder.setWordles(new ArrayList<>());
        newFolder.setFolders(new ArrayList<>());

        folderService.save(newFolder);

        return ResponseEntity.status(HttpStatus.CREATED).body(newFolder);
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @DeleteMapping("/deleteFolders")
    public ResponseEntity<?> deleteFolders(@RequestBody List<Long> foldersIds) {
        for (Long folderId : foldersIds) {
            if (!folderService.existsById(folderId))
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            Folder folder = folderService.getById(folderId);
            folderService.delete(folder);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @PostMapping("/newFolderInsideFolder/{newFolderName}/{folderId}")
    public ResponseEntity<?> createFolderInsideFolder(@PathVariable String newFolderName, @PathVariable Long folderId, @RequestBody String professorName) {
        if (!folderService.existsById(folderId))
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        User professor = userService.getByUserName(professorName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Folder parentFolder = folderService.getById(folderId);

        if (parentFolder.getFolders().stream().anyMatch(folder -> folder.getName().equals(newFolderName)))
            return new ResponseEntity<>(HttpStatus.CONFLICT);

        Folder newFolder = new Folder(newFolderName, professor);
        newFolder.setParentFolder(parentFolder);
        newFolder.setWordles(new ArrayList<>());
        newFolder.setFolders(new ArrayList<>());

        parentFolder.getFolders().add(newFolder);

        folderService.save(parentFolder);

        return ResponseEntity.status(HttpStatus.CREATED).body(newFolder);
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @GetMapping("/getFoldersByProfessor/{professorName}")
    public ResponseEntity<List<FolderDTO>> getFoldersByProfessor(@PathVariable String professorName) {
        User professor = userService.getByUserName(professorName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<FolderDTO> toReturn = new ArrayList<>();

        for (Folder f : folderService.getByProfessor(professor)) {
            if (f.getParentFolder() == null)
                toReturn.add(new FolderDTO(f));
        }
        return ResponseEntity.status(HttpStatus.OK).body(toReturn);
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @PostMapping("/editFolder/{oldFolderNameId}")
    public ResponseEntity<?> editFolder(@PathVariable Long oldFolderNameId, @RequestBody String newFolderName) {
        if (!folderService.existsById(oldFolderNameId))
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Folder folder = folderService.getById(oldFolderNameId);
        if (!folder.getName().equals(newFolderName)) {
            folder.setName(newFolderName);
            folderService.save(folder);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @GetMapping("/getFolder/{folderId}")
    public ResponseEntity<FolderDTO> getFolder(@PathVariable Long folderId) {
        if(!folderService.existsById(folderId))
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Folder folder = folderService.getById(folderId);
        return ResponseEntity.status(HttpStatus.OK).body(new FolderDTO(folder));    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @GetMapping("/getFoldersByFolderId/{folderId}")
    public ResponseEntity<List<Folder>> getFoldersByFolderId(@PathVariable Long folderId) {
        if (!folderService.existsById(folderId))
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Folder parentFolder = folderService.getById(folderId);

        return ResponseEntity.status(HttpStatus.OK).body(parentFolder.getFolders());
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @GetMapping("/getWordlesByFolderId/{folderId}")
    public ResponseEntity<List<Wordle>> getWordlesByFolderName(@PathVariable Long folderId) {
        if(!folderService.existsById(folderId))
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return ResponseEntity.status(HttpStatus.OK).body(folderService.getById(folderId).getWordles());
    }

    @PreAuthorize("hasRole('PROFESSOR') || hasRole('ADMIN')")
    @PostMapping("/moveToFolder/{folderId}")
    public ResponseEntity<?> moveToFolder(@PathVariable Long folderId, @RequestBody List<String> wordles) {
        if (folderId == 0) {
            for (String wordle : wordles) {
                if (!wordleService.existsByWord(wordle))
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                Wordle wordleToMove = wordleService.getByWord(wordle);
                wordleToMove.setFolder(null);
                wordleService.save(wordleToMove);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            if (!folderService.existsById(folderId))
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);

            Folder folder = folderService.getById(folderId);

            for (String wordle : wordles) {
                if (!wordleService.existsByWord(wordle))
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                Wordle wordleToMove = wordleService.getByWord(wordle);
                wordleToMove.setFolder(folder);
                wordleService.save(wordleToMove);
                folder.getWordles().add(wordleToMove);
            }
            folderService.save(folder);

            return new ResponseEntity<>(HttpStatus.OK);
        }
    }
}
