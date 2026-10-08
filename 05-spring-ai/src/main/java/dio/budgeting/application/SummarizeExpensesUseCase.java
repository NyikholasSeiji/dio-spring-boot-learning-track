package dio.budgeting.application;

import dio.budgeting.application.output.ExpenseSummaryOutput;
import dio.budgeting.application.output.ExpenseSummaryOutput.CategoryTotal;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SummarizeExpensesUseCase {
    private final TransactionRepository transactionRepository;

    public SummarizeExpensesUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Tool(name = "summarize-expenses",
            description = "Resume os gastos registrados: quantidade e total (em reais) por categoria e o total geral")
    public ExpenseSummaryOutput execute() {
        Map<Category, List<Transaction>> byCategory = transactionRepository.findAll().stream()
                .collect(Collectors.groupingBy(Transaction::getCategory));

        List<CategoryTotal> categories = Arrays.stream(Category.values())
                .map(category -> {
                    var transactions = byCategory.getOrDefault(category, List.of());
                    return CategoryTotal.of(category, transactions.size(), sumAmounts(transactions));
                })
                .toList();

        var all = byCategory.values().stream().flatMap(List::stream).toList();
        return ExpenseSummaryOutput.of(categories, all.size(), sumAmounts(all));
    }

    private static long sumAmounts(List<Transaction> transactions) {
        return transactions.stream().mapToLong(Transaction::getAmount).sum();
    }
}
