package com.dotcms.rendering.velocity.services;

import static com.dotmarketing.util.Constants.CONTAINER_FOLDER_PATH;
import static com.dotmarketing.util.Constants.CONTAINER_META_INFO_FILE_NAME;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import org.apache.velocity.runtime.resource.ResourceManager;



public class VelocityResourceKey implements Serializable {

    private final static char RESOURCE_TEMPLATE = ResourceManager.RESOURCE_TEMPLATE + '0';

    private static final long serialVersionUID = 1L;
    public final String path, language, id1, id2, cacheKey;
    public final VelocityType type;
    public final PageMode mode;

    public VelocityResourceKey(final Object obj) {
        this((String) obj);
    }
    
    public VelocityResourceKey(final Field asset, Optional<Contentlet> con, PageMode mode) {
        this("/" + mode.name() + "/" + (con.isPresent() ?  con.get().getInode() : FieldLoader.FIELD_CONSTANT  ) + "/" + asset.id() +  "." + VelocityType.FIELD.fileExtension);
    }
    
    public VelocityResourceKey(final Template asset, final PageMode mode) {
        this("/" + mode.name() + "/" + asset.getIdentifier() +  "." + VelocityType.TEMPLATE.fileExtension);
    }
    public VelocityResourceKey(final Container asset, final PageMode mode) {
        this(asset, Container.LEGACY_RELATION_TYPE, mode);
    }

    public VelocityResourceKey(final FileAssetContainer asset, final Host host, final PageMode mode) {
        this(normalizeForwardSlashes(
                "/" + mode.name() + "/" + host.getHostname() + "/" + asset.getPath() + "/"
                        + CONTAINER_META_INFO_FILE_NAME + "-" + asset.getLanguageId() + "."
                        + VelocityType.CONTAINER.fileExtension));
    }

    public VelocityResourceKey(final Container asset, final String uuid, final PageMode mode) {
        this("/" + mode.name() + "/" + asset.getIdentifier() + "/" + uuid +  "." + VelocityType.CONTAINER.fileExtension);
    }

    public VelocityResourceKey(final HTMLPageAsset asset, final PageMode mode, final long language) {
        this("/" + mode.name() + "/" + asset.getIdentifier() + "_" + language + "." + VelocityType.HTMLPAGE.fileExtension);
    }
    public VelocityResourceKey(final Contentlet asset, final PageMode mode, final long language) {
        this("/" + mode.name() + "/" + asset.getIdentifier() + "_" + language + "." + VelocityType.CONTENT.fileExtension);
    }
    public VelocityResourceKey(final String filePath) {
        path = cleanKey(filePath);
        final String[] pathArry = path.split("[/\\.]", 0);
        this.mode = PageMode.get(pathArry[1]);
        this.language = pathArry[2].indexOf("_") > -1 ? pathArry[2].substring(pathArry[2].indexOf("_") + 1, pathArry[2].length())
                : String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId());

       if(isFileAssetContainer()){
           this.id1 = null;
           this.id2 = null;
       } else {
           this.id1 = pathArry[2].indexOf("_") > -1 ? pathArry[2].substring(0, pathArry[2].indexOf("_")) : pathArry[2];
           this.id2 = pathArry.length > 4 ? pathArry[3] : null;
        }
        this.type = VelocityType.resolveVelocityType(filePath);
        this.cacheKey = cacheKey();
    }

    private String cacheKey() {

        if (type == VelocityType.CONTAINER) {

            return isFileAssetContainer() ?
            normalize(path) :

            new StringBuilder()
                    .append(mode.name())
                    .append('-')
                    .append(id1)
                    .append('-')
                    .append(language)
                    .append(".")
                    .append(type.fileExtension)
                    .toString();

        } else {
            return path;
        }


    }

    private String cleanKey(final String key) {
        String newkey = (key.charAt(0) == RESOURCE_TEMPLATE) ? key.substring(1) : key;
        newkey = (newkey.charAt(0) != '/') ? "/" + newkey : newkey;
        return (newkey.contains("\\")) ? UtilMethods.replace(newkey, "\\", "/") : newkey;

    }

    private boolean isFileAssetContainer(){
        return path.contains(CONTAINER_FOLDER_PATH);
    }

    public static String normalize(final String path) {
        final Iterator<String> iterator = Arrays.stream(path.split("/")).iterator();
        final StringBuilder stringBuilder = new StringBuilder("/");
        while (iterator.hasNext()) {
            final String str = iterator.next();
            if (UtilMethods.isSet(str) && !str.matches("^[0-9].*$")) {
                stringBuilder.append(str);
                if (iterator.hasNext()) {
                    stringBuilder.append("/");
                }
            }
        }
        return normalizeForwardSlashes(stringBuilder.toString());
    }

    public static String normalizeForwardSlashes(final String in){
        return in.replaceAll("/+","/");
    }

    @Override
    public String toString() {
        return "VelocityResourceKey [path=" + path + ", language=" + language + ", id1=" + id1 + ", id2=" + id2 + ", type=" + type
                + ", mode=" + mode + "]";
    }



}
