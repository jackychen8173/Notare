package com.notare.coderun;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OutputMatchTest {

    @Test
    void forgivesOnlyWhatStudentsCantSee() {
        assertThat(OutputMatch.matches("Hello\nWorld", "Hello\r\nWorld\r\n")).isTrue();
        assertThat(OutputMatch.matches("a\nb", "a  \nb\t\n\n\n")).isTrue();
        assertThat(OutputMatch.matches("", null)).isTrue();

        assertThat(OutputMatch.matches("Hello", "hello")).isFalse();
        assertThat(OutputMatch.matches("a b", "a  b")).isFalse();
        assertThat(OutputMatch.matches("a\nb", "a\n\nb")).isFalse();
        assertThat(OutputMatch.matches("  indented", "indented")).isFalse();
    }

    @Test
    void suggestedScoreScalesTheAssignmentPoints() {
        assertThat(TestService.suggestedScore(10, 3, 4)).isEqualByComparingTo(new BigDecimal("7.50"));
        assertThat(TestService.suggestedScore(3, 1, 3)).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(TestService.suggestedScore(null, 3, 4)).isNull();
        assertThat(TestService.suggestedScore(10, 0, 0)).isNull();
    }
}
