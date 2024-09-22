package com.wflow.workflow.execute;

import cn.hutool.http.HttpRequest;
import com.wflow.exception.BusinessException;
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import lombok.extern.slf4j.Slf4j;

import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * @author : JoinFyc
 * @date : 2023/10/28
 */
@Slf4j
public class JsExecute {

    //创建执行沙箱环境
    private static NashornSandbox sandbox;

    //TODO 这里配置允许暴露给的java类，可以在js里面直接调用
    private static final Class[] alowJsCallJavaClass = new Class[]{
            HttpRequest.class, System.class
    };

    public JsExecute(ExecutorService executorService) {
        if (Objects.isNull(sandbox)) {
            sandbox = NashornSandboxes.create();
            //沙箱资源限制
            sandbox.setMaxCPUTime(20000);
            sandbox.setMaxMemory(10 * 1024 * 1024);
            sandbox.allowNoBraces(false);
            sandbox.setMaxPreparedStatements(30);
            sandbox.setExecutor(executorService);
            sandbox.allowGlobalsObjects(true);
            sandbox.allowPrintFunctions(true);
            sandbox.disallowAllClasses();
            //这里配置允许暴露的java类，可以在js里面直接调用
            for (Class clazz : alowJsCallJavaClass) {
                sandbox.allow(clazz);
            }
        }
    }

    public <T> T execute(String func, String script, Class<T> resultType, Object... args) {
        Invocable invocable = sandbox.getSandboxedInvocable();
        try {
            sandbox.eval(script);
            Object result = invocable.invokeFunction(func, args);
            log.debug("资源服务执行js解码结果[{}]", result);
            if (result instanceof ScriptObjectMirror) {
                //返回的是js对象 = Map<String, Object>
                return ((ScriptObjectMirror) result).to(resultType);
            } else {
                //返回普通类型数据
                return resultType.cast(result);
            }
        } catch (ScriptException | NoSuchMethodException e) {
            log.error("js 函数[{}] [{}]执行异常: [{}]", func, script, e.getMessage());
            throw new BusinessException("js函数解析执行异常:" + e.getMessage());
        }
    }

    public void executeVoid(String func, String script, Object... args) {
        execute(func, script, Object.class, args);
    }
}
