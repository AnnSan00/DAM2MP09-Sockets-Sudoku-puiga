package com.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Estat d'una partida de Sudoku (una sola partida compartida per tots els jugadors).
 *
 * Genera un tauler complet aleatori i després en treu caselles assegurant que
 * el puzle tingui UNA sola solució (així mai donem per mala una resposta vàlida).
 */
final class SudokuGame {

    static final int EMPTY = 0;   // casella buida
    static final int GIVEN = 1;   // casella inicial (no editable)
    static final int SOLVED = 2;  // casella encertada per un jugador (bloquejada)

    static final int IGNORED = -1;
    static final int WRONG = 0;
    static final int CORRECT = 1;

    private static final int HOLES = 45;

    private final Random rnd = new Random();
    private final int[] solution = new int[81];
    private final int[] board = new int[81];
    private final int[] status = new int[81];
    private int remaining;

    SudokuGame() {
        fill(solution, 0);
        System.arraycopy(solution, 0, board, 0, 81);

        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < 81; i++) positions.add(i);
        Collections.shuffle(positions, rnd);

        int removed = 0;
        for (int pos : positions) {
            if (removed >= HOLES) break;
            int backup = board[pos];
            board[pos] = 0;
            if (countSolutions(board.clone(), 2) == 1) {
                removed++;
            } else {
                board[pos] = backup;
            }
        }

        for (int i = 0; i < 81; i++) {
            status[i] = board[i] == 0 ? EMPTY : GIVEN;
        }
        remaining = removed;
    }

    // ---------- Generació ----------

    private boolean fill(int[] g, int pos) {
        if (pos == 81) return true;
        List<Integer> digits = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(digits, rnd);
        for (int d : digits) {
            if (canPlace(g, pos, d)) {
                g[pos] = d;
                if (fill(g, pos + 1)) return true;
                g[pos] = 0;
            }
        }
        return false;
    }

    private static boolean canPlace(int[] g, int pos, int d) {
        int r = pos / 9, c = pos % 9;
        for (int i = 0; i < 9; i++) {
            if (g[r * 9 + i] == d || g[i * 9 + c] == d) return false;
        }
        int br = r / 3 * 3, bc = c / 3 * 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (g[(br + i) * 9 + bc + j] == d) return false;
            }
        }
        return true;
    }

    /** Compta solucions (s'atura en arribar a limit). */
    private static int countSolutions(int[] g, int limit) {
        int best = -1, bestCount = 10;
        for (int pos = 0; pos < 81; pos++) {
            if (g[pos] != 0) continue;
            int cnt = 0;
            for (int d = 1; d <= 9; d++) if (canPlace(g, pos, d)) cnt++;
            if (cnt == 0) return 0;
            if (cnt < bestCount) { best = pos; bestCount = cnt; }
        }
        if (best == -1) return 1;

        int total = 0;
        for (int d = 1; d <= 9; d++) {
            if (canPlace(g, best, d)) {
                g[best] = d;
                total += countSolutions(g, limit - total);
                g[best] = 0;
                if (total >= limit) break;
            }
        }
        return total;
    }

    // ---------- Joc ----------

    /** Intenta posar value a (row, col). Retorna IGNORED, WRONG o CORRECT. */
    synchronized int play(int row, int col, int value) {
        if (row < 0 || row > 8 || col < 0 || col > 8 || value < 1 || value > 9) return IGNORED;
        int pos = row * 9 + col;
        if (status[pos] != EMPTY) return IGNORED; // ja bloquejada
        if (solution[pos] == value) {
            board[pos] = value;
            status[pos] = SOLVED;
            remaining--;
            return CORRECT;
        }
        return WRONG;
    }

    synchronized boolean isFinished() { return remaining <= 0; }
    synchronized int[] boardCopy() { return board.clone(); }
    synchronized int[] statusCopy() { return status.clone(); }
}
