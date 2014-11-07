package org.avasquez.seccloudfs.apache.vfs.utils;

import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.FileProvider;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

/**
 * Factory Bean that helps create and configure a {@link org.apache.commons.vfs2.impl.DefaultFileSystemManager}.
 *
 * @author avasquez
 */
public class DefaultFileSystemManagerFactoryBean implements FactoryBean<DefaultFileSystemManager> {

    private UserAuthenticator authenticator;
    private Map<String, FileProvider> providers;

    private DefaultFileSystemManager fileSystemManager;

    public void setAuthenticator(UserAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Required
    public void setProviders(Map<String, FileProvider> providers) {
        this.providers = providers;
    }

    @Override
    public DefaultFileSystemManager getObject() throws Exception {
        return fileSystemManager;
    }

    @PostConstruct
    public void init() throws FileSystemException {
        fileSystemManager = new DefaultFileSystemManager();

        for (Map.Entry<String, FileProvider> providerEntry : providers.entrySet()) {
            fileSystemManager.addProvider(providerEntry.getKey(), providerEntry.getValue());
        }

        if (authenticator != null) {
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(new FileSystemOptions(), authenticator);
        }

        fileSystemManager.init();
    }

    @PreDestroy
    public void destroy() {
        fileSystemManager.close();
    }

    @Override
    public Class<?> getObjectType() {
        return DefaultFileSystemManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
