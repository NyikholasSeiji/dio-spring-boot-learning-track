package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.InvalidTransactionException;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistTransactionUseCaseTest {
    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
    private final PersistTransactionUseCase useCase = new PersistTransactionUseCase(repository);

    @Test
    void should_returnValueInReais_when_amountIsInCents() {
        var output = useCase.execute(new PersistTransactionInput("Farmácia", 4050, Category.PHARMA));

        assertThat(output.value()).isEqualTo(40.50);
        assertThat(output.category()).isEqualTo("PHARMA");
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void should_notSave_when_inputIsInvalid() {
        assertThatThrownBy(() -> useCase.execute(new PersistTransactionInput("Farmácia", 0, Category.PHARMA)))
                .isInstanceOf(InvalidTransactionException.class);

        assertThat(repository.findAll()).isEmpty();
    }
}
