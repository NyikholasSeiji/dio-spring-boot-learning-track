package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerTest {
    private InMemoryTransactionRepository repository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        repository = new InMemoryTransactionRepository();
        var controller = new TransactionController(
                new PersistTransactionUseCase(repository),
                new ListTransactionsByCategoryUseCase(repository),
                new SummarizeExpensesUseCase(repository),
                mock(TranscriptionModel.class),
                new ByteArrayResource("Você é um assistente financeiro.".getBytes()),
                mock(ChatClient.Builder.class, RETURNS_SELF),
                mock(TextToSpeechModel.class));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void should_return201_when_transactionIsValid() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Mercado\", \"category\": \"GROCERIES\", \"amount\": 8000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Mercado"))
                .andExpect(jsonPath("$.amount").value(80.0));
    }

    @Test
    void should_return400_when_amountIsNotPositive() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Mercado\", \"category\": \"GROCERIES\", \"amount\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Transação inválida"))
                .andExpect(jsonPath("$.detail").value("O valor da transação deve ser maior que zero."));
    }

    @Test
    void should_return400_when_descriptionIsMissing() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"GROCERIES\", \"amount\": 8000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A descrição da transação é obrigatória."));
    }

    @Test
    void should_returnSummary_when_summaryIsRequested() throws Exception {
        repository.save(new Transaction("Mercado", 8000, Category.GROCERIES));
        repository.save(new Transaction("Gasolina", 15000, Category.AUTO));

        mockMvc.perform(get("/transactions/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.total").value(230.0))
                .andExpect(jsonPath("$.categories[0].category").value("GROCERIES"))
                .andExpect(jsonPath("$.categories[0].total").value(80.0))
                .andExpect(jsonPath("$.categories[2].category").value("AUTO"))
                .andExpect(jsonPath("$.categories[2].total").value(150.0));
    }

    @Test
    void should_listByCategory_when_categoryIsRequested() throws Exception {
        repository.save(new Transaction("Mercado", 8000, Category.GROCERIES));
        repository.save(new Transaction("Gasolina", 15000, Category.AUTO));

        mockMvc.perform(get("/transactions/AUTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Gasolina"));
    }

    @Test
    void should_return400_when_audioFileIsEmpty() throws Exception {
        var emptyFile = new MockMultipartFile("file", "audio.m4a", "audio/mp4", new byte[0]);

        mockMvc.perform(multipart("/transactions/ai").file(emptyFile))
                .andExpect(status().isBadRequest());
    }
}
