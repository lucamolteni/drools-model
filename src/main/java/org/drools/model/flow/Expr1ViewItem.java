package org.drools.model.flow;

import org.drools.model.Condition;
import org.drools.model.Variable;
import org.drools.model.functions.Predicate1;

public class Expr1ViewItem<T> extends AbstractExprViewItem<T> implements ExprViewItem {
    private final Predicate1<T> predicate;

    public Expr1ViewItem(Variable<T> var, Predicate1<T> predicate) {
        super(var);
        this.predicate = predicate;
    }

    public Predicate1<T> getPredicate() {
        return predicate;
    }

    @Override
    public Condition.Type getType() {
        return Condition.SingleType.INSTANCE;
    }
}