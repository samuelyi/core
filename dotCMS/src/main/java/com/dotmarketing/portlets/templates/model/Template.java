package com.dotmarketing.portlets.templates.model;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.links.model.Link;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;

/** @author Hibernate CodeGenerator */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Template extends WebAsset implements Serializable, Comparable, ManifestItem {

	public static final String SYSTEM_TEMPLATE = "SYSTEM_TEMPLATE";

	private static final long serialVersionUID = 1L;

	public static String ANONYMOUS_PREFIX = "anonymous_layout_";

	/** nullable persistent field */
	private String body;

    /** nullable persistent field */
	private String selectedimage;
    /** nullable persistent field */
	private String image;

	//	*********************** BEGIN GRAZIANO issue-12-dnd-template
	private Boolean drawed=false;

	private String drawedBody;

	private Integer countAddContainer;

	private Integer countContainers;

	private String headCode;
	//	*********************** END GRAZIANO issue-12-dnd-template

	private String theme;

	private String themeName;

	public static final String THEME_HTML_HEAD = "html_head.vtl";
	public static final String THEME_HEADER = "header.vtl";
	public static final String THEME_FOOTER = "footer.vtl";
	public static final String THEME_TEMPLATE = "template.vtl";
	public static final List<String> THEME_FILES = new ArrayList<String>();
	public static final String THEMES_PATH = "/application/themes/";

	static {
		THEME_FILES.add(THEME_HTML_HEAD);
		THEME_FILES.add(THEME_HEADER);
		THEME_FILES.add(THEME_FOOTER);
	}

	/** default constructor */
	public Template() {
		this.image = "";
		super.setType("template");
	}

	/**
	 * It is a Template save with a auto generated title.
	 *
	 * @return
	 */
	public boolean isAnonymous () {
		return getTitle().startsWith(ANONYMOUS_PREFIX);
	}

	/**
	 * Returns true if this is a Template or false if its a Layout
	 * @return
	 */
	public boolean isTemplate() {
		return this.isShowOnMenu();
	}

	public void setIsTemplate(final boolean isTemplate) {
		this.setShowOnMenu(isTemplate);
	}

    public String getURI(Folder folder) {
    	String folderPath = "";
		try {
			folderPath = APILocator.getIdentifierAPI().find(folder.getIdentifier()).getPath();
		} catch (Exception e) {
			Logger.error(this,e.getMessage());
			throw new DotRuntimeException(e.getMessage(),e);
		}
    	return folderPath + this.getInode();
    }

	/**
	 * @return Returns the image.
	 */
	public String getImage() {
		return image;
	}
	/**
	 * @param image The image to set.
	 */
	public void setImage(String image) {
		this.image = image;
	}
	/**
	 * @return Returns the selectedimage.
	 */
	public String getSelectedimage() {
		return selectedimage;
	}
	/**
	 * @param selectedimage The selectedimage to set.
	 */
	public void setSelectedimage(String selectedimage) {
		this.selectedimage = selectedimage;
	}
	/** nullable persistent field */
	private String header;

	/** nullable persistent field */
	private String footer;

	public String getInode() {
		if(InodeUtils.isSet(this.inode))
			return this.inode;

		return "";
	}

	/**
	 * Sets the inode.
	 * @param inode The inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}

	/**
	 * Returns the body.
	 * @return String
	 */
	public String getBody() {
		return body;
	}

	/**
	 * Sets the body.
	 * @param body The body to set
	 */
	public void setBody(String body) {
		this.body = body;
	}


	/**
	 * Returns the footer.
	 * @return String
	 */
	public String getFooter() {
		return footer;
	}

	/**
	 * Returns the header.
	 * @return String
	 */
	public String getHeader() {
		return header;
	}

	/**
	 * Sets the footer.
	 * @param footer The footer to set
	 */
	public void setFooter(String footer) {
		this.footer = footer;
	}

	/**
	 * Sets the header.
	 * @param header The header to set
	 */
	public void setHeader(String header) {
		this.header = header;
	}

	/**
	 * Identify the drawed template
	 * @return
	 */
	public Boolean isDrawed() {
		return (this.drawed==null) ? UtilMethods.isSet(drawedBody) : this.drawed;
	}

	/**
	 * Sets the boolean for drawed template
	 * @param drawed
	 */
	public void setDrawed(Boolean drawed) {
	    this.drawed = (null!=drawed) ? drawed : false;
	}

	public String getDrawedBody() {
		return drawedBody;
	}

	public void setDrawedBody(String drawedBody) {
		this.drawedBody = drawedBody;
	}

	public void setDrawedBody(TemplateLayout templateLayout) {
		try {
			this.drawedBody = JsonTransformer.mapper.writeValueAsString(templateLayout);
		} catch (JsonProcessingException e) {
			throw new DotRuntimeException(e);
		}
	}

	public Integer getCountAddContainer() {
		return countAddContainer != null ? countAddContainer : 0;
	}

	public void setCountAddContainer(Integer countAddContainer) {
		this.countAddContainer = countAddContainer;
	}

	public Integer getCountContainers() {
		return countContainers != null ? countContainers : 0;
	}

	public void setCountContainers(Integer countContainers) {
		this.countContainers = countContainers;
	}

	public String getHeadCode() {
		return headCode;
	}

	public void setHeadCode(String headCode) {
		this.headCode = headCode;
	}

	public String getTheme() {
		return theme;
	}

	public void setTheme(String theme) {
		this.theme = theme;
	}

	public String getThemeName() {
		return themeName;
	}

	public void setThemeName(String themeName) {
		this.themeName = themeName;
	}

	public int compareTo(Object compObject){

		if(!(compObject instanceof Template))return -1;

		Template template = (Template) compObject;
		return (template.getTitle().compareTo(this.getTitle()));

	}

    /**
     * @author David H Torres
     */
	@Override
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
		accepted.add(new PermissionSummary("view",
				"view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("edit",
				"edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("publish",
				"publish-permission-description",
				PermissionAPI.PERMISSION_PUBLISH));
		accepted.add(new PermissionSummary("edit-permissions",
				"edit-permissions-permission-description",
				PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}
	@JsonIgnore
	public Permissionable getParentPermissionable() throws DotDataException {

		try {
			User systemUser = APILocator.getUserAPI().getSystemUser();
			HostAPI hostAPI = APILocator.getHostAPI();
			Host host = hostAPI.findParentHost(this, systemUser, false);

			if (host == null) {
				host = hostAPI.findSystemHost(systemUser, false);
			}
			return host;
		} catch (DotSecurityException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	@JsonIgnore
	public Map<String, Object> getMap () throws DotStateException, DotDataException, DotSecurityException {
		final Map<String, Object> map = super.getMap();
		map.put("anonymous", this.isAnonymous());
		return map;
	}

	@Override
	public ManifestInfo getManifestInfo(){
		return new ManifestInfoBuilder()
			.objectType(PusheableAsset.TEMPLATE.getType())
			.id(this.getIdentifier())
			.inode(this.inode)
			.title(this.getTitle())
			.build();
	}
}
