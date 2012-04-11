package choco.proba;

import choco.kernel.ResolutionPolicy;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.ConstraintFactory;
import solver.constraints.nary.Sum;
import solver.constraints.nary.alldifferent.AllDifferent;
import solver.constraints.nary.alldifferent.AllDifferentProba;
import solver.constraints.nary.lex.LexChain;
import solver.constraints.unary.Relation;
import solver.search.loop.monitors.SearchMonitorFactory;
import solver.search.strategy.StrategyFactory;
import solver.variables.IntVar;
import solver.variables.VariableFactory;
import solver.variables.view.Views;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: chameau
 * Date: 27/02/12
 */
public class GolombRulerBenchProbas extends AbstractBenchProbas {

    public GolombRulerBenchProbas(int n, AllDifferent.Type type, int frequency, boolean active,
                                 AbstractBenchProbas.Distribution dist, BufferedWriter out, int seed) throws IOException {
        super(new Solver(), n, type, frequency, active, dist, out, seed);
    }

    @Override
    void configSearchStrategy() {
        //System.out.println("---------------------------");
        //SearchMonitorFactory.log(solver,true,true);
        solver.set(StrategyFactory.inputOrderMinVal(vars, solver.getEnvironment()));
    }

    @Override
    void solveProcess() {
        solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, (IntVar) solver.getVars()[size - 1]);
        //System.out.println(dist + ": "+ solver.getVars()[size - 1]);
    }

    @Override
    void buildProblem(int size, boolean proba) {
        Collection<Constraint> allCstrs = new ArrayList<Constraint>();
        Collection<IntVar> allVars = new ArrayList<IntVar>();
        IntVar[] ticks;
        IntVar[] diffs;
        ticks = new IntVar[size];
        for (int i = 0; i < ticks.length; i++) {
            ticks[i] = VariableFactory.bounded("a_" + i, 0, ((size < 31) ? (1 << (size + 1)) - 1 : 9999), solver);
            allVars.add(ticks[i]);
        }

        allCstrs.add(ConstraintFactory.eq(ticks[0], 0, solver));
        allCstrs.add(new LexChain(true, solver, ticks));

        diffs = new IntVar[(size * size - size) / 2];
        for (int k = 0, i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size; j++, k++) {
                diffs[k] = Views.sum(ticks[j], Views.minus(ticks[i]));
                allVars.add(diffs[k]);
                allCstrs.add(new Relation(diffs[k], Relation.R.GQ, (j - i) * (j - i + 1) / 2, solver));
                allCstrs.add(Sum.leq(new IntVar[]{diffs[k], ticks[size - 1]}, new int[]{1, -1}, -((size - 1 - j + i) * (size - j + i)) / 2, solver));
            }
        }
        if (proba) {
            allCstrs.add(new AllDifferentProba(diffs, solver, type, this.count));
        } else {
            allCstrs.add(new AllDifferent(diffs, solver, type));
        }

        // break symetries
        if (size > 2) {
            allCstrs.add(ConstraintFactory.lt(diffs[0], diffs[diffs.length - 1], solver));
        }

        this.cstrs = allCstrs.toArray(new Constraint[allCstrs.size()]);
        this.allVars = allVars.toArray(new IntVar[allVars.size()]);
        this.vars = ticks;
    }
}
