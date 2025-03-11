package com.template.OAuth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class MessageService {

    private final MessageSource messageSource;

    @Autowired
    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Get a message in the user's current locale
     *
     * @param code Message code from properties file
     * @return Localized message
     */
    public String getMessage(String code) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, null, code, locale);
    }

    /**
     * Get a message with parameters in the user's current locale
     *
     * @param code Message code from properties file
     * @param args Parameters to substitute in the message
     * @return Localized message with parameters
     */
    public String getMessageWithArgs(String code, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, code, locale);
    }

    /**
     * Get a message in a specific locale
     *
     * @param code   Message code from properties file
     * @param locale Specific locale to use
     * @return Localized message
     */
    public String getMessageForLocale(String code, Locale locale) {
        return messageSource.getMessage(code, null, code, locale);
    }
}