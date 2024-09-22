package com.wflow.workflow.utils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 优雅的实现if else等判断，链式编程
 * @author : JoinFyc
 * @date : 2024/9/12
 */
public class Executor {

    private boolean stop = false;

    public static Executor builder(){
        return new Executor();
    }

    public Executor ifTrueNext(Supplier<Boolean> compare, DoThings call){
        return ifTrueNext(compare.get(), call);
    }

    public Executor ifTrueNext(boolean compare, DoThings call){
        if (compare){
            call.execute();
        }
        return this;
    }

    public Executor ifNotBlankNext(String data, Consumer<String> compare){
        if (null != data && !"".equals(data.trim())){
            compare.accept(data);
        }
        return this;
    }

    public <T> Executor ifNotNullNext(T data, Consumer<T> compare){
        if (null != data){
            compare.accept(data);
        }
        return this;
    }

    @FunctionalInterface
    public interface DoThings{
        void execute();
    }
}
