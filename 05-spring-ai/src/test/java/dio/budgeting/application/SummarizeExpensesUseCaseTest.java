package dio.budgeting.application;

import dio.budgeting.application.output.ExpenseSummaryOutput.CategoryTotal;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SummarizeExpensesUseCaseTest {
    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
    private final SummarizeExpensesUseCase useCase = new SummarizeExpensesUseCase(repository);

    @Test
    void should_sumByCategoryAndTotal_when_thereAreTransactions() {
        repository.save(new Transaction("Mercado", 8000, Category.GROCERIES));
        repository.save(new Transaction("Padaria", 1250, Category.GROCERIES));
        repository.save(new Transaction("Farmácia", 4000, Category.PHARMA));

        var summary = useCase.execute();

        assertThat(summary.count()).isEqualTo(3);
        assertThat(summary.total()).isEqualTo(132.50);
        assertThat(summary.categories()).containsExactly(
                new CategoryTotal("GROCERIES", 2, 92.50),
                new CategoryTotal("PHARMA", 1, 40.00),
                new CategoryTotal("AUTO", 0, 0.00));
    }

    @Test
    void should_returnZeros_when_thereAreNoTransactions() {
        var summary = useCase.execute();

        assertThat(summary.count()).isZero();
        assertThat(summary.total()).isZero();
        assertThat(summary.categories()).hasSize(Category.values().length)
                .allSatisfy(c -> assertThat(c.total()).isZero());
    }
}
