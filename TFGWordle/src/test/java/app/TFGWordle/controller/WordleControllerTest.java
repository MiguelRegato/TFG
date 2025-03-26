package app.TFGWordle.controller;

import app.TFGWordle.dto.FolderDTO;
import app.TFGWordle.dto.WordleDTO;
import app.TFGWordle.model.*;
import app.TFGWordle.security.entity.User;
import app.TFGWordle.security.service.UserService;
import app.TFGWordle.service.ContestService;
import app.TFGWordle.service.ContestStateService;
import app.TFGWordle.service.FolderService;
import app.TFGWordle.service.WordleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class WordleControllerTest {

    private final String BASE_PATH = "/api/wordle";

    private final WordleService wordleService = mock(WordleService.class);

    private final ContestService contestService = mock(ContestService.class);

    private final UserService userService = mock(UserService.class);

    private final FolderService folderService = mock(FolderService.class);

    private final ContestStateService contestStateService = mock(ContestStateService.class);

    private final WordleController wordleController = new WordleController(wordleService, contestService, userService, folderService, contestStateService);

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(wordleController).build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Authentication authentication = mock(Authentication.class);

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void createWordlesSuccess() throws Exception {
        Long contestId = 1L;
        Long folderId = 1L;
        String professorName = "Profesor";
        User professor = new User();
        professor.setUsername(professorName);

        Contest contest = new Contest();
        contest.setId(contestId);

        List<String> wordles = Arrays.asList("word1", "word2");
        List<Wordle> savedWordles = Arrays.asList(new Wordle(), new Wordle());

        when(contestService.existsById(contestId)).thenReturn(true);
        when(contestService.getById(contestId)).thenReturn(contest);
        when(folderService.existsById(folderId)).thenReturn(true);
        when(userService.getByUserName(professorName)).thenReturn(Optional.of(professor));
        when(wordleService.saveAll(anyList())).thenReturn(savedWordles);
        when(contestService.save(any())).thenReturn(contest);

        mockMvc.perform(post(BASE_PATH + "/newWordles/" + contestId + "/" + professorName + "/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wordles)))
                .andExpect(status().isCreated());

    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void newWordlesContestNotFound() throws Exception {
        Long contestId = 1L;
        Long folderId = 2L;
        String professorName = "professor123";
        List<String> wordles = Arrays.asList("word1", "word2");

        when(contestService.existsById(contestId)).thenReturn(false);

        mockMvc.perform(post(BASE_PATH + "/newWordles/" + contestId + "/" + professorName + "/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wordles)))
                .andExpect(status().isNotFound());

        verify(contestService).existsById(contestId);
        verifyNoInteractions(folderService, userService, wordleService);
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void newWordlesFolderNotFound() throws Exception {
        Long contestId = 1L;
        Long folderId = 2L;
        String professorName = "professor123";
        List<String> wordles = Arrays.asList("word1", "word2");

        when(contestService.existsById(contestId)).thenReturn(true);
        when(folderService.existsById(folderId)).thenReturn(false);

        mockMvc.perform(post(BASE_PATH + "/newWordles/" + contestId + "/" + professorName + "/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wordles)))
                .andExpect(status().isNotFound());

        verify(contestService).existsById(contestId);
        verify(folderService).existsById(folderId);
        verifyNoInteractions(userService, wordleService);
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void newWordlesProfessorNotFound() throws Exception {
        Long contestId = 1L;
        Long folderId = 2L;
        String professorName = "professor123";
        List<String> wordles = Arrays.asList("word1", "word2");

        when(contestService.existsById(contestId)).thenReturn(true);
        when(folderService.existsById(folderId)).thenReturn(true);
        when(userService.getByUserName(professorName)).thenReturn(Optional.empty());

        mockMvc.perform(post(BASE_PATH + "/newWordles/" + contestId + "/" + professorName + "/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wordles)))
                .andExpect(status().isNotFound());

        verify(contestService).existsById(contestId);
        verify(folderService).existsById(folderId);
        verify(userService).getByUserName(professorName);
        verifyNoInteractions(wordleService);
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void deleteWordlesSuccess() throws Exception {
        Wordle wordle1 = new Wordle();
        wordle1.setId(1L);
        wordle1.setWord("word1");
        wordle1.setContests(Collections.emptyList());

        Wordle wordle2 = new Wordle();
        wordle2.setId(2L);
        wordle2.setWord("word2");
        wordle2.setContests(Collections.emptyList());

        List<Wordle> wordlesToDelete = Arrays.asList(wordle1, wordle2);

        when(wordleService.existsById(1L)).thenReturn(true);
        when(wordleService.getById(1L)).thenReturn(wordle1);
        when(wordleService.existsById(2L)).thenReturn(true);
        when(wordleService.getById(2L)).thenReturn(wordle2);

        mockMvc.perform(delete(BASE_PATH + "/deleteWordles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(wordlesToDelete)))
                .andExpect(status().isOk());

        verify(wordleService, times(2)).delete(any(Wordle.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void deleteWordlesWordleNotFound() throws Exception {
        Wordle wordle1 = new Wordle();
        wordle1.setId(1L);
        wordle1.setWord("word1");

        List<Wordle> wordlesToDelete = Collections.singletonList(wordle1);

        when(wordleService.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete(BASE_PATH + "/deleteWordles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wordlesToDelete)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No existe la palabra word1"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void deleteWordlesWordleInContest() throws Exception {
        Wordle wordle1 = new Wordle();
        wordle1.setId(1L);
        wordle1.setWord("word1");

        Contest contest = new Contest();
        contest.setId(100L);
        contest.setWordles(new ArrayList<>(Collections.singletonList(wordle1)));
        contest.setWordlesLength(new ArrayList<>(Collections.singletonList(wordle1.getWord().length())));

        wordle1.setContests(Collections.singletonList(contest));

        when(wordleService.existsById(1L)).thenReturn(true);
        when(wordleService.getById(1L)).thenReturn(wordle1);
        when(contestService.getById(100L)).thenReturn(contest);
        when(contestService.save(any(Contest.class))).thenReturn(contest);

        mockMvc.perform(delete(BASE_PATH + "/deleteWordles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(wordle1))))
                .andExpect(status().isOk());

        verify(contestService).save(any(Contest.class));
        verify(wordleService).delete(wordle1);
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void updateWordlesSuccess() throws Exception {
        String initialWord = "originalWord";
        String updatedWord = "newWord";

        Wordle existingWordle = new Wordle();
        existingWordle.setWord(initialWord);
        existingWordle.setId(1L);
        existingWordle.setContests(new ArrayList<>());

        Contest contest = new Contest();
        contest.setId(1L);
        contest.setContestName("Contest 1");
        contest.setCompetition(new Competition());
        contest.setWordles(new ArrayList<>());
        contest.setWordlesLength(new ArrayList<>());

        List<Contest> contestsToUpdate = Collections.singletonList(contest);

        when(wordleService.existsByWord(initialWord)).thenReturn(true);
        when(wordleService.getByWord(initialWord)).thenReturn(existingWordle);
        when(contestService.existsById(1L)).thenReturn(true);
        when(contestService.getById(1L)).thenReturn(contest);

        mockMvc.perform(post(BASE_PATH + "/updateWordle/" + initialWord + "/" + updatedWord)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contestsToUpdate)))
                .andExpect(status().isOk());

        verify(wordleService).save(any(Wordle.class));
        verify(contestService).save(any(Contest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void updateWordlesWordleNotFound() throws Exception {
        String initialWord = "nonExistentWord";
        String updatedWord = "newWord";

        List<Contest> contestsToUpdate = new ArrayList<>();

        when(wordleService.existsByWord(initialWord)).thenReturn(false);

        mockMvc.perform(post(BASE_PATH + "/updateWordle/" + initialWord + "/" + updatedWord)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contestsToUpdate)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void updateWordlesContestNotFound() throws Exception {
        String initialWord = "originalWord";
        String updatedWord = "newWord";

        Wordle existingWordle = new Wordle();
        existingWordle.setWord(initialWord);
        existingWordle.setId(1L);
        existingWordle.setContests(new ArrayList<>());

        Contest contest = new Contest();
        contest.setId(1L);
        contest.setContestName("Contest 1");

        List<Contest> contestsToUpdate = Collections.singletonList(contest);

        when(wordleService.existsByWord(initialWord)).thenReturn(true);
        when(wordleService.getByWord(initialWord)).thenReturn(existingWordle);
        when(contestService.existsById(1L)).thenReturn(false);

        mockMvc.perform(post(BASE_PATH + "/updateWordle/" + initialWord + "/" + updatedWord)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contestsToUpdate)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWordlesByContestSuccess() throws Exception {
        Long contestId = 1L;
        String username = "studentUser";

        Contest contest = new Contest();
        contest.setId(contestId);

        List<Wordle> wordles = List.of(new Wordle(), new Wordle());

        User student = new User();
        student.setUsername(username);
        student.setId(1L);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(student);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(contestService.existsById(contestId)).thenReturn(true);
        when(contestService.getById(contestId)).thenReturn(contest);
        when(wordleService.findByContestId(contestId)).thenReturn(wordles);


        mockMvc.perform(get(BASE_PATH + "/getWordlesByContest/" + contestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(wordleService).findByContestId(contestId);
    }

    @Test
    void getWordlesByContestContestNotFound() throws Exception {
        Long contestId = 1L;

        when(contestService.existsById(contestId)).thenReturn(false);

        mockMvc.perform(get(BASE_PATH + "/getWordlesByContest/" + contestId))
                .andExpect(status().isNotFound());
    }

    /*
    @Test
    void getWordlesByContest_UserNotFound() throws Exception {
        Long contestId = 1L;
        String username = "unknownUser";

        Contest contest = new Contest();
        contest.setId(contestId);

        Authentication authentication = mock(Authentication.class);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(contestService.existsById(contestId)).thenReturn(true);
        when(contestService.getById(contestId)).thenReturn(contest);
        when(userService.getByUserName(username)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_PATH + "/getWordlesByContest/" + contestId))
                .andExpect(status().isNotFound());

        verify(wordleService, never()).findByContestId(anyLong());
    }

    @Test
    void getWordlesByContest_StudentNotFinishedGames() throws Exception {
        Long contestId = 1L;
        String username = "studentUser";

        Contest contest = new Contest();
        contest.setId(contestId);
        contest.setWordles(List.of(new Wordle(), new Wordle()));

        User student = new User();
        student.setUsername(username);
        student.setId(1L);

        JsonNode mockState = objectMapper.readTree("{\"games\":[{\"finished\":true},{\"finished\":false}]}");

        when(contestService.existsById(contestId)).thenReturn(true);
        when(contestService.getById(contestId)).thenReturn(contest);
        when(userService.getByUserName(username)).thenReturn(Optional.of(student));
        when(contestStateService.getContestState(contestId, student.getId())).thenReturn(new ContestState());

        mockMvc.perform(get(BASE_PATH + "/getWordlesByContest/" + contestId))
                .andExpect(status().isBadRequest());

    }
*/

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getWordlesByProfessorSuccess() throws Exception {
        String professorName = "Profesor";
        Long professorId = 1L;

        User professor = new User();
        professor.setUsername(professorName);
        professor.setId(professorId);

        Wordle wordle = new Wordle();
        WordleDTO wordleDTO = new WordleDTO(wordle);

        List<WordleDTO> wordles = List.of(wordleDTO);

        when(userService.getByUserName(professorName)).thenReturn(Optional.of(professor));
        when(wordleService.findByProfessorId(professorId)).thenReturn(List.of(wordle));

        mockMvc.perform(get(BASE_PATH + "/getWordlesByProfessor/" + professorName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(wordles.size()));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getWordlesByProfessorUserNotFound() throws Exception {
        String professorName = "Profesor";

        when(userService.getByUserName(professorName)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_PATH + "/getWordlesByProfessor/" + professorName))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getContestsWhereIsUsedSuccess() throws Exception {
        String word = "test";

        Wordle wordle = new Wordle();
        wordle.setWord(word);
        wordle.setContests(List.of(new Contest()));

        when(wordleService.existsByWord(word)).thenReturn(true);
        when(wordleService.getByWord(word)).thenReturn(wordle);

        mockMvc.perform(get(BASE_PATH + "/getContestsWhereIsUsed/" + word))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getContestsWhereIsUsedWordleNotFound() throws Exception {
        String word = "test";

        when(wordleService.existsByWord(word)).thenReturn(false);

        mockMvc.perform(get(BASE_PATH + "/getContestsWhereIsUsed/" + word))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void checkWordleAttemptSuccess() throws Exception {
        Long contestId = 1L;
        int wordleIndex = 0;
        String word = "apple";
        String userEmail = "user@example.com";
        User user = new User();
        user.setId(100L);
        user.setEmail(userEmail);

        Contest contest = new Contest();
        contest.setId(contestId);
        Wordle wordle = new Wordle();
        wordle.setWord(word);
        contest.setWordles(List.of(wordle));

        Map<Character, Integer> letterCounts = new HashMap<>();
        letterCounts.put('a', 1);
        letterCounts.put('p', 2);
        letterCounts.put('l', 1);
        letterCounts.put('e', 1);
        ContestState contestState = new ContestState();
        contestState.setLetterCountsList(List.of(letterCounts));

        when(contestService.existsById(contestId)).thenReturn(true);
        when(userService.getByEmail(userEmail)).thenReturn(Optional.of(user));
        when(contestService.getById(contestId)).thenReturn(contest);
        when(contestStateService.getContestState(contestId, user.getId())).thenReturn(contestState);

        mockMvc.perform(get(BASE_PATH + "/checkWordleAttempt/" + contestId + "/" + wordleIndex + "/" + word + "/" + userEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(5))
                .andExpect(jsonPath("$[0]").value(2))
                .andExpect(jsonPath("$[1]").value(2))
                .andExpect(jsonPath("$[2]").value(2))
                .andExpect(jsonPath("$[3]").value(2))
                .andExpect(jsonPath("$[4]").value(2));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void checkWordleAttemptContestNotFound() throws Exception {
        Long contestId = 1L;
        int wordleIndex = 0;
        String word = "apple";
        String userEmail = "user@example.com";

        when(contestService.existsById(contestId)).thenReturn(false);

        mockMvc.perform(get(BASE_PATH + "/checkWordleAttempt/" + contestId + "/" + wordleIndex + "/" + word + "/" + userEmail))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void checkWordleAttemptUserNotFound() throws Exception {
        Long contestId = 1L;
        int wordleIndex = 0;
        String word = "apple";
        String userEmail = "user@example.com";

        when(contestService.existsById(contestId)).thenReturn(true);
        when(userService.getByEmail(userEmail)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_PATH + "/checkWordleAttempt/" + contestId + "/" + wordleIndex + "/" + word + "/" + userEmail))
                .andExpect(status().isNotFound());
    }

    /*
    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getWordleInContestSuccess() throws Exception {
        Long contestId = 1L;
        int wordleIndex = 0;
        String userName = "user";

        User user = new User();
        user.setUsername(userName);

        Contest contest = new Contest();
        contest.setId(contestId);
        Wordle wordle = new Wordle();
        wordle.setWord("test");
        contest.setWordles(List.of(wordle));

        JsonNode stateNode = mock(JsonNode.class);
        ObjectNode gamesNode = mock(ObjectNode.class);
        when(gamesNode.get("finished")).thenReturn(mock(BooleanNode.class));
        when(gamesNode.get("finished").asBoolean()).thenReturn(true);
        when(stateNode.get("games")).thenReturn(gamesNode);

        ContestState contestState = new ContestState();
        contestState.setState(stateNode);

        when(contestService.existsById(contestId)).thenReturn(true);
        when(userService.getByUserName(userName)).thenReturn(Optional.of(user));
        when(contestStateService.getContestState(contestId, user.getId())).thenReturn(contestState);
        when(contestService.getById(contestId)).thenReturn(contest);

        mockMvc.perform(get(BASE_PATH + "/getWordleInContest/" + contestId + "/" + wordleIndex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("test"));
    }
    */

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void createFolderSuccess() throws Exception {
        String folderName = "newFolder";
        String professorName = "professor@example.com";

        when(folderService.existsByName(folderName)).thenReturn(false);

        User professor = new User();
        professor.setUsername(professorName);
        when(userService.getByUserName(professorName)).thenReturn(Optional.of(professor));

        Folder newFolder = new Folder(folderName, professor);
        newFolder.setWordles(new ArrayList<>());
        newFolder.setFolders(new ArrayList<>());

        mockMvc.perform(post(BASE_PATH + "/newFolder/" + folderName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professorName))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(folderName));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void createFolderNameExists() throws Exception {
        String folderName = "existingFolder";
        String professorName = "professor@example.com";

        when(folderService.existsByName(folderName)).thenReturn(true);

        mockMvc.perform(post(BASE_PATH + "/newFolder/" + folderName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professorName))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void createFolderProfessorNotExists() throws Exception {
        String folderName = "existingFolder";
        String professorName = "professor@example.com";

        when(folderService.existsByName(folderName)).thenReturn(false);
        when(userService.getByUserName(professorName)).thenReturn(Optional.empty());

        mockMvc.perform(post(BASE_PATH + "/newFolder/" + folderName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professorName))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void deleteFoldersSuccess() throws Exception {

        when(folderService.existsById(1L)).thenReturn(true);
        when(folderService.existsById(2L)).thenReturn(true);

        Folder folder1 = new Folder();
        folder1.setId(1L);
        Folder folder2 = new Folder();
        folder2.setId(2L);

        when(folderService.getById(1L)).thenReturn(folder1);
        when(folderService.getById(2L)).thenReturn(folder2);

        doNothing().when(folderService).delete(folder1);
        doNothing().when(folderService).delete(folder2);

        mockMvc.perform(delete(BASE_PATH + "/deleteFolders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2]"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void deleteFoldersFolderNotFound() throws Exception {

        when(folderService.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/deleteFolders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void createFolderInsideFolderSuccess() throws Exception {
        String newFolderName = "NewFolder";
        Long folderId = 1L;
        String professorName = "Profesor";

        User professor = new User();
        professor.setUsername(professorName);

        Folder parentFolder = new Folder();
        parentFolder.setId(folderId);
        parentFolder.setFolders(new ArrayList<>());

        Folder newFolder = new Folder(newFolderName, professor);
        newFolder.setParentFolder(parentFolder);

        when(folderService.existsById(folderId)).thenReturn(true);
        when(userService.getByUserName(professorName)).thenReturn(Optional.of(professor));
        when(folderService.getById(folderId)).thenReturn(parentFolder);

        mockMvc.perform(post(BASE_PATH + "/newFolderInsideFolder/" + newFolderName + "/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professorName))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(newFolderName));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void createFolderInsideFolderFolderNotFound() throws Exception {
        String newFolderName = "New Folder";
        Long folderId = 1L;
        String professorName = "professor@example.com";

        when(folderService.existsById(folderId)).thenReturn(false);

        mockMvc.perform(post(BASE_PATH + "/newFolderInsideFolder/" + newFolderName + "/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professorName))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void createFolderInsideFolderUserNotFound() throws Exception {
        String newFolderName = "New Folder";
        Long folderId = 1L;
        String professorName = "professor@example.com";

        when(folderService.existsById(folderId)).thenReturn(true);
        when(userService.getByUserName(professorName)).thenReturn(Optional.empty());

        mockMvc.perform(post(BASE_PATH + "/newFolderInsideFolder/" + newFolderName + "/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professorName))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void createFolderInsideFolderFolderAlreadyExists() throws Exception {
        String newFolderName = "NewFolder";
        Long folderId = 1L;
        String professorName = "professor@example.com";

        User user = new User();
        user.setUsername(professorName);

        Folder parentFolder = new Folder();
        parentFolder.setId(folderId);
        parentFolder.setFolders(Arrays.asList(new Folder("NewFolder", user)));

        when(folderService.existsById(folderId)).thenReturn(true);
        when(userService.getByUserName(professorName)).thenReturn(Optional.of(user));
        when(folderService.getById(folderId)).thenReturn(parentFolder);

        mockMvc.perform(post(BASE_PATH + "/newFolderInsideFolder/" + newFolderName + "/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professorName))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getFoldersByProfessorSuccess() throws Exception {
        String professorName = "Profesor";

        User professor = new User();
        professor.setUsername(professorName);

        Folder mainFolder1 = new Folder("MainFolder1", professor);
        Folder mainFolder2 = new Folder("MainFolder2", professor);
        mainFolder1.setParentFolder(null);
        mainFolder2.setParentFolder(null);

        Folder subFolder1 = new Folder("Subfolder1", professor);
        subFolder1.setParentFolder(mainFolder1);

        Folder subFolder2 = new Folder("Subfolder2", professor);
        subFolder2.setParentFolder(mainFolder2);

        List<Folder> folders = Arrays.asList(mainFolder1, mainFolder2, subFolder1, subFolder2);

        List<FolderDTO> folderDTOs = Arrays.asList(new FolderDTO(mainFolder1), new FolderDTO(mainFolder2));

        when(userService.getByUserName(professorName)).thenReturn(Optional.of(professor));
        when(folderService.getByProfessor(professor)).thenReturn(folders);

        mockMvc.perform(get(BASE_PATH + "/getFoldersByProfessor/" + professorName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("MainFolder1"))
                .andExpect(jsonPath("$[1].name").value("MainFolder2"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getFoldersByProfessorUserNotFound() throws Exception {
        String professorName = "Profesor";

        when(userService.getByUserName(professorName)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_PATH + "/getFoldersByProfessor/" + professorName))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getFoldersByProfessorFolderNotFound() throws Exception {
        String professorName = "Profesor";

        User user = new User();
        user.setUsername(professorName);

        when(userService.getByUserName(professorName)).thenReturn(Optional.of(user));
        when(folderService.getByProfessor(user)).thenReturn(new ArrayList<>());

        mockMvc.perform(get(BASE_PATH + "/getFoldersByProfessor/" + professorName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void editFolderSuccess() throws Exception {
        Long oldFolderId = 1L;
        String newFolderName = "New Folder Name";

        Folder existingFolder = new Folder("Old Folder Name", new User());
        existingFolder.setId(oldFolderId);

        when(folderService.existsById(oldFolderId)).thenReturn(true);
        when(folderService.getById(oldFolderId)).thenReturn(existingFolder);

        mockMvc.perform(post(BASE_PATH + "/editFolder/" + oldFolderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newFolderName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());

        verify(folderService, times(1)).save(argThat(folder -> folder.getName().equals(newFolderName)));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void editFolderFolderNotFound() throws Exception {
        Long oldFolderId = 1L;
        String newFolderName = "New Folder Name";

        when(folderService.existsById(oldFolderId)).thenReturn(false);

        mockMvc.perform(post(BASE_PATH + "/editFolder/" + oldFolderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newFolderName));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void editFolderFolderSameName() throws Exception {
        Long oldFolderId = 1L;
        String newFolderName = "Existing Folder Name";

        Folder existingFolder = new Folder("Existing Folder Name", new User());
        existingFolder.setId(oldFolderId);

        when(folderService.existsById(oldFolderId)).thenReturn(true);
        when(folderService.getById(oldFolderId)).thenReturn(existingFolder);

        mockMvc.perform(post(BASE_PATH + "/editFolder/" + oldFolderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newFolderName))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getFolderSuccess() throws Exception {
        Long folderId = 1L;

        Folder folder = new Folder();
        folder.setId(folderId);

        when(folderService.existsById(folderId)).thenReturn(true);
        when(folderService.getById(folderId)).thenReturn(folder);

        mockMvc.perform(get(BASE_PATH + "/getFolder/" + folderId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getFolderNotFound() throws Exception {
        Long folderId = 1L;

        when(folderService.existsById(folderId)).thenReturn(false);

        mockMvc.perform(get(BASE_PATH + "/getFolder/" + folderId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getFoldersByFolderIdSuccess() throws Exception {
        Long folderId = 1L;

        Folder folder = new Folder();
        folder.setId(folderId);

        Folder folderInside = new Folder();
        folderInside.setParentFolder(folder);

        folder.setFolders(List.of(folderInside));

        when(folderService.existsById(folderId)).thenReturn(true);
        when(folderService.getById(folderId)).thenReturn(folder);

        mockMvc.perform(get(BASE_PATH + "/getFoldersInsideFolder/" + folderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getFoldersByFolderEmptyFolder() throws Exception {
        Long folderId = 1L;

        Folder folder = new Folder();
        folder.setId(folderId);
        folder.setFolders(new ArrayList<>());

        when(folderService.existsById(folderId)).thenReturn(true);
        when(folderService.getById(folderId)).thenReturn(folder);

        mockMvc.perform(get(BASE_PATH + "/getFoldersInsideFolder/" + folderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getFoldersByFolderFolderNotFound() throws Exception {
        Long folderId = 1L;

        when(folderService.existsById(folderId)).thenReturn(false);

        mockMvc.perform(get(BASE_PATH + "/getFoldersInsideFolder/" + folderId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getWordlesByFolderIdSuccess() throws Exception {
        Long folderId = 1L;

        Folder folder = new Folder();
        folder.setId(folderId);

        Wordle wordle = new Wordle();
        wordle.setFolder(folder);
        folder.setWordles(List.of(wordle));

        when(folderService.existsById(folderId)).thenReturn(true);
        when(folderService.getById(folderId)).thenReturn(folder);

        mockMvc.perform(get(BASE_PATH + "/getWordlesInsideFolder/" + folderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getWordlesByFolderIdEmpty() throws Exception {
        Long folderId = 1L;

        Folder folder = new Folder();
        folder.setId(folderId);
        folder.setWordles(new ArrayList<>());

        when(folderService.existsById(folderId)).thenReturn(true);
        when(folderService.getById(folderId)).thenReturn(folder);

        mockMvc.perform(get(BASE_PATH + "/getWordlesInsideFolder/" + folderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void getWordlesByFolderNotFound() throws Exception {
        Long folderId = 1L;

        when(folderService.existsById(folderId)).thenReturn(false);

        mockMvc.perform(get(BASE_PATH + "/getWordlesInsideFolder/" + folderId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void moveToFolderNoFolderSuccess() throws Exception {
        Long folderId = 0L;
        List<String> wordles = List.of("wordle1", "wordle2");

        Wordle wordle1 = new Wordle();
        wordle1.setWord(wordles.get(0));
        Wordle wordle2 = new Wordle();
        wordle2.setWord(wordles.get(1));

        when(wordleService.existsByWord("wordle1")).thenReturn(true);
        when(wordleService.existsByWord("wordle2")).thenReturn(true);
        when(wordleService.getByWord("wordle1")).thenReturn(wordle1);
        when(wordleService.getByWord("wordle2")).thenReturn(wordle2);

        mockMvc.perform(post(BASE_PATH + "/moveToFolder/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(wordles)))
                .andExpect(status().isOk());

        assertNull(wordle1.getFolder());
        assertNull(wordle2.getFolder());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void moveToFolderNoFolderNotFound() throws Exception {
        Long folderId = 0L;
        List<String> wordles = List.of("wordle");

        when(wordleService.existsByWord("wordle")).thenReturn(false);

        mockMvc.perform(post(BASE_PATH + "/moveToFolder/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(wordles)))
                .andExpect(status().isNotFound());

    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void moveToFolderExistingFolderSuccess() throws Exception {
        Long folderId = 1L;
        List<String> wordles = List.of("wordle1", "wordle2");

        Folder folder = new Folder();
        folder.setId(folderId);

        Wordle wordle1 = new Wordle();
        wordle1.setWord(wordles.get(0));
        Wordle wordle2 = new Wordle();
        wordle2.setWord(wordles.get(1));

        when(folderService.existsById(folderId)).thenReturn(true);
        when(folderService.getById(folderId)).thenReturn(folder);
        when(wordleService.existsByWord("wordle1")).thenReturn(true);
        when(wordleService.existsByWord("wordle2")).thenReturn(true);
        when(wordleService.getByWord("wordle1")).thenReturn(wordle1);
        when(wordleService.getByWord("wordle2")).thenReturn(wordle2);

        mockMvc.perform(post(BASE_PATH + "/moveToFolder/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(wordles)))
                .andExpect(status().isOk());

        assertEquals(folder, wordle1.getFolder());
        assertEquals(folder, wordle2.getFolder());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFESSOR"})
    void moveToFolderExistingFolderNotFound() throws Exception {
        Long folderId = 1L;
        List<String> wordles = List.of("wordle1", "wordle2");

        when(folderService.existsById(folderId)).thenReturn(false);

        mockMvc.perform(post(BASE_PATH + "/moveToFolder/" + folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(wordles)))
                .andExpect(status().isNotFound());
    }
}