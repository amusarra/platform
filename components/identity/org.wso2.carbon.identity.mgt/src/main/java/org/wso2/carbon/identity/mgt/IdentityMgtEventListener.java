/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.wso2.carbon.identity.mgt;

import org.apache.axiom.om.util.Base64;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.core.identity.IdentityCacheEntry;
import org.wso2.carbon.caching.core.identity.IdentityCacheKey;
import org.wso2.carbon.identity.mgt.beans.UserMgtBean;
import org.wso2.carbon.identity.mgt.cache.LoginAttemptCache;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.identity.mgt.util.Utils;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserOperationEventListener;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.jdbc.JDBCRealmConstants;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is an implementation of UserOperationEventListener.  This defines additional operations
 * for some of core user management operations
 * 
 */
public class IdentityMgtEventListener extends AbstractUserOperationEventListener {

    private LoginAttemptCache cache = LoginAttemptCache.getCacheInstance();

    private static final Log log = LogFactory.getLog(IdentityMgtEventListener.class);
    
    private int maxLoginAttempts;

    private String defaultPassword;

    private RealmConfiguration realmConfig;

    private AccountRecoveryProcessor processor;

    public IdentityMgtEventListener() {

        try {
            realmConfig = IdentityMgtServiceComponent.getRealmService().
                                                                getBootstrapRealmConfiguration();
            String  maxLoginAttemptProperty = realmConfig.
                                    getUserStoreProperty(IdentityMgtConstants.MAX_FAILED_ATTEMPT);
            if(maxLoginAttemptProperty != null){
                maxLoginAttempts = Integer.valueOf(maxLoginAttemptProperty);
            } else {
                maxLoginAttempts = IdentityMgtConstants.DEFAULT_MAX_FAILED_LOGIN_ATTEMPT;
            }

            String defaultPasswordProperty = realmConfig.
                                    getUserStoreProperty(IdentityMgtConstants.DEFAULT_PASSWORD);
            if(defaultPasswordProperty != null){
                defaultPassword = defaultPasswordProperty.trim();
            }

            processor = IdentityMgtServiceComponent.getRecoveryProcessor();
            
        } catch (Exception e) {
            maxLoginAttempts = IdentityMgtConstants.DEFAULT_MAX_FAILED_LOGIN_ATTEMPT;
        }     
    }

    @Override
    public boolean doPreAuthenticate(String userName, Object credential,
                                    UserStoreManager userStoreManager) throws UserStoreException {

        if(log.isDebugEnabled()){
            log.debug("Pre authenticator is called in IdentityMgtEventListener");
        }

        int tenantId = userStoreManager.getTenantId();

        if(maxLoginAttempts == 0){
            if(log.isDebugEnabled()){
                log.debug("Max failed login attempts are not defined.");
            }
            return true;
        }

        int failedAttempts =  cache.getValueFromCache(userName, tenantId);

        if(failedAttempts < 0){
            log.warn("User account is locked");
            return false;
        }

        if(failedAttempts == (maxLoginAttempts-1)){
            if(log.isDebugEnabled()){
                log.debug("User has exceed the max failed login attempts. User account would be locked");
            }
            try {
                Utils.persistAccountStatus(userName, tenantId, UserCoreConstants.USER_LOCKED);
            } catch (IdentityMgtException e) {
                log.error("Error while persisting user account status : LOCKED");
            }
            return true;
        }

        return true;
    }

    @Override
    public boolean doPostAuthenticate(String userName, boolean authenticated,
                                      UserStoreManager userStoreManager) throws UserStoreException {

        if(log.isDebugEnabled()){
            log.debug("Post authenticator is called in IdentityMgtEventListener");
        }

        int tenantId = userStoreManager.getTenantId();
        
        if(authenticated){
            cache.clearCacheEntry(userName, tenantId);
        } else {
            cache.addToCache(userName, tenantId);
        }

        return true;
    }


    @Override
    public boolean doPreAddUser(String userName, Object credential, String[] roleList,
                Map<String, String> claims, String profile, UserStoreManager userStoreManager)
                                                                        throws UserStoreException {

        if(log.isDebugEnabled()){
            log.debug("Pre add user is called in IdentityMgtEventListener");
        }

        processUserChallenges(claims,false);

        //lock account and persist
        lockUserAccount(userName, userStoreManager.getTenantId());
        claims.put(UserCoreConstants.ClaimTypeURIs.ACCOUNT_STATUS, UserCoreConstants.USER_LOCKED);
        
        if(credential == null){
            if(log.isDebugEnabled()){
                log.debug("Credentials are not null. Using default user password as credentials");
            }
            ((AbstractUserStoreManager)userStoreManager).doAddUser(userName, defaultPassword,
                                                                        roleList, claims, profile);
            UserMgtBean bean = new UserMgtBean();
            bean.setUserId(userName);
            bean.setRecoveryType(IdentityMgtConstants.RECOVERY_TYPE_TEMPORARY_PASSWORD);
            try {
                processor.processRecoveryUsingEmail(bean, userStoreManager.getTenantId());
            } catch (IdentityMgtException e) {
                log.error("Error while sending temporary password to user's email account");
            }

            return false;
        }
        return true;
    }


    @Override
    public boolean doPostAddUser(String userName, UserStoreManager userStoreManager)
                                                                    throws UserStoreException {
        UserMgtBean bean = new UserMgtBean();
        bean.setUserId(userName);
        bean.setRecoveryType(IdentityMgtConstants.RECOVERY_TYPE_ACCOUNT_CONFORM);
        try {
            processor.processRecoveryUsingEmail(bean, userStoreManager.getTenantId());
        } catch (IdentityMgtException e) {
            log.error("Error while sending confirmation link to user's email account");
        }

        return true;

    }

    @Override
    public boolean doPreSetUserClaimValue(String userName, String claimURI, String claimValue,
                String profileName, UserStoreManager userStoreManager) throws UserStoreException {

        if(UserCoreConstants.ClaimTypeURIs.PRIMARY_CHALLENGES.equals(claimURI)){

            throw new UserStoreException("Primary challenges can not be modified");

        } else if(UserCoreConstants.ClaimTypeURIs.ACCOUNT_STATUS.equals(claimURI)){

            if(isLoggedInUser(userName)){
                throw new UserStoreException("You are not authorized to change account status");
            }

            if(UserCoreConstants.USER_LOCKED.equals(claimValue)){
                lockUserAccount(userName, userStoreManager.getTenantId());
            }
            return true;

        } else {

            List<String> challengesUri = processor.getQuestionProcessor().
                            getChallengeQuestionUris(userName, userStoreManager.getTenantId());
            if(challengesUri.contains(claimURI)){
                if(claimValue != null){
                    claimValue = claimValue.trim();
                    String question = claimValue.substring(0,claimValue.indexOf(","));
                    String answer = claimValue.substring(claimValue.indexOf(",")+ 1);
                    if(question != null && answer != null){
                        question = question.trim();
                        answer = answer.trim();
                        claimValue =question + "," + doHash(answer.toLowerCase());
                        ((AbstractUserStoreManager) userStoreManager).
                                doSetUserClaimValue(userName, claimURI, claimValue, profileName);
                    }
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean doPreSetUserClaimValues(String userName, Map<String, String> claims,
                String profileName, UserStoreManager userStoreManager) throws UserStoreException {


        if(claims.containsKey(UserCoreConstants.ClaimTypeURIs.PRIMARY_CHALLENGES)){

            throw new UserStoreException("Primary challenges can not be modified");

        } else if(claims.containsKey(UserCoreConstants.ClaimTypeURIs.ACCOUNT_STATUS)){

            if(isLoggedInUser(userName)){
                throw new UserStoreException("You are not authorized to change account status");
            }

            if(UserCoreConstants.USER_LOCKED.
                        equals(claims.get(UserCoreConstants.ClaimTypeURIs.ACCOUNT_STATUS))){
                lockUserAccount(userName, userStoreManager.getTenantId());
            }
            return true;
        }
        
        processUserChallenges(claims,false);
        return true;

    }

    private void lockUserAccount(String userName, int tenantId){

        IdentityCacheKey cacheKey = new IdentityCacheKey(tenantId, userName);
        IdentityCacheEntry cacheEntry = new IdentityCacheEntry(-10);
        cache.addToCache(cacheKey, cacheEntry);
    }


    private void unlockUserAccount(String userName, int tenantId){
        
        cache.clearCacheEntry(userName, tenantId);
        try {
            Utils.persistAccountStatus(userName, tenantId, UserCoreConstants.USER_UNLOCKED);
        } catch (IdentityMgtException e) {
            log.error("Error while persisting user account status : LOCKED");
        }
    }

    private boolean isLoggedInUser(String userName){

        MessageContext msgContext = MessageContext.getCurrentMessageContext();
        HttpServletRequest request = (HttpServletRequest) msgContext
                .getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            String loggedInUser = (String) httpSession.getAttribute(ServerConstants.USER_LOGGED_IN);
            if(loggedInUser != null && loggedInUser.equals(userName)){
                return true;
            }
        }

        return false;
    }


    private void processUserChallenges(Map<String, String> claims, boolean primary)
                                                                        throws UserStoreException {

        String challengeUris = claims.get(UserCoreConstants.ClaimTypeURIs.CHALLENGES_URI);
        if(challengeUris != null){
            Map<String, String> challengeMap = new HashMap<String, String>();
            String[] challenges;

            if(challengeUris.contains(",")){
                challenges = challengeUris.split(",");
            } else {
                challenges = new String[]{challengeUris};
            }

            for(String challenge : challenges){
                challenge = challenge.trim();
                String challengeValue = claims.get(challenge);
                if(challengeValue != null){
                    challengeValue = challengeValue.trim();
                    String question = challengeValue.
                        substring(0,challengeValue.indexOf(","));
                    String answer = challengeValue.
                        substring(challengeValue.indexOf(",")+ 1);
                    if(question != null && answer != null){
                        question = question.trim();
                        answer = answer.trim();
                        challengeMap.put(question, answer);
                        claims.put(challenge, question + "," + doHash(answer.toLowerCase()));
                    } else {
                        claims.remove(challenge);
                    }
                }
            }

            if(primary){
                String value = "";
                for(Map.Entry<String, String> entry : challengeMap.entrySet()){
                    if("".equals(value)){
                        value = entry.getKey() + "=" + doHash(entry.getValue().toLowerCase());
                    } else {
                        value = value + "," + entry.getKey() + "=" + doHash(entry.getValue().toLowerCase());
                    }
                }
    
                if(!"".equals(value)){
                    claims.put(UserCoreConstants.ClaimTypeURIs.PRIMARY_CHALLENGES, value);
                }
            }
        }
    }

    private String doHash(String input) throws UserStoreException {
        try {
            MessageDigest dgst;
            String digsestFunction = realmConfig.getUserStoreProperties().get(
                                                                JDBCRealmConstants.DIGEST_FUNCTION);
            if (digsestFunction != null) {
                dgst = MessageDigest.getInstance(digsestFunction);
            } else {
                dgst = MessageDigest.getInstance("SHA-256");
            }
            byte[] byteValue = dgst.digest(input.getBytes());
            input = Base64.encode(byteValue);
            return input;
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
            throw new UserStoreException(e.getMessage(), e);
        }
    }
}
