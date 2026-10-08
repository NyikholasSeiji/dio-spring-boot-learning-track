package dio.budgeting.infrastructure.http.response;

import dio.budgeting.application.output.ExpenseSummaryOutput;

import java.util.List;

public record ExpenseSummaryResponse(List<CategoryTotalResponse> categories, long count, double total) {
    public record CategoryTotalResponse(String category, long count, double total) {
    }

    public static ExpenseSummaryResponse from(ExpenseSummaryOutput output) {
        var categories = output.categories().stream()
                .map(c -> new CategoryTotalResponse(c.category(), c.count(), c.total()))
                .toList();
        return new ExpenseSummaryResponse(categories, output.count(), output.total());
    }
}
