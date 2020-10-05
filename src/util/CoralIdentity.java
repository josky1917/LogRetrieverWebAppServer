package com.amazon.aws.vpn.telemetry.horizonte.webapp.util;

import com.amazon.coral.service.Call;
import com.amazon.coral.service.Identity;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * CoralIdentity.
 * This class is used for integrate identity into coral client initialization.
 *
 * @param <I>
 */
public class CoralIdentity<I> {
    private final AWSCredentialsProvider provider;

    /**
     * Constructor.
     *
     * @param provider provider
     */
    public CoralIdentity(final AWSCredentialsProvider provider) {
        this.provider = provider;
    }

    /**
     * AddToClient.
     *
     * @param clazz  Class Type
     * @param client Client obj
     * @return Class
     */
    public I addToClient(final Class<I> clazz, final Object client) {
        class CredentialsAddingHandler implements InvocationHandler {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws IllegalAccessException, InvocationTargetException {
                Object returnValue;
                if (args == null) {
                    returnValue = method.invoke(client);
                } else {
                    returnValue = method.invoke(client, args);
                }

                try {
                    Identity identity = getIdentity();
                    final Call call = (Call) returnValue;
                    call.setIdentity(identity);
                    return call;
                } catch (Exception e) {
                    return returnValue;
                }
            }
        }

        return (I) Proxy.newProxyInstance(
                client.getClass().getClassLoader(),
                new Class[]{clazz},
                new CredentialsAddingHandler());
    }

    /**
     * Get Credential.
     *
     * @return identity
     */
    public Identity getIdentity() {
        final AWSCredentials credentials = provider.getCredentials();
        final Identity identity = new Identity();
        identity.setAttribute(Identity.AWS_ACCESS_KEY, credentials.getAWSAccessKeyId());
        identity.setAttribute(Identity.AWS_SECRET_KEY, credentials.getAWSSecretKey());
        return identity;
    }
}
