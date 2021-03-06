/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.newsletter.action;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.modules.newsletter.service.NewsletterService;
import org.jahia.params.valves.TokenAuthValveImpl;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.rules.BackgroundAction;
import org.jahia.services.notification.HttpClientService;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An action and a background task that sends the content of the specified node as a newsletter
 * to its subscribers.
 * 
 * @author Thomas Draier
 * @author Sergiy Shyrkov
 */
public class SendAsNewsletterAction extends Action implements BackgroundAction {

    private static final Logger logger = LoggerFactory.getLogger(SendAsNewsletterAction.class);

    @Autowired
    private transient HttpClientService httpClientService;
    @Autowired
    private transient NewsletterService newsletterService;
    private String localServerURL;

    public ActionResult doExecute(final HttpServletRequest req, final RenderContext renderContext,
                                  Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver)
            throws Exception {
        JCRNodeWrapper node = resource.getNode();
        Map<String, String> newsletterVersions = new HashMap<String, String>();

        boolean newsletterSent = newsletterService.sendIssueToSubscribers(node, renderContext, newsletterVersions);

        if(newsletterSent){
            return ActionResult.OK;
        }else {
            return ActionResult.INTERNAL_ERROR;
        }
    }

    public void executeBackgroundAction(JCRNodeWrapper node) {
        // do local post on node.getPath/sendAsNewsletter.do
        try {
            Map<String,String> headers = new HashMap<String,String>();
            headers.put("jahiatoken",TokenAuthValveImpl.addToken(node.getSession().getUser()));
            headers.put("accept", "text/plain");
            String out = httpClientService.executePost(localServerURL + Render.getRenderServletPath() + "/live/"
                            + node.getResolveSite().getDefaultLanguage() + node.getPath()
                            + ".sendAsNewsletter.do", null, headers);
            logger.info(out);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public HttpClientService getHttpClientService() {
        return httpClientService;
    }

    public void setHttpClientService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    public NewsletterService getNewsletterService() {
        return newsletterService;
    }

    public void setNewsletterService(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    public String getLocalServerURL() {
        return localServerURL;
    }

    public void setLocalServerURL(String localServerURL) {
        this.localServerURL = localServerURL;
    }
}
