package io.joyrpc.protocol.dubbo.serialization.hessian;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.joyrpc.com.caucho.hessian.io.AbstractHessianInput;
import io.joyrpc.com.caucho.hessian.io.AutowiredObjectDeserializer;
import io.joyrpc.protocol.dubbo.message.DubboInvocation;
import io.joyrpc.util.ClassUtils;
import org.apache.dubbo.common.utils.ReflectUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import static io.joyrpc.protocol.dubbo.message.DubboInvocation.DUBBO_GROUP_KEY;
import static io.joyrpc.protocol.dubbo.message.DubboInvocation.DUBBO_VERSION_KEY;

/**
 * DubboInvocation反序列化
 */
public class DubboInvocationDeserializer implements AutowiredObjectDeserializer {

    @Override
    public Class<?> getType() {
        return DubboInvocation.class;
    }

    @Override
    public Object readObject(AbstractHessianInput in) throws IOException {

        String dubboVersion = in.readString();
        String className = in.readString();
        String version = in.readString();
        String methodName = in.readString();
        String desc = in.readString();

        Object[] args;
        Class<?>[] pts;
        Method method = null;

        try {
            if (!methodName.equals("$invoke") && !methodName.equals("$invokeAsync")) {
                method = ClassUtils.getPublicMethod(className, methodName);
                pts = method.getParameterTypes();
            } else {
                methodName = null;
                pts = ReflectUtils.desc2classArray(desc);
            }
            if (pts.length == 0) {
                args = new Object[0];
            } else {
                args = new Object[pts.length];
                for (int i = 0; i < args.length; i++) {
                    args[i] = in.readObject(pts[i]);
                }
            }
        } catch (Exception e) {
            throw new IOException("Read dubbo invocation data failed.", e);
        }

        //获取传参信息
        Map<String, Object> attachments = (Map<String, Object>) in.readObject(Map.class);
        //设置 dubboVersion
        attachments.put(DUBBO_VERSION_KEY, dubboVersion);
        //获取别名
        String alias = (String) attachments.get(DUBBO_GROUP_KEY);
        //创建DubboInvocation对象
        DubboInvocation invocation = new DubboInvocation();
        invocation.setClassName(className);
        invocation.setAlias(alias);
        invocation.setAttachments(attachments);
        invocation.setArgs(args);
        invocation.setVersion(version);
        invocation.setParameterTypesDesc(desc);
        if (invocation.isGeneric()) {
            methodName = (String) args[0];
            try {
                method = ClassUtils.getPublicMethod(className, methodName);
            } catch (Exception e) {
                throw new IOException("Read dubbo invocation data failed.", e);
            }
            String[] ptNames = new String[pts.length];
            if (pts.length > 0) {
                for (int i = 0; i < ptNames.length; i++) {
                    ptNames[i] = pts[i].getName();
                }
            }
            invocation.setArgsType(ptNames);
        } else {
            invocation.setArgsType(pts);
        }

        invocation.setMethodName(methodName);
        invocation.setMethod(method);

        return invocation;
    }
}
