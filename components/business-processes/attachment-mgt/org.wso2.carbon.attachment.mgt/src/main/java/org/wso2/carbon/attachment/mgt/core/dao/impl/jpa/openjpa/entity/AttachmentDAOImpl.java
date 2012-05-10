/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.attachment.mgt.core.dao.impl.jpa.openjpa.entity;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.attachment.mgt.core.dao.AttachmentDAO;
import org.wso2.carbon.attachment.mgt.core.exceptions.AttachmentMgtException;

import javax.activation.DataHandler;
import javax.persistence.*;
import java.io.IOException;
import java.sql.Timestamp;

/**
 * OpenJPA based DAO impl for the Attachment
 */
@Entity
@Table(name = "ATTACHMENT")
public class AttachmentDAOImpl implements AttachmentDAO {
    /**
     * Class Logger
     */
    private static Log log = LogFactory.getLog(AttachmentDAOImpl.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "CREATED_TIME", nullable = false, columnDefinition = "Timestamp NOT NULL WITH DEFAULT",
            insertable = false, updatable = false)
    private Timestamp createdTime;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "CREATED_BY", nullable = false)
    private String createdBy;

    @Column(name = "CONTENT_TYPE", nullable = false)
    private String contentType;

    @Column(name = "URL", nullable = false)
    private String url;

    @Lob
    @Column(name = "CONTENT")
    //private InputStream content;    //TODO: Here didn't use a byte[], Check whether it's OK
    private byte[] content;    //TODO: Here didn't use a byte[], Check whether it's OK


    @Override
    public Long getID() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Timestamp getCreatedTime() {
        return createdTime;
    }

    @Override
    public void setCreatedTime(Timestamp createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public DataHandler getContent() {
        //DataHandler data= new DataHandler(this.content, this.contentType);
        //FileDataSource dataSource = new FileDataSource(new File("/home/denis/Desktop/camera-specs.txt"));
        //DataHandler data = new DataHandler(dataSource);

        ByteArrayDataSource rawData= new ByteArrayDataSource(this.content);
        return new DataHandler(rawData);
    }

    @Override
    public void setContent(DataHandler content) throws AttachmentMgtException {
        try {
            //Here we are giving priority to setContentType() and setName(). If those values are already assigned,
            // then name and contentType in the content DataHandler will be ignored.
            if (this.name == null) {
                setName(content.getName());
            }
            if (this.contentType == null) {
                setContentType(content.getContentType());
            }

            this.content = IOUtils.toByteArray(content.getInputStream());
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new AttachmentMgtException(e.getLocalizedMessage(), e);
        }
    }
}
