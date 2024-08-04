package cn.syx.sharding.strategy.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.util.*;

public class InlineExpressionParser {

    private static final char SPLITTER = ',';
    private static final Map<String, Script> SCRIPTS = new HashMap<>();
    private static final GroovyShell SHELL = new GroovyShell();
    private final String inlineExpression;

    public static String handlePlaceHolder(String inlineExpression) {
        return inlineExpression.contains("$->{") ? inlineExpression.replaceAll("\\$->\\{", "\\${") : inlineExpression;
    }

    public List<String> splitAndEvaluate() {
        return null == this.inlineExpression ? Collections.emptyList() : this.flatten(this.evaluate(this.split()));
    }

    public Closure<?> evaluateClosure() {
        return (Closure<?>) this.evaluate(Joiner.on("").join("{it -> \"", this.inlineExpression, "\"}"));
    }

    private List<Object> evaluate(List<String> inlineExpressions) {
        List<Object> result = new ArrayList<>(inlineExpressions.size());

        StringBuilder expression;
        for (Iterator<String> line = inlineExpressions.iterator(); line.hasNext();
             result.add(this.evaluate(expression.toString()))) {
            String each = line.next();
            expression = new StringBuilder(handlePlaceHolder(each));
            if (!each.startsWith("\"")) {
                expression.insert(0, "\"");
            }

            if (!each.endsWith("\"")) {
                expression.append("\"");
            }
        }

        return result;
    }

    private Object evaluate(String expression) {
        Script script;
        if (SCRIPTS.containsKey(expression)) {
            script = SCRIPTS.get(expression);
        } else {
            script = SHELL.parse(expression);
            SCRIPTS.put(expression, script);
        }

        return script.run();
    }

    private List<String> split() {
        List<String> result = new ArrayList<>();
        StringBuilder segment = new StringBuilder();
        int bracketsDepth = 0;

        for (int i = 0; i < this.inlineExpression.length(); ++i) {
            char each = this.inlineExpression.charAt(i);
            switch (each) {
                case '$':
                    if ('{' == this.inlineExpression.charAt(i + 1)) {
                        ++bracketsDepth;
                    }

                    if ("->{".equals(this.inlineExpression.substring(i + 1, i + 4))) {
                        ++bracketsDepth;
                    }

                    segment.append(each);
                    break;
                case ',':
                    if (bracketsDepth > 0) {
                        segment.append(each);
                    } else {
                        result.add(segment.toString().trim());
                        segment.setLength(0);
                    }
                    break;
                case '}':
                    if (bracketsDepth > 0) {
                        --bracketsDepth;
                    }

                    segment.append(each);
                    break;
                default:
                    segment.append(each);
            }
        }

        if (!segment.isEmpty()) {
            result.add(segment.toString().trim());
        }

        return result;
    }

    private List<String> flatten(List<Object> segments) {
        List<String> result = new ArrayList<>();
        Iterator<Object> var3 = segments.iterator();

        while (var3.hasNext()) {
            Object each = var3.next();
            if (each instanceof GString) {
                result.addAll(this.assemblyCartesianSegments((GString) each));
            } else {
                result.add(each.toString());
            }
        }

        return result;
    }

    private List<String> assemblyCartesianSegments(GString segment) {
        Set<List<String>> cartesianValues = this.getCartesianValues(segment);
        List<String> result = new ArrayList<>(cartesianValues.size());

        for (List<String> each : cartesianValues) {
            result.add(this.assemblySegment(each, segment));
        }

        return result;
    }

    private Set<List<String>> getCartesianValues(GString segment) {
        List<Set<String>> result = new ArrayList<>(segment.getValues().length);
        Object[] var3 = segment.getValues();
        int var4 = var3.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            Object each = var3[var5];
            if (null != each) {
                if (each instanceof Collection) {
                    result.add(Sets.newLinkedHashSet(Collections2.transform((Collection) each, Object::toString)));
                } else {
                    result.add(Sets.newHashSet(each.toString()));
                }
            }
        }

        return Sets.cartesianProduct(result);
    }

    private String assemblySegment(List<String> cartesianValue, GString segment) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < segment.getStrings().length; ++i) {
            result.append(segment.getStrings()[i]);
            if (i < cartesianValue.size()) {
                result.append(cartesianValue.get(i));
            }
        }

        return result.toString();
    }

    public InlineExpressionParser(String inlineExpression) {
        this.inlineExpression = inlineExpression;
    }
}
