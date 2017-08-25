package org.drools.model.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.drools.model.AccumulateFunction;
import org.drools.model.Condition;
import org.drools.model.Condition.Type;
import org.drools.model.Constraint;
import org.drools.model.DataSourceDefinition;
import org.drools.model.Pattern;
import org.drools.model.Variable;
import org.drools.model.constraints.SingleConstraint1;
import org.drools.model.constraints.SingleConstraint2;
import org.drools.model.patterns.AccumulatePatternImpl;
import org.drools.model.patterns.CompositePatterns;
import org.drools.model.patterns.InvokerMultiValuePatternImpl;
import org.drools.model.patterns.InvokerSingleValuePatternImpl;
import org.drools.model.patterns.OOPathImpl;
import org.drools.model.patterns.PatternImpl;
import org.drools.model.view.AccumulateExprViewItem;
import org.drools.model.view.CombinedExprViewItem;
import org.drools.model.view.Expr1ViewItemImpl;
import org.drools.model.view.Expr2ViewItemImpl;
import org.drools.model.view.ExprViewItem;
import org.drools.model.view.InputViewItem;
import org.drools.model.view.OOPathViewItem;
import org.drools.model.view.OOPathViewItem.OOPathChunk;
import org.drools.model.view.SetViewItem;
import org.drools.model.view.ViewItem;
import org.drools.model.view.ViewItemBuilder;

import static java.util.stream.Collectors.toList;
import static org.drools.model.DSL.input;
import static org.drools.model.constraints.AbstractSingleConstraint.fromExpr;

public class ViewBuilder {

    private ViewBuilder() { }

    public static CompositePatterns viewItems2Patterns( ViewItemBuilder[] viewItemBuilders ) {
        if (viewItemBuilders.length == 1 && viewItemBuilders[0] instanceof ExprViewItem && ((ExprViewItem) viewItemBuilders[0]).getType() == Type.AND) {
            return viewItems2Condition( Arrays.asList(((CombinedExprViewItem) viewItemBuilders[0]).getExpressions()), new HashMap<>(), new HashSet<>(), Type.AND, true );
        }
        List<ViewItem> viewItems = Stream.of( viewItemBuilders ).map( ViewItemBuilder::get ).collect( toList() );
        return viewItems2Condition( viewItems, new HashMap<>(), new HashSet<>(), Type.AND, true );
    }

    public static CompositePatterns viewItems2Condition(List<ViewItem> viewItems, Map<Variable<?>, InputViewItem<?>> inputs,
                                                Set<Variable<?>> usedVars, Condition.Type type, boolean topLevel) {
        List<Condition> conditions = new ArrayList<>();
        Map<Variable<?>, Condition> conditionMap = new HashMap<>();
        for (ViewItem viewItem : viewItems) {
            if ( viewItem instanceof CombinedExprViewItem ) {
                CombinedExprViewItem combined = (CombinedExprViewItem) viewItem;
                conditions.add( viewItems2Condition( Arrays.asList( combined.getExpressions() ), inputs, usedVars, combined.getType(), false ) );
                continue;
            }

            Variable<?> patterVariable = viewItem.getFirstVariable();
            if ( viewItem instanceof InputViewItem ) {
                inputs.put( patterVariable, (InputViewItem) viewItem );
                continue;
            }

            if ( viewItem instanceof SetViewItem ) {
                SetViewItem setViewItem = (SetViewItem) viewItem;
                Pattern pattern = setViewItem.isMultivalue() ?
                                  new InvokerMultiValuePatternImpl( DataSourceDefinitionImpl.DEFAULT,
                                                                    setViewItem.getInvokedFunction(),
                                                                    patterVariable,
                                                                    setViewItem.getInputVariables() ) :
                                  new InvokerSingleValuePatternImpl( DataSourceDefinitionImpl.DEFAULT,
                                                                     setViewItem.getInvokedFunction(),
                                                                     patterVariable,
                                                                     setViewItem.getInputVariables() );
                conditionMap.put( patterVariable, pattern );
                conditions.add( pattern );
                continue;
            }

            usedVars.add(patterVariable);
            Condition condition;
            if ( type == Type.AND ) {
                condition = conditionMap.get( patterVariable );
                if ( condition == null ) {
                    condition = new PatternImpl( patterVariable, Constraint.EMPTY, getDataSourceDefinition( inputs, patterVariable ) );
                    conditions.add( condition );
                    conditionMap.put( patterVariable, condition );
                }
            } else {
                condition = new PatternImpl( patterVariable, Constraint.EMPTY, getDataSourceDefinition( inputs, patterVariable ) );
                conditions.add( condition );
            }

            if (viewItem instanceof ExprViewItem ) {
                for (Variable var : viewItem.getVariables()) {
                    if (var.isFact()) {
                        inputs.putIfAbsent( var, (InputViewItem) input( var ) );
                    }
                }
            }

            Condition modifiedPattern = viewItem2Condition( viewItem, condition, usedVars );
            conditions.set( conditions.indexOf( condition ), modifiedPattern );
            if (type == Type.AND) {
                conditionMap.put( patterVariable, modifiedPattern );
            }
        }

        CompositePatterns condition = new CompositePatterns( type, conditions, usedVars );
        if ( type == Type.AND ) {
            if ( topLevel && inputs.size() > usedVars.size() ) {
                inputs.keySet().removeAll( usedVars );
                for ( Map.Entry<Variable<?>, InputViewItem<?>> entry : inputs.entrySet() ) {
                    conditions.add( 0, new PatternImpl( entry.getKey(), Constraint.EMPTY, entry.getValue().getDataSourceDefinition() ) );
                }
            }
        }
        return condition;
    }

    private static DataSourceDefinition getDataSourceDefinition( Map<Variable<?>, InputViewItem<?>> inputs, Variable var ) {
        InputViewItem input = inputs.get(var);
        return input != null ? input.getDataSourceDefinition() : DataSourceDefinitionImpl.DEFAULT;
    }

    private static Condition viewItem2Condition( ViewItem viewItem, Condition condition, Set<Variable<?>> usedVars ) {
        if (viewItem instanceof Expr1ViewItemImpl ) {
            Expr1ViewItemImpl expr = (Expr1ViewItemImpl)viewItem;
            ( (PatternImpl) condition ).addConstraint( new SingleConstraint1( expr ) );
            return condition;
        }

        if (viewItem instanceof Expr2ViewItemImpl ) {
            Expr2ViewItemImpl expr = (Expr2ViewItemImpl)viewItem;
            ( (PatternImpl) condition ).addConstraint( new SingleConstraint2( expr ) );
            return condition;
        }

        if (viewItem instanceof AccumulateExprViewItem) {
            AccumulateExprViewItem acc = (AccumulateExprViewItem)viewItem;
            for ( AccumulateFunction accFunc : acc.getFunctions()) {
                usedVars.add(accFunc.getVariable());
            }
            return new AccumulatePatternImpl( (Pattern) viewItem2Condition( acc.getExpr(), condition, usedVars ), acc.getFunctions() );
        }

        if (viewItem instanceof OOPathViewItem) {
            OOPathViewItem<?,?> oopath = ( (OOPathViewItem) viewItem );
            if (oopath.getChunks().size() > 1) {
                throw new UnsupportedOperationException();
            }
            OOPathChunk chunk = oopath.getChunks().get( 0 );
            for (Variable var : chunk.getExpr().getVariables()) {
                usedVars.add(var);
            }
            ( (PatternImpl) condition ).addConstraint( fromExpr( chunk.getExpr() ) );
            OOPathImpl oopathPattern = new OOPathImpl( oopath.getSource(), oopath.getChunks() );
            oopathPattern.setFirstCondition( condition );
            return oopathPattern;
        }

        throw new UnsupportedOperationException( "Unknown ViewItem: " + viewItem );
    }
}
