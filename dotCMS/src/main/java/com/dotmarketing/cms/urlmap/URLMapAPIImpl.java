package com.dotmarketing.cms.urlmap;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.content.elasticsearch.util.ESUtils;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.business.web.VariantWebAPI.RenderContext;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.API;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation for the {@link URLMapAPI}.
 *
 */
public class URLMapAPIImpl implements URLMapAPI {

    private final Collection<ContentTypeURLPattern> patternsCache= new ArrayList<>();
    private final UserWebAPI wuserAPI = WebAPILocator.getUserWebAPI();
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
    private final ContentTypeAPI typeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

    @Override
    public boolean isUrlPattern(final UrlMapContext urlMapContext)
            throws DotDataException, DotSecurityException {
        return getContentlet(urlMapContext) != null;
    }

    @Override
    public Optional<URLMapInfo> processURLMap(final UrlMapContext context)
            throws DotSecurityException, DotDataException {

        final Contentlet contentlet = getContentlet(context);
        if (contentlet == null) {
            return Optional.empty();
        }

        final ContentType contentType = contentlet.getContentType();
        final Optional<Identifier> optDetailIdentifier = this.getDetailPageUri(contentType, context.getHost());

        if(!optDetailIdentifier.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new URLMapInfo(contentlet, optDetailIdentifier.get(), context.getUri()));
    }

    /**
     * Returns the {@link Contentlet} object that matches the {@link UrlMapContext#getUri()} of a specific URL Map.
     *
     * @param urlMapContext The instance of the URL Map Context.
     *
     * @return The Contentlet matching the URL Map. If no match is found, a {@code null} object is returned.
     */
    private Contentlet getContentlet(final UrlMapContext urlMapContext) throws DotSecurityException {

        Contentlet matchingContentlet = null;

        try {
            // We could have multiple matches as multiple content types could have the same
            // URLMap pattern and we need to evaluate all until we find content match.
            final List<Matches> matchesFound = this.findMatch(urlMapContext.getUri());
            if (!matchesFound.isEmpty()) {

                for (final Matches matches : matchesFound) {
                    final ContentType contentType = typeAPI.find(matches.getPatternChange().getStructureInode());

                    matchingContentlet = this
                            .getContentlet(matches, contentType, urlMapContext);
                    if (null != matchingContentlet) {
                        break;
                    }
                }

            }
        } catch (final DotDataException e) {
            Logger.error(this.getClass(), String.format("An error occurred when finding contentlet matches for URL " +
                    "Map [%s]", urlMapContext.getUri()), e);
            return null;
        }

        return matchingContentlet;
    }

    private Optional<Identifier> getDetailPageUri(final ContentType contentType, Host currentHost) {
        if (contentType == null || UtilMethods.isEmpty(contentType.detailPage())) {
            return Optional.empty();
        }
        
        try {
            final Identifier identifier = this.identifierAPI.find(contentType.detailPage());
            if (identifier == null || !UtilMethods.isSet(identifier.getId())) {
                Logger.info(this.getClass(),
                        "No valid detail page for Content Type '" + contentType.name()
                                + "'. Looking for detail page id=" + contentType
                                .detailPage());
                return Optional.empty();
            }
            
            //if the detail page is on this host, send it!
            if(identifier.getHostId().equals(currentHost.getIdentifier())) {
                return Optional.of(identifier);
            }
            
            // look for it on the current host
            final Identifier myHostIdentifier = this.identifierAPI.find(currentHost, identifier.getPath());
            if (myHostIdentifier == null || !UtilMethods.isSet(myHostIdentifier.getId())) {
                Logger.info(this.getClass(),
                        "No valid detail page for Content Type '" + contentType.name()
                                + "'. Looking for a detail page=" + identifier.getPath() + " on Site " + currentHost.getHostname());
                return Optional.empty();
            }

            return Optional.of(myHostIdentifier);
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), e);
            return Optional.empty();
        }
    } 
    

    /**
     * Return all the matches related to a given URI, multiple content types could use the URLMap
     * pattern and on those cases we need to evaluate all the matches.
     *
     * @param uri URI to evaluate for matches
     * @return List of found matches
     * @throws DotDataException
     */
    private List<Matches> findMatch(final String uri) throws DotDataException {

        // We want to avoid unnecessary lookups for vanity urls when browsing in the backend
        final boolean filtered = CMSUrlUtil.BACKEND_FILTERED_COLLECTION.stream()
                .anyMatch(uri::startsWith);
        if (filtered) {
            return Collections.emptyList();
        }

        if (this.shouldLoadPatterns()) {
            this.loadPatterns();
        }

        final List<Matches> foundMatches = new ArrayList<>();

        final String url =
                !uri.endsWith(StringPool.FORWARD_SLASH) ? uri + StringPool.FORWARD_SLASH : uri;

        for (final ContentTypeURLPattern contentTypeURLPattern : this.patternsCache) {

            final List<RegExMatch> matches = RegEX
                    .findForUrlMap(url, contentTypeURLPattern.getRegEx());
            if (matches != null && !matches.isEmpty()) {

                /*
                We need to make sure we have an exact match, we could have regex too generic, like
                a regex in the root: "/{urlTitle}" resulting in a regex like "/(.+)/" which basically
                will match any url.
                 */
                for (final RegExMatch regExMatch : matches) {
                    if (regExMatch.getMatch().equals(url)) {
                        foundMatches.add(new Matches(contentTypeURLPattern, matches));
                    }
                }

            }
        }

        return foundMatches;
    }

    /**
     * Builds the part of the Lucene query that specifies what fields from the given {@link ContentType} are being
     * referenced by the URL Map.
     *
     * @param contentType The Content Type that the URL Map belongs to.
     * @param matches     The fields that are referenced byt the URL Map.
     *
     * @return The fields from the Content Type that match the fields referenced in the URL Map.
     */
    private String buildFields(final ContentType contentType,
            final Matches matches) {

        final StringBuilder query = new StringBuilder();
        final List<RegExMatch> groups = matches.getMatches().get(0).getGroups();
        final List<String> fieldMatches = matches.getPatternChange().getFieldMatches();
        int counter = 0;
        final Map<String, Field> fieldMap = contentType.fieldMap();

        for (final RegExMatch regExMatch : groups) {

            String value = regExMatch.getMatch();
            if (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            query.append('+')
                    .append(contentType.variable()).append('.');

            final String variableName = fieldMatches.get(counter);
            final Field field = fieldMap.get(variableName);
            if (null == field || null == field.dataType()) {
                // The field in the URL Map doesn't belong to any field in the Content Type. Just move on
                continue;
            }
            if (field.dataType().equals(DataTypes.INTEGER) || field.dataType().equals(DataTypes.FLOAT)){
                query.append(variableName);
            } else {
                query.append(variableName).append("_dotRaw");
            }

            query.append(':')
                .append(ESUtils.escapeExcludingSlashIncludingSpace(value)).append(' ');
            counter++;
        }

        return query.toString();
    }

    /**
     * Based on the valid matches, this method will find any {@link Contentlet} of the specified {@link ContentType}
     * that matches referenced fields in the URL Map.
     *
     * @param matches
     * @param contentType The Content Type that the URL Map belongs to.
     * @param context     The instance of the URL Map Context.
     *
     * @return The Contentlet that matches the URL Map.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException
     */
    private Contentlet getContentlet(
            final Matches matches,
            final ContentType contentType,
            final UrlMapContext context)
             throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;

        final String query = this.buildContentQuery(matches, contentType, context);
        final List<Contentlet> contentletSearches =
                ContentUtils.pull(query, 0, 2, "score",this.wuserAPI.getSystemUser(), true);

        if (!contentletSearches.isEmpty()) {

            int idx = 0;
            if (contentletSearches.size() == 2) {
                // prefer session setting
                final Contentlet second = contentletSearches.get(1);
                if (second.getLanguageId() == context.getLanguageId()) {
                    idx = 1;
                }
            }

            contentlet = contentletSearches.get(idx);
            checkContentPermission(context, contentlet);

            final String currentVariantId = WebAPILocator.getVariantWebAPI().currentVariantId();

            if (!VariantAPI.DEFAULT_VARIANT.name().equals(currentVariantId)) {

                final RenderContext renderContext = WebAPILocator.getVariantWebAPI()
                        .getRenderContext(context.getLanguageId(), contentlet.getIdentifier(),
                                context.getMode(), context.getUser());

                final ContentletVersionInfo contentletVersionInfo = APILocator.getVersionableAPI()
                        .getContentletVersionInfo(contentlet.getIdentifier(),
                                renderContext.getCurrentLanguageId(),
                                renderContext.getCurrentVariantKey())
                        .orElseThrow();

                final String inode = context.getMode().showLive
                        ? contentletVersionInfo.getLiveInode()
                        : contentletVersionInfo.getWorkingInode();

                contentlet = APILocator.getContentletAPI().find(inode, context.getUser(), false);
            }
        }

       final Contentlet finalContentlet = contentlet;

        if(context.isGraphQL()) {
            return Try.of(() -> new DotTransformerBuilder().
                    graphQLDataFetchOptions().content(finalContentlet).build().hydrate().get(0)).getOrNull();
        } else {
            return Try.of(() -> new DotTransformerBuilder().
                    defaultOptions().content(finalContentlet).build().hydrate().get(0)).getOrNull();
        }
    }

    private void checkContentPermission(final UrlMapContext context, final Contentlet contentlet)
            throws DotDataException, DotSecurityException {

        final boolean havePermission = this.permissionAPI.doesUserHavePermission(
                contentlet, PermissionLevel.READ.getType(), context.getUser(), context.getMode().respectAnonPerms);

        if (!havePermission) {
            throw new DotSecurityException(String.format("User does not have permission in content: %s", contentlet.getName()));
        }
    }

    /**
     * Builds the Lucene query used to find the specific {@link Contentlet} that matches a given URL Map for a
     * Content Type.
     *
     * @param matches     The set of URL Maps that match a specific Content Type.
     * @param contentType The Content Type that matches the URL Map.
     * @param context     The instance of the URL Map Context.
     *
     * @return The Lucene query that will return a potential match for the URL Map.
     */
    private String buildContentQuery(
            final Matches matches,
            final ContentType contentType,
            final UrlMapContext context) {

        final StringBuilder query = new StringBuilder();

        query.append("+contentType:")
            .append(contentType.variable())
            .append(" +" + ESMappingConstants.VARIANT + ":")
            .append(VariantAPI.DEFAULT_VARIANT.name())
            .append(" +deleted:false ")
            .append(" +(conhost:")
                .append(context.getHost().getIdentifier())
                .append(" OR conhost:")
                .append(Host.SYSTEM_HOST)
            .append(")");
        if (context.getMode().showLive) {
            query.append(" +live:true ");
        } else {
            query.append(" +working:true ");
        }
        query.append(" ");
        query.append(this.buildFields(contentType, matches));
        
        // score the current language higher
        query.append(" languageId:").append(context.getLanguageId());

        return query.toString();
    }

    /**
     * Determines whether the official list of URL Map Patterns must be reloaded based on any of the following conditions:
     * <ol>
     *     <li>The cached master RegEx is null.</li>
     *     <li>The list of cached patterns is empty.</li>
     * </ol>
     *
     * @return If the list of patterns must be reloaded, returns {@code true}. Otherwise, returns {@code false}.
     */
    private boolean shouldLoadPatterns() {
        String mastRegEx = null;

        try {
            mastRegEx = CacheLocator.getContentTypeCache().getURLMasterPattern();
        } catch (DotCacheException e2) {
            Logger.error(URLMapAPIImpl.class, e2.getMessage(), e2);
        }

        return mastRegEx == null || patternsCache.isEmpty();
    }

    /**
     * Builds the list of URL maps and sorts them by the number of slashes in
     * the URL (highest to lowest). This method is called only when a new URL
     * map is added, and is marked as <code>synchronized</code> to avoid data
     * inconsistency.
     *
     * @return A <code>String</code> containing a Regex, which contains all the URL maps in the
     * system.
     * @throws DotDataException An error occurred when retrieving information from the database.
     */
    private synchronized void loadPatterns() throws DotDataException {
        
        if(!shouldLoadPatterns()) {
            return;
        }
        
        patternsCache.clear();

        final List<SimpleStructureURLMap> urlMaps = typeAPI.findStructureURLMapPatterns();

        if (urlMaps != null && !urlMaps.isEmpty()) {
            final StringBuilder masterRegEx = new StringBuilder("^(");

            final int startLength = masterRegEx.length();

            for (final SimpleStructureURLMap urlMap : urlMaps) {
                final String regEx = StructureUtil.generateRegExForURLMap(urlMap.getURLMapPattern());

                if (!UtilMethods.isSet(regEx) || regEx.trim().length() < 3) {
                    continue;
                }

                patternsCache.add(new ContentTypeURLPattern(
                        regEx, urlMap.getInode(),
                        urlMap.getURLMapPattern(), getFieldMatches(urlMap)
                ));

                if (masterRegEx.length() > startLength) {
                    masterRegEx.append('|');
                }

                masterRegEx.append(regEx);
            }

            masterRegEx.append(")");

            CacheLocator.getContentTypeCache().addURLMasterPattern(masterRegEx.toString());
        }
    }

    @NotNull
    private List<String> getFieldMatches(final SimpleStructureURLMap urlMap) {
        final List<RegExMatch> fieldMatches = RegEX.find(urlMap.getURLMapPattern(), "{([^{}]+)}");
        final List<String> fields = new ArrayList<>();
        for (final RegExMatch regExMatch : fieldMatches) {
            fields.add(regExMatch.getGroups().get(0).getMatch());
        }
        return fields;
    }

    private class Matches {
        final ContentTypeURLPattern patternChange;
        final List<RegExMatch> matches;

        public Matches(final ContentTypeURLPattern patternChange, final List<RegExMatch> matches) {
            this.patternChange = patternChange;
            this.matches = matches;
        }

        public ContentTypeURLPattern getPatternChange() {
            return patternChange;
        }

        public List<RegExMatch> getMatches() {
            return matches;
        }
    }

}
