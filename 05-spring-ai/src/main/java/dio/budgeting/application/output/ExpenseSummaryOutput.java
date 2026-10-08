package dio.budgeting.application.output;

import dio.budgeting.domain.Category;

import java.util.List;

public record ExpenseSummaryOutput(List<CategoryTotal> categories, long count, double total) {
    public record CategoryTotal(String category, long count, double total) {
        public static CategoryTotal of(Category category, long count, long totalInCents) {
            return new CategoryTotal(category.name(), count, Money.centsToReais(totalInCents));
        }
    }

    public static ExpenseSummaryOutput of(List<CategoryTotal> categories, long count, long totalInCents) {
        return new ExpenseSummaryOutput(categories, count, Money.centsToReais(totalInCents));
    }
}
