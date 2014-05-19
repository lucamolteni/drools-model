package org.drools.model.patterns;

import org.drools.model.Constraint;
import org.drools.model.DataSource;
import org.drools.model.SinglePattern;
import org.drools.model.Type;
import org.drools.model.Variable;
import org.drools.model.constraints.AbstractConstraint;
import org.drools.model.constraints.SingleConstraint1;
import org.drools.model.constraints.SingleConstraint2;
import org.drools.model.functions.Predicate1;
import org.drools.model.functions.Predicate2;

import static org.drools.model.DSL.bind;

public class PatternBuilder {

    private Variable[] joinVars;
    private DataSource dataSource;

    public PatternBuilder from(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public PatternBuilder using(Variable... joinVars) {
        this.joinVars = joinVars;
        return this;
    }

    public <T> BoundPatternBuilder<T> filter(Type<T> type) {
        return filter((Variable<T>) bind(type));
    }

    public <T> BoundPatternBuilder<T> filter(Variable<T> var) {
        return new BoundPatternBuilder<T>(var, joinVars, dataSource);
    }

    public interface ValidBuilder<T> {
        SinglePattern<T> get();
    }

    public static class BoundPatternBuilder<T> implements ValidBuilder<T> {
        private final Variable<T> variable;
        private final Variable[] joinVars;
        private DataSource dataSource;

        private BoundPatternBuilder(Variable<T> variable, Variable[] joinVars, DataSource dataSource) {
            this.variable = variable;
            this.joinVars = joinVars;
            this.dataSource = dataSource;
        }

        public BoundPatternBuilder<T> from(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public ConstrainedPatternBuilder<T> with(Predicate1<T> predicate) {
            return with(new SingleConstraint1<T>(variable, predicate));
        }

        public ConstrainedPatternBuilder<T> with(Constraint constraint) {
            return new ConstrainedPatternBuilder(variable, joinVars, constraint, dataSource);
        }

        public <A, B> ConstrainedPatternBuilder<T> with(Variable<A> var1, Variable<B> var2, Predicate2<A, B> predicate) {
            return with(new SingleConstraint2<A, B>(var1, var2, predicate));
        }

        public <A> ConstrainedPatternBuilder<T> with(Variable<A> var2, Predicate2<T, A> predicate) {
            return with(new SingleConstraint2<T, A>(variable, var2, predicate));
        }

        @Override
        public SinglePattern<T> get() {
            return joinVars == null ?
                   new SinglePatternImpl(variable, Constraint.True, dataSource) :
                   new JoinPatternImpl(variable, joinVars, Constraint.True, dataSource);
        }
    }

    public static class ConstrainedPatternBuilder<T> implements ValidBuilder<T> {
        private final Variable<T> variable;
        private final Variable[] joinVars;
        private Constraint constraint;
        private DataSource dataSource;

        private ConstrainedPatternBuilder(Variable<T> variable, Variable[] joinVars, Constraint constraint, DataSource dataSource) {
            this.variable = variable;
            this.joinVars = joinVars;
            this.constraint = constraint;
            this.dataSource = dataSource;
        }

        public ConstrainedPatternBuilder<T> from(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public ConstrainedPatternBuilder<T> and(Predicate1<T> predicate) {
            return and(new SingleConstraint1<T>(variable, predicate));
        }

        public ConstrainedPatternBuilder<T> and(Constraint constraint) {
            this.constraint = ((AbstractConstraint)this.constraint).and(constraint);
            return this;
        }

        public ConstrainedPatternBuilder<T> or(Predicate1<T> predicate) {
            return or(new SingleConstraint1<T>(variable, predicate));
        }

        public ConstrainedPatternBuilder<T> or(Constraint constraint) {
            this.constraint = ((AbstractConstraint)this.constraint).or(constraint);
            return this;
        }

        public <A, B> ConstrainedPatternBuilder<T> and(Variable<A> var1, Variable<B> var2, Predicate2<A, B> predicate) {
            return and(new SingleConstraint2<A, B>(var1, var2, predicate));
        }

        public <A> ConstrainedPatternBuilder<T> and(Variable<A> var1, Predicate2<T, A> predicate) {
            return and(new SingleConstraint2<T, A>(variable, var1, predicate));
        }

        public ConstrainedPatternBuilder<T> and(AbstractConstraint constraint) {
            this.constraint = ((AbstractConstraint)this.constraint).and(constraint);
            return this;
        }

        public <A, B> ConstrainedPatternBuilder<T> or(Variable<A> var1, Variable<B> var2, Predicate2<A, B> predicate) {
            return or(new SingleConstraint2<A, B>(var1, var2, predicate));
        }

        public <A> ConstrainedPatternBuilder<T> or(Variable<A> var1, Predicate2<T, A> predicate) {
            return or(new SingleConstraint2<T, A>(variable, var1, predicate));
        }

        public ConstrainedPatternBuilder<T> or(AbstractConstraint constraint) {
            this.constraint = ((AbstractConstraint)this.constraint).or(constraint);
            return this;
        }

        @Override
        public SinglePattern<T> get() {
            return joinVars == null ?
                   new SinglePatternImpl(variable, constraint, dataSource) :
                   new JoinPatternImpl(variable, joinVars, constraint, dataSource);
        }
    }
}