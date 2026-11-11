package com.notare.gradebook;

import com.notare.gradebook.dto.GradebookResponse.Category;
import com.notare.gradebook.dto.GradebookResponse.Cell;
import com.notare.gradebook.dto.GradebookResponse.Column;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * How a course grade adds up. Only released, scored work counts: work not handed in or not graded
 * yet is left out rather than counted as zero (the "grade so far", as Google Classroom does it).
 * <ul>
 *   <li>No categories: total points earned over total points possible.</li>
 *   <li>Categories: each category's points earned over points possible, then the average of those
 *       weighted by the category weights. Categories with nothing graded yet are left out and the
 *       remaining weights scaled up, so weights don't have to add up to 100. Work outside every
 *       category doesn't count.</li>
 * </ul>
 */
final class GradeMath {

    private static final MathContext PRECISION = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private GradeMath() {
    }

    record Result(List<BigDecimal> categoryPercents, BigDecimal coursePercent) {
    }

    static boolean countsTowardGrade(BigDecimal pointsPossible, UUID categoryId, boolean courseHasCategories) {
        return pointsPossible != null
                && pointsPossible.signum() > 0
                && (!courseHasCategories || categoryId != null);
    }

    /** cells is aligned with columns. */
    static Result grade(List<Category> categories, List<Column> columns, List<Cell> cells) {
        BigDecimal earnedAll = BigDecimal.ZERO;
        BigDecimal possibleAll = BigDecimal.ZERO;
        BigDecimal[] earned = new BigDecimal[categories.size()];
        BigDecimal[] possible = new BigDecimal[categories.size()];

        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            Cell cell = cells.get(i);
            if (!column.countsTowardGrade() || cell.points() == null) {
                continue;
            }
            earnedAll = earnedAll.add(cell.points());
            possibleAll = possibleAll.add(column.pointsPossible());
            for (int c = 0; c < categories.size(); c++) {
                if (Objects.equals(categories.get(c).id(), column.categoryId())) {
                    earned[c] = earned[c] == null ? cell.points() : earned[c].add(cell.points());
                    possible[c] = possible[c] == null ? column.pointsPossible() : possible[c].add(column.pointsPossible());
                }
            }
        }

        if (categories.isEmpty()) {
            return new Result(List.of(), percent(ratio(earnedAll, possibleAll)));
        }

        List<BigDecimal> categoryPercents = new ArrayList<>();
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weights = BigDecimal.ZERO;
        for (int c = 0; c < categories.size(); c++) {
            BigDecimal ratio = ratio(earned[c], possible[c]);
            categoryPercents.add(percent(ratio));
            BigDecimal weight = categories.get(c).weightPercent();
            if (ratio != null && weight != null && weight.signum() > 0) {
                weighted = weighted.add(ratio.multiply(weight, PRECISION));
                weights = weights.add(weight);
            }
        }
        // Every graded category has weight 0: fall back to plain points rather than show nothing.
        BigDecimal course = weights.signum() > 0 ? weighted.divide(weights, PRECISION) : ratio(earnedAll, possibleAll);
        return new Result(categoryPercents, percent(course));
    }

    /** One decimal place, e.g. 87.5. */
    static BigDecimal percent(BigDecimal ratio) {
        return ratio == null ? null : ratio.multiply(HUNDRED).setScale(1, RoundingMode.HALF_UP);
    }

    static BigDecimal ratio(BigDecimal earned, BigDecimal possible) {
        if (earned == null || possible == null || possible.signum() <= 0) {
            return null;
        }
        return earned.divide(possible, PRECISION);
    }
}
