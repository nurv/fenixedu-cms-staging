/**
 * Copyright © 2014 Instituto Superior Técnico
 * <p/>
 * This file is part of FenixEdu CMS.
 * <p/>
 * FenixEdu CMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * FenixEdu CMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu CMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.cms.ui;

import javax.servlet.http.HttpServletRequest;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.BennuSpringController;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.cms.domain.SiteActivity;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.LocalizedString;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import com.google.common.base.Strings;

@BennuSpringController(AdminSites.class)
@RequestMapping("/cms/sites/new")
public class CreateSite {

    @RequestMapping(method = RequestMethod.GET)
    public String create(Model model) {
        model.addAttribute("templates", Site.getTemplates());
        model.addAttribute("folders", Bennu.getInstance().getCmsFolderSet());
        return "fenixedu-cms/create";
    }

    @RequestMapping(method = RequestMethod.POST)
    public RedirectView create(Model model, @RequestParam LocalizedString name, @RequestParam LocalizedString description,
            @RequestParam String template, HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "false") Boolean published, @RequestParam String folder,
            @RequestParam(required = false) boolean embedded, RedirectAttributes redirectAttributes) {
        if (name.isEmpty()) {
            redirectAttributes.addFlashAttribute("emptyName", true);
            return new RedirectView("/cms/sites/new", true);
        } else {
            if (published == null) {
                published = false;
            }
            createSite(name, description, published, template, folder, embedded);
            return new RedirectView("/cms/sites/", true);
        }
    }

    @Atomic
    private void createSite(LocalizedString name, LocalizedString description, boolean published, String template, String folder,
            boolean embedded) {
        Site site = new Site(name,description);
        
        if (!Strings.isNullOrEmpty(folder)) {
            site.setFolder(FenixFramework.getDomainObject(folder));
        }
        site.setEmbedded(embedded);
        site.updateMenuFunctionality();
        site.setPublished(published);
        
        if (!template.equals("null")) {
            Site.templateFor(template).makeIt(site);
        }
        
        SiteActivity.createdSite(site, Authenticate.getUser());
    }

}
