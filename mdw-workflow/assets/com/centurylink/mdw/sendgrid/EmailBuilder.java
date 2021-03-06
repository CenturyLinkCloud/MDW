package com.centurylink.mdw.sendgrid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.util.ExpressionUtil;

/**
 * Builds the sendgrid model for a templated email.
 */
public class EmailBuilder {

    private Asset template;
    private ProcessRuntimeContext context;

    public EmailBuilder(Asset template, ProcessRuntimeContext context) {
        this.template = template;
        this.context = context;
    }

    private String from = "support@example.com";
    public EmailBuilder from(String from) {
        this.from = from;
        return this;
    }

    private String subject = "MDW Notice";
    public EmailBuilder subject(String subject) {
        this.subject = subject;
        return this;
    }

    private String[] to;
    public EmailBuilder to(String...to) {
        this.to = to;
        return this;
    }

    private String[] cc;
    public EmailBuilder cc(String...cc) {
        this.cc = cc;
        return this;
    }

    public Email create() throws IOException {
        // build the sendgrid model object
        Email email = new Email();
        email.setFrom(new Address(from));
        email.setPersonalizations(new ArrayList<>());
        Personalization personalization = new Personalization();
        email.getPersonalizations().add(personalization);
        personalization.setSubject(subject);
        if (this.to != null) {
            List<Address> to = new ArrayList<>();
            personalization.setTo(to);
            for (String recipient : this.to) {
                to.add(new Address(recipient));
            }
        }
        if (this.cc != null && this.cc.length > 0) {
            List<Address> cc = new ArrayList<>();
            personalization.setCc(cc);
            for (String ccRecipient : this.cc) {
                cc.add(new Address(ccRecipient));
            }
        }
        List<Message> content = new ArrayList<>();
        email.setContent(content);
        Message message = new Message();
        content.add(message);
        message.setType(template.getContentType());
        String body = template.getText();
        Map<String,String> images = new HashMap<>();
        body = ExpressionUtil.substituteImages(body, images);
        if (!images.isEmpty()) {
            for (String cid : images.keySet()) {
                String imageAsset = images.get(cid);
                Asset asset = AssetCache.getAsset(imageAsset);
                if (asset != null) {
                    Attachment attachment = new Attachment();
                    if (email.getAttachments() == null) {
                        email.setAttachments(new ArrayList<>());
                    }
                    email.getAttachments().add(attachment);
                    attachment.setContent_id(cid);
                    attachment.setFilename(asset.getName());
                    attachment.setType(asset.getContentType());
                    attachment.setContent(Base64.getEncoder().encodeToString(asset.getContent()));
                }
                else {
                    context.logWarn("Image asset not found: " + imageAsset);
                }
            }
        }
        message.setValue(context.evaluateToString(body));
        return email;
    }
}
