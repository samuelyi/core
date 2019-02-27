package com.dotcms.rendering.velocity.services;

import static com.dotmarketing.util.Constants.CONTAINER_FOLDER_PATH;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.util.PageMode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class VelocityResourceKeyTest {

    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();


    }

    @Test
    public void testKeyGenerationSymmetry() throws Exception {

        FileAssetContainer fileAssetContainer =  new FileAssetContainer();
        fileAssetContainer.setPath(CONTAINER_FOLDER_PATH + "/large-column/" );
        fileAssetContainer.setLanguage(1);

        Host host = new Host();
        host.setHostname("demo.dotcms.com");
        final String stringForm = "/LIVE/demo.dotcms.com/application/containers/large-column/container.vtl-1.container";

        final VelocityResourceKey  velocityResourceKey1 =  new VelocityResourceKey(fileAssetContainer,host,PageMode.LIVE);
        Assert.assertEquals("", stringForm, velocityResourceKey1.cacheKey);

        final VelocityResourceKey  velocityResourceKey2 =  new VelocityResourceKey(stringForm);
        Assert.assertEquals("", stringForm, velocityResourceKey2.cacheKey);

        Assert.assertEquals("", velocityResourceKey1.path, velocityResourceKey2.path);

    }

}
