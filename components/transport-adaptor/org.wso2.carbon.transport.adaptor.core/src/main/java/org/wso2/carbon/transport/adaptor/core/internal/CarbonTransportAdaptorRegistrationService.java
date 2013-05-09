package org.wso2.carbon.transport.adaptor.core.internal;/*
 * Copyright 2004,2005 The Apache Software Foundation.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.transport.adaptor.core.TransportAdaptorRegistrationService;
import org.wso2.carbon.transport.adaptor.core.TransportFactory;
import org.wso2.carbon.transport.adaptor.core.TransportAdaptorService;
import org.wso2.carbon.transport.adaptor.core.exception.TransportConfigException;
import org.wso2.carbon.transport.adaptor.core.internal.ds.TransportServiceValueHolder;

public class CarbonTransportAdaptorRegistrationService implements TransportAdaptorRegistrationService {

    private static final Log log = LogFactory.getLog(TransportAdaptorService.class);

    @Override
    public void registerTransportAdaptor(String className) throws TransportConfigException {

        try {
            Class transportTypeFactoryClass = Class.forName(className);
            TransportFactory factory =
                    (TransportFactory) transportTypeFactoryClass.newInstance();
            ((CarbonTransportAdaptorService) (TransportServiceValueHolder.getCarbonTransportAdaptorService())).registerTransportAdaptor(factory.getTransport());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new TransportConfigException("TransportAdaptor class " + className + " can not be found", e);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
            throw new TransportConfigException("Can not access the class " + className, e);
        } catch (InstantiationException e) {
            log.error(e.getMessage(), e);
            throw new TransportConfigException("Can not instantiate the class " + className, e);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throw new TransportConfigException("Can not process class " + className, e);
        }


    }

    @Override
    public void unRegisterTransportAdaptor(String className) {
        // No unregister
    }
}
