/**
 * Copyright © 2014 Instituto Superior Técnico
 *
 * This file is part of FenixEdu CMS.
 *
 * FenixEdu CMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu CMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu CMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.cms.domain;

import com.google.api.client.util.Lists;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.AnyoneGroup;
import org.fenixedu.bennu.core.groups.DynamicGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.groups.UserGroup;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.portal.domain.MenuContainer;
import org.fenixedu.bennu.portal.domain.MenuFunctionality;
import org.fenixedu.bennu.portal.domain.MenuItem;
import org.fenixedu.bennu.portal.domain.PortalConfiguration;
import org.fenixedu.cms.CMSConfigurationManager;
import org.fenixedu.cms.domain.component.Component;
import org.fenixedu.cms.domain.component.ListCategoryPosts;
import org.fenixedu.cms.domain.component.ViewPost;
import org.fenixedu.cms.domain.wraps.UserWrap;
import org.fenixedu.cms.domain.wraps.Wrap;
import org.fenixedu.cms.domain.wraps.Wrappable;
import org.fenixedu.cms.exceptions.CmsDomainException;
import org.fenixedu.cms.routing.CMSBackend;
import org.fenixedu.cms.routing.CMSEmbeddedBackend;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.consistencyPredicates.ConsistencyPredicate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fenixedu.commons.i18n.LocalizedString.*;

public class Site extends Site_Base implements Wrappable, Sluggable, Cloneable {
    /**
     * maps the registered template types on the tempate classes
     */
    protected static final HashMap<String, Class<?>> TEMPLATES = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(Site.class);

    /**
     * registers a new site template
     *
     * @param type the type of the template. This must be unique on the
     *             application.
     * @param c    the class to be registered as a template.
     */
    public static void register(String type, Class<?> c) {
        TEMPLATES.put(type, c);
    }

    /**
     * searches for a {@link SiteTemplate} by type.
     *
     * @param type the type of the {@link SiteTemplate} wanted.
     * @return the {@link SiteTemplate} with the given type if it exists or null
     * otherwise.
     */
    public static SiteTemplate templateFor(String type) {
        try {
            return (SiteTemplate) TEMPLATES.get(type).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Error while instancing a site template", e);
            return null;
        }
    }

    /**
     * @return mapping between the type and description for all the registered {@link SiteTemplate}.
     */
    public static HashMap<String, String> getTemplates() {
        HashMap<String, String> map = new HashMap<>();

        for (Class<?> c : TEMPLATES.values()) {
            RegisterSiteTemplate registerSiteTemplate = c.getAnnotation(RegisterSiteTemplate.class);
            map.put(registerSiteTemplate.type(), registerSiteTemplate.name() + " - " + registerSiteTemplate.description());
        }

        return map;
    }

    /**
     * the logged {@link User} creates a new {@link Site}.
     */
    public Site(LocalizedString name, LocalizedString description) {
        super();
        if (Authenticate.getUser() == null) {
            throw CmsDomainException.forbiden();
        }
        this.setCreatedBy(Authenticate.getUser());
        this.setCreationDate(new DateTime());

        this.setCanViewGroup(AnyoneGroup.get());
        this.setCanPostGroup(UserGroup.of(Authenticate.getUser()));
        this.setCanAdminGroup(DynamicGroup.get("managers"));
        this.setBennu(Bennu.getInstance());

        new PersistentSiteViewersGroup(this);

        this.setName(name);
        this.setSlug(StringNormalizer.slugify(name.getContent()));
        this.setDescription(description);
        setPublished(false);
    }

    /**
     * returns the group of people who can view this site.
     *
     * @return group the access group for this site
     */
    public Group getCanViewGroup() {
        return getViewGroup().toGroup();
    }

    /**
     * sets the access group for this site
     *
     * @param group the group of people who can view this site
     */
    @Atomic
    public void setCanViewGroup(Group group) {
        setViewGroup(group.toPersistentGroup());
    }

    /**
     * returns the group of people who can post this site.
     *
     * @return the access group for this site
     */
    public Group getCanPostGroup() {
        return getPostGroup().toGroup();
    }

    /**
     * sets the access group of people who can post in this site
     *
     * @param group the group of people who can view this site
     */
    @Atomic
    public void setCanPostGroup(Group group) {
        setPostGroup(group.toPersistentGroup());
    }

    /**
     * returns the group of people who can post this site.
     *
     * @return the access group for this site
     */
    public Group getCanAdminGroup() {
        return getAdminGroup().toGroup();
    }

    /**
     * sets the access group of people who can post in this site
     *
     * @param group the group of people who can view this site
     */
    @Atomic
    public void setCanAdminGroup(Group group) {
        setAdminGroup(group.toPersistentGroup());
    }

    /**
     * searches for a {@link Site} by slug.
     *
     * @param slug the slug of the {@link Site} wanted.
     * @return the {@link Site} with the given slug if it exists, or null
     * otherwise.
     */
    public static Site fromSlug(String slug) {
        if (slug == null) {
            return null;
        }
        Site match = siteCache.computeIfAbsent(slug, Site::manualFind);
        if (match == null) {
            return null;
        }
        if (!FenixFramework.isDomainObjectValid(match) || !match.getSlug().equals(slug)) {
            siteCache.remove(slug, match);
            return fromSlug(slug);
        }
        return match;
    }

    private static final Map<String, Site> siteCache = new ConcurrentHashMap<>();

    private static Site manualFind(String slug) {
        return Bennu.getInstance().getSitesSet().stream().filter(site -> site.getSlug() != null && site.getSlug().equals(slug))
                .findAny().orElse(null);
    }

    /**
     * searches for a {@link Page} by slug on this {@link Site}.
     *
     * @param slug the slug of the {@link Page} wanted.
     * @return the {@link Page} with the given slug if it exists on this site,
     * or null otherwise.
     */
    public Page pageForSlug(String slug) {
        return getPagesSet().stream().filter(page -> slug.equals(page.getSlug())).findAny().orElse(null);
    }

    /**
     * searches for a {@link Post} by slug on this {@link Site}.
     *
     * @param slug the slug of the {@link Post} wanted.
     * @return the {@link Post} with the given slug if it exists on this site,
     * or null otherwise.
     */
    public Post postForSlug(String slug) {
        return getPostSet().stream().filter(post -> slug.equals(post.getSlug())).findAny().orElse(null);
    }

    /**
     * searches for a {@link Category} by slug on this {@link Site}.
     *
     * @param slug the slug of the {@link Category} wanted.
     * @return the {@link Category} with the given slug if it exists on this
     * site, or null otherwise.
     */
    public Category categoryForSlug(String slug) {
        return getCategoriesSet().stream().filter(category -> slug.equals(category.getSlug())).findAny().orElse(null);
    }

    @Atomic
    /**
     * searches for a {@link Category} by slug on this {@link Site} or if one does not exist, creates one.
     *
     * @param slug the slug of the {@link Category} wanted.
     * @param name {@link Category} name.
     * @return the {@link Category} with the given slug if it exists on this site, or null otherwise.
     */
    public Category getOrCreateCategoryForSlug(String slug, LocalizedString name) {
        Category c = categoryForSlug(slug);
        if (c == null) {
            c = new Category(this);
            c.setName(name);
            c.setSlug(slug);
        }
        return c;
    }

    @Override
    public void setSlug(String slug) {
        super.setSlug(SlugUtils.makeSlug(this, slug));
    }

    /**
     * saves the name of the site and creates a new slug for the site.
     */
    @Override
    public void setName(LocalizedString name) {
        LocalizedString prevName = getName();
        super.setName(name);
        if (prevName == null) {
            String slug = StringNormalizer.slugify(name.getContent());
            setSlug(slug);
        }
    }

    /**
     * searches for a {@link Menu} by oid on this {@link Site}.
     *
     * @param oid the slug of the {@link Menu} wanted.
     * @return the {@link Menu} with the given oid if it exists on this site, or
     * null otherwise.
     */
    public Menu menuForOid(String oid) {
        Menu menu = FenixFramework.getDomainObject(oid);
        if (menu == null || menu.getSite() != this) {
            return null;
        } else {
            return menu;
        }
    }

    /**
     * searches for a {@link Menu} by its slug on this {@link Site}.
     *
     * @param slug the slug of the {@link Menu} wanted.
     * @return the {@link Menu} with the given oid if it exists on this site, or
     * null otherwise.
     */
    public Menu menuForSlug(String slug) {
        return getMenusSet().stream().filter(x -> slug.equals(x.getSlug())).findAny().orElse(null);
    }

    @Atomic
    private void deleteMenuFunctionality() {
        MenuFunctionality mf = this.getFunctionality();
        this.setFunctionality(null);
        mf.delete();
    }

    /**
     * Updates the site's slug and it's respective MenuFunctionality. It should
     * be used after setting the site's description, name and slug.
     */

    public void updateMenuFunctionality() {
        Preconditions.checkNotNull(getDescription());
        Preconditions.checkNotNull(getName());
        Preconditions.checkNotNull(getSlug());
        Preconditions.checkArgument(isValidSlug(getSlug()));

        if (getFolder() == null) {
            MenuContainer parent =
                    getFunctionality() == null ? PortalConfiguration.getInstance().getMenu() : getFunctionality().getParent();
            if (getFunctionality() != null) {
                deleteMenuFunctionality();
            }
            this.setFunctionality(new MenuFunctionality(parent, getEmbedded(), getSlug(),
                    getEmbedded() ? CMSEmbeddedBackend.BACKEND_KEY : CMSBackend.BACKEND_KEY, "anyone", this.getDescription(),
                    this.getName(), getSlug()));
            getFunctionality().setAccessGroup(SiteViewersGroup.get(this));
        }
    }

    @Override
    public Site clone(CloneCache cloneCache) {
        return cloneCache.getOrClone(this, obj -> {
            Set<Page> pages = new HashSet<>(getPagesSet());
            Set<Menu> menus = new HashSet<>(getMenusSet());
            Set<Category> categories = new HashSet<>(getCategoriesSet());
            HashSet<Post> posts = new HashSet<>(getPostSet());
            LocalizedString name = getName() != null ? fromJson(getName().json()) : null;
            LocalizedString description = getDescription() != null ? fromJson(getDescription().json()) : null;
            Site clone = new Site(name, description);
            cloneCache.setClone(Site.this, clone);
            clone.setCanViewGroup(getCanViewGroup());
            clone.setCanAdminGroup(getCanAdminGroup());
            clone.setCanPostGroup(getCanPostGroup());
            clone.setThemeType(getThemeType());
            clone.setTheme(getTheme());
            clone.setEmbedded(getEmbedded());
            clone.setFolder(getFolder());
            clone.setAlternativeSite(getAlternativeSite());
            clone.setAnalyticsCode(getAnalyticsCode());
            clone.setGoogleAPIConnection(getGoogleAPIConnection());
            clone.setStyle(getStyle());
            clone.setPrimaryBennu(getPrimaryBennu());
            clone.setBennu(getBennu());
            for (Page originalPage : pages) {
                Page pageClone = originalPage.clone(cloneCache);
                clone.addPages(pageClone);
                pageClone.setSlug(originalPage.getSlug());
            }
            for (Post originalPost : posts) {
                Post postClone = originalPost.clone(cloneCache);
                clone.addPost(postClone);
                postClone.setSlug(originalPost.getSlug());
            }
            for (Menu originalMenu : menus) {
                Menu menuClone = originalMenu.clone(cloneCache);
                clone.addMenus(menuClone);
                menuClone.setSlug(originalMenu.getSlug());
            }
            for (Category originalCategory : categories) {
                Category categoryClone = originalCategory.clone(cloneCache);
                clone.addCategories(categoryClone);
                categoryClone.setSlug(originalCategory.getSlug());
            }
            clone.setInitialPage(getInitialPage() != null ? getInitialPage().clone(cloneCache) : null);
            clone.updateMenuFunctionality();
            return clone;
        });
    }

    @Atomic
    public void delete() {
        MenuFunctionality mf = this.getFunctionality();
        this.setFunctionality(null);
        this.setFolder(null);
        this.setInitialPage(null);

        if (mf != null) {
            mf.delete();
        }

        getViewerGroup().delete();
        this.setViewGroup(null);
        this.setPostGroup(null);
        this.setAdminGroup(null);
        this.setTheme(null);
        this.setCreatedBy(null);
        this.setBennu(null);

        if (getGoogleAPIConnection() != null) {
            getGoogleAPIConnection().delete();
        }

        getActivityLinesSet().stream().forEach(org.fenixedu.cms.domain.SiteActivity::delete);
        getPostSet().stream().forEach(Post::delete);
        getCategoriesSet().stream().forEach(Category::delete);
        getMenusSet().stream().forEach(org.fenixedu.cms.domain.Menu::delete);
        getPagesSet().stream().forEach(org.fenixedu.cms.domain.Page::delete);

        this.deleteDomainObject();
    }

    /**
     * @return the {@link ViewPost} of this {@link Site} if it is defined, or
     * null otherwise.
     */
    public Page getViewPostPage() {
        for (Page page : getPagesSet()) {
            for (Component component : page.getComponentsSet()) {
                if (component.componentType() == ViewPost.class) {
                    return page;
                }
            }
        }
        return null;
    }

    /**
     * @return true if a site is the default site, meaning if this site should
     * respond to '/' requests
     */
    public boolean isDefault() {
        return Bennu.getInstance().getDefaultSite() == this;
    }

    /**
     * @return the {@link ListCategoryPosts} of this {@link Site} if it is
     * defined, or null otherwise.
     */
    public Page getViewCategoryPage() {
        for (Page page : getPagesSet()) {
            for (Component component : page.getComponentsSet()) {
                if (component.getClass() == ListCategoryPosts.class) {
                    return page;
                }
            }
        }
        return null;
    }

    /**
     * @return the static directory of this {@link Site}.
     */
    public String getStaticDirectory() {
        if (CMSConfigurationManager.isInThemeDevelopmentMode()) {
            return CoreConfiguration.getConfiguration().applicationUrl() + "/" + getBaseUrl() + "/static";
        } else {
            return getTheme().getAssetsPath();
        }
    }

    /**
     * @return the object associated with this {@link Site}.
     */
    public DomainObject getObject() {
        return null;
    }

    @Override
    public boolean isValidSlug(String slug) {
        Stream<MenuItem> menuItems = Bennu.getInstance().getConfiguration().getMenu().getOrderedChild().stream();
        return !Strings.isNullOrEmpty(slug)
                && (slug.equals(getSlug()) || menuItems.map(MenuItem::getPath).noneMatch(path -> path.equals(slug)));
    }

    public String getBaseUrl() {
        if (getFolder() != null) {
            return getFolder().getBaseUrl(this);
        } else {
            return getSlug();
        }
    }

    public List<Post> getLatestPosts() {
        return getPostSet().stream().sorted(Post.CREATION_DATE_COMPARATOR).limit(5).collect(Collectors.toList());
    }

    public String getFullUrl() {
        return CoreConfiguration.getConfiguration().applicationUrl() + "/" + getBaseUrl();
    }

    public String getRssUrl() {
        return getFullUrl() + "/rss";
    }

    public String getEditUrl() {
        return CoreConfiguration.getConfiguration().applicationUrl() + "/cms/sites/" + getSlug();
    }

    @Override
    public void setFolder(CMSFolder folder) {
        super.setFolder(folder);
        if (folder != null && getFunctionality() != null) {
            deleteMenuFunctionality();
        }
    }

    @ConsistencyPredicate
    public boolean checkHasEitherFunctionalityOrFolder() {
        return getFunctionality() != null || getFolder() != null;
    }

    public class SiteWrap extends Wrap {

        public boolean isAdmin() {
            return Site.this.getCanAdminGroup().isMember(Authenticate.getUser());
        }

        public boolean canPost() {
            return Site.this.getCanAdminGroup().isMember(Authenticate.getUser()) || isAdmin();
        }

        public LocalizedString getName() {
            return Site.this.getName();
        }

        public LocalizedString getDescription() {
            return Site.this.getDescription();
        }

        public UserWrap getCreatedBy() {
            return new UserWrap(Site.this.getCreatedBy());
        }

        public DateTime getCreationDate() {
            return Site.this.getCreationDate();
        }

        public String getRssUrl() {
            return Site.this.getRssUrl();
        }

        public String getAnalyticsCode() {
            return Site.this.getAnalyticsCode();
        }

        // TODO: Most likely this should be Wrappable
        public Object getSiteObject() {
            return Site.this.getObject();
        }

        public String getAddress() {
            return Site.this.getFullUrl();
        }

        public String getEditAddress() {
            return Site.this.getEditUrl();
        }
    }

    @Override
    public Wrap makeWrap() {
        return new SiteWrap();
    }

    public List<Page> getSortedPages() {
        return getPagesSet().stream().sorted(Comparator.comparing(page -> page.getName().getContent()))
                .collect(Collectors.toList());
    }

    @Override
    public void setTheme(CMSTheme theme) {
        super.setTheme(theme);
        if (theme != null) {
            setThemeType(theme.getType());
        } else {
            setThemeType(null);
        }
    }

    @Override
    public CMSTheme getTheme() {
        String themeType = getThemeType();
        CMSTheme theme = super.getTheme();
        if (themeType != null) {
            if (theme != null && theme.getType().equals(themeType)) {
                return theme;
            }

            CMSTheme otherTheme = CMSTheme.forType(themeType);

            if (otherTheme == null) {
                return CMSTheme.getDefaultTheme();
            } else {
                return otherTheme;
            }
        } else {
            return null;
        }
    }

    public void pushActivity(SiteActivity siteActivity) {
        siteActivity.setNext(null);
        siteActivity.setPrevious(getLastActivityLine());
        setLastActivityLine(siteActivity);
    }

    public static class SiteActivities {
        private LocalDate date;
        private List<SiteActivity> items;

        public LocalDate getDate() {
            return date;
        }

        private void setDate(LocalDate date) {
            this.date = date;
        }

        public List<SiteActivity> getItems() {
            return items;
        }

        private void setItems(List<SiteActivity> items) {
            this.items = items;
        }
    }

    public List<SiteActivities> getLastFiveDaysOfActivity() {
        List<SiteActivities> result = Lists.newArrayList();

        SiteActivity pivot = getLastActivityLine();
        LocalDate current = null;
        List<SiteActivity> day = null;

        while (pivot != null) {
            LocalDate date = pivot.getEventDate().toLocalDate();

            if (!date.equals(current)) {
                if (result.size() != 5) {
                    day = Lists.newArrayList();
                    SiteActivities sas = new SiteActivities();
                    sas.setItems(day);
                    sas.setDate(date);
                    result.add(0, sas);
                    current = date;
                } else {
                    break;
                }
            }

            day.add(0, pivot);

            pivot = pivot.getPrevious();
        }

        return result;
    }
}
