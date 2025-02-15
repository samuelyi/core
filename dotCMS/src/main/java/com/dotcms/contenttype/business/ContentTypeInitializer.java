package com.dotcms.contenttype.business;

import com.dotcms.config.DotInitializer;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Initialiaze content types
 * @author jsanca
 */
public class ContentTypeInitializer implements DotInitializer {

    public static final String LEGACY_FAVORITE_PAGE_VAR_NAME = "favoritePage";
    static final String FAVORITE_PAGE_VAR_NAME = "dotFavoritePage";

    @Override
    public void init() {

        this.checkFavoritePage();
    }

    private void checkFavoritePage() {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        //I'll try to remove the existing content type using the legacy var name (favoritePage), as it was renamed to dotFavoritePage
        //By the time it was created as `favoritePage`, this feature had not been released, so I don't need to worry about existing pieces of content
        ContentType contentType = Try.of(()->contentTypeAPI.find(LEGACY_FAVORITE_PAGE_VAR_NAME)).getOrNull();
        if (null != contentType){
            try {
                contentTypeAPI.delete(contentType);
            } catch (DotSecurityException | DotDataException e) {
                Logger.warnAndDebug(this.getClass(), e);
            }
        }

        contentType = Try.of(()->contentTypeAPI.find(FAVORITE_PAGE_VAR_NAME)).getOrNull();
        if (null == contentType) {
            Logger.info(this, "Creating the Favorite Page Content Type...");
            final ImmutableSimpleContentType.Builder builder = ImmutableSimpleContentType.builder();
            builder.name("Dot Favorite Page")
                    .variable(FAVORITE_PAGE_VAR_NAME)
                    .host(Host.SYSTEM_HOST)
                    .host(Folder.SYSTEM_FOLDER)
                    .fixed(true)
            ;
            final SimpleContentType simpleContentType = builder.build();

            saveFavoritePageFields(contentTypeAPI, simpleContentType);
        } else {
            // if the content type exists, we need to see if latest changes are there, otherwise we need to redefine the content type.
            if (contentType.fieldMap().get("url").unique() || !contentType.fieldMap().get("order").indexed() || !contentType.fixed()) {
                Logger.debug(ContentTypeInitializer.class, "dotFavoritePage CT Needs to be regenerated.");
                this.saveFavoritePageFields(contentTypeAPI, contentType);
            }
        }
    }

    private void saveFavoritePageFields(final ContentTypeAPI contentTypeAPI, final ContentType simpleContentType) {
        try {

            final List<Field> newFields = new ArrayList<>();
            final ImmutableBinaryField screenshotField = ImmutableBinaryField.builder().name("Screenshot").variable("screenshot").build();
            final ImmutableTextField   titleField      = ImmutableTextField.builder().name("title").variable("title").build();
            final ImmutableTextField   urlField        = ImmutableTextField.builder().name("url").variable("url").required(true).indexed(true).unique(false).build();
            final ImmutableTextField   orderField      = ImmutableTextField.builder().name("order").dataType(DataTypes.INTEGER).variable("order").indexed(true).build();

            newFields.add(screenshotField);
            newFields.add(titleField);
            newFields.add(urlField);
            newFields.add(orderField);
            final ContentType savedContentType = contentTypeAPI.save(simpleContentType, newFields, null);

            final Set<String> workflowIds = new HashSet<>();
            workflowIds.add(WorkflowAPI.SYSTEM_WORKFLOW_ID);

            APILocator.getWorkflowAPI().saveSchemeIdsForContentType(savedContentType, workflowIds);
            Logger.debug(ContentTypeInitializer.class, "dotFavoritePage CT Saved.");
        } catch (DotDataException | DotSecurityException e) {

            Logger.warnAndDebug(this.getClass(), e);
        }
    }
}
