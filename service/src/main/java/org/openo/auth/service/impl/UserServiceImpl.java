/*
 * Copyright 2016 Huawei Technologies Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openo.auth.service.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.openo.auth.common.CommonUtil;
import org.openo.auth.common.IJsonService;
import org.openo.auth.common.JsonFactory;
import org.openo.auth.common.keystone.KeyStoneConfigInitializer;
import org.openo.auth.constant.Constant;
import org.openo.auth.constant.ErrorCode;
import org.openo.auth.entity.ClientResponse;
import org.openo.auth.entity.ModifyPassword;
import org.openo.auth.entity.ModifyUser;
import org.openo.auth.entity.UserDetailsUI;
import org.openo.auth.entity.keystone.req.KeyStoneConfiguration;
import org.openo.auth.entity.keystone.resp.UserCreateWrapper;
import org.openo.auth.exception.AuthException;
import org.openo.auth.rest.client.UserServiceClient;
import org.openo.auth.service.inf.IUserDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Implementation class for user service delegate.
 * <br/>
 * 
 * @author
 * @version  
 */
public class UserServiceImpl implements IUserDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    /**
     * Perform Create user Operation.
     * <br/>
     * 
     * @param request : HttpServletRequest Object
     * @param response : HttpServletResponse Object
     * @return response for the create user operation.
     * @since  
     */
    public Response createUser(HttpServletRequest request, HttpServletResponse response) {

        Response res = null;

        try {

            String authToken = request.getHeader(Constant.TOKEN_AUTH);

            LOGGER.info("authToken = " + authToken);

            UserDetailsUI userInfo = CommonUtil.getInstance().getUserInfo(request, response);

            LOGGER.info("userInfo UserName= " + userInfo.getUserName());

            CheckUserInfoRule.checkInfo(userInfo);

            KeyStoneConfiguration keyConf = KeyStoneConfigInitializer.getKeystoneConfiguration();

            String json = getJsonService().createUserJson(userInfo, keyConf);

            LOGGER.info("json = " + json);

            ClientResponse resp = UserServiceClient.getInstance().createUser(json, authToken);

            int status = resp.getStatus();

            response.setStatus(status);

            String respBody = resp.getBody();

            /* assign the role to the user */
            if(status / 200 == 1) {
                respBody = getJsonService().responseForCreateUser(resp.getBody());
            }
            res = Response.status(status).entity(respBody).build();

        } catch(Exception e) {
            LOGGER.error("Exception Caught while connecting client ... " + e);
            throw new AuthException(HttpServletResponse.SC_REQUEST_TIMEOUT, ErrorCode.FAILURE_INFORMATION);
        }
        return res;
    }

    /**
     * Get the Json Service instance according the service registered in the
     * <tt>auth_service.properties</tt> file.
     * <br/>
     * 
     * @return jsonService : An instance of <tt>JsonService</tt> class.
     * @since  
     */
    private IJsonService getJsonService() {

        IJsonService jsonService = JsonFactory.getInstance().getJsonService();

        if(null == jsonService) {
            LOGGER.error("Exception Caught while connecting client ... ");
            throw new AuthException(HttpServletResponse.SC_REQUEST_TIMEOUT, ErrorCode.AUTH_LOAD_FAILED);
        }
        return jsonService;
    }

    /**
     * Perform Modify user Operation.
     * <br/>
     * 
     * @param request : HttpServletRequest Object
     * @param response : HttpServletResponse Object
     * @param userId : user id for which user need to be modified.
     * @return response for the modify user operation.
     * @since  
     */
    public Response modifyUser(final HttpServletRequest request, HttpServletResponse response, String userId) {

        String authToken = request.getHeader(Constant.TOKEN_AUTH);

        LOGGER.info("authToken = " + authToken);

        ModifyUser modifyUser = CommonUtil.getInstance().modifyUserJson(request, response);
        String json = getJsonService().modifyUserJson(modifyUser);

        ClientResponse resp = UserServiceClient.getInstance().modifyUser(userId, json, authToken);

        int status = resp.getStatus();

        response.setStatus(status);

        String respBody = resp.getBody();

        if(status / 200 == 1) {
            respBody = getJsonService().responseForModifyUser(resp.getBody());
        }

        Response res = null;
        try {
            res = Response.status(status).entity(respBody).build();
        } catch(Exception e) {
            LOGGER.error("Exception Caught " + e);
            throw new AuthException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.COMMUNICATION_ERROR);
        }

        return res;

    }

    /**
     * Perform the Delete User operation for Auth Service.
     * <br/>
     * 
     * @param request : HttpServletRequest Object
     * @param response : HttpServletRequest Object
     * @param userId : user id which needs to be deleted.
     * @return Returns the status for the following operation.
     * @since  
     */
    public int deleteUser(HttpServletRequest request, HttpServletResponse response, String userId) {

        String authToken = request.getHeader(Constant.TOKEN_AUTH);

        LOGGER.info("authToken" + authToken);

        int status = UserServiceClient.getInstance().deleteUser(userId, authToken);

        response.setStatus(status);

        return status;

    }

    /**
     * Fetch details for the specific user.
     * <br/>
     * 
     * @param request : HttpServletRequest Object
     * @param response : HttpServletRequest Object
     * @param userId : user id for which details needs to be fetched.
     * @return response for the get user details operation
     * @since  
     */
    public Response getUserDetails(HttpServletRequest request, HttpServletResponse response, String userId) {

        String authToken = request.getHeader(Constant.TOKEN_AUTH);

        LOGGER.info("authToken = " + authToken);

        ClientResponse resp = UserServiceClient.getInstance().getUserDetails(userId, authToken);

        int status = resp.getStatus();

        response.setStatus(status);

        String respBody = resp.getBody();

        if(status / 200 == 1) {
            respBody = getJsonService().responseForCreateUser(resp.getBody());
        }

        Response res = null;
        try {
            res = Response.status(status).entity(respBody).build();
        } catch(Exception e) {
            LOGGER.error("Exception Caught " + e);
            throw new AuthException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.COMMUNICATION_ERROR);
        }

        return res;

    }

    /**
     * Fetches the user details of all user.
     * <br/>
     * 
     * @param request : HttpServletRequest Object
     * @param response : HttpServletRequest Object
     * @return response for the get user details operation
     * @since  
     */
    public Response getUserDetails(HttpServletRequest request, HttpServletResponse response) {

        String authToken = request.getHeader(Constant.TOKEN_AUTH);

        LOGGER.info("authToken = " + authToken);

        ClientResponse resp = UserServiceClient.getInstance().getUserDetails(authToken);

        int status = resp.getStatus();

        response.setStatus(status);

        String respBody = resp.getBody();

        if(status / 200 == 1) {
            respBody = getJsonService().responseForMultipleUsers(resp.getBody());
        }

        Response res = null;
        try {
            res = Response.status(status).entity(respBody).build();
        } catch(Exception e) {
            LOGGER.error("Exception Caught " + e);
            throw new AuthException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.COMMUNICATION_ERROR);
        }

        return res;

    }

    /**
     * Modify the password for the user
     * <br/>
     * 
     * @param request : HttpServletRequest Object
     * @param response : HttpServletRequest Object
     * @param userId : user id for which the password needs to be changed
     * @return Returns the status for the following operation.
     * @throws IOException
     * @since  
     */
    public int modifyPasword(HttpServletRequest request, HttpServletResponse response, String userId)
            throws IOException {

        String authToken = request.getHeader(Constant.TOKEN_AUTH);

        UserCreateWrapper user = null;

        LOGGER.info("authToken = " + authToken);

        ModifyPassword modifyPwd = CommonUtil.getInstance().modifyPasswordJson(request, response);

        ClientResponse resp = UserServiceClient.getInstance().getUserDetails(userId, authToken);

        if(resp.getStatus() / 200 == 1) {
            user = getJsonService().keyStoneRespToCreateUserObj(resp.getBody());
        }

        if(user != null && user.getUser() != null && StringUtils.isNotEmpty(user.getUser().getName())) {
            CheckUserInfoRule.checkPassword(modifyPwd.getPassword(), user.getUser().getName());
        }

        String json = getJsonService().modifyPasswordJson(modifyPwd);

        int status = UserServiceClient.getInstance().modifyPassword(userId, json, authToken);

        response.setStatus(status);

        return status;

    }

    /**
     * Assigning Default Role and Default Project to the users created.
     * <br/>
     * 
     * @param authToken : Auth Token, representing the current session.
     * @param keyConf : Default KeyStone configuration
     * @param userId : userId for which roles need to be assigned.
     * @return Return the status for the operation.
     * @since  
     */
    private int assignRolesToUser(String authToken, KeyStoneConfiguration keyConf, String userId) {

        String projectId = keyConf.getProjectId();

        String roleId = keyConf.getRoleId();

        return UserServiceClient.getInstance().assignRolesToUser(authToken, projectId, userId, roleId);
    }

}
