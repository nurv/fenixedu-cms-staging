package org.fenixedu.bennu.cms.portal;

import org.fenixedu.bennu.cms.domain.Site;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.LocalizedString;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import pt.ist.fenixframework.Atomic;

@SpringFunctionality(app = AdminSites.class, title = "application.create-site.title")
@RequestMapping("/cms/sites/new")
public class CreateSite {

    @RequestMapping(method = RequestMethod.GET)
    public String create(Model model) {
        model.addAttribute("templates", Site.getTemplates());
        return "create";
    }

    @RequestMapping(method = RequestMethod.POST)
    public RedirectView create(Model model, @RequestParam LocalizedString name, @RequestParam LocalizedString description,
            @RequestParam String template, @RequestParam(required = false, value = "false") Boolean published,
            RedirectAttributes redirectAttributes) {
        if (name.isEmpty()) {
            redirectAttributes.addFlashAttribute("emptyName", true);
            return new RedirectView("/cms/sites/new", true);
        } else {
            if (published == null) {
                published = false;
            }
            createSite(name, description, published, template);
            return new RedirectView("/cms/sites/", true);
        }
    }

    @Atomic
    private void createSite(LocalizedString name, LocalizedString description, boolean published, String template) {
        Site site = new Site();
        site.setBennu(Bennu.getInstance());
        site.setDescription(description);
        site.setName(name);
        site.setSlug(StringNormalizer.slugify(name.getContent()));
        site.setPublished(published);

        if (!template.equals("null")) {
            Site.templateFor(template).makeIt(site);
        }
    }

}
