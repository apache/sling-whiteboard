/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.cli.impl.mail;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.sling.cli.impl.Credentials;
import org.apache.sling.cli.impl.CredentialsService;
import org.apache.sling.cli.impl.people.Member;
import org.apache.sling.cli.impl.people.MembersFinder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = Mailer.class
)
public class Mailer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mailer.class);

    private static final Properties SMTP_PROPERTIES = new Properties() {{
        put("mail.smtp.host", "mail-relay.apache.org");
        put("mail.smtp.port", "465");
        put("mail.smtp.auth", "true");
        put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        put("mail.smtp.socketFactory.fallback", "false");
    }};

    @Reference
    private CredentialsService credentialsService;

    @Reference
    private MembersFinder membersFinder;

    public void send(String to, String subject, String body) {
        Properties properties = new Properties(SMTP_PROPERTIES);
        Session session = Session.getInstance(properties);
        try {
            MimeMessage message = new MimeMessage(session);
            Member sender = membersFinder.getCurrentMember();
            Credentials credentials = credentialsService.getCredentials();
            message.setFrom(new InternetAddress(sender.getEmail(), sender.getEmail(), StandardCharsets.UTF_8.name()));
            message.setSubject(subject);
            message.setText(body, StandardCharsets.UTF_8.name());
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            Transport.send(message, new Address[] {new InternetAddress(to)}, credentials.getUsername(), credentials.getPassword());
        } catch (MessagingException | UnsupportedEncodingException e) {
            LOGGER.error(String.format("Unable to send email with Subject '%s' to '%s'.", subject, to), e);
        }

    }

}
