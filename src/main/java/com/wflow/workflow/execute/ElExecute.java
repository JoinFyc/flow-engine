package com.wflow.workflow.execute;

import com.wflow.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author : JoinFyc
 * @date : 2023/10/29
 */
@Slf4j
public class ElExecute {

    private static final ExpressionParser expressionParser = new SpelExpressionParser();

    public <T> T execute(String el, Map<String, Object> params, Class<T> resultType){
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            Optional.ofNullable(params).ifPresent(context::setVariables);
            context.setBeanResolver(SpringBeanResolver.getBean(SpringBeanResolver.class));
            Expression expression = expressionParser.parseExpression(el);
            return expression.getValue(context, resultType);
        } catch (ParseException | EvaluationException e) {
            log.error("EL表达式[{}]异常", el, e);
            throw new BusinessException("EL表达式解析异常: " + e.getMessage());
        }
    }

    public static void validate(String el){
        expressionParser.parseExpression(el);
    }
}
