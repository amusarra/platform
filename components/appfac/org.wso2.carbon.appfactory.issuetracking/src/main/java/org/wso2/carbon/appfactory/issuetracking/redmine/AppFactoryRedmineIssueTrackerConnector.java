/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.appfactory.issuetracking.redmine;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueStatus;
import com.taskadapter.redmineapi.bean.Membership;
import com.taskadapter.redmineapi.bean.Role;
import com.taskadapter.redmineapi.bean.Tracker;
import com.taskadapter.redmineapi.bean.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.application.mgt.service.ApplicationManagementException;
import org.wso2.carbon.appfactory.application.mgt.service.UserInfoBean;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.issuetracking.AbstractRepositoryConnector;
import org.wso2.carbon.appfactory.issuetracking.UserIssues;
import org.wso2.carbon.appfactory.issuetracking.beans.GenericIssue;
import org.wso2.carbon.appfactory.issuetracking.beans.GenericIssueType;
import org.wso2.carbon.appfactory.issuetracking.beans.GenericUser;
import org.wso2.carbon.appfactory.issuetracking.beans.IssueSummary;
import org.wso2.carbon.appfactory.issuetracking.beans.Project;
import org.wso2.carbon.appfactory.issuetracking.beans.ProjectApplicationMapping;
import org.wso2.carbon.appfactory.issuetracking.beans.Version;
import org.wso2.carbon.appfactory.issuetracking.exception.IssueTrackerException;
import org.wso2.carbon.appfactory.issuetracking.internal.ServiceContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class AppFactoryRedmineIssueTrackerConnector extends AbstractRepositoryConnector {
    private static final Log log = LogFactory.getLog(AppFactoryRedmineIssueTrackerConnector.class);
    public static final String REDMINE_ISSUE_TRACKER_CONFIG = "IssueTrackerConnector.redmine.Property.";
    public static final String REDMINE_URL = REDMINE_ISSUE_TRACKER_CONFIG + "Url";
    public static final String REDMINE_ADMIN_USERNAME = REDMINE_ISSUE_TRACKER_CONFIG + "AdminUsername";
    public static final String REDMINE_ADMIN_PASSWORD = REDMINE_ISSUE_TRACKER_CONFIG + "AdminPassword";
    public static final String REDMINE_DEFAULT_ROLE = REDMINE_ISSUE_TRACKER_CONFIG + "DefaultRole";
    public static final String REDMINE_AUTHENTICATOR_ID = REDMINE_ISSUE_TRACKER_CONFIG + "AuthenticatorId";
    public static final String REDMINE_ISSUES = "issues";

    private RedmineManager manager;

    public AppFactoryRedmineIssueTrackerConnector(AppFactoryConfiguration conf) {
        setConfiguration(conf);
        String redmineURL = getConfiguration().getFirstProperty(REDMINE_URL);
        String adminUsername = getConfiguration().getFirstProperty(REDMINE_ADMIN_USERNAME);
        String adminPassword = getConfiguration().getFirstProperty(REDMINE_ADMIN_PASSWORD);
        manager = new RedmineManager(redmineURL, adminUsername, adminPassword);
    }

    @Override
    public String reportIssue(GenericIssue genericIssue, String projectID)
            throws IssueTrackerException {
        Issue issue;
        try {
            issue = manager.createIssue(projectID, getNewRedmineIssue(genericIssue));
        } catch (RedmineException e) {
            String msg = "Error while  reporting issue for " + projectID;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return String.valueOf(issue.getId());
    }

    private Issue getNewRedmineIssue(GenericIssue genericIssue) throws IssueTrackerException {
        genericIssue.setIssueKey("1");
        Issue redmineIssue = getRedmineIssue(genericIssue);
        redmineIssue.setId(null);
        return redmineIssue;
    }

    private User getUserByLogin(String assignee) throws RedmineException {
        List<User> userList = manager.getUsers();
        for (User user : userList) {
            if (user.getLogin().equals(assignee)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public String updateIssue(GenericIssue genericIssue, String projectID)
            throws IssueTrackerException {
        Issue issue = getRedmineIssue(genericIssue);
        try {
            manager.update(issue);
        } catch (RedmineException e) {
            String msg = "Error while  updating issue " + genericIssue.getIssueKey() + " for " + projectID;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return String.valueOf(issue.getId());
    }

    @Override
    public List<GenericIssue> getAllIssuesOfProject(String project) throws IssueTrackerException {
        List<GenericIssue> issues;
        try {
            issues = getGenericIssues(manager.getIssues(project.toLowerCase(), null));
        } catch (RedmineException e) {
            String msg = "Error while getting all issues of " + project;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return issues;
    }
    
    
    
    
    

    /**
     *
     * @param pParameters
     * @return
     * @throws IssueTrackerException
     */
    @Override
    public List<GenericIssue> getAllIssuesWithParameters(Map<String, String> pParameters) throws IssueTrackerException {
        List<GenericIssue> issues;
        try {
            issues = getGenericIssues(manager.getIssues(pParameters));
        } catch (RedmineException e) {
            String msg = "Error while getting all issues with parametrs";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return issues;
    }
    
    

    private List<GenericIssue> getGenericIssues(List<Issue> issues) throws RedmineException {
        List<GenericIssue> issueList = new ArrayList<GenericIssue>();
        List<User> users = manager.getUsers();
        List<IssueStatus> statuses = manager.getStatuses();
        for (Issue redmineIssue : issues) {
            issueList.add(getGenericIssue(redmineIssue, users, statuses));
        }
        return issueList;
    }

    private GenericIssue getGenericIssue(Issue redmineIssue, List<User> users,
                                         List<IssueStatus> statuses) {
        GenericIssue issue = new GenericIssue();
        issue.setIssueKey(String.valueOf(redmineIssue.getId()));
        issue.setDescription(redmineIssue.getDescription());

        issue.setReporter(getUserById(redmineIssue.getAuthor().getId(), users));
        issue.setStatus(getIssueStatusFromId(redmineIssue.getStatusId(), statuses));
        issue.setSummary(redmineIssue.getSubject());
        issue.setType(redmineIssue.getTracker().getName());
        issue.setUrl(getConfiguration().getFirstProperty(REDMINE_URL) + "/" + REDMINE_ISSUES + "/" + redmineIssue.getId());
        if(redmineIssue.getTargetVersion()!=null){
        	issue.setTargetVersion(redmineIssue.getTargetVersion().getName());
        }else{
        	issue.setTargetVersion("NA");
        }
        
        //setting optional fields
        User assignee=redmineIssue.getAssignee();
        if(assignee!=null){
        issue.setAssignee(getUserById(assignee.getId(), users));
        }
        return issue;
    }

    private String getUserById(Integer assigneeId, List<User> users) {
        for (User user : users) {
            if (user.getId().equals(assigneeId)) {
                return user.getLogin();
            }
        }
        return null;
    }

    private Issue getRedmineIssue(GenericIssue issue) throws IssueTrackerException {
        Issue redmineIssue = new Issue();
        try {
            redmineIssue.setId(Integer.parseInt(issue.getIssueKey()));
            redmineIssue.setDescription(issue.getDescription());
            redmineIssue.setTracker(getTrackerByName(issue.getType()));
            redmineIssue.setAssignee(getUserByLogin(issue.getAssignee()));
            IssueStatus status = getIssueStatusByName(issue.getStatus());
            redmineIssue.setStatusId(status.getId());
            redmineIssue.setStatusName(status.getName());
            redmineIssue.setSubject(issue.getSummary());
            redmineIssue.setAuthor(getUserByLogin(issue.getReporter()));
        } catch (RedmineException e) {
            String msg = "Error while converting issue from " + issue.getIssueKey() + " to Redmine issue";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return redmineIssue;
    }

    private IssueStatus getIssueStatusByName(String statusName) throws IssueTrackerException {
        try {
            List<IssueStatus> statuses = manager.getStatuses();
            for (IssueStatus status : statuses) {
                if (status.getName().equals(statusName)) {
                    return status;
                }
            }
        } catch (RedmineException e) {
            String msg = "Error while getting Redmine issue status for " + statusName;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return null;
    }

    private String getIssueStatusFromId(Integer statusId, List<IssueStatus> statuses) {
        for (IssueStatus status : statuses) {
            if (status.getId().equals(statusId)) {
                return status.getName();
            }
        }
        return null;
    }

    @Override
    public boolean createProject(Project project) throws IssueTrackerException {
        com.taskadapter.redmineapi.bean.Project redmineProject = new com.taskadapter.redmineapi.bean.Project();
        redmineProject.setName(project.getName());
        redmineProject.setIdentifier(project.getKey());
        redmineProject.setDescription(project.getDescription());
        try {
            manager.createProject(redmineProject);
            Version version = new Version();
            version.setName("trunk");
            createVersionInProject(project,version);
            return true;
        } catch (RedmineException e) {
            String msg = "Error while creating project in Redmine " + project.getName();
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
    }

 

	@Override
	public boolean addUserToProject(GenericUser user, Project project) throws IssueTrackerException {
		boolean result = false;

		// TODO:differentiate invite user and update user role
		Membership membership = new Membership();
		com.taskadapter.redmineapi.bean.Project redmineProject = getProjectByKey(project.getKey());
		List<Role> availableRedmineRoles = null;
		List<Role> presentUserRoles = new LinkedList<Role>();

		try {
			availableRedmineRoles = manager.getRoles();
		} catch (RedmineException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		};
		// even though project is created successfully, project is not
		// retrieved.
		// As a workaround, try to add the project again. This will cause error
		// but when we try to
		// retrieve project again, it succeeds.
		if (redmineProject == null) {
			try {
				this.createProject(project);
			} catch (Exception e) {
				// ignore
			}
			redmineProject = getProjectByKey(project.getKey());
			log.debug("Redmine project retrieved for adding user is " + redmineProject);
		}
		membership.setProject(redmineProject);
		User redmineUser;

		try {
			// check whether the user is available in the redmine(not
			// necessarily in the project)
			redmineUser = getUserByLogin(user.getUsername());

			if (redmineUser == null) {
				// user has not been registered to redmine. need
				// (registration+adding to role)
				UserInfoBean userInfoBean =
				                            ServiceContainer.getApplicationManagementService().
                                                    getUserInfoBean(user.getUsername());
				activateUser(userInfoBean);
				redmineUser = getUserByLogin(user.getUsername());
				membership.setUser(redmineUser);
				membership.setRoles(setRolesToBeApplied(membership, user, presentUserRoles,
				                                        availableRedmineRoles));
				addUserToTheProject(membership, redmineProject);
				result = true;

			} else {
				// user has been registered in the redmine already, check
				// whether the user registered in the project
				// check for each membership
				Membership membershipTemp = isUserInTheProject(project, redmineUser);
				if (membershipTemp != null) {// user is in the project already
					presentUserRoles = membershipTemp.getRoles();// get the
					// existing
					// roles for user
					List<Role> rolesOfMembership = new ArrayList<Role>();
					for (Role roleTemp : presentUserRoles) {
						rolesOfMembership.add(roleTemp);

					}
					String redmineRole = null;
					String[] appFactoryRoles = user.getRoles();
					Map<String, String> roleMap = getRoleMap(getConfiguration());
					String defaultRole = getConfiguration().getFirstProperty(REDMINE_DEFAULT_ROLE);
					for (String appFactoryRole : appFactoryRoles) {

						redmineRole = roleMap.get(appFactoryRole);
						rolesOfMembership.add(setRolesList(defaultRole, availableRedmineRoles,
						                                   redmineRole));

					}

					// membershipTemp.setRoles(setRolesToBeApplied(membership,
					// user, presentUserRoles,availableRedmineRoles));
					membershipTemp.setRoles(rolesOfMembership);
					manager.update(membershipTemp);

				} else {// user is not in the project

					// set user roles for new user
					membership.setUser(redmineUser);
					membership.setRoles(setRolesToBeApplied(membership, user, presentUserRoles,
					                                        availableRedmineRoles));
					return addUserToTheProject(membership, redmineProject);

				}

			}

		} catch (RedmineException e) {
			String msg =
			             "Error while adding a user " + membership.getUser().getFullName() +
			                     " to project " + redmineProject.getName();
			log.error(msg, e);
			if (e.getMessage().equals("User has already been taken\n")) {
				throw new IssueTrackerException("0", e);
			} else {
				throw new IssueTrackerException(msg, e);
			}

		} catch (ApplicationManagementException e) {
			String msg =
			             "Error while getting user information of " +
			                     membership.getUser().getFullName();
			log.error(msg, e);
			throw new IssueTrackerException(msg, e);
		}
		return result;

	}

	private Membership isUserInTheProject(Project project, User redmineUser) {
		Membership result = null;
		try {
			List<Membership> projectMembers = manager.getMemberships(project.getKey());
			for (Membership member : projectMembers) {
				if (member.getUser().getId().equals(redmineUser.getId())) {
					result = member;
					break;
				}

			}

		} catch (RedmineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}
	
	@Override
	/* This method update a given user with given set of roles */
	public boolean updateUserOfProject(GenericUser user, Project project)
	                                                                     throws IssueTrackerException {

		String[] appFactoryRoles = user.getRoles();
		Map<String, String> roleMap = getRoleMap(getConfiguration());
		String defaultRole = getConfiguration().getFirstProperty(REDMINE_DEFAULT_ROLE);
		List<Role> finalRoles = new ArrayList<Role>();
		com.taskadapter.redmineapi.bean.Project redmineProject = getProjectByKey(project.getKey());
		List<Role> availableRedmineRoles = null;
		User redmineUser;

		try {

			redmineUser = getUserByLogin(user.getUsername());
			availableRedmineRoles = manager.getRoles();
			Membership membership = isUserInTheProject(project, redmineUser);
			// membership.setProject(redmineProject);
			// membership.setUser(redmineUser);
			String redmineRole = null;
			if (appFactoryRoles.length > 0) {
				for (String appFactoryRole : appFactoryRoles) {

					redmineRole = roleMap.get(appFactoryRole);
					finalRoles.add(setRolesList(defaultRole, availableRedmineRoles, redmineRole));
				}

				membership.setRoles(finalRoles);
				manager.update(membership);
				return true;
			}

		} catch (RedmineException e1) {
			// TODO: log the error show proper error message
			e1.printStackTrace();
			return false;
		}
		return false;
		
	}

	private List<Role> setRolesToBeApplied(Membership membership, GenericUser user,
	                                       List<Role> presentUserRoles,
	                                       List<Role> availableRedmineRoles)
	                                                                        throws RedmineException,
	                                                                        IssueTrackerException {

		String defaultRole = null;
		String redmineRole = null;
		Map<String, String> roleMap = getRoleMap(getConfiguration());
		String[] appFactoryRoles = user.getRoles();// roles that has been asked
		// to add
		List<Role> roles = new ArrayList<Role>();
		defaultRole = getConfiguration().getFirstProperty(REDMINE_DEFAULT_ROLE);
		for (String appFactoryRole : appFactoryRoles) {
			if (!presentUserRoles.isEmpty()) {
				for (Role role : presentUserRoles) {
					redmineRole = roleMap.get(appFactoryRole);
					if (!(redmineRole.equals(role.getName()))) {
						roles.add(setRolesList(defaultRole, availableRedmineRoles, redmineRole));
					}
				}
			} else {
				redmineRole = roleMap.get(appFactoryRole);
				roles.add(setRolesList(defaultRole, availableRedmineRoles, redmineRole));

			}
		}
		return roles;

	}

	private Role setRolesList(String defaultRole, List<Role> availableRedmineRoles,
	                          String redmineRole) throws IssueTrackerException {
		if (redmineRole != null) {
			return getRedmineRoleByName(redmineRole, availableRedmineRoles);
		} else if (defaultRole != null) {
			return getRedmineRoleByName(defaultRole, availableRedmineRoles);
		} else {
			String msg = "Define proper role mapping or default role";
			log.error(msg);
			throw new IssueTrackerException(msg);
		}

	}

	/*
	 * simply add the user in the membership for given roles to the given
	 * project
	 */

	private boolean addUserToTheProject(Membership membership,
	                                    com.taskadapter.redmineapi.bean.Project redmineProject)
	                                                                                           throws IssueTrackerException {
		boolean result = false;
		membership.setProject(redmineProject);
		try {
			manager.addMembership(membership);
			result = true;

		} catch (RedmineException e) {
			String msg =
			             "Error while adding a user " + membership.getUser().getFullName() +
			                     " to project " + redmineProject.getName();
			log.error(msg, e);
			if (e.getMessage().equals("User has already been taken\n")) {
				throw new IssueTrackerException("0", e);
			} else {
				throw new IssueTrackerException(msg, e);

			}

		}
		return result;
	}
	
	/* this method removes a user from project */
	public boolean removeUserFromProject(GenericUser user, Project project)
	                                                                       throws IssueTrackerException {
		// TODO:check whether user is in the app

		com.taskadapter.redmineapi.bean.Project redmineProject = getProjectByKey(project.getKey());
		User redmineUser;
		try {
			redmineUser = getUserByLogin(user.getUsername());
			Membership membership = isUserInTheProject(project, redmineUser);
			manager.delete(membership);
			return true;

		}

		catch (RedmineException e1) {
			// TODO: log the error show proper error message
			e1.printStackTrace();
			return false;
		}
	}
	
    private Role getRedmineRoleByName(String redmineRole, List<Role> availableRedmineRoles) {
        for (Role role : availableRedmineRoles) {
            if (redmineRole.equals(role.getName())) {
                return role;
            }
        }
        return null;
    }
    

    private Map<String, String> getRoleMap(AppFactoryConfiguration configuration) {
        /*  key:appFactoryRoles role value:Redmine  role*/
        Map<String, String> roleMap = new HashMap<String, String>();
        String roles[] = configuration.getProperties("IssueTrackerConnector.redmine.RoleMap.Role");
        for (String role : roles) {
            roleMap.put(role, configuration.getFirstProperty("IssueTrackerConnector.redmine.RoleMap.Role." + role + ".RedmineRole"));
        }
        return roleMap;
    }

    private boolean activateUser(UserInfoBean user) throws IssueTrackerException {
        int authenticatorId = Integer.parseInt(getConfiguration().getFirstProperty(REDMINE_AUTHENTICATOR_ID));
        return RedmineUserUtil.addUser(user, authenticatorId);
    }

    private com.taskadapter.redmineapi.bean.Project getProjectByKey(String key)
            throws IssueTrackerException {
        try {
            List<com.taskadapter.redmineapi.bean.Project> projects = manager.getProjects();
            for (com.taskadapter.redmineapi.bean.Project project : projects) {
                if (project.getIdentifier().equals(key)) {
                    return project;
                }
            }
        } catch (RedmineException e) {
            String msg = "Error while getting Redmine project " + key;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }

        return null;
    }

    @Override
    public ProjectApplicationMapping getProjectApplicationMapping() {
        return new ProjectApplicationMapping() {
            @Override
            public String getProjectKey(String applicationKey) {
                return applicationKey.toLowerCase();
            }

            @Override
            public String getApplicationKey(String projectKey) {
                return projectKey;
            }
        };
    }

    @Override
    public String[] getIssueStatuses() throws IssueTrackerException {
        List<String> statuses = new ArrayList<String>();
        try {
            for (IssueStatus status : manager.getStatuses()) {
                statuses.add(status.getName());
            }
        } catch (RedmineException e) {
            String msg = "Error while getting all Redmine issue statuses";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return statuses.toArray(new String[statuses.size()]);
    }

    @Override
    public GenericIssueType[] getIssueTypes() throws IssueTrackerException {
        List<GenericIssueType> types = new ArrayList<GenericIssueType>();
        GenericIssueType type;
        try {
            for (Tracker tracker : manager.getTrackers()) {
                type = new GenericIssueType();
                type.setIssueType(tracker.getName());
                types.add(type);
            }

        } catch (RedmineException e) {
            String msg = "Error while getting all Redmine issue types";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return types.toArray(new GenericIssueType[types.size()]);
    }

    public Tracker getTrackerByName(String name) throws IssueTrackerException {

        try {
            for (Tracker tracker : manager.getTrackers()) {
                if (tracker.getName().equals(name)) {
                    return tracker;
                }
            }
        } catch (RedmineException e) {
            String msg = "Error while getting all Redmine Tracker for  issue statuses " + name;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return null;
    }

    @Override
    public GenericIssue getIssueByKey(String key, String projectID) throws IssueTrackerException {
        Integer id;
        Issue redmineIssue;
        List<User> users;
        List<IssueStatus> statuses;
        try {
            id = Integer.parseInt(key);
        } catch (NumberFormatException ex) {
            String msg = "Invalid key is provided " + key;
            log.error(msg, ex);
            throw new IssueTrackerException(msg, ex);
        }
        try {
            redmineIssue = manager.getIssueById(id, RedmineManager.INCLUDE.changesets);
            users = manager.getUsers();
            statuses = manager.getStatuses();
        } catch (RedmineException e) {
            String msg = "Error while getting issue details of " + key + " for " + projectID;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return getGenericIssue(redmineIssue, users, statuses);
    }

    @Override
    public String[] getAvailableAssignees(String projectID) throws IssueTrackerException {
        List<String> users = new ArrayList<String>();
        List<User> redmineUsers;
        try {
            List<Membership> membershipList = manager.getMemberships(projectID);
            redmineUsers = manager.getUsers();
            for (Membership membership : membershipList) {
                users.add(getUserById(membership.getUser().getId(), redmineUsers));
            }
        } catch (RedmineException e) {
            String msg = "Error while getting all available Redmine issue assignees";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return users.toArray(new String[users.size()]);
    }

    @Override
    public void createVersionInProject(Project project, Version version)
            throws IssueTrackerException {
        com.taskadapter.redmineapi.bean.Version redmineVersion = new com.taskadapter.redmineapi.bean.Version();
        redmineVersion.setName(version.getName());
        try {
            redmineVersion.setProject(getProjectByKey(project.getKey()));
            manager.createVersion(redmineVersion);
        } catch (IssueTrackerException e) {
            String msg = "Error while getting Redmine project by name";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        } catch (RedmineException e) {
            String msg = "Error while creating a Redmine project version for " + project.getKey();
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }

    }

    @Override
    public String getUrlForReportIssue(String project) throws IssueTrackerException {
        return (getConfiguration().getFirstProperty(REDMINE_URL) + "/projects/" + project);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        manager.shutdown();
    }
    
    public UserIssues[] getAssignerIssueCount() throws IssueTrackerException {
    	try {
			List<User> userList = manager.getUsers();
			ArrayList<UserIssues> userIssuesList = new ArrayList<UserIssues>();
			
			for (User user : userList) {
				Map<String, String> parameterMap = new HashMap<String, String>();
				parameterMap.put("assigned_to_id", user.getId().toString());
				List<GenericIssue> allIssues = getAllIssuesWithParameters(parameterMap);
				
				if(allIssues.size() > 0) {
					UserIssues userIssues = new UserIssues();
					userIssues.setUserName(user.getFullName());
					userIssues.setIssueCount(new Integer(allIssues.size()).toString());
					
					userIssuesList.add(userIssues);
				}
			}
			
			return userIssuesList.toArray(new UserIssues[userIssuesList.size()]);
		} catch (RedmineException e) {
            String msg = "Error while retrieving redmine users";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
		}
    }
    
    public UserIssues[] getReporterIssueCount() throws IssueTrackerException {
    	try {
			List<User> userList = manager.getUsers();
			ArrayList<UserIssues> userIssuesList = new ArrayList<UserIssues>();
			
			for (User user : userList) {
				Map<String, String> parameterMap = new HashMap<String, String>();
				parameterMap.put("author_id", user.getId().toString());
				List<GenericIssue> allIssues = getAllIssuesWithParameters(parameterMap);
				
				if(allIssues.size() > 0) {
					UserIssues userIssues = new UserIssues();
					userIssues.setUserName(user.getFullName());
					userIssues.setIssueCount(new Integer(allIssues.size()).toString());
					
					userIssuesList.add(userIssues);
				}
			}
			
			return userIssuesList.toArray(new UserIssues[userIssuesList.size()]);
		} catch (RedmineException e) {
            String msg = "Error while retrieving redmine users";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
		}
    }
   
    /**
     * 
     */
	@Override
	public List<IssueSummary> getIssuesSummary(String projectID)
			throws IssueTrackerException {
		List<GenericIssue> allIssuesOfProject = getAllIssuesOfProject(projectID);
		List<IssueSummary> issueSummaries = new ArrayList<IssueSummary>();
		for (GenericIssue genericIssue : allIssuesOfProject) {
			IssueSummary issueSummary = loadSummaryExist(issueSummaries,
					projectID, genericIssue.getTargetVersion());
			issueSummary.increaseCount(genericIssue.getType(),
					genericIssue.getStatus());

		}
		return issueSummaries;
	}

	private IssueSummary loadSummaryExist(List<IssueSummary> issueSummaries,
			String appKey, String version) {
		IssueSummary issueSummary = new IssueSummary();
		issueSummary.setAppKey(appKey);
		issueSummary.setVersion(version);
		int index = issueSummaries.indexOf(issueSummary);
		if (index != -1) {
			return issueSummaries.get(index);
		}
		issueSummaries.add(issueSummary);
		return issueSummary;
	}
}
